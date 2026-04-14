# Skill: Exception Handling

## When to use this skill

- When fixing a Ch7.1 (catch-log-continue) finding identified by the plugin
- When writing any new code that catches, throws, or propagates exceptions
- When adding a new service method, pipeline stage, or boundary adapter

This skill does not apply to test classes. Use `assertThrows` for expected
exceptions in tests.

> **Note on examples:** All class names in code examples are illustrative
> only — `ProfilerException`, `AnalysisException`, `DataSourceException` etc.
> do not exist in the codebase. Use the exception naming and creation rules
> below to find or create the correct type for your context. Where examples
> use real third-party exceptions these are identified inline.

---

## Exception rules

**Creating exceptions:**
- Create a domain-specific unchecked exception for each distinct failure
  concern — `AnalysisException`, `DataSourceException`, `RenderException`
  are examples of the right shape
- Extend `RuntimeException`, not `Exception`
- Every domain exception must accept `(String message, Throwable cause)` —
  this constructor is mandatory; without it the exception cannot be chained
- Name exceptions after the domain concern, not the layer:
  `QueryBuildException` not `ServiceLayerException`

**Using exceptions:**
- Do not throw `RuntimeException` directly — always throw a domain-specific
  subclass
- Do not declare checked exceptions on service interfaces
- Do not catch `Exception` or `Throwable` — flag for human review if you
  encounter this; it is never correct in internal or boundary layers

**If no suitable exception exists for your context:** create one following
the rules above, or flag for human review if you are uncertain:
```java
// TODO: Ch7.1 — requires human review: no suitable exception type exists
```

---

## Before you write or fix anything

**If fixing existing code:**
- Identify layer: internal (service, pipeline, domain) or boundary
  (controller, scheduler, JDBC adapter, third-party call, entry point)
- Check AutoCloseable: if any resource in the try block implements
  `AutoCloseable`, restructure as try-with-resources before choosing
  a pattern — see the try-with-resources example below
- Search callers: search for all callers of the containing method; if
  propagating would break a caller or affect a public API, stop and
  flag for human review
- Identify exception type: find or create the correct domain exception
  following the exception rules above before writing any throw statement

**If writing new code:**
- Identify layer: same definitions as above
- Check AutoCloseable: if any resource you are about to use implements
  `AutoCloseable`, use try-with-resources from the start
- Identify exception type: find or create the correct domain exception
  following the exception rules above before writing any throw statement
- Do not use placeholders: do not write a catch block intending to
  improve it later — choose and apply the correct pattern before
  committing

**How to flag for human review:** add a TODO comment at the site and stop:
```java
// TODO: Ch7.1 — requires human review: [reason]
```

**Try-with-resources restructuring:**
```java
// Illustrative only — class names are theoretical
// IOException is real (java.io.IOException)
// BEFORE
InputStream is = connection.getInputStream();
try {
    return parse(is);
} catch (IOException e) {
    log.error("Parse failed", e);
    is.close();
}

// AFTER — restructured, then Pattern 2 applied
try (InputStream is = connection.getInputStream()) {
    return parse(is);
} catch (IOException e) {
    throw new DataSourceException(
        "Parse failed for connection: " + connection.id(), e);
}
```

Do not manually close resources in catch or finally blocks.

---

## Choose a pattern

Every catch block must satisfy exactly one of these:

| Pattern | Use when |
|---|---|
| 1 — Wrap and propagate | Internal layer, no fallback, not a loop |
| 2 — Boundary translation | Translating third-party or I/O exceptions to domain exceptions |
| 3 — Genuine recovery | Valid fallback exists, caller receives a successful result |
| 4 — Batch collection | Inside a loop, one failure must not abort the batch |

**If more than one pattern applies:** Pattern 4 takes precedence over
Pattern 2 when inside a loop — use `BatchErrorCollector` and apply
boundary translation inside the collector lambda. Name the batch
exception after the operation being performed:

```java
// Illustrative only — class names are theoretical
// DataAccessException is real (jOOQ)
BatchErrorCollector errors = new BatchErrorCollector();
for (Row row : rows) {
    errors.collect(row.id(), () -> {
        try {
            return dslContext.fetch(buildQuery(row));
        } catch (DataAccessException e) {
            throw new DataSourceException(
                "Query failed for row: " + row.id(), e);
        }
    });
}
errors.throwIfAny(BatchQueryException::new);
```

