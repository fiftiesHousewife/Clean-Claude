---
name: clean-code-dependency-updates
description: Apply when fixing E1 findings from the Clean Code plugin (outdated dependencies reported by the Ben-Manes versions plugin). Use when the user asks to bump library versions, update dependencies, or clear outdated-dependency warnings.
---

# Skill: Dependency Updates

## When to use this skill

- When fixing an [E1](../../../HEURISTICS.md#e1-build-requires-more-than-one-step) finding from the plugin
- When the user asks to update outdated dependencies or bump a library to the latest version

## Rules

1. **One dep per commit.** Easier to revert if a bump breaks something.
2. **Refuse major-version bumps by default.** Major version changes often carry breaking changes. Flag them and ask before proceeding. A bump from `3.28.0 → 4.0.0` is major; `3.28.0 → 3.28.5` is patch.
3. **Single source of truth.** Versions live in `gradle/libs.versions.toml`. Edit the `[versions]` block there — never hardcode in a `build.gradle.kts`.
4. **Verify after each bump.** Run `./gradlew build` (or at minimum `./gradlew :<module>:test` for the affected module) before moving on.
5. **Don't chase the latest across the board.** If a library is stable at a well-tested version and newer releases offer no relevant improvements, leave it. Staleness is preferable to untested churn.

## The fix

For each E1 finding:

1. Parse the message: `Outdated dependency <group>:<artifact> [<current> -> <available>]`.
2. Check `gradle/libs.versions.toml` for the matching entry. Dependencies declare their version either via `version.ref = "<alias>"` (preferred) or inline as `version = "..."`.
3. Update the version:
   - If `version.ref`, update the alias in `[versions]`.
   - If inline, update the inline `version = "..."` string.
4. Run `./gradlew :<module>:test` (or `./gradlew build` for deps used everywhere).
5. If tests fail, inspect the release notes of the new version. Revert and flag the dep as incompatible; do not force-update.
6. Commit. Message format: `chore(deps): bump <artifact> to <version>` — one dep per commit.

## Example

```
Finding: Outdated dependency com.github.javaparser:javaparser-core [3.26.2 -> 3.28.0]
```

1. Open `gradle/libs.versions.toml`.
2. Find `javaparser = "3.26.2"` under `[versions]`.
3. Change to `javaparser = "3.28.0"`.
4. `./gradlew :core:test` (core depends on javaparser).
5. Tests pass → `git add gradle/libs.versions.toml && git commit -m "chore(deps): bump javaparser to 3.28.0"`.

## Do not

- Commit multiple dep bumps in a single commit.
- Bump across a major version boundary without user approval.
- Edit `build.gradle.kts` to hardcode a version.
- Skip running tests between bumps.
- Try to "fix" a test failure by patching the code to match the new library — that almost always hides a real incompatibility.

## When the finding is not resolvable

If the new version has real breaking changes and fixing the callers is out of scope, document the decision with `@SuppressCleanCode({HeuristicCode.E1}, reason="Upstream 4.x requires X refactor, see issue #123", until="2026-07-01")` on a method whose source file is close to the dep usage. (Note: E1 findings have no source anchor, so file-level `@SuppressCleanCode` may not match — in that case use `cleanCode.disabledRecipes = listOf("E1")` scoped to the single run, or leave the finding and track it in an issue.)

---

*Traceability: Clean Code [E1](../../../HEURISTICS.md#e1-build-requires-more-than-one-step) — "Build Requires More Than One Step" (Ch.17 p.287). Detection: `DependencyUpdatesFindingSource` wrapping the Ben-Manes versions plugin.*
