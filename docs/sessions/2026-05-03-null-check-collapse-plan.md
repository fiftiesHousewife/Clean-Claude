# Null-check-then-return collapse recipe — backlog

## Why

A specific bookkeeping shape clutters method bodies:

```java
if (foo == null) {
    return false;
}
return bah;
```

shrinks cleanly to:

```java
return foo != null && bah;
```

Same semantics, half the lines, the intent ("we need foo, otherwise no") reads off the single return. Distinct from the broader G29 invert-negative case because the literal-`false` short-circuit lets us collapse to `&&`.

## Scope

### Detection — `recipes/NullCheckThenFalseRecipe`

Within a `J.Block` (typically the tail of a method body), match the exact shape:

- An `if` statement whose condition is `<expr> == null` (or `null == <expr>`).
- The `if`'s then-branch is a single `return <literal-false>;` (or a `Block` wrapping just that).
- The `if` has no `else` branch.
- The next statement is a single `return <expr2>;` (any expression).
- `<expr>` is a stable reference (a simple identifier, field access, or method call without side-effects we can statically detect).

Emit code: `G29` with metadata `{ "shape": "null-check-collapse" }`.

**Skip** when:
- The `if` body has more than the single return (logging, etc.).
- The return value in the `if` branch is anything other than the boolean literal `false`.
- The trailing return is also a literal — collapsing to `foo != null && true` is identical to `foo != null`, which the recipe should produce, but flag separately so reviewers see the intent change.
- `<expr>` is an assignment expression or method call with side effects (we'd evaluate it twice in the rewrite).

The mirror shape (`if (foo != null) return bah; return false;` collapsing to `return foo != null && bah;`) is also valid and the same recipe should match it via a normalised view.

### Refactoring — `refactoring/CollapseNullCheckThenFalseRecipe`

For a detected match, rewrite the two-statement tail into a single `return <expr> != null && <expr2>;` (or `<expr> == null || <expr2>` for the `!= null then return true` cousin shape).

Whitespace policy:
- Preserve the leading whitespace of the original `if` statement so the rewrite keeps its indent.
- Drop the trailing newline of the deleted return so we don't leave a dangling blank line.

## Acceptance

- `NullCheckThenFalseRecipeTest` — fixtures:
  - happy `if (foo == null) return false; return bah;` (flag + rewrite)
  - mirror `if (foo != null) return bah; return false;` (flag + rewrite)
  - skip: `if (foo == null) { log.warn(...); return false; } return bah;`
  - skip: `if (foo == null) return null;` (return type isn't boolean / not literal-false)
  - skip: side-effecting receiver `if (cache.compute() == null) return false; return cache.compute();` (would evaluate twice)
- Round-trip: applying the recipe to its own output is a no-op.
- Sweep harness, confirm zero false rewrites.

## Backlog status

- [ ] `NullCheckThenFalseRecipe` (detection)
- [ ] `OpenRewriteFindingSource` mapping
- [ ] `CollapseNullCheckThenFalseRecipe` (transform)
- [ ] `RefactoringRegistry` G29 entry adds the new transform
