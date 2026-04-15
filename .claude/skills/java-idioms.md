# Skill: Java Idioms

## When to use this skill

- When fixing a [J1](../../HEURISTICS.md#j1-avoid-long-import-lists-by-using-wildcards), [J2](../../HEURISTICS.md#j2-dont-inherit-constants), [J3](../../HEURISTICS.md#j3-constants-versus-enums), [G4](../../HEURISTICS.md#g4-overridden-safeties), [G25](../../HEURISTICS.md#g25-replace-magic-numbers-with-named-constants), or [G26](../../HEURISTICS.md#g26-be-precise) finding identified by the plugin
- When writing new code that introduces constants, enums, imports, or
  numeric/type-sensitive values
- When reviewing a class that implements an interface solely to inherit
  its constants

This skill does not apply to test classes, except for J1 (wildcard
imports) and G25 (magic numbers in assertions are acceptable — do not
extract assertion expected values to constants).

> **Note on examples:** All class names in code examples are illustrative
> only. Use the action rules below to determine the correct change for
> your context.

---

## Before you act

**If fixing existing code:**
- Identify the finding: match it to one row in the action table below
- Check scope: each finding targets a single site — do not refactor
  surrounding code unless the finding requires it
- Search callers: for inherited constants and enum conversions, search all references to the constants
  being moved; update every call site in the same change

**If writing new code:**
- Do not introduce any anti-pattern listed in the action table — write
  the correct idiom from the start
- Every constant must have a name derived from its business meaning,
  never from its value or type

---

## Action table

| Finding | Action | Notes |
|---|---|---|
| Wildcard import | Replace each `*` import with explicit imports for every used type | If more than 8 types are imported from a single package, keep the wildcard and flag for human review — the class may have too many dependencies |
| Inherited constants | Remove `implements ConstantsInterface`. Add `import static` for each constant used. | If the interface defines non-constant methods too, only remove the `implements` if the class does not override any of those methods |
| Constants vs enums | Extract related `static final` fields to an enum | See enum extraction rules below |
| Overridden safeties | Depends on the safety being overridden — see Overridden safeties section below | Never silently delete a safety override |
| Magic number | Extract to a named `static final` constant | Name after business meaning, not value |
| Imprecise type | Narrow the type to the most specific correct choice | See type narrowing table below |

---

## Wildcard imports

Replace every wildcard import with explicit imports for the types
actually used in the file.

```java
// Illustrative only
// BEFORE
import java.util.*;
import org.springframework.web.bind.annotation.*;

// AFTER
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
```

---

## Inherited constants

Replace `implements ConstantsInterface` with static imports. The class
must not inherit an interface solely to use its constants.

```java
// Illustrative only
// BEFORE
public class ReportController implements ReportConstants {

    public Report generate() {
        return reportService.generate(DEFAULT_PAGE_SIZE, MAX_COLUMNS);
    }
}

// AFTER
import static com.example.report.ReportConstants.DEFAULT_PAGE_SIZE;
import static com.example.report.ReportConstants.MAX_COLUMNS;

public class ReportController {

    public Report generate() {
        return reportService.generate(DEFAULT_PAGE_SIZE, MAX_COLUMNS);
    }
}
```

---

## Constants vs enums

When two or more `static final` fields share a common prefix or
represent values from the same domain concept, extract them to an enum.

**Enum extraction rules:**
- Name the enum after the shared concept, not the prefix:
  `STATUS_ACTIVE`, `STATUS_INACTIVE` becomes `enum Status { ACTIVE, INACTIVE }`
- If the constants carry values (strings, ints), add a field to the enum
- Move the enum to its own file

```java
// Illustrative only
// BEFORE
public class OrderService {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_CANCELLED = "CANCELLED";
}

// AFTER — enum in its own file
public enum OrderStatus {
    PENDING("PENDING"),
    SHIPPED("SHIPPED"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED");

    private final String value;

    OrderStatus(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
```

---

## Overridden safeties

Each overridden safety has its own fix. Do not silently delete the
override — understand why it exists first.

**Empty catch block:** apply the exception-handling skill. Choose the
correct pattern (wrap and propagate, boundary translation, genuine
recovery, or batch collection). An empty catch is never acceptable.

**`@SuppressWarnings("unchecked")`:** extract the unchecked cast into a
single-purpose method annotated with `@SuppressWarnings("unchecked")`.
The annotation must not appear on a method that does anything else.

```java
// Illustrative only
// BEFORE
@SuppressWarnings("unchecked")
public List<Widget> loadWidgets(final Object rawData) {
    final var widgets = (List<Widget>) rawData;
    widgets.forEach(this::validate);
    return widgets;
}

// AFTER
public List<Widget> loadWidgets(final Object rawData) {
    final var widgets = castToWidgetList(rawData);
    widgets.forEach(this::validate);
    return widgets;
}

@SuppressWarnings("unchecked")
List<Widget> castToWidgetList(final Object rawData) {
    return (List<Widget>) rawData;
}
```

**Other `@SuppressWarnings` values:** flag for human review. Do not
remove the annotation without fixing the underlying warning.

```java
// TODO: G4 — requires human review: @SuppressWarnings("deprecation") on method X
```

---

## Magic numbers

Extract numeric and string literals to named `static final` constants.
Name the constant after its business meaning, not its value.

```java
// Illustrative only
// BEFORE
client.variable("first", 50);
if (retryCount > 3) { ... }

// AFTER
private static final int PAGE_SIZE = 50;
private static final int MAX_RETRIES = 3;

client.variable("first", PAGE_SIZE);
if (retryCount > MAX_RETRIES) { ... }
```

**Acceptable magic numbers — do not extract:**
- `0`, `1`, `-1` when used as loop bounds, index offsets, or identity values
- Assertion expected values in test classes
- Enum ordinals defined in the enum itself

---

## Imprecise types

Narrow types to the most specific correct choice. Using a broad type
when a narrow one exists invites bugs.

| Broad type | Narrow type | When |
|---|---|---|
| `double` / `float` | `BigDecimal` | Money, financial calculations, any value requiring exact decimal precision |
| `String` (date) | `LocalDate` | Calendar dates without time |
| `String` (timestamp) | `Instant` | Points in time, UTC timestamps |
| `String` (duration) | `Duration` | Elapsed time, timeouts |
| `Object` | The actual type | Any situation where the concrete type is known |
| `Map<String, Object>` | A record or typed class | Structured data with known fields |
| `List` (raw) | `List<SpecificType>` | Always parameterise collections |

```java
// Illustrative only
// BEFORE
final double price = 19.99;
final String createdAt = "2024-03-15";
final Map<String, Object> config = loadConfig();

// AFTER
final BigDecimal price = new BigDecimal("19.99");
final LocalDate createdAt = LocalDate.parse("2024-03-15");
final DashboardConfig config = loadConfig();
```

---

## Do not

- Name a constant after its value: `FIFTY = 50`, `THREE = 3`,
  `FIVE_HUNDRED = 500` — name it after what it means
- Create an enum for unrelated constants that happen to share a type —
  enums represent a single domain concept with a closed set of values
- Remove `@SuppressWarnings` without fixing the underlying warning that
  caused it
- Extract assertion expected values in tests to constants — test
  literals are documentation
- Introduce wildcard imports in new code
- Use `implements` on an interface solely to access its constants
- Fix multiple findings in a single task — one finding per task keeps each
  fix independently reviewable and revertable
- Expand scope beyond the identified location without explicit instruction
- Apply this skill to test classes except for wildcard imports and magic numbers (see exemptions
  at top)

---

*Traceability: Clean Code [J1](../../HEURISTICS.md#j1-avoid-long-import-lists-by-using-wildcards), [J2](../../HEURISTICS.md#j2-dont-inherit-constants), [J3](../../HEURISTICS.md#j3-constants-versus-enums), [G4](../../HEURISTICS.md#g4-overridden-safeties), [G25](../../HEURISTICS.md#g25-replace-magic-numbers-with-named-constants), [G26](../../HEURISTICS.md#g26-be-precise)*
