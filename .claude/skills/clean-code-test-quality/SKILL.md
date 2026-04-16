---
name: clean-code-test-quality
description: Apply when fixing test-quality findings from the Clean Code plugin — T1 (insufficient tests), T3 (don't skip trivial tests), T4 (an ignored test is a question about an ambiguity). Use when the user asks to improve test readability, wrap assertions in assertAll, rename test methods, or replace manual temp files with @TempDir.
---

# Skill: Test Quality

## When to use this skill

- When fixing a [T1](../../../HEURISTICS.md#t1-insufficient-tests) finding
  identified by the plugin
- When writing any new test class or test method
- When reviewing test code for readability and completeness

This skill ONLY applies to test classes. Do not apply these rules to
production code.

> **Note on examples:** All class names in code examples are illustrative
> only. Apply the rules below to find or create the correct names and
> patterns for your context.

---

## Before you act

**If fixing existing test code:**
- Identify the finding: match it to one row in the action table below
- Check that the test still passes after applying the fix
- Do not weaken or remove assertions — only restructure them

**If writing new test code:**
- Choose names, assertion patterns, and temp file handling from the
  start using the rules below
- Do not write tests intending to improve them later

---

## Action table

| Finding | Action |
|---|---|
| Multiple consecutive assertions | Wrap in `assertAll` with lambdas |
| Underscores in test method name | Rename to camelCase behaviour description |
| Test method prefixed with "test" | Remove prefix, describe the behaviour |
| Manual temp file/directory creation | Use JUnit `@TempDir` |
| Test requires reading impl to understand | Restructure as arrange-act-assert with descriptive name |

---

## Wrap assertions in assertAll

When a test method has 3 or more assertions on the same result, wrap
them in `assertAll` so all failures are reported together.

```java
// Illustrative only
// BEFORE — stops at first failure, hides subsequent problems
@Test
void returnsPageWithCorrectFields() {
    final var page = service.fetchPage(filter);
    assertThat(page.totalCount()).isEqualTo(10);
    assertThat(page.pets()).hasSize(10);
    assertThat(page.next()).isNull();
}

// AFTER — all three are reported even if the first fails
@Test
void returnsPageWithCorrectFields() {
    final var page = service.fetchPage(filter);
    assertAll(
            () -> assertThat(page.totalCount()).isEqualTo(10),
            () -> assertThat(page.pets()).hasSize(10),
            () -> assertThat(page.next()).isNull()
    );
}
```

Two assertions on the same result are acceptable without `assertAll`.

---

## Test method naming

Test method names are plain English camelCase sentences describing
the behaviour under test. No underscores. No "test" prefix.

```java
// Illustrative only
// BEFORE
@Test void test_fetchPage_returnsResults() {}
@Test void testFetchPageReturnsResults() {}

// AFTER
@Test void fetchesPageOfPets() {}
@Test void returnsEmptyPageWhenNoPetsMatch() {}
@Test void propagatesErrorWhenUpstreamFails() {}
```

Name the test after WHAT the system does, not what the test does.

---

## Use @TempDir for temporary files

Never create temporary files or directories manually in tests. Use
JUnit's `@TempDir` annotation, which handles cleanup automatically.

```java
// Illustrative only
// BEFORE — manual temp directory management
@Test
void processesFile() throws IOException {
    Path tempDir = Files.createTempDirectory("test");
    Files.writeString(tempDir.resolve("data.csv"), "id,name\n1,Bella\n");
    // ... must remember to clean up
}

// AFTER — JUnit manages lifecycle
@TempDir
Path tempDir;

@Test
void processesFile() throws IOException {
    Files.writeString(tempDir.resolve("data.csv"), "id,name\n1,Bella\n");
    // tempDir cleaned up automatically after test
}
```

---

## Self-documenting tests

A reader must understand what behaviour is being tested without reading
the implementation. Structure tests as: arrange, act, assert.

```java
// Illustrative only
// BEFORE — requires reading implementation to understand intent
@Test
void returnsPage() {
    wireMock.stubFor(post(urlEqualTo("/api")).willReturn(okJson(RESPONSE)));
    var result = client.fetchPage(filter, null).block();
    assertThat(result).isNotNull();
}

// AFTER — intent is clear from names and structure alone
@Test
void returnsPageWithCorrectNodeCountAndCursor() {
    wireMock.stubFor(post(urlEqualTo("/api"))
            .willReturn(okJson(petResponse(10, "cursor_abc"))));

    final var page = client.fetchPage(filter, null).block();

    assertAll(
            () -> assertThat(page.nodes()).hasSize(10),
            () -> assertThat(page.pageInfo().endCursor()).isEqualTo("cursor_abc")
    );
}
```

---

## Do not

- Add comments to test methods — the method name is the documentation
- Use underscores or "test" prefix in test method names
- Create temporary files or directories manually — use `@TempDir`
- Have 3+ consecutive assertions without `assertAll`
- Write a test that requires reading the implementation to understand
- Weaken or remove assertions when restructuring — only reorganise
- Apply this skill to production code
- Fix multiple findings in a single task — one finding per task keeps
  each fix independently reviewable and revertable

---

*Traceability: Clean Code [T1](../../../HEURISTICS.md#t1-insufficient-tests), [T3](../../../HEURISTICS.md#t3-dont-skip-trivial-tests), [T4](../../../HEURISTICS.md#t4-an-ignored-test-is-a-question-about-an-ambiguity)*
