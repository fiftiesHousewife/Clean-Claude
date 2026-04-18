# Session 2026-04-18 — extract_method recipe, agent drift, and the OpenRewrite idempotency false positive

A long session chasing why the MCP_RECIPES variant of the three-way rework harness cost 7.5× vanilla on a pure-extract fixture. Three bugs found and fixed, one more surfaced and deferred. Also established that the Gradle Tooling API is worth the migration, that textual splicing beats AST print-back for safety, and that "trust the formatter" is the right answer to most whitespace questions.

Paired artefacts:
- Code changes: see `git log` since 2026-04-18 across `refactoring/`, `mcp/`, `plugin/`.
- Full chat transcript: `~/.claude/projects/-Users-pippanewbold-CleanClaude/7a82f2f1-d1e8-49d0-be37-deada5cedb77.jsonl` (12 MB, 6k lines JSONL).

## Starting state

The rework harness has three variants — VANILLA (plain agent), MCP_GRADLE_ONLY (agent + verify_build/run_tests/format), MCP_RECIPES (full MCP surface including extract_method). Running on `PipelineFixture.java`, a "pure-extract-method fixture" with one long method carrying five blank-line-separated phases. Run 0 (pre-session): VANILLA $0.51, MCP_GRADLE_ONLY $0.85, MCP_RECIPES $3.80 — MCP_RECIPES took 6× the time with 60 turns because the agent never successfully invoked `extract_method`, hit a rejection, then spent ~30 tool calls spelunking the recipe source, writing debug tests, even disassembling OpenRewrite jars.

## What we tried, in order

### 1. Prompt guardrails

**Tried:** Added a "if extract_method returns ANY error, fall back to Edit immediately; DO NOT diagnose the recipe or read its source; stay within target files" block to `PromptBuilder.recipesBlock()`, plus a shared "do not Read/Edit under `refactoring/`, `mcp/`, `plugin/`" guardrail in the target-files preamble.

**Expected:** Agent would stop drifting on rejection, saving most of the 49 turns Run 0 wasted.

**Got:** Worked cleanly in Run 2. Agent said "Falling back to manual Edit per instructions" and moved on. Single biggest cost-reduction of the session.

**Lesson:** Prompts that tell the agent what to *do next on failure* are more powerful than prompts that describe tool preconditions. "On rejection, do X" beats "use this tool when Y".

### 2. Source-path inference fix (TDD)

**Hypothesis from prior session memory:** OpenRewrite's `JavaParser.parse(String)` regex-scans the source for `class <ident>` to derive a synthetic source path. PipelineFixture's Javadoc contains `"the class holds instance state"`, which matches `class holds` before the real `class PipelineFixture`, so the CU's source path becomes `holds.java` and `ExtractMethodRecipe.sourcePathMatches` returns false.

**Tried:** Wrote a failing test with PipelineFixture's exact Javadoc shape. Reproduced the rejection. Fixed by switching from `parse(String)` to `parseInputs(List.of(Parser.Input.fromString(file, source)), null, ctx)` — passing an explicit path bypasses the regex heuristic. Test passes.

**Expected:** MCP_RECIPES would now successfully call `extract_method` on at least one phase.

**Got:** Tests green, but Run 2 still rejected lines 64-67 with "recipe rejected the range". The source-path fix was necessary but not sufficient — there was a *second* bug below it.

**Lesson:** When a sub-agent leaves a memory with a surprising theory, verify it with an actual test before trusting. The theory was *true* but didn't explain *all* the rejections.

### 3. Gradle Tooling API migration

**Tried:** Replaced `GradleInvoker`'s `ProcessBuilder(["./gradlew", ...])` with a persistent `GradleConnector.newConnector().forProjectDirectory(root).connect()`. Single `ProjectConnection` for the MCP server's lifetime, closed in `McpServer.main`'s try-with-resources. Added `org.gradle:gradle-tooling-api:9.0.0` from `https://repo.gradle.org/gradle/libs-releases` (not on Maven Central) plus `slf4j-nop` runtime.

