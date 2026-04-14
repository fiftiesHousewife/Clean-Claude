# Skill: Conditionals and Expressions

## When to use this skill

- When fixing a G23, G28, G29, G33, or G19 finding identified by the plugin
- When writing any new code that contains conditional logic, boolean
  expressions, or boundary calculations
- When refactoring a method that has grown complex due to inline conditions

This skill does not apply to test classes. Assertions and test setup
conditionals are not subject to these rules.

> **Note on examples:** All class names and method names in code examples
> are illustrative only — `DiscountCalculator`, `ShippingStrategy`,
> `OrderValidator` etc. do not exist in the codebase. Apply the pattern
> rules below to find or create the correct abstraction for your context.

---

## Before you write or fix anything

**If fixing existing code:**
- Identify the finding: determine which pattern (G23, G28, G29, G33, G19)
  the finding maps to using the pattern table below
- Search callers: if extracting a method or introducing a new type, search
  for all callers of the containing method; if the change would alter a
  public API signature, stop and flag for human review
- Check test coverage: if the conditional being refactored is covered by
  existing tests, verify the tests still pass after the change — do not
  delete or weaken assertions

**If writing new code:**
- Choose the pattern before writing the implementation — do not write
  inline conditionals intending to extract later
- Do not use placeholders: do not write a complex condition intending to
  refactor it in a follow-up — apply the correct pattern before committing

**How to flag for human review:** add a TODO comment at the site and stop:
```java
// TODO: G23 — requires human review: [reason]
```

---

## Choose a pattern

Every conditional finding must satisfy exactly one of these:

| Pattern | Finding | Use when |
|---|---|---|
| 1 — Replace with Polymorphism | G23 | A switch or if/else chain dispatches on type or kind to select behaviour |
| 2 — Extract Boolean Method | G28 | A compound boolean expression is used inline in an `if`, `while`, or ternary |
| 3 — Invert Negative Conditional | G29 | A condition uses negation (`!`, `not`) and a positive equivalent exists or can be created |
| 4 — Extract Boundary Variable | G33 | An arithmetic boundary expression (`size() - 1`, `length + offset`) is used inline |
| 5 — Extract Explanatory Variable | G19 | A complex inline expression obscures the intent of the code around it |

**If more than one pattern applies:** apply them in order — Pattern 1
takes priority over Pattern 2, and so on. A single site may require
multiple patterns applied sequentially (e.g. extract an explanatory
variable, then extract a boolean method that uses it).

---

## Pattern 1: Replace with Polymorphism (G23)

Replace if/else or switch chains that dispatch on type with polymorphism —
an enum with behaviour, a strategy interface, or a map of functions.

**Tiebreaker:** if the switch has fewer than 5 branches AND no expected
growth (the set of cases is closed and stable), a switch expression is
acceptable. If you are unsure whether the set is closed, flag for review:
```java
// TODO: G23 — requires human review: unsure if case set is closed
```

```java
// Illustrative only
// BEFORE — if/else chain dispatches on type string
public double calculateShipping(final String method, final double weight) {
    if ("STANDARD".equals(method)) {
        return weight * 1.5;
    } else if ("EXPRESS".equals(method)) {
        return weight * 3.0 + 5.0;
    } else if ("OVERNIGHT".equals(method)) {
        return weight * 5.0 + 15.0;
    } else {
        throw new IllegalArgumentException("Unknown method: " + method);
    }
}

// AFTER — enum with behaviour
public enum ShippingMethod {
    STANDARD {
        @Override
        public double cost(final double weight) {
            return weight * 1.5;
        }
    },
    EXPRESS {
        @Override
        public double cost(final double weight) {
            return weight * 3.0 + 5.0;
        }
    },
    OVERNIGHT {
        @Override
        public double cost(final double weight) {
            return weight * 5.0 + 15.0;
        }
    };

    public abstract double cost(double weight);
}
```

```java
// Illustrative only
// AFTER (alternative) — switch expression for a closed, small set
public double calculateShipping(final ShippingMethod method, final double weight) {
    return switch (method) {
        case STANDARD  -> weight * 1.5;
        case EXPRESS   -> weight * 3.0 + 5.0;
        case OVERNIGHT -> weight * 5.0 + 15.0;
    };
}
```

---

## Pattern 2: Extract Boolean Method (G28)

Extract a compound boolean expression into a named method that returns
`boolean`. The method name must describe the business condition, not the
implementation.

The extracted method must be side-effect-free — it must not modify any
state, call any mutating method, or perform I/O.

