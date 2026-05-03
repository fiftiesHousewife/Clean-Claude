# String-template detection recipe — backlog

## Why

Long string concatenations like

```java
final String row = libraryAlias
        + " = { module = \"" + module + "\", version.ref = \"" + versionAlias + "\" }";
```

are noisier than they need to be. JDK 21 string templates (or 25's stable form) make the same intent obvious:

```java
final String row = STR."\{libraryAlias} = { module = \"\{module}\", version.ref = \"\{versionAlias}\" }";
```

There is no existing Checkstyle / SpotBugs / PMD rule for this in our profile, so the candidate finder has to come from us.

## Scope

A new recipe in `recipes/` — call it `StringConcatenationCandidateRecipe` — that emits a finding when:

- A string-typed expression is built from `+` of three or more operands, AND
- At least one operand is a non-literal (a method call, identifier, or arithmetic expression), AND
- At least one operand is a literal containing escaped quotes or whitespace (so we're not just flagging trivial `a + b`).

Code: a new heuristic `G37` (string-template candidate) — or piggyback on `G24` (clarity / formatting) until we have enough volume to justify a dedicated bucket.

Confidence: MEDIUM. String templates are a JDK 21 preview / JDK 25 stable feature; the host project may not have it enabled, in which case the suggestion is informational.

**Skip** when:
- The expression is fully composed of compile-time constants (no template benefit).
- The receiver is a `StringBuilder` / `StringBuffer` (different recipe — already covered by `ReplaceStringBuilderWithTextBlockRecipe`).
- Existing tools (Checkstyle MagicNumber / PMD ConsecutiveLiteralAppends) already flagged the same line; check the source-tag overlap before emitting to avoid duplicate work for the user.

## Optional follow-up: rewrite recipe

Once the detection candidate ships and produces clean diffs, build a paired transform in `refactoring/`:

- `ConvertConcatToStringTemplateRecipe` — rewrites the matched `+` chain into a named-constant `String FORMAT = "...{module}...";` plus `String.format` (or `STR.` if string templates are enabled). Keeping the template as a constant rather than inlining it makes the message reusable, testable, and discoverable in IDE search.
- Each rewrite extracts the literal scaffolding to a `private static final String <name>_FORMAT = ...;` constant adjacent to the call site (or hoists to the class's existing constants block if one exists). Reuse the same constant when the recipe sees the same shape twice in one file.
- Gate by a per-project switch (extension property) since not every host project has previewed string templates yet.

## Acceptance

- `StringConcatenationCandidateRecipeTest` — fixtures for: pure-literal (skip), 2-operand mixed (skip — too small), 3+-operand mixed (flag), `StringBuilder.append` chain (skip — not the same shape).
- Wired through `OpenRewriteFindingSource` to surface as a clean-code finding.
- Document in CLAUDE.md that the recipe shows up in the report; users can suppress per-line if they prefer the explicit form.

## Backlog status

- [ ] `StringConcatenationCandidateRecipe` (detection)
- [ ] `OpenRewriteFindingSource` mapping + heuristic code
- [ ] `ConvertConcatToStringTemplateRecipe` (transform; gated)
- [ ] README mention
