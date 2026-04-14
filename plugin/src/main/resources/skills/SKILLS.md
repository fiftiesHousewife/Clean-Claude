# Skill Files

Skill files teach Claude Code how to fix specific types of findings from the
Clean Code plugin. They are read once before fixing a finding, and followed
literally.

Developers: these are also a reference for the patterns each concern requires.

## Catalogue

| Concern                      | File                                | Codes                        | When to read                                    |
|------------------------------|-------------------------------------|------------------------------|-------------------------------------------------|
| Exception handling           | `exception-handling.md`             | Ch7.1                        | Catch blocks, propagation, boundary translation |
| Null handling                | `null-handling.md`                  | Ch7.2                        | Null checks, Optional, fail-fast patterns       |
| Function design              | `functions.md`                      | Ch3.1-3, F1-3, G5, G30, G34 | Method length, parameters, flag args, loops     |
| Class design                 | `classes.md`                        | Ch10.1-2, G8, G14, G17, G18 | Class size, SRP, records, feature envy          |
| Naming                       | `naming.md`                         | N1, N5-7, G11, G16          | Short names, encoding, side-effects, ternaries  |
| Conditionals and expressions | `conditionals-and-expressions.md`   | G19, G23, G28, G29, G33     | Switch-on-type, complex conditions, boundaries  |
| Comments and clutter         | `comments-and-clutter.md`           | C3, C5, G9, G10, G12, G24   | Dead code, redundant comments, formatting       |
| Java idioms                  | `java-idioms.md`                    | J1-3, G4, G25, G26          | Imports, constants, enums, magic numbers        |

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
- Test infrastructure (T1-T9)
- Environment (E1, E2)

These appear in CLAUDE.md as annotatable stubs for team-authored guidance.
