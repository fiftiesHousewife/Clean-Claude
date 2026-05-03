# Predicate-helper extraction recipe — plan

## Why

The IDE's "early-return" inspection currently flags both useful and useless cases. The Clean Code review session of 2026-05-03 surfaced three categories:

1. **Visitor methods with guard chain + single rewrite call** (e.g. `JulToSlf4jRecipe.visitMethodInvocation`, `PrintStackTraceToLogRecipe.visitMethodInvocation`) — the IDE warning is *correct* and the right rewrite is "extract a `targetXxxFor(method): Optional<T>` predicate helper, then `.map(rewrite).orElse(visited)`" (Style B). The win is that the visitor body becomes a one-line statement of intent and the guards become a coherent predicate.

2. **Predicate-style filter chains** (Style A — wrap-and-unwrap `Optional<Integer>` for every step) — wrong shape; awkward to read because each `.filter(level -> ...)` ignores its parameter.

3. **Int-returning string-position utilities** (e.g. `PrintfToSlf4jFormatConverter.skipArgumentIndex`) — early returns are *the right shape* because every code path returns a valid int. Filter chains here would be strictly worse.

We currently produce one undifferentiated G-code (G29 / negative conditional) signal that conflates all three, and the user has to manually decide which style applies. This recipe makes the right call automatically.

## Scope

A new recipe in the `recipes/` module — call it `PredicateHelperCandidateRecipe` — that **detects** category-1 cases only and emits a finding with confidence MEDIUM (because predicate-helper extraction is judgement-heavy) and a paired recipe in `refactoring/` — call it `ExtractPredicateHelperRecipe` — that **applies** the rewrite.

### Detection (recipes/PredicateHelperCandidateRecipe)

A method matches when *all* of the following hold:

- It is a method declaration whose name starts with `visit` (cheap PoC filter — broaden once the recipe lands).
- Its return type is a non-`void` reference type.
- The body is exactly one local-variable declaration `final T x = super.visitX(...);` followed by:
  - One or more `if (...) return x;` guard clauses (each returns the same name `x`), then
  - A single trailing `return rewriteExpression;` whose top-level expression is *not* `x`.
- Each guard's predicate references the local `x` directly (so the predicate can later be hoisted into a helper that takes the visited node as its argument).

Emit code: `G29` (today's negative-conditional bucket) with metadata `{ "style": "extract-predicate" }` so downstream tooling can distinguish from the simpler invert-negative case.

**Skip** when:
- The method body contains side-effecting statements before any guard (e.g. logging, mutation).
- Any guard returns a value other than the seeded local (e.g. `return visited.withModifiers(...)` — the helper-extraction shape doesn't apply).
- The method has fewer than 2 guard clauses (one guard is the existing G29 invert case).
- The trailing rewrite is not a single expression (a multi-statement tail isn't a clean `.map` candidate).

### Refactoring (refactoring/ExtractPredicateHelperRecipe)

For a method that matches the detection shape, transform:

```
@Override
public T visitX(...) {
    final T visited = super.visitX(...);
    if (cond1(visited)) return visited;
    if (cond2(visited)) return visited;
    if (cond3(visited)) return visited;
    return rewriteCall(visited, ...);
}
```

into:

```
@Override
public T visitX(...) {
    final T visited = super.visitX(...);
    return targetForX(visited)
            .map(target -> rewriteCall(visited, target, ...))
            .orElse(visited);
}

private Optional<U> targetForX(final T visited) {
    if (cond1(visited)) return Optional.empty();
    if (cond2(visited)) return Optional.empty();
    if (cond3(visited)) return Optional.empty();
    return Optional.of(/* the value rewriteCall would have computed */);
}
```

The hard part is identifying `U` (the predicate helper's value type) and the projection logic for `targetForX`'s final `return Optional.of(...)`. Heuristic for v1: when the trailing rewrite is `JavaTemplate.builder("..." + JUL_TO_LOG4J.get(visited.X) + "...")...`, the projection is `JUL_TO_LOG4J.get(visited.X)` returning `Optional<String>`. Make the recipe deliberately conservative and fail to a no-op when it can't infer `U`.

Constructed name: `targetFor` + visited method name (camel-cased), e.g. `targetForMethodInvocation`. If the class already declares a method by that name, skip and emit a finding.

### What this recipe deliberately does NOT do

- It does not touch int-returning utility methods like `skipArgumentIndex`. The detection filter excludes them (return type must be a reference type) and the user's analysis confirms the early-return shape is correct there.
- It does not generate `Optional.filter` chains (Style A). The output is always Style B (extract helper, single `.map().orElse(visited)` at the call site).
- It does not handle multi-arg rewrite expressions where the rewrite needs more than `visited + projection`. Those need agent judgement.

## Acceptance

- `PredicateHelperCandidateRecipeTest` — fixtures for `JulToSlf4j`-shaped, `PrintStackTraceToLog`-shaped, and one negative case (int utility method) with assertions on detected vs. skipped.
- `ExtractPredicateHelperRecipeTest` — golden-output tests showing the before/after on the two real visitors. Round-trip: applying the recipe to the rewritten source must be a no-op.
- Wired into `HarnessRecipePass.deterministicRecipes` only after the test sweep against the plugin's own source produces zero spurious diffs.
- `RefactoringRegistry` gets a new entry: `G29 → [..., ExtractPredicateHelperRecipe]` (alongside `CollapseSiblingGuardsRecipe` and `InvertNegativeConditionalRecipe`).

## Backlog status

- [ ] Build `PredicateHelperCandidateRecipe` (detection)
- [ ] Build `ExtractPredicateHelperRecipe` (transform)
- [ ] Sweep harness, verify zero false-positive rewrites
- [ ] Wire into HarnessRecipePass + RefactoringRegistry

## Notes

- This is a category-3 recipe (judgement-heavy); confidence stays MEDIUM until we have at least 20 confirmed clean rewrites in the wild.
- Detection alone is shippable. Splitting detection (recipes/) and transform (refactoring/) lets the report flag the candidates immediately while transform development continues.
- After this lands, audit the existing G29 mapping — `InvertNegativeConditionalRecipe` should run only on single-guard cases and the new recipe takes over the multi-guard ones.
