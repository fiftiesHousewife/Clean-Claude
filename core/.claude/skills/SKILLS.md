# Skill Files

Skill files teach Claude Code how to fix specific types of findings from the
Clean Code plugin. They are read once before fixing a finding, and followed
literally.

Developers: these are also a reference for the patterns each concern requires.

## Catalogue

| Concern | File | Codes | When to read |
|---|---|---|---|
| Exception handling | `exception-handling.md` | [Ch7.1](../../HEURISTICS.md#ch71-use-exceptions-rather-than-return-codes) | Catch blocks, propagation, boundary translation |
| Null handling | `null-handling.md` | [Ch7.2](../../HEURISTICS.md#ch72-dont-return-null) | Null checks, Optional, fail-fast patterns |
| Function design | `functions.md` | [F1](../../HEURISTICS.md#f1-too-many-arguments)–[F3](../../HEURISTICS.md#f3-flag-arguments), [G5](../../HEURISTICS.md#g5-duplication), [G30](../../HEURISTICS.md#g30-functions-should-do-one-thing), [G34](../../HEURISTICS.md#g34-functions-should-descend-only-one-level-of-abstraction) | Method length, parameters, flag args, loops |
| Class design | `classes.md` | [G8](../../HEURISTICS.md#g8-too-much-information), [G14](../../HEURISTICS.md#g14-feature-envy), [G17](../../HEURISTICS.md#g17-misplaced-responsibility), [G18](../../HEURISTICS.md#g18-inappropriate-static) | Class size, SRP, records, feature envy |
| Naming | `naming.md` | [N1](../../HEURISTICS.md#n1-choose-descriptive-names), [N5](../../HEURISTICS.md#n5-use-long-names-for-long-scopes)–[N7](../../HEURISTICS.md#n7-names-should-describe-side-effects), [G11](../../HEURISTICS.md#g11-inconsistency), [G16](../../HEURISTICS.md#g16-obscured-intent) | Short names, encoding, side-effects, ternaries |
| Conditionals and expressions | `conditionals-and-expressions.md` | [G19](../../HEURISTICS.md#g19-use-explanatory-variables), [G23](../../HEURISTICS.md#g23-prefer-polymorphism-to-ifelse-or-switchcase), [G28](../../HEURISTICS.md#g28-encapsulate-conditionals), [G29](../../HEURISTICS.md#g29-avoid-negative-conditionals), [G33](../../HEURISTICS.md#g33-encapsulate-boundary-conditions) | Switch-on-type, complex conditions, boundaries |
| Comments and clutter | `comments-and-clutter.md` | [C3](../../HEURISTICS.md#c3-redundant-comment), [C5](../../HEURISTICS.md#c5-commented-out-code), [G9](../../HEURISTICS.md#g9-dead-code), [G10](../../HEURISTICS.md#g10-vertical-separation), [G12](../../HEURISTICS.md#g12-clutter), [G24](../../HEURISTICS.md#g24-follow-standard-conventions) | Dead code, redundant comments, formatting |
| Java idioms | `java-idioms.md` | [J1](../../HEURISTICS.md#j1-avoid-long-import-lists-by-using-wildcards)–[J3](../../HEURISTICS.md#j3-constants-versus-enums), [G4](../../HEURISTICS.md#g4-overridden-safeties), [G25](../../HEURISTICS.md#g25-replace-magic-numbers-with-named-constants), [G26](../../HEURISTICS.md#g26-be-precise) | Imports, constants, enums, magic numbers |
| Test quality | `test-quality.md` | [T1](../../HEURISTICS.md#t1-insufficient-tests), [T3](../../HEURISTICS.md#t3-dont-skip-trivial-tests), [T4](../../HEURISTICS.md#t4-an-ignored-test-is-a-question-about-an-ambiguity) | Assertions, test naming, @TempDir, self-documenting tests |
| Project conventions | `project-conventions.md` | (team-specific) | Naming, libraries, architecture, domain rules |

## How skill files work

1. The plugin writes findings to `CLAUDE.md`
2. Each finding section includes a pointer:
   `> Read .claude/skills/<file>.md before addressing these.`
3. Claude Code reads the skill file once, then applies its patterns to fix
   the finding
4. Each skill file is self-contained — no cross-references to other skill
   files

## Not covered by skill files

Some heuristics require human judgment and cannot be addressed autonomously:

- Narrative-only codes (G2, G3, G6, G13, G20, G21, G22, G27, G31, G32)
- Test infrastructure (T2, T5-T9)
- Environment (E1, E2)

These appear in CLAUDE.md as annotatable stubs for team-authored guidance.