```java
// Illustrative only
// BEFORE — compound condition inline
if (order.total() > 100 && order.customer().loyaltyTier() >= 2
        && !order.hasAppliedDiscount()) {
    applyDiscount(order);
}

// AFTER — extracted to a named boolean method
if (isEligibleForDiscount(order)) {
    applyDiscount(order);
}

boolean isEligibleForDiscount(final Order order) {
    return order.total() > 100
            && order.customer().loyaltyTier() >= 2
            && !order.hasAppliedDiscount();
}
```

Name the method with an `is`, `has`, or `can` prefix. Do not use `check`
or `validate` — those imply side effects.

---

## Pattern 3: Invert Negative Conditional (G29)

Replace negated conditions with their positive equivalents. Positive
conditions are easier to read and less error-prone.

If the positive method does not exist on the type, create it. Place it
on the type that owns the data, not on the caller.

```java
// Illustrative only
// BEFORE — double negation
if (!order.isNotEmpty()) {
    return emptyPage();
}

// AFTER — positive method
if (order.isEmpty()) {
    return emptyPage();
}
```

```java
// Illustrative only
// BEFORE — negated boolean flag
if (!isDisabled) {
    startProcessing();
}

// AFTER — inverted to positive
if (isEnabled) {
    startProcessing();
}
```

When inverting a field or variable name, search for every usage of the
old name and invert the logic at each site. Do not leave mixed positive
and negative references to the same concept.

---

## Pattern 4: Extract Boundary Variable (G33)

Extract arithmetic boundary expressions into a named `final` local
variable at the point of use. The variable name must describe the
boundary's meaning, not the arithmetic.

```java
// Illustrative only
// BEFORE — boundary expression inline
for (int i = 0; i < items.size() - 1; i++) {
    compare(items.get(i), items.get(i + 1));
}

// AFTER — boundary extracted to named variable
final int lastIndex = items.size() - 1;
for (int i = 0; i < lastIndex; i++) {
    final int nextIndex = i + 1;
    compare(items.get(i), items.get(nextIndex));
}
```

```java
// Illustrative only
// BEFORE — boundary calculation repeated
final String chunk = payload.substring(offset, offset + chunkSize);

// AFTER — boundary extracted
final int chunkEnd = offset + chunkSize;
final String chunk = payload.substring(offset, chunkEnd);
```

---

## Pattern 5: Extract Explanatory Variable (G19)

Extract a complex inline expression into a named `final` local variable
declared immediately before the usage site. The variable name must
describe the meaning of the result, not the computation.

```java
// Illustrative only
// BEFORE — complex expression inline
if (employee.age() >= 65 || (employee.age() >= 55 && employee.yearsOfService() >= 20)) {
    initiateRetirement(employee);
}

// AFTER — explanatory variables
final boolean isStandardRetirementAge = employee.age() >= 65;
final boolean isEarlyRetirementEligible = employee.age() >= 55
        && employee.yearsOfService() >= 20;

if (isStandardRetirementAge || isEarlyRetirementEligible) {
    initiateRetirement(employee);
}
```

```java
// Illustrative only
// BEFORE — opaque transformation
return columns.stream()
        .filter(c -> c.type().equals("DOUBLE") && stats.get(c.name()).distinctCount() > 10)
        .toList();

// AFTER — predicate extracted to explanatory variable
final Predicate<Column> isContinuousMeasure = c ->
        c.type().equals("DOUBLE") && stats.get(c.name()).distinctCount() > 10;

return columns.stream()
        .filter(isContinuousMeasure)
        .toList();
```

If the extracted expression is reused across multiple methods, promote
it to a package-private method (Pattern 2) instead of a local variable.

---

## Do not

- Introduce polymorphism (Pattern 1) for a switch with fewer than 5
  branches over a closed set — use a switch expression instead
- Extract a boolean method that has side effects — the method must be
  pure
- Leave mixed positive and negative references after inverting a
  conditional — update every usage site
- Name a boundary variable after the arithmetic (`sizeMinus1`) — name it
  after the meaning (`lastIndex`)
- Name an explanatory variable after the computation (`ageCheck`) — name
  it after the business concept (`isEarlyRetirementEligible`)
- Extract a variable that is used only once and is already clear in
  context — extraction must improve readability, not add noise
- Fix multiple findings in a single task — one finding per task keeps
  each fix independently reviewable and revertable
- Expand scope beyond the identified location without explicit instruction
- Apply this skill to test classes

---

*Traceability: Clean Code Ch17 (Smells and Heuristics) — G19, G23, G28, G29, G33*
