# Skill: Null Handling

## When to use this skill

- When fixing a Ch7.2 (Don't Return Null) finding identified by the plugin
- When writing any new method that could return no result
- When writing any method that receives a value that must not be null
- When reviewing code that uses `if (x != null)` as control flow

This skill does not apply to test classes. Null assertions in tests
(`assertThat(x).isNull()`, `assertThat(x).isNotNull()`) are correct
test idioms.

> **Note on examples:** All class names in code examples are illustrative
> only. Use the pattern rules below to determine the correct change for
> your context.

---

## Before you act

**If fixing existing code:**
- Identify the null pattern: match it to one row in the pattern table
  below
- Search callers: if changing a return type from nullable to `Optional`
  or to a null object, search for all callers and update them in the
  same change
- Check API contracts: if the method is part of a public API or
  interface, changing the return type may break consumers — flag for
  human review if uncertain

**If writing new code:**
- Choose the correct pattern from the table before writing the method
  signature
- Never return null from a public method — choose Optional, an empty
  collection, or a null object
- Never use `if (x != null)` as control flow — choose
  `Objects.requireNonNull` at the boundary or narrow the type

**How to flag for human review:** add a TODO comment at the site and stop:
```java
// TODO: Ch7.2 — requires human review: [reason]
```

---

## Choose a pattern

Every null-related code site must satisfy exactly one of these:

| Pattern | Use when |
|---|---|
| 1 — Optional return | Method may legitimately have no result |
| 2 — Objects.requireNonNull | Input must not be null; fail fast at boundary |
| 3 — Null object / empty collection | Caller needs a usable value, absence is not an error |
| 4 — Type narrowing | Null check exists because the type is too broad; narrow it |

**If more than one pattern could apply:** Pattern 2 takes precedence at
API boundaries (constructors, public method parameters). Pattern 1 takes
precedence for return values. Pattern 3 takes precedence when the caller
iterates or delegates without checking.

---

## Pattern 1: Optional return

Use when a method may legitimately have no result. The caller must
handle the empty case explicitly.

```java
// Illustrative only
// BEFORE
public ColumnConfig findColumn(final String name) {
    return columnMap.get(name); // returns null if absent
}

// caller
final ColumnConfig config = registry.findColumn(name);
if (config != null) {
    applyConfig(config);
}

// AFTER
public Optional<ColumnConfig> findColumn(final String name) {
    return Optional.ofNullable(columnMap.get(name));
}

// caller
registry.findColumn(name).ifPresent(this::applyConfig);
```

**Return type rules:**
- Use `Optional<T>` for single-value returns that may be absent
- Never use `Optional` as a field type, method parameter, or collection
  element
- Never call `.get()` without `.isPresent()` — prefer `.orElseThrow()`,
  `.ifPresent()`, or `.map()`

---

## Pattern 2: Objects.requireNonNull

Use at API boundaries — constructors, public method entry points, and
anywhere a null input indicates a programming error.

```java
// Illustrative only
// BEFORE
public ReportService(final ReportRepository repo, final TemplateEngine engine) {
    this.repo = repo;
    this.engine = engine;
}

// AFTER
public ReportService(final ReportRepository repo, final TemplateEngine engine) {
    this.repo = Objects.requireNonNull(repo, "repo");
    this.engine = Objects.requireNonNull(engine, "engine");
}
```

**Message rules:**
- The message is the parameter name — short, lowercase, no sentence
- `Objects.requireNonNull` throws `NullPointerException` with the
  message — this is correct and expected behaviour

---

## Pattern 3: Null object / empty collection

Use when the caller needs a usable value and absence is not an error.
Return an empty collection, an empty string, or a domain-specific null
object instead of null.

```java
// Illustrative only
// BEFORE
public List<Pet> findByStatus(final String status) {
    final var result = petRepository.findByStatus(status);
    if (result == null) {
        return null;
    }
    return result;
}

// AFTER
public List<Pet> findByStatus(final String status) {
    final var result = petRepository.findByStatus(status);
    return result != null ? result : List.of();
}
```

```java
// Illustrative only — null object pattern
// BEFORE
public Dashboard loadDashboard(final String id) {
    return dashboardMap.get(id); // null if not found
}

// AFTER
private static final Dashboard EMPTY_DASHBOARD = new Dashboard("", List.of(), List.of());

public Dashboard loadDashboard(final String id) {
    return dashboardMap.getOrDefault(id, EMPTY_DASHBOARD);
}
```

Prefer `Map.getOrDefault()`, `Map.computeIfAbsent()`, and
`List.of()` / `Map.of()` over null checks.

---

## Pattern 4: Type narrowing

Use when a null check exists because the variable type is too broad.
Narrow the type so the null check becomes unnecessary.

```java
// Illustrative only
// BEFORE
public void processData(final Object input) {
    if (input != null && input instanceof ReportData) {
        final ReportData data = (ReportData) input;
        renderer.render(data);
    }
}

// AFTER
public void processData(final ReportData input) {
    Objects.requireNonNull(input, "input");
    renderer.render(input);
}
```

When the broad type comes from a framework or library (e.g. `Object`
from a deserialiser), apply Pattern 2 after the narrowing cast instead.

---

## Where null checks ARE acceptable

Null is not universally wrong. These uses are correct and must not be
changed:

| Context | Acceptable use |
|---|---|
| `Objects.requireNonNull` at API boundary | Fail fast — Pattern 2 above |
| Jackson `@JsonInclude(NON_NULL)` | Omit null fields from JSON output |
| JPA entity ID before persistence | Entity ID is null before `save()` — this is the JPA contract |
| Spring `@Nullable` on optional parameters | Framework convention for optional injection |
| Reactive `switchIfEmpty` / `defaultIfEmpty` | Reactor's null-safe equivalent of Pattern 3 |

---

## Do not

- Return null from a public method — use Optional, an empty collection,
  or a null object
- Use `if (x != null)` as control flow to branch logic or silently
  skip operations
- Chain `Optional.map().orElse(null)` — this reintroduces the null that
  Optional was meant to eliminate; use `.orElseThrow()`, `.ifPresent()`,
  or `.orElse(meaningfulDefault)` instead
- Use `Optional` as a field type, method parameter, or collection element
- Call `Optional.get()` without a preceding `.isPresent()` check —
  prefer `.orElseThrow()`
- Catch `NullPointerException` — fix the root cause instead of catching
  the symptom
- Use `Optional.of()` when the value may be null — use
  `Optional.ofNullable()`
- Replace `Objects.requireNonNull` with an `if (x == null) throw` — the
  standard library method is clearer and more concise
- Expand scope beyond the identified location without explicit instruction
- Apply this skill to test classes

---

*Traceability: Clean Code Ch7 (Error Handling) — Ch7.2*
