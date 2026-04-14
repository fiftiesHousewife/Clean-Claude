# Skill: Comments and Clutter

## When to use this skill

- When fixing a C3, C5, G9, G10, G12, or G24 finding identified by the plugin
- When reviewing any file for unnecessary noise before committing
- When moving or reorganising declarations within a class

This skill does not apply to test classes, except for G12 (unused imports)
and G24 (formatting). Test classes are exempt from C3, C5, G9, and G10.

> **Note on examples:** All class names in code examples are illustrative
> only. Use the action rules below to determine the correct change for
> your context.

---

## Before you act

**If fixing existing code:**
- Identify the finding: match it to one row in the action table below
- Check safety gates: before deleting anything, check the "When deletion
  is NOT safe" section — annotated methods, serialised fields, and
  package-private methods called from tests all require human review
- One finding per task: do not combine multiple finding fixes in a single
  change unless they are in the same method and the same finding type

**If writing new code:**
- Do not introduce any pattern listed in the action table — write clean
  code from the start
- Every local variable and field declaration must be `final` and placed
  on the line immediately before its first use

---

## Action table

Every finding maps to exactly one action. There are no pattern choices
here — these are deletions and moves, not replacements.

| Finding | Action | Safety gate |
|---|---|---|
| C3 — Redundant comment | Delete the comment. The code must speak for itself. If removing the comment makes the code unclear, rename the variable or method instead of keeping the comment. | None |
| C5 — Commented-out code | Delete entirely. Recover from git history if needed. | None |
| G9 — Dead code | Delete the unused variable, field, or method. | Check annotations before deleting — see safety gates below |
| G10 — Vertical separation | Move the declaration to the line immediately before its first use. Declare it `final`. | None |
| G12 — Clutter | Delete unused imports and empty statements (standalone semicolons, empty blocks). | None |
| G24 — Convention violation | Apply project formatting: always use curly braces on `if`/`else`/`for`/`while`/`do-while`, enforce consistent whitespace, enforce 120-character line width. | None |

---

## C3 — Redundant comment

A comment is redundant if it restates what the code already says. Delete
it. If the code is unclear without the comment, the fix is a better name,
not a better comment.

```java
// Illustrative only
// BEFORE
// fetch all pets from the database
final List<Pet> pets = petRepository.findAll();

// AFTER
final List<Pet> pets = petRepository.findAll();
```

```java
// Illustrative only
// BEFORE
private String name; // the name of the pet

// AFTER
private String name;
```

Comments that explain *why* something non-obvious is done are not
redundant. Do not delete these:

```java
// Correct — explains a non-obvious decision
// Do NOT call .url() here; it replaces the WebClient base URL rather than appending.
return HttpGraphQlClient.builder(webClient).build();
```

---

## C5 — Commented-out code

Delete the entire block. Do not convert it to a `@Disabled` test. Do not
move it to a separate file. Git history preserves everything.

```java
// Illustrative only
// BEFORE
public void regenerate(final String dashboardId) {
    // final var oldConfig = configLoader.load(dashboardId);
    // if (oldConfig != null) {
    //     storage.delete(oldConfig);
    // }
    final var config = configLoader.load(dashboardId);
    storage.persist(config);
}

// AFTER
public void regenerate(final String dashboardId) {
    final var config = configLoader.load(dashboardId);
    storage.persist(config);
}
```

---

## G9 — Dead code

Delete the unused variable, field, or method. Before deleting a method,
check the safety gates below.

```java
// Illustrative only
// BEFORE
public class ReportService {
    private final ObjectMapper mapper;     // never read
    private final ReportRepository repo;

    public ReportService(final ReportRepository repo, final ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }
}

// AFTER
public class ReportService {
    private final ReportRepository repo;

    public ReportService(final ReportRepository repo) {
        this.repo = repo;
    }
}
```

---

## G10 — Vertical separation

Move the declaration to the line immediately before its first use.
Always declare as `final`.

```java
// Illustrative only
// BEFORE
public Mono<PetPage> listPets(final String status) {
    final PetFilter filter = PetFilter.builder().status(status).build();

    log.debug("Fetching pets with filter: {}", filter);

    final int pageSize = 50;

    return petService.fetchPage(filter, pageSize);
}

// AFTER
public Mono<PetPage> listPets(final String status) {
    final PetFilter filter = PetFilter.builder().status(status).build();
    log.debug("Fetching pets with filter: {}", filter);
    final int pageSize = 50;
    return petService.fetchPage(filter, pageSize);
}
```

---

## G12 — Clutter

Delete unused imports, empty statements, and empty blocks.

```java
// Illustrative only
// BEFORE
import java.util.Map;
import java.util.HashMap;    // unused
import java.util.ArrayList;  // unused

public class Config {
    ;                         // empty statement
}

// AFTER
import java.util.Map;

public class Config {
}
```

---

## G24 — Convention violations

Apply project formatting rules. Always use curly braces, even for
single-line bodies.

```java
// Illustrative only
// BEFORE
if (exports.contains("users"))
    tasks.add(csvExportService.exportUsers(users).flux());

// AFTER
if (exports.contains("users")) {
    tasks.add(csvExportService.exportUsers(users).flux());
}
```

---

## When deletion is NOT safe

Before deleting a method or field for G9, check for these annotations.
If any are present, do not delete — flag for human review instead:

| Annotation | Why it may appear unused |
|---|---|
| `@Override` | Called via the parent type or interface |
| `@Bean` | Invoked by the Spring container, not by application code |
| `@EventListener` | Invoked by the Spring event system |
| `@Scheduled` | Invoked by the Spring scheduler |
| `@Autowired` (on a method) | Invoked by the Spring container for injection |
| `@PostConstruct` | Invoked by the container after construction |
| `@PreDestroy` | Invoked by the container before shutdown |
| `@JsonProperty` / `@JsonCreator` | Used by Jackson during (de)serialisation |

Also do not delete:
- **Serialised fields** — fields in classes implementing `Serializable`
  may be read by external systems even if unused in this codebase
- **Package-private methods called from tests** — search the test source
  tree for callers before deleting any package-private method

**How to flag for human review:** add a TODO comment at the site and stop:
```java
// TODO: G9 — requires human review: method is annotated @Bean, may be invoked by container
```

---

## Do not

- Add comments to explain what you deleted or why
- Convert commented-out code to `@Disabled` tests
- Delete `@Override` methods without checking the interface or parent class
- Delete `@Bean`, `@EventListener`, `@Scheduled`, `@PostConstruct`, or
  `@PreDestroy` methods — flag for human review
- Move a declaration further from its first use than it was before
- Reorder method declarations unless required by G10 for a local variable
- Combine multiple finding types in a single task unless they are in the
  same method
- Apply C3, C5, G9, or G10 to test classes
- Expand scope beyond the identified location without explicit instruction

---

*Traceability: Clean Code C3, C5, G9, G10, G12, G24*
