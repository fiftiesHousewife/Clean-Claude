# Skill: Naming

## When to use this skill

- When fixing an [N1](../../HEURISTICS.md#n1-choose-descriptive-names), [N5](../../HEURISTICS.md#n5-use-long-names-for-long-scopes), [N6](../../HEURISTICS.md#n6-avoid-encodings), [N7](../../HEURISTICS.md#n7-names-should-describe-side-effects), [G11](../../HEURISTICS.md#g11-inconsistency), or [G16](../../HEURISTICS.md#g16-obscured-intent) finding identified by the plugin
- When writing any new code that introduces classes, methods, fields, or variables
- When renaming an existing symbol as part of a refactoring task

This skill does not apply to test classes, except for the nested ternary
rule which applies everywhere.

> **Note on examples:** All class names and method names in code examples
> are illustrative only — `ProfileService`, `fetchReport`, `OrderProcessor`
> etc. do not necessarily exist in the codebase. Apply the naming rules
> below to find or create the correct name for your context.

---

## Before you rename anything

**If fixing existing code:**
- Search usages: find every call site, import, and reflection reference
  for the symbol being renamed — rename all of them in the same change
- Check serialisation contracts: if the symbol is a field on a record,
  DTO, or entity, check for `@JsonProperty`, `@Column`, `@SerializedName`,
  or any annotation that binds the field to an external name; if one exists,
  keep the annotation value unchanged and only rename the Java symbol
- Check public API surface: if the symbol appears in a REST endpoint path,
  query parameter, or OpenAPI annotation, stop and flag for human review
- Update Javadoc: if the old name appears in any Javadoc `{@link}` or
  `@param` tag, update those references

**If writing new code:**
- Choose the name using the rules below before writing any implementation
- Do not use placeholder names intending to rename later — choose the
  final name before committing

**How to flag for human review:** add a TODO comment at the site and stop:
```java
// TODO: N1 — requires human review: [reason]
```

---

## Canonical verb table

Use these verbs consistently across the codebase. Do not introduce
synonyms — inconsistency makes the codebase harder to navigate.

| Action | Canonical verb | Do not use |
|---|---|---|
| Return an existing value | `get` | `fetch`, `retrieve`, `obtain`, `find` (unless querying a collection with possible absence — then `find` returning `Optional` is correct) |
| Create a new instance | `create` | `make`, `build` (unless constructing via a Builder pattern — `build()` on a Builder is correct) |
| Remove permanently | `delete` | `remove` (unless operating on a collection — `list.remove()` is correct) |
| Transform to a different type | `toX` | `convertToX`, `asX`, `mapToX` |
| Check a boolean condition | `is` / `has` / `can` | `check`, `verify`, `validate` (unless performing validation that throws) |
| Persist to storage | `save` | `store`, `persist`, `write` |
| Load from storage | `load` | `read`, `pull` |

**Tiebreaker:** if a method in the same class or interface already uses a
non-canonical verb consistently, match the existing verb for that class
and flag the inconsistency for a separate cleanup pass. Do not introduce
a second verb into the same type.

---

## Short name rules

Name length must be proportional to the scope in which the name lives.

| Scope | Acceptable length | Examples |
|---|---|---|
| Lambda parameter (single-line) | 1–2 characters | `e`, `tc`, `p` |
| Loop variable (`for`, `forEach`) | 1–3 characters | `i`, `row`, `col` |
| Local variable (method body) | Full word, no abbreviation | `filter`, `pageInfo`, `outputStream` |
| Field (instance or static) | Full descriptive phrase | `columnConfigIds`, `dashboardStorage` |
| Method | Full verb + noun phrase | `getActiveUsers`, `createReportBundle` |
| Class | Full noun phrase | `DashboardConfigLoader`, `CsvExportService` |

Single-character names are only permitted in lambdas and loop indices.
Everywhere else, use a full descriptive name.

---

## Encoding removal

Strip type-encoding prefixes and suffixes that duplicate information
the type system already provides.

| Encoding | Fix |
|---|---|
| Hungarian prefix (`strName`, `iCount`, `lstItems`) | Remove prefix: `name`, `count`, `items` |
| `I`-prefix on interfaces (`IRepository`, `IService`) | Remove prefix: `Repository`, `Service` |
| `Impl` suffix on implementations (`ServiceImpl`) | Name after what distinguishes it: `JdbcService`, `CachingService`, `DefaultService` |
| Type-in-name (`nameString`, `countInteger`) | Remove type suffix: `name`, `count` |
| `m_` or `_` field prefix (`m_name`, `_count`) | Remove prefix: `name`, `count` |

```java
// Illustrative only
// BEFORE
public interface IReportService { ... }
public class ReportServiceImpl implements IReportService { ... }
String strFilename = "data.csv";
List<String> lstColumns = List.of("id", "name");

// AFTER
public interface ReportService { ... }
public class JdbcReportService implements ReportService { ... }
String filename = "data.csv";
List<String> columns = List.of("id", "name");
```

---

## Side-effect naming

A method named `getX` must not modify state. If it does, rename it to
describe the full behaviour or split it into two methods.

```java
// Illustrative only
// BEFORE — getConnection silently initialises the pool
public Connection getConnection() {
    if (pool == null) {
        pool = createPool();
    }
    return pool.acquire();
}

// AFTER (option A) — rename to describe the side effect
public Connection getOrInitialiseConnection() {
    if (pool == null) {
        pool = createPool();
    }
    return pool.acquire();
}

// AFTER (option B) — split into two methods (preferred)
public void initialisePool() {
    if (pool == null) {
        pool = createPool();
    }
}

public Connection getConnection() {
    return pool.acquire();
}
```

If a `getX` method lazily initialises a cache or memoised value, that
is acceptable — the side effect is invisible to the caller. Document
this with a brief comment explaining the lazy initialisation.

---

## Inconsistency

When the codebase uses one name for a concept, every new occurrence of
that concept must use the same name. Do not introduce synonyms.

```java
// Illustrative only
// BEFORE — three names for the same concept
class UserService    { User fetchUser(String id) { ... } }
class OrderService   { Order getOrder(String id) { ... } }
class ReportService  { Report retrieveReport(String id) { ... } }

// AFTER — one verb for the concept "return an existing value"
class UserService    { User getUser(String id) { ... } }
class OrderService   { Order getOrder(String id) { ... } }
class ReportService  { Report getReport(String id) { ... } }
```

Before introducing a new method name, search the codebase for existing
methods that perform the same action on a different type. Match the
existing verb.

---

## Nested ternary / obscured intent

Never nest ternary expressions. A single ternary is acceptable only when
both branches are simple values or method calls with no further nesting.

```java
// Illustrative only
// BEFORE — nested ternary, intent is obscured
final String label = type == MEASURE ? "Metric"
    : type == TEMPORAL ? "Date"
    : type == IDENTIFIER ? "ID" : "Other";

// AFTER (option A) — extract to named method
final String label = labelForType(type);

String labelForType(final ColumnType type) {
    if (type == MEASURE) {
        return "Metric";
    }
    if (type == TEMPORAL) {
        return "Date";
    }
    if (type == IDENTIFIER) {
        return "ID";
    }
    return "Other";
}

// AFTER (option B) — switch expression (Java 14+)
final String label = switch (type) {
    case MEASURE    -> "Metric";
    case TEMPORAL   -> "Date";
    case IDENTIFIER -> "ID";
    default         -> "Other";
};
```

**Tiebreaker:** if the branches map a type to a value with no logic,
prefer a switch expression. If the branches contain any logic or method
calls, extract to a named method.

---

## Do not

- Rename a field without checking for `@JsonProperty`, `@Column`,
  `@SerializedName`, or equivalent serialisation annotations — the
  external contract may break silently
- Use abbreviations except for universally understood terms: `id`, `url`,
  `csv`, `http`, `sql`, `io`
- Include type information in variable names: `nameString`,
  `userList`, `countInt`
- Prefix interfaces with `I` or suffix implementations with `Impl`
- Name a `getX` method that modifies state without renaming or splitting
- Nest ternary expressions — extract to a method or use if/else
- Introduce a synonym for a verb already used in the same codebase for
  the same action
- Fix multiple naming findings in a single task — one finding per task
  keeps each fix independently reviewable and revertable
- Expand scope beyond the identified location without explicit instruction
- Apply this skill to test classes (except nested ternary, which
  applies everywhere)

---

*Traceability: Clean Code Ch2 (Meaningful Names) — [N1](../../HEURISTICS.md#n1-choose-descriptive-names), [N5](../../HEURISTICS.md#n5-use-long-names-for-long-scopes), [N6](../../HEURISTICS.md#n6-avoid-encodings), [N7](../../HEURISTICS.md#n7-names-should-describe-side-effects); Ch17 (Smells and Heuristics) — [G11](../../HEURISTICS.md#g11-inconsistency), [G16](../../HEURISTICS.md#g16-obscured-intent)*
