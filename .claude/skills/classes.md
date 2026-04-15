# Skill: Classes

## When to use this skill

- When fixing a [Ch10.1](../../HEURISTICS.md#ch101-classes-should-be-small), [Ch10.2](../../HEURISTICS.md#ch102-the-single-responsibility-principle), [G8](../../HEURISTICS.md#g8-too-much-information), [G14](../../HEURISTICS.md#g14-feature-envy), [G17](../../HEURISTICS.md#g17-misplaced-responsibility), or [G18](../../HEURISTICS.md#g18-inappropriate-static) finding
  identified by the plugin
- When writing a new class, splitting an existing class, or moving
  methods between classes
- When a class exceeds {{classLineCount}} lines or a record exceeds {{recordComponentCount}} fields

This skill does not apply to test classes. Test classes may exceed
normal size limits when they contain many independent test methods.

> **Note on examples:** All class names in code examples are illustrative
> only — `OrderService`, `ShippingDetails`, `ReportRenderer` etc. do not
> exist in the codebase. Use the naming and splitting rules below to find
> or create the correct names for your context.

---

## Class rules

**Size:**
- No class should exceed {{classLineCount}} lines. A class approaching this limit is
  a signal to split it by responsibility.
- Prefer classes of around {{classTargetLines}} lines. A class that grows past roughly
  100 lines is a signal to split it.
- If you need the word "and" to describe what a class does, it has
  more than one responsibility and must be split.

**Records:**
- Records with more than {{recordComponentCount}} fields require a nested static Builder
  class.
- Records are the default choice for value objects, DTOs, parameter
  objects, and configuration bundles.
- Record fields are always `final` by definition — do not add mutable
  state via instance methods that store to external collections.

**Visibility:**
- Expose the minimum possible surface. Default to package-private for
  classes and methods unless they must be public.
- Fields are always `private final` unless the class is a record.
- Do not add public getters speculatively — add them when a caller
  needs them.

**Naming:**
- Name classes after what they represent or produce, not after their
  role: `CsvResponse` not `CsvHelper`, `Pages` not `PaginationManager`.
- No `*Helper`, `*Util`, `*Manager`, `*Processor` suffixes.
- When splitting a class, name each resulting class after what it does,
  not after the original class: `DashboardStorage` and
  `DashboardStartup`, not `DashboardConfigLoaderPart1`.

---

## Before you write or fix anything

**If fixing existing code:**
- Count lines: if the class exceeds {{classLineCount}} lines, it must be split —
  identify responsibilities by looking for field clusters, method
  groups that share the same subset of fields, and section comments
- Check SRP: describe the class in one sentence without using "and" —
  if you cannot, list the responsibilities and plan one class per
  responsibility
- Check for feature envy: if a method makes more calls to another
  class's methods than to its own, it belongs on the other class —
  count external vs internal calls
- Check field visibility: any non-private mutable field is a finding
  — make it `private final` and add an accessor only if callers exist
- Check static methods: a static method that accesses no static state
  and could operate on an instance of a specific type should be an
  instance method on that type

**If writing new code:**
- Design the class with one responsibility from the start — write the
  class-level Javadoc sentence first; if it needs "and", split before
  writing any code
- If the class needs more than 5 fields, reconsider whether it has
  a single responsibility
- If the class is a record with more than {{recordComponentCount}} fields, add a Builder
  from the start
- Do not create static utility methods — create a small class with
  instance methods that can be injected, or use a `private`
  constructor and static factory if truly stateless

---

## Choose a pattern

Every class-level finding maps to exactly one of these:

| Pattern | Use when |
|---|---|
| 1 — Split by Responsibility | Class too large, multiple field clusters, needs "and" to describe |
| 2 — Add Builder | Record has more than {{recordComponentCount}} fields or complex construction |
| 3 — Move Method | Method makes more external calls than internal |
| 4 — Reduce Visibility | Public mutable fields or over-exposed API surface |
| 5 — Convert Static to Instance | Static method that should be an instance method on an injectable class |

**If more than one pattern applies:** Split by Responsibility (Pattern 1)
first, then apply the remaining patterns to the resulting classes.
Splitting often resolves feature envy and visibility problems naturally.

---

## Pattern 1: Split by Responsibility

**How to identify responsibilities:**
- Field clusters: groups of fields that are always used together form
  a responsibility
- Method groups: methods that call the same subset of fields form a
  responsibility
- Section comments in the class body: each commented section is likely
  a separate responsibility
- The "and" test: if describing the class requires "and", each clause
  is a responsibility

```java
// Illustrative only — class names are theoretical
// BEFORE — loads config AND manages cache AND regenerates on startup
class DashboardConfigLoader {
    // fields for disk I/O
    private final Path storageDir;
    private final ObjectMapper objectMapper;

    // fields for in-memory registry
    private final Map<String, DashboardConfig> registry;

    // fields for startup logic
    private final ColumnClassifier classifier;
    private final DataSource dataSource;

    void loadFromDisk() { /* uses storageDir, objectMapper */ }
    DashboardConfig getConfig(String id) { /* uses registry */ }
    void regenerateAll() { /* uses classifier, dataSource */ }
    // ... 200+ lines
}

// AFTER — three classes, each with one job
class DashboardStorage {
    private final Path storageDir;
    private final ObjectMapper objectMapper;

    DashboardBundle loadBundle(final String id) { /* ... */ }
    void persistBundle(final DashboardBundle bundle) { /* ... */ }
}

class DashboardConfigLoader {
    private final Map<String, DashboardConfig> registry;

    DashboardConfig getConfig(final String id) { /* ... */ }
    void register(final String id, final DashboardConfig config) { /* ... */ }
}

class DashboardStartup {
    private final ColumnClassifier classifier;
    private final DataSource dataSource;
    private final DashboardStorage storage;
    private final DashboardConfigLoader configLoader;

    void regenerateAll() { /* ... */ }
}
```

**Splitting rules:**
- Each resulting class must make sense on its own — not a "main" class
  and a "helper"
- Name each class after what it does, not after the original class
- Move fields with their methods — do not leave orphan fields behind
- If a split class has only one public method, consider whether it
  should be a function object (a class whose sole purpose is a single
  operation)

---

## Pattern 2: Add Builder

Records with more than {{recordComponentCount}} fields need a Builder to keep construction
sites readable. Use Lombok `@Builder` — do not write builders by hand.

```java
// Illustrative only — class names are theoretical
// BEFORE — unwieldy construction
record ShippingDetails(
        String recipientName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String phoneNumber) {}

// caller
new ShippingDetails("Alice", "123 Main St", null, "London",
    null, "SW1A 1AA", "GB", "+44 7700 900000");

// AFTER — Lombok builder
@Builder
record ShippingDetails(
        String recipientName,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String phoneNumber) {}

// caller
ShippingDetails.builder()
    .recipientName("Alice")
    .addressLine1("123 Main St")
    .city("London")
    .postalCode("SW1A 1AA")
    .country("GB")
    .phoneNumber("+44 7700 900000")
    .build();
```

**Builder rules:**
- Always use Lombok `@Builder` — never write a builder by hand
- If the project does not have Lombok, add it before applying this pattern
- For classes (not records), use `@Value @Builder` to get immutability
  and a builder in one annotation
- If some fields are mandatory, add `@Builder.Default` for optional
  fields and validate mandatory fields in a compact constructor or
  custom `build()` method

---

## Pattern 3: Move Method

When a method calls more methods on another class than on its own,
move it to the class it is envying.

**Threshold:** if more than 50% of the method's calls are to a single
external class, the method belongs on that class.

```java
// Illustrative only — class names are theoretical
// BEFORE — OrderPrinter envies Order
class OrderPrinter {
    String formatSummary(final Order order) {
        return order.id() + ": "
            + order.customerName() + " - "
            + order.items().size() + " items, "
            + order.totalPrice().formatted();
    }
}

// AFTER — method moved to Order
record Order(String id, String customerName,
        List<Item> items, Money totalPrice) {

    String formatSummary() {
        return id + ": " + customerName + " - "
            + items.size() + " items, "
            + totalPrice.formatted();
    }
}
```

**Move method rules:**
- Count external calls vs internal calls before moving — only move
  when the threshold is met
- If the method uses fields from both classes roughly equally, it may
  need to be split rather than moved — flag for human review
- After moving, check whether the source class still has a reason to
  exist; if not, inline its remaining methods and delete it
- If moving the method would create a circular dependency between
  packages, flag for human review

**How to flag for human review:**
```java
// TODO: G14 — requires human review: [reason]
```

---

## Pattern 4: Reduce Visibility

Make fields `private final` and expose only what callers require.

```java
// Illustrative only — class names are theoretical
// BEFORE — public mutable field
class QueryExecutor {
    public Connection connection;
    public int timeout = 30;

    ResultSet execute(final String sql) { /* ... */ }
}

// AFTER — private final, accessor only where needed
class QueryExecutor {
    private final Connection connection;
    private final int timeout;

    QueryExecutor(final Connection connection, final int timeout) {
        this.connection = connection;
        this.timeout = timeout;
    }

    ResultSet execute(final String sql) { /* ... */ }

    int timeout() { return timeout; }
}
```

**Visibility rules:**
- Fields: always `private final` (records are exempt — fields are
  `final` by definition)
- Methods: package-private by default, `public` only when called from
  outside the package
- Classes: package-private by default, `public` only when referenced
  from outside the package
- Do not add accessors speculatively — add them when a caller exists
- If reducing visibility breaks a caller, the caller may have feature
  envy (Pattern 3) — check before adding a public accessor

---

## Pattern 5: Convert Static to Instance

Replace inappropriate static methods with instance methods on an
injectable class.

**When a static method is inappropriate:**
- It accesses no static state and could operate on an instance
- It would benefit from polymorphism or testing via dependency injection
- It is a non-trivial operation (more than a simple factory or
  constant accessor)

**When static is acceptable:**
- Static factory methods: `of(...)`, `from(...)`, `builder()`
- Constant accessors
- Pure functions with no dependencies: `Math.max`, `Objects.requireNonNull`

```java
// Illustrative only — class names are theoretical
// BEFORE — static method that should be an instance method
class ReportRenderer {
    static String renderHtml(final Report report,
            final TemplateEngine engine) {
        return engine.process("report", report.asContext());
    }
}

// Usage — forced to pass the engine every time
ReportRenderer.renderHtml(report, engine);

// AFTER — instance method on injectable class
class ReportRenderer {
    private final TemplateEngine engine;

    ReportRenderer(final TemplateEngine engine) {
        this.engine = engine;
    }

    String renderHtml(final Report report) {
        return engine.process("report", report.asContext());
    }
}

// Usage — engine is injected once
renderer.renderHtml(report);
```

---

## Do not

- Allow a class to exceed {{classLineCount}} lines — split before it reaches the limit
- Use the word "and" to describe what a class does — split it
- Name a split-off class after the original: `FooHelper`, `FooPart2`,
  `FooExtra`
- Use `*Helper`, `*Util`, `*Manager`, `*Processor` suffixes
- Add public fields to non-record classes
- Add public getters speculatively — add them when a caller exists
- Create static utility methods when an injectable instance class is
  appropriate
- Move a method without counting external vs internal calls first
- Move a method if it would create a circular package dependency —
  flag for human review instead
- Apply this skill to test classes
- Fix multiple findings in a single task — one finding per task keeps
  each fix independently reviewable and revertable
- Expand scope beyond the identified location without explicit
  instruction

---

*Traceability: Clean Code Ch10 (Classes) — [Ch10.1](../../HEURISTICS.md#ch101-classes-should-be-small), [Ch10.2](../../HEURISTICS.md#ch102-the-single-responsibility-principle); Heuristics [G8](../../HEURISTICS.md#g8-too-much-information), [G14](../../HEURISTICS.md#g14-feature-envy), [G17](../../HEURISTICS.md#g17-misplaced-responsibility), [G18](../../HEURISTICS.md#g18-inappropriate-static)*