Logging and continuing satisfies none of these patterns.

---

## Pattern 1: Wrap and propagate

Default for internal layers.

```java
// Illustrative only — class names are theoretical
// BEFORE
try {
    profileEngine.run(query);
} catch (ProfilerException e) {
    log.error("Profiler failed", e);
}

// AFTER
try {
    profileEngine.run(query);
} catch (ProfilerException e) {
    throw new AnalysisException(
        "Profiler failed for query: " + query.id(), e);
}
```

**Message rules:**
- Include the identity of the failing entity: `query.id()`, `report.name()`,
  `userId`
- Chain the original exception as cause — never omit it
- Do not duplicate the exception class name in the message
- Do not include stack trace content in the message string

---

## Pattern 2: Boundary translation

At the boundary between a third-party library or I/O operation and the
domain layer. Prevents internal layers depending on third-party exception
types. Same message and chaining rules as Pattern 1.

```java
// Illustrative only — class names are theoretical
// DataAccessException is real (jOOQ); JsonProcessingException is real (Jackson)
// JDBC boundary
try {
    return dslContext.fetch(query);
} catch (DataAccessException e) {
    throw new DataSourceException(
        "Query failed: " + query.getSQL(), e);
}

// JSON parsing boundary
try {
    return objectMapper.readValue(payload, ReportDefinition.class);
} catch (JsonProcessingException e) {
    throw new ConfigurationException(
        "Invalid report definition payload: " + payload.length() + " bytes", e);
}
```

---

## Pattern 3: Genuine recovery

Only when a valid fallback exists and the caller receives a real result.
Do not use if the fallback returns null, empty, or void.

```java
// Illustrative only — class names are theoretical
try {
    return isoCodeCache.lookup(code);
} catch (CacheException e) {
    log.warn("Cache miss for ISO code {}, falling back to direct lookup", code);
    return isoCodeService.lookup(code);
}
```

The WARN log here is correct — it documents that execution is on a
degraded path, not hiding a failure.

If in doubt, use Pattern 1.

---

## Pattern 4: Batch collection

Inside a loop where one item failing must not abort the batch.
Do not use outside loops. Always call `throwIfAny` after the loop.

Required import: `com.citi.platform.util.BatchErrorCollector`

```java
// Illustrative only — class names are theoretical
// BEFORE
for (Report report : reports) {
    try {
        renderer.render(report);
    } catch (RenderException e) {
        log.error("Render failed for {}", report.name(), e);
    }
}

// AFTER
BatchErrorCollector errors = new BatchErrorCollector();
for (Report report : reports) {
    errors.collect(report.name(), () -> renderer.render(report));
}
errors.throwIfAny(BatchRenderException::new);
```

`throwIfAny` takes a constructor reference of the form
`ExceptionType::new` where `ExceptionType` has a constructor accepting
`(String message, List<BatchError> errors)`. `BatchError` is a real class
included in the `com.citi.platform.util.BatchErrorCollector` import above —
no additional import is required. Verify the target exception has this
constructor before using the method reference.

`BatchRenderException` in the example is illustrative — create or find
a domain exception named after the batch operation being performed, with
the required constructor shape.

---

## Where logging belongs

| Situation | Permitted |
|---|---|
| Pattern 3 (genuine recovery) — any layer | WARN only |
| All other patterns — any layer | No logging in catch block |
| Outermost handler (entry point, scheduler, controller) | ERROR or WARN |

Internal layers wrap and propagate — they do not log. This ensures one
log entry per failure rather than a cascade for the same root cause.

---

## Do not

- Keep a log statement alongside a rethrow — remove it
- Use `throw e` without wrapping — always add context
- Throw `RuntimeException` directly — always throw a domain-specific subclass
- Declare checked exceptions on service interfaces
- Create an exception that does not extend `RuntimeException`
- Create an exception without a `(String message, Throwable cause)` constructor
- Introduce checked exceptions anywhere in the codebase
- Close resources manually when try-with-resources applies
- Catch `Exception` or `Throwable` — flag for human review; this is
  never correct in internal or boundary layers
- Fix multiple Ch7.1 findings in a single task — one finding per task
  keeps each fix independently reviewable and revertable
- Expand scope beyond the identified location without explicit instruction
- Apply this skill to test classes

---

*Traceability: Clean Code Ch7 (Error Handling) — Ch7.1*
