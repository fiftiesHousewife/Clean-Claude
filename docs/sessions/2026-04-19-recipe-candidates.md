# 2026-04-19 — Recipe candidates from the 4-way run

Cross-variant pattern analysis of what the agent actually did on the 10-file
batch. Anything that recurred across ≥3 of the 4 variants is a candidate
because "the LLM does this the same way every time" is the precise shape of
a mechanical transform we should be able to lift into a recipe.

Ordered by estimated leverage (highest-impact first).

## Recipe candidates (new)

### 1. `ExtractMagicNumberConstantRecipe` (G25 — numbers)
**Every variant** extracted named constants for numeric literals:
`MAX_SCORE=10000`, `SESSION_TTL_MS`, `MAX_ATTEMPTS=5`, `INITIAL_DELAY_MS=100`,
`MAX_DELAY_MS=30000`, `BACKOFF_MULTIPLIER=2.0`, `URGENT_REPEATS=3`,
`MAX_DISPLAY_NAME_LENGTH=80`, `RESERVATION_DISCOUNT`. Today's
`ExtractClassConstantRecipe` extracts strings only. Extend to numeric
literals with heuristics for name derivation (preceding field assignment,
preceding comment, nearby identifier).

Notes: name-suggestion is the hard part. Safe fallback: generate
`CONSTANT_<line>` if no heuristic fires, let the agent rename later.

### 2. `ReturnInsteadOfMutateArgRecipe` (F2)
**Every variant** converted `void settleOrders(Map<String,Integer> stock, ...)`
to `Map<String,Integer> settleOrders(...)` returning a new map, and the same
pattern on `void validate(List<String> errors, ...)`. This is one of the two
most common findings we surface and the transform is mechanical when the
argument is a `Collection` or `Map` and the method has no return value.

Scope: private/package-private methods only (public is a contract change).
Signature rewrite + call-site rewrite. This is a ported version of
IntelliJ's "Make Method Return Value" refactor.

### 3. `ExtractInlineHelperFromRepeatedPredicateRecipe`
**Every variant** extracted named boolean helpers:
`isEscapedQuote`, `isContributing`/`isCountableLot`/`isValidLot`/`isBalanceable`,
`isExpired`, `isContributing`. Pattern: a compound boolean expression
appears in a stream `filter` / `if` and would read better as a named
predicate method.

Detection: a boolean expression ≥2 operators used in ≥2 sites or inside a
too-long method. Emit a G19 finding (explanatory variable/helper).

### 4. `CatchLogReturnEmptyRemoval` (Ch7.1)
Multiple variants removed `catch (Exception e) { log.warn(...); return empty; }`
blocks. The transform: drop the catch block and let the exception propagate
(adding `throws` to the caller if needed), OR wrap in try-with-resources if
the catch was paired with cleanup.

Narrow first cut: only remove the catch when the catch body is a single
`log` call and a bare `return;` or `return List.of();`. Still high-value —
several sandbox fixtures had this.

### 5. `LogErrToAuditRecipe` (G4)
**Every variant** removed `System.err.println(...)` / `e.printStackTrace()`
and replaced with either `java.lang.System.Logger` or an append to an
existing `audit` list parameter. On sandbox specifically the audit pattern
is the right fit (no SLF4J on classpath). Project-conventions recipe: if
the enclosing method has a `List<String>` parameter named `audit` or a
field of that type, append; else fall back to `System.Logger`.

### 6. `StringConcatenationToChainedAppendRecipe` already exists as
`ChainConsecutiveBuilderCallsRecipe` — no new recipe, but we saw it fire
in every HARNESS run, so the value is confirmed. The only tail: extend
to `log.append(...).append(...).append(...)` chains where the receiver
isn't named `sb`.

### 7. `ReplaceForWithStreamCountRecipe` (G30 sub-pattern)
Multiple variants converted counting loops (`int count = 0; for (...) if (...) count++;`)
to `stream().filter(...).count()`. Very narrow pattern but very common. We
already have `ReplaceForAddNCopiesRecipe` — this is its sibling.

### 8. `CollectForEachToMapPutRecipe` (G30 sub-pattern)
`for (X x : xs) map.put(x.key(), x.value())` → `xs.stream().collect(Collectors.toMap(...))`.
Recurred in InventoryBalancer-style fixes.

## Recipe improvements (existing)

### A. `MakeMethodStaticRecipe` — improve the G18 **detection**, not the fix
Variants rejected G18 on `parseRow`, `execute`, `userIdFor`, `lookupOrNull`,
`open`, `close`, `activeSessionCount` — **every variant produced the same
rejection text with the same reason**. The recipe correctly refused all of
them; the DETECTION side of G18 is what's noisy. The detector is reading
"doesn't use this.field" too narrowly — it misses writes to enclosing fields
like `this.rowsParsed++` and `this.sessions.put(...)`. Fix the detector
(in `refactoring/` wherever G18 is emitted) and the false-positive rate
drops meaningfully across every future run.

### B. `ShortenFullyQualifiedReferencesRecipe` — handle `Map.Entry`
Every variant replaced `Map.Entry` FQN with `import java.util.Map.Entry; Entry`.
Today's ShortenFQR may or may not cover nested types — if it misses, extend.

### C. `CollapseSiblingGuardsRecipe` — handle sequential guards with same return
Pattern seen in SessionStore and UserAccountService: 3-5 guard clauses
that each `return null;` (or the same constant). Current recipe merges
siblings with identical bodies; extend to "same constant-return body" and
to sequential (non-sibling) guards whose conditions can be `||`-joined.

### D. `DeleteSectionCommentsRecipe` — extend to `// Step N:` prefix
Current only matches `// Phase N:`. OrchestratorFixture had both patterns.

## Patterns the agent introduced (candidates for post-agent recipes)

These are regressions we'd want the new feedback loop OR a post-agent
cleanup recipe to catch:

- **`StringBuilder sb` param threading** — multiple variants re-introduced
  this when extracting helpers (e.g. `private void appendSection(StringBuilder sb, ...)`).
- **`new ArrayList<>()` + mutation** inside methods that could return
  `Stream.toList()` — an agent-flavoured style reversion.
- **Redundant `else` after `return`** — several extracted helpers had
  this shape after collapse-and-extract.

The feedback loop shipped today will catch these if analysis sees them;
`RedundantElseAfterReturnRecipe` would be ~30 lines and is pure OpenRewrite.

## Prioritisation

If we ship two of these before the next run, pick:
1. **`ReturnInsteadOfMutateArgRecipe`** (F2) — highest per-finding value,
   F2 is the #2 remaining code after G30, and the transform is mechanical
   enough to write confidently.
2. **G18 detector fix** — silences ~5 false positives per run, reducing
   wasted agent turns. Not a new recipe; a fix to detection.

The other candidates are smaller per-unit wins but easy enough that a
half-day pass through the list would ship several.