**Expected:** Faster tool calls (no `./gradlew` wrapper JVM per call), single-daemon guarantee.

**Got:** Compiles and bundles cleanly. 744 Tooling API classes in the fat jar. End-to-end behaviour unchanged (tools still work; caveat that it's not battle-tested under concurrent MCP calls).

**Lesson:** Low-friction infrastructure upgrade that pays off quietly — you don't see the latency win in a cost report, but it's there.

### 4. Rejection-reason plumbing

**Tried:** Added `AtomicReference<String> lastRejectionReason` to `ExtractMethodRecipe`, with `compareAndSet` at each reject path (path mismatch, target-not-found, not-extractable, reparse-failed). `ExtractMethodTool` surfaces it: `"extract_method rejected: " + reason`.

**Expected:** Diagnostics instead of guessing.

**Got:** Run 2's rejection said `"lines 64-67 do not align with a contiguous run of top-level statements in a single method body"` — false, because my in-process test of the same input passed. That pointed to a divergence between the in-process test path and the direct `java -jar` invocation path.

**Lesson:** Diagnostics that surface specific reasons are table stakes for tools that can reject. The generic "recipe rejected" we had before was actively harmful — the agent treated it as a bug to investigate.

### 5. Divergence between in-process and fat-jar invocations

**Tried:** Ran the MCP tool directly via `java -jar mcp-1.0-SNAPSHOT.jar <<'EOF' {"jsonrpc":"2.0",...}` with the exact same file/line arguments the agent used. Reproduced the rejection at the JSONRPC boundary.

**Expected:** Match the in-process test.

**Got:** Different error. Fat-jar said the file parsed as `ParseError`. In-process test said everything was fine. Classpath difference.

**Lesson:** Always reproduce failures at the same boundary the failing caller uses. In-process unit tests can mask classpath / service-loader issues that only manifest in the real runtime.

### 6. Surfacing the real parse error

**Tried:** Extended the MCP tool to inspect `SourceFile` results for `ParseError` instances and extract `ParseExceptionResult` messages.

**Got:** `IllegalStateException: ... is not print idempotent. \n<diff showing the Javadoc was mangled>`. The mangled diff showed multi-line Javadoc collapsed into one giant line with line breaks and spaces dropped. Em-dashes and `{@link}` references were involved.

**Lesson:** OpenRewrite has a `requirePrintEqualsInput` check that runs after parsing: it prints the AST and compares byte-for-byte against the input. On mismatch, it returns `ParseError`. The check is defensive but over-triggers on Javadoc with certain characters.

### 7. Is the check wrong, or is the output actually mangled?

**Tried:** Wrote a diagnostic test: parse with `ctx.putMessage("org.openrewrite.requirePrintEqualsInput", false)`, call `cu.printAll()`, compare to source.

**Got:** `printed.equals(source) == true` — 3125 bytes identical. Also `cu.printEqualsInput(input, ctx) == true`. Both the printer *and* the comparison method agreed on identity.

**Lesson:** The `requirePrintEqualsInput` check is producing false positives somewhere — possibly via a different comparison path used only in the fat-jar classpath (suspect: `EncodingDetectingInputStream` vs direct String compare, perhaps involving BOM or charset handling). Confirmed by setting the flag to `false` in the MCP tool: the fat-jar invocation then succeeded and wrote the file.

### 8. But disabling the check risks corrupting files

**Tried:** Ran the extraction with the check disabled. Checked the output's Javadoc.

**Got:** Javadoc *was* mangled in the output file — line breaks dropped, words concatenated, the whole block destroyed. Bad. Just disabling the check trades "false rejections" for "silent corruption", which is worse.

**Lesson:** The root issue is that OpenRewrite's AST print-back *does* mangle Javadoc in certain cases. The idempotency check was the only thing preventing us from writing the mangled output.

### 9. Textual splicing (the proper fix)

**Tried:** Bypass AST print-back entirely. `ExtractMethodRecipe.extractTextually`: use the AST only to validate and compute the new method/call-site text, then splice:

```
output = source[0..rangeLineStart)
       + bodyIndent + callSite + '\n'
       + source[rangeLineEndExclusive..afterEnclosingMethod)
       + '\n' + indentedNewMethod
       + source[afterEnclosingMethod..)
```

Source bytes *outside* the extraction region are preserved verbatim. Required adding byte offsets (`extractedStartOffset`, `extractedEndOffset`, `bodyCloseBraceOffset`) to `ExtractionTarget`, and line-boundary helpers (`startOfLine`, `endOfLine`) to `LineIndex`. Invariant check: re-parse the spliced output; if it fails, error out rather than write.

**Got:** Works. Javadoc preserved byte-for-byte. Extracted method syntactically correct.

**Lesson:** When a library's AST round-trip is unreliable for part of the source (here Javadoc), operate textually on that part. AST is for analysis; source bytes are the ground truth for output.

### 10. The whitespace rabbit hole (a cautionary tale)

**Tried:** The new helper method is generated as a string. Its indentation has to match the enclosing class. I wrote `indentOfLine`, `outdentOneLevel`, `isSignatureLine`, `appendReindentedMethod`, plus a `dedent()` in `ExtractionSource` with special-case logic for the case where the first line is stripped of its indent but subsequent lines aren't. ~60 lines of whitespace-manipulation code.

**Expected:** Produce perfectly indented output.

**Got:** Produced *acceptable* output after several iterations. User asked: *"what is all this whitespace stuff you are doing"* and then *"cant you just use the formatter"* and then *"what is it that cares about the whitespace, is it the test expectation?"*

**Answer on inspection:** Nothing programmatic cared. None of the tests assert indentation. `parsesCleanly` already guarantees syntactic validity. The only consumer of the whitespace was a human reading the file in a terminal.

**Lesson:** Before writing a formatter, ask what requires the output to be formatted. If the answer is "nothing, but it looks ugly", the correct solution is to invoke the project's real formatter (Spotless) post-hoc, not to reimplement formatting inline. Left the current (reasonable) indent logic in place but added "run formatter post-extraction" to the backlog.

### 11. Four-way variant (not wired)

**Discussed:** Added a fourth variant `HARNESS_RECIPES_THEN_AGENT` — harness runs all applicable safe recipes first, then hands the partially-fixed file to the agent. Isolates recipe value from agent value.

**Not done:** Descoped to next session. Agreed soak testing should come before the 4th variant because soak will surface more recipe bugs that would destabilize a 4-way run.

## Quantitative results

Three-way run on PipelineFixture.java across three iterations of the session:

| | Run 0 (start) | Run 1 (source-path fix + prompt) | Run 2 (textual splice) |
|---|---:|---:|---:|
| VANILLA cost | $0.51 | $0.66 | — |
| MCP_GRADLE_ONLY cost | $0.85 | $0.82 | — |
| MCP_RECIPES cost | $3.80 | $0.87 | not yet re-run |
| VANILLA duration | 93s | 112s | — |
| MCP_RECIPES duration | 574s | 95s | — |
| MCP_RECIPES turns | 60 | 14 | — |
| extract_method invocations that succeeded | 0 | 0 | ≥1 (verified via direct JSONRPC) |

The ~$3 cost reduction on MCP_RECIPES between Run 0 and Run 1 came almost entirely from the prompt guardrail. The source-path fix alone would not have helped because the agent's first `extract_method` call still rejected (second bug below it). But the guardrail told the agent "fall back immediately" so it stopped burning turns diagnosing.

## Bugs found, fixed, deferred

### Fixed
- **Source-path inference from Javadoc:** `parse(String)` → `parseInputs(Parser.Input.fromString(path, source))`.
- **AST print-back mangles Javadoc:** bypassed via textual splicing of the original source bytes.
- **Generic rejection reason:** surfaced via `lastRejectionReason()` + a specific reason at each reject path.
- **Per-call `./gradlew` wrapper JVM:** replaced with Gradle Tooling API `ProjectConnection` reused for the server's lifetime.

### Known, deferred
- **Javadoc mangling on OpenRewrite AST re-print** (upstream bug or input-edge-case). Working theory: em-dash (U+2014) combined with `{@link}` references. Would report upstream with a minimal reproducer if we pursue it.
- **Idempotency false positive in `requirePrintEqualsInput`:** `printAll()` returns byte-identical output but the check still fails — possibly different comparison path. Not yet root-caused.
- **`verify_build` / `run_tests` / `format` don't pass `-PcleanCodeSelfApply=true`**, so sandbox-targeted calls silently fall back to Bash. Cosmetic (agent recovers) but adds cost.
- **No soak-test corpus for recipes.** The Javadoc bug was found by accident. A soak suite of real Java files (various Javadoc shapes, methods, comments, character sets, line endings) running every recipe would have caught it in under a minute.
- **Sandbox has no spotless wired** — `:sandbox:spotlessApply` errors out.
- **Dual code path in ExtractMethodRecipe:** original AST-based `attemptExtraction` is still there, unused by MCP. Collapse to one path.
- **Hand-rolled indent logic in splice.** Run the real formatter instead.

## Methodology notes worth writing up

1. **Prompt guardrails as control-plane.** The biggest single cost win was not a code fix — it was telling the agent what to do on tool failure. "Fall back to Edit; do not diagnose the recipe" collapsed a 60-turn drift into a 14-turn clean run.

2. **Diagnosis vs. reproduction.** The prior session's sub-agent left a plausible theory in memory. The theory was right, but "right" didn't mean "complete." Verifying via test-first surfaced a second, deeper bug that the memory note didn't cover.

3. **Boundary mismatch.** In-process tests passed; direct `java -jar` invocation failed. Same Java code, same inputs, different classpath. Always reproduce failures at the boundary the failing caller uses.

4. **Silent failures cascade into what looks like drift.** `verify_build` returning "module not found" (because it didn't know about a required Gradle property) caused the agent to fall back to Bash. Observers saw the Bash calls and wrote "agent drift" in their notes. It wasn't drift — it was correct error recovery following a silent-tool-failure signal.

5. **"What cares about this?"** A good question to ask when you find yourself 60 lines into hand-rolling something. If the answer is "a human reading output", consider whether an existing tool (formatter, linter, printer) should own that concern instead.

6. **Textual splicing > AST print-back when the source is ground truth.** OpenRewrite is great for analysis and in-place AST transformations that you then print. But when ANY part of the print path is unreliable (and parsers have edge cases), textual operations on the original source keep the bytes you didn't touch byte-exact. The AST becomes an oracle for *what to change* rather than *what to emit*.

## Open research / paper angles

- **Cost curve of agent drift.** Under what conditions does "tool returns unhelpful error" collapse into multi-dollar spelunking runs? Is there a detectable signal (turn count growth rate? file-reads-outside-target rate?) that could trigger an interruption?

- **Recipe-vs-LLM dispatch.** On a fixture with exactly one extractable range, a deterministic recipe wins on cost and correctness. On ambiguous findings, the agent wins. Where's the crossover, and is it predictable from findings metadata?

- **False-positive defense checks.** OpenRewrite's `requirePrintEqualsInput` is a defensive check that sometimes mis-fires. Defensive checks have their own failure modes — adding one without an escape hatch can block valid inputs. Is there a design pattern for defensive checks with escape hatches that stays safe?

- **Prompt as guardrail vs prompt as instruction.** "Use this tool when X" describes the happy path. "On failure, do Y" describes the recovery path. Most prompt engineering is the former. The recovery path is where the money is.

## For the paper

If the paper is "building reliable LLM agents for code refactoring", this session's three-way/four-way harness and cost telemetry are the core instrument. Key figures:

- Before/after cost table (above).
- Turn-count distribution (Run 0 MCP_RECIPES had one outlier that dominated total cost).
- Recovery-path prompt additions (one block of text → $3 saved).
- The textual-splicing pattern as a reusable technique for "LLM plus deterministic tool" systems where the tool's output must be integrated into existing source.
