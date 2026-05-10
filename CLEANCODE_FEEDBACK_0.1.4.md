# cleancode 0.1.4 — feedback with reproducers

Six bugs / quirks observed during 1.1's clean-up pass against
clean-logging on cleancode 0.1.4. Each section gives a one-line
description, the affected source/rule, a reproducer, and a sketch of
the fix. The reproducers are all observable from the public
clean-logging repo at HEAD (commits up to 5e89183) so any of these
can be replayed by checking out clean-logging and running the listed
command.

---

## 1. E1 reports alpha versions as stale-upgrade targets

**Symptom.** `analyseCleanCode` lists `slf4j-simple` as outdated with
upgrade target `2.1.0-alpha1`, even though the convention plugin's
own `dependencyUpdates` config filters
`alpha|beta|rc|milestone|preview|snapshot` candidates out of the
release-eligible set.

**Reproducer.**

```bash
cd ~/clean-logging
./gradlew dependencyUpdates analyseCleanCode
jq -r '.findings[] | select(.code == "E1") | .message' \
  build/reports/clean-code/findings.json | grep slf4j-simple
```

Expected (after fix): no result. Actual: at least one finding pointing
at `2.1.0-alpha1`.

**Likely fix.** The E1 rule reads
`build/dependencyUpdates/report.json` produced by the ben-manes
plugin. That report respects the `rejectVersionIf` filter the
plugin's caller sets (here, the recipe-library convention plugin
filters prerelease tokens). So E1 should already be filtered.
Unless cleancode is reading the unfiltered upstream feed itself,
something else is going on — but the simpler fix is to apply the
same prerelease-token filter inside cleancode's E1 ingestion before
emitting findings, so this works regardless of how the report was
produced.

---

## 2. E1 reports cleancode-internal dependencies that consumers can't act on

**Symptom.** `analyseCleanCode` lists checkstyle, pmd-ant/cli/java,
and spotbugs-gradle-plugin as outdated, even though those are
managed by the cleancode plugin's classpath, not the consuming
project's.

**Reproducer.**

```bash
cd ~/clean-logging
./gradlew dependencyUpdates analyseCleanCode
jq -r '.findings[] | select(.code == "E1") | .message' \
  build/reports/clean-code/findings.json | grep -E 'checkstyle|pmd|spotbugs'
```

Expected (after fix): no result. Actual: 4-5 findings pointing at
versions the consuming project can't bump because they're declared
inside cleancode itself.

**Likely fix.** E1 currently emits a finding for every entry in the
`dependencyUpdates` report. The fix is to scope it to deps actually
declared in the project's catalog or build files — a
`dependencyOrigin` filter that only emits findings for configurations
the project owns (i.e. exclude buildscript-classpath / plugin
classpath transitives). Cosmetically, you could also tag findings
with their origin so consumers see at a glance "this is mine to
bump" vs "this is a sub-plugin's".

---

## 3. G5 conflates two distinct rules under one code

**Symptom.** Two structurally different cpd-based findings surface
under the same `G5` code:

- "Duplicated block (X tokens)" — actual CPD block-duplication.
- "String X appears 2 times — extract to a named constant" — string-literal duplication, a different cpd subrule.

The fixes for the two are different (refactor vs. extract a
constant), but the rule code is identical, so they're indistinguishable
in tooling that buckets by code (e.g. CI dashboards, the cleancode
plugin's own summary).

**Reproducer.**

```bash
cd ~/clean-logging
./gradlew analyseCleanCode
jq -r '.findings[] | select(.code == "G5") | .message[0:80]' \
  build/reports/clean-code/findings.json | sort -u
```

Expected (after fix): each rule has its own code, e.g. `G5a`
(duplicated block) and `G5b` (duplicated string literal). Actual:
the two messages share `G5`.

**Likely fix.** Split into two codes in the cleancode rule registry,
then map cpd's `cpd-duplication` subrule and `cpd-string-literal`
subrule (or whatever the underlying detectors are) to those codes
respectively.

---

## 4. Findings JSON `source` field is null on every record

**Symptom.** The CLI summary emitted by `analyseCleanCode` nicely
categorises findings by source ("OpenRewrite: 8 / CPD: 6 / ..."),
but `findings.json` records all have `"source": null`. The source
attribution lives in the CLI presenter and never makes it into the
machine-readable export.

**Reproducer.**

```bash
cd ~/clean-logging
./gradlew analyseCleanCode
jq -r '[.findings[] | .source] | unique' \
  build/reports/clean-code/findings.json
```

Expected: `["openrewrite", "cpd", "jacoco", ...]` (or similar).
Actual: `[null]`.

**Impact.** Tooling can't group/filter findings by source via the
JSON without re-deriving from the `tool` / `ruleRef` strings.

**Likely fix.** In whatever code path builds the per-finding record
for the JSON exporter, fill in the `source` field with the same
attribution the CLI presenter computes.

---

## 5. T1 (coverage) finding has `null:null` location

**Symptom.** The coverage finding is project-global but the JSON
record carries `"location": {"file": null, "line": null}`. Cosmetic,
but readers expect either no location or one pointing at the
coverage report.

**Reproducer.**

```bash
cd ~/clean-logging
./gradlew test analyseCleanCode
jq -r '.findings[] | select(.code == "T1") | "\(.sourceFile // "null"):\(.startLine // "null")  \(.message[0:60])"' \
  build/reports/clean-code/findings.json
```

Expected: either no `sourceFile`/`startLine` keys, or
`build/reports/jacoco/test/html/index.html:1`. Actual: `null:null`.

**Likely fix.** For project-global findings, omit the location
entirely (or point at the report HTML).

---

## 6. SkillFileScaffolder writeHashFile fails under sandbox / read-only filesystems (0.1.3 only — confirm fixed in 0.1.4)

**Symptom (0.1.3).** Running `analyseCleanCode` (or any task that
applies the cleancode plugin) against a read-only or sandboxed home
directory produces a stack trace ending in
`SkillFileScaffolder.writeHashFile:178 -> java.io.IOException`.

**Reproducer (0.1.3).**

```bash
git clone https://github.com/fiftiesHousewife/clean-logging
cd clean-logging
git checkout 587ca76   # last commit pinned to cleancode 0.1.3
chmod -w ~/.claude/skills 2>/dev/null  # or run under a sandbox
./gradlew analyseCleanCode
```

Expected: graceful no-op (skip skill scaffolding when target is not
writable). Actual (0.1.3): full stack trace, build sometimes fails.

**Status.** Has not been observed during this session on 0.1.4.
Suspected fix: the SkillFileScaffolder catches the IOException and
logs at warn level. Worth confirming via a deliberate read-only
reproduction on 0.1.4 before closing.

---

## What's already on cleancode's side

For (1), (2), (3), (4), (5) the underlying detectors (cpd, jacoco,
ben-manes versions plugin) are correct — the bugs are all in the
cleancode plugin's ingestion / classification / export layer. The
fixes are local to the plugin, not to the upstream tools.

(6) was probably fixed in 0.1.4 already; the rest persist on 0.1.4.
