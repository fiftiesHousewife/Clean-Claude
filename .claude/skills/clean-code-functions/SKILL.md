---
name: clean-code-functions
description: Apply when fixing function-design findings from the Clean Code plugin — Ch3.1 (small functions), Ch3.2, Ch3.3, F1 (too many arguments), F2 (output arguments), F3 (flag arguments), G5 (duplication), G30 (do one thing), G34 (one level of abstraction). Use when the user asks to refactor a function, reduce arguments, split a long method, or extract duplication.
---

# Skill: Functions

## When to use this skill

- When fixing a [Ch3.1](../../../HEURISTICS.md#ch31-small-functions), Ch3.2, Ch3.3, [F1](../../../HEURISTICS.md#f1-too-many-arguments), [F2](../../../HEURISTICS.md#f2-output-arguments), [F3](../../../HEURISTICS.md#f3-flag-arguments), [G5](../../../HEURISTICS.md#g5-duplication), [G30](../../../HEURISTICS.md#g30-functions-should-do-one-thing), or [G34](../../../HEURISTICS.md#g34-functions-should-descend-only-one-level-of-abstraction) finding
  identified by the plugin
- When writing any new method, refactoring an existing method, or adding
  parameters to an existing signature
- When extracting shared logic across classes or modules

This skill does not apply to test classes. Test methods have different
size and naming conventions — see the test standards in CLAUDE.md.

> **Note on examples:** All class names and method names in code examples
> are illustrative only — `ReportGenerator`, `OrderService`,
> `ShippingDetails` etc. do not exist in the codebase. Use the naming
> and extraction rules below to find or create the correct names for your
> context.

---

## Function rules

**Size:**
- A method should do one thing. If you need the word "and" to describe
  what it does, it has more than one responsibility and must be split.
- Section comments inside a method body are a smell — each commented
  section should become its own named method.
- Methods should be small enough that they need no comments to explain
  their internal flow.

**Arguments:**
- Prefer zero arguments (niladic), then one (monadic), then two (dyadic).
  Three arguments (triadic) require justification. More than three is
  not permitted without a parameter object.
- Never pass a boolean flag argument — split into two clearly-named
  methods.
- Never use an output argument (a collection or object passed in to be
  mutated). Return the result instead.

**Naming:**
- Name methods after what they return or what they change — not after
  where they are called from.
- Do not prefix with `handle`, `process`, `do`, `perform`.
- A method that returns a value reads as a noun phrase or verb phrase:
  `buildFilter`, `fetchPage`, `activeUsers`.
- A method that performs a side effect reads as a command:
  `persistBundle`, `sendNotification`, `deleteExpired`.

---

## Before you write or fix anything

**If fixing existing code:**
- Count lines: if the method exceeds 20 lines, it is a candidate for
  Extract Method — look for section comments, blank-line separations,
  or nested conditionals as split points
- Count parameters: if the method has more than three parameters, look
  for data clumps — groups of parameters that travel together across
  multiple method signatures
- Check for flag arguments: any `boolean` parameter that controls
  branching inside the method must be split
- Check for output arguments: any collection or mutable object passed
  as a parameter and modified inside the method must be replaced with
  a return value
- Search for duplication: search for the method body or key expressions
  across the codebase; if the same logic exists in two or more places,
  extract it

**If writing new code:**
- Design the signature first: decide the return type, then the minimal
  set of parameters needed to produce that return value
- If you need more than three parameters, introduce a parameter object
  before writing the body
- If the method needs a boolean to vary behaviour, write two methods
  from the start
- Do not write a long method intending to extract later — write small
  methods from the start

---

## Choose a pattern

Every function-level finding maps to exactly one of these:

| Pattern | Use when |
|---|---|
| 1 — Extract Method | Long method, section comments, mixed abstraction levels |
| 2 — Parameter Object | Too many arguments or data clump |
| 3 — Split Flag Argument | Boolean parameter controls branching |
| 4 — Eliminate Output Argument | Mutable collection/object passed in and modified |
| 5 — Extract Duplication | Same logic in 2+ places |

**If more than one pattern applies:** Extract Method (Pattern 1) first,
then apply the remaining patterns to the extracted methods. Reducing
method size often eliminates the other problems.

---

## Pattern 1: Extract Method

Split a long method or a method with section comments into small,
named methods. Each extracted method does one thing.

```java
// Illustrative only — class names are theoretical
// BEFORE — section comments signal mixed abstraction
void generateReport(final Report report) {
    // validate inputs
    Objects.requireNonNull(report.title(), "title");
    Objects.requireNonNull(report.dataSource(), "dataSource");

    // fetch data
    final List<Row> rows = dataSource.fetch(report.query());

    // format output
    final byte[] csv = csvFormatter.format(rows, report.columns());

    // persist
    storage.persist(report.title(), csv);
}

// AFTER — each section is a named method
void generateReport(final Report report) {
    validateReport(report);
    final List<Row> rows = fetchData(report);
    final byte[] csv = formatOutput(rows, report.columns());
    storage.persist(report.title(), csv);
}

void validateReport(final Report report) {
    Objects.requireNonNull(report.title(), "title");
    Objects.requireNonNull(report.dataSource(), "dataSource");
}

List<Row> fetchData(final Report report) {
    return dataSource.fetch(report.query());
}

byte[] formatOutput(final List<Row> rows, final List<Column> columns) {
    return csvFormatter.format(rows, columns);
}
```

**Naming rules for extracted methods:**
- Name after WHAT the method does, not WHERE it is called from:
  `validateReport` not `generateReportStep1`
- Name after the return value when the method produces one:
  `fetchData` not `doFetch`
- If you cannot name the method without using "and", it still does
  more than one thing — split further

---

## Pattern 2: Parameter Object

Replace a long parameter list or a recurring group of parameters with
a single record.

```java
// Illustrative only — class names are theoretical
// BEFORE — too many arguments, data clump
Mono<Page<Order>> searchOrders(
        final String customerId,
        final String status,
        final LocalDate fromDate,
        final LocalDate toDate,
        final String sortField,
        final String sortDirection,
        final String cursor) {
    // ...
}

// AFTER — parameter object groups the clump
record OrderSearchCriteria(
        String customerId,
        String status,
        LocalDate fromDate,
        LocalDate toDate,
        String sortField,
        String sortDirection) {}

Mono<Page<Order>> searchOrders(
        final OrderSearchCriteria criteria,
        final String cursor) {
    // ...
}
```

**Parameter object rules:**
- Use a Java `record` — never a mutable class
- Name after the concept the parameters represent, not after the
  method they serve: `OrderSearchCriteria` not `SearchOrdersParams`
- Include a static `builder()` method when three or more fields exist
- Cursor/pagination arguments stay separate from domain criteria —
  they are a different concern
- If the same group of parameters appears in two or more method
  signatures, the parameter object is mandatory

---

## Pattern 3: Split Flag Argument

Replace a boolean parameter with two clearly-named methods.

```java
// Illustrative only — class names are theoretical
// BEFORE — flag argument
List<User> findUsers(final String department, final boolean includeInactive) {
    if (includeInactive) {
        return repository.findByDepartment(department);
    }
    return repository.findActiveByDepartment(department);
}

// AFTER — two methods, no flag
List<User> findActiveUsers(final String department) {
    return repository.findActiveByDepartment(department);
}

List<User> findAllUsers(final String department) {
    return repository.findByDepartment(department);
}
```

If the two branches share significant setup logic, extract the shared
part into a private method that both call — do not reintroduce the
flag internally.

---

## Pattern 4: Eliminate Output Argument

Replace a mutated parameter with a return value.

```java
// Illustrative only — class names are theoretical
// BEFORE — output argument
void collectActiveIds(final List<User> users, final List<String> result) {
    users.stream()
        .filter(User::isActive)
        .map(User::id)
        .forEach(result::add);
}

// AFTER — return the result
List<String> collectActiveIds(final List<User> users) {
    return users.stream()
        .filter(User::isActive)
        .map(User::id)
        .toList();
}
```

If the caller needs to aggregate results from multiple calls, the
caller builds the collection — each method returns its own result
and the caller combines them.

---

## Pattern 5: Extract Duplication

Same logic in two or more places must be extracted. Where to
put the extracted method depends on scope:

| Scope | Placement |
|---|---|
| Same class | Package-private method in that class |
| Same package, different classes | New class named after what it does |
| Different packages | Flag for human review — cross-package extraction may affect module boundaries |

```java
// Illustrative only — class names are theoretical
// BEFORE — duplicated in OrderController and InvoiceController
final String filename = title.replaceAll("[^a-zA-Z0-9]", "_") + ".csv";
return ResponseEntity.ok()
    .header(HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename=\"" + filename + "\"")
    .contentType(MediaType.parseMediaType("text/csv"))
    .body(bytes);

// AFTER — extracted to CsvResponse (same package or shared module)
public final class CsvResponse {
    public static ResponseEntity<byte[]> of(
            final byte[] bytes, final String filename) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(bytes);
    }
    private CsvResponse() {}
}
```

**How to flag cross-package duplication for human review:**
```java
// TODO: G5 — requires human review: duplicated logic across packages
```

---

## Try-catch extraction rule

Extract the body of a try block into its own method. The containing
method becomes the error-handling wrapper.

```java
// Illustrative only — class names are theoretical
// BEFORE — try-catch mixed with logic
void importData(final Path source) {
    try {
        final List<Row> rows = parser.parse(source);
        rows.forEach(validator::validate);
        repository.saveAll(rows);
    } catch (final ParseException e) {
        throw new ImportException(
            "Import failed for: " + source.getFileName(), e);
    }
}

// AFTER — separated concerns
void importData(final Path source) {
    try {
        parseValidateAndSave(source);
    } catch (final ParseException e) {
        throw new ImportException(
            "Import failed for: " + source.getFileName(), e);
    }
}

void parseValidateAndSave(final Path source) {
    final List<Row> rows = parser.parse(source);
    rows.forEach(validator::validate);
    repository.saveAll(rows);
}
```

---

## Do not

- Write a method longer than 20 lines without checking for extraction
  opportunities
- Use section comments as a substitute for extracted methods — if you
  write a comment describing what the next block does, extract it
- Add a parameter to an existing method when the count is already three
  or more — introduce a parameter object first
- Pass a boolean flag to vary method behaviour — split into two methods
- Mutate a collection passed as a parameter — return the result instead
- Name an extracted method after its call site: `step1`, `part2`,
  `helperForX`
- Name a parameter object after the method it serves: `FetchParams`,
  `DoSearchArgs`
- Extract duplication across packages without flagging for human review
- Apply this skill to test classes
- Fix multiple findings in a single task — one finding per task keeps
  each fix independently reviewable and revertable
- Expand scope beyond the identified location without explicit
  instruction

---

*Traceability: Clean Code Ch3 (Functions) — [Ch3.1](../../../HEURISTICS.md#ch31-small-functions), Ch3.2, Ch3.3; Heuristics [F1](../../../HEURISTICS.md#f1-too-many-arguments), [F2](../../../HEURISTICS.md#f2-output-arguments), [F3](../../../HEURISTICS.md#f3-flag-arguments), [G5](../../../HEURISTICS.md#g5-duplication), [G30](../../../HEURISTICS.md#g30-functions-should-do-one-thing), [G34](../../../HEURISTICS.md#g34-functions-should-descend-only-one-level-of-abstraction)*
