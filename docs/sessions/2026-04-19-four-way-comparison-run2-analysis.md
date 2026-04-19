# 4-way comparison — 2026-04-19 run 2 (late afternoon, post feedback-loop)

Second 4-way run of the day, against the same 10-file batch as this morning's
run 1 (archived at `2026-04-19-four-way-comparison-{raw,analysis}.md`).

Changes since morning:
- Feedback-loop retry (`maxRetries=1`) now enabled on every variant.
- `ReturnInsteadOfMutateArgRecipe` (F2) added to `HarnessRecipePass`.
- `InappropriateStaticRecipe` detector updated to catch unqualified
  instance-field writes (e.g. `rowsParsed++`).

Raw report: `2026-04-19-four-way-comparison-run2-raw.md`.

## Headline numbers

| Variant | actions | rej | findings | fixed | introduced | wall | cost | turns | $/action |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| VANILLA | 17 | 5 | 43→**18** | 30 | 5 | 13:23 | $4.63 | 65 | 0.272 |
| MCP_GRADLE_ONLY | 16 | **0** | 43→16 | 33 | 6 | 16:18 | $5.40 | 60 | 0.338 |
| MCP_RECIPES | 16 | 1 | 43→15 | 33 | 5 | 19:32 | $6.15 | 68 | 0.385 |
| HARNESS_RECIPES_THEN_AGENT | **23** | 6 | 43→**14** | **34** | 5 | **12:41** | **$3.53** | **44** | **0.154** |

Aggregate wall: 1h 2m 32s (vs 35m 20s this morning — **+77%**).
Aggregate cost: $19.71 (vs ~$12.68 this morning — **+55%**).
Aggregate final findings: 63 (vs ~94 this morning — **-33%**).

## Hypothesis evaluation (against this morning's baseline)

### 1. Introduced count drops across every variant — **CONFIRMED**

| Variant | this morning | tonight | Δ |
|---|---:|---:|---:|
| VANILLA | 10 | 5 | **-5** |
| MCP_GRADLE_ONLY | 7 | 6 | -1 |
| MCP_RECIPES | 10 | 5 | **-5** |
| HARNESS | 10 | 5 | **-5** |

Clear, consistent drop on three of four variants. MCP_GRADLE_ONLY moved the
least — unclear whether that's because its morning baseline was already low
(7, the lowest of the four) or because the retry pass is less effective when
the prompt has no deterministic-recipes block anchoring it.

### 2. VANILLA benefits most — **CONFIRMED (tied)**

VANILLA's 10→5 is tied for the largest absolute drop with MCP_RECIPES and
HARNESS. It is not uniquely the biggest beneficiary, but the hypothesis
that the simplest variant would reap the most from feedback-loop retries
holds at the "tied for biggest" level.

### 3. HARNESS rejected count drops toward 0 — **STALE-JAR FALSE NEGATIVE** (revised)

Initial read: disconfirmed. After investigation: **the detector fix works;
the 4-way run used a stale jar.**

What actually happened:

- Direct soak tests against the exact sandbox shapes (CsvParser, HttpRetryPolicy,
  SessionStore.*, NotificationDispatcher) added to
  `InappropriateStaticRecipeTest` **all pass** on the current
  `InappropriateStaticRecipe` source. The detector correctly recognises
  unqualified `field.method(...)` invocations and field reads/writes.
- Rebuilding the `recipes` jar and re-running `./gradlew
  :sandbox:analyseCleanCode` collapses G18 from **14 → 6** findings. The
  six remaining are all legitimate (methods that genuinely have no
  instance state).
- Inspecting `~/.m2/repository/org/fiftieshousewife/cleancode/recipes/1.0-SNAPSHOT/`
  showed the recipes jar was last published 2026-04-17 23:03 — **before the
  G18 fix commit (2026-04-19 10:31)**. The 2026-04-19 handoff published only
  `refactoring` and `plugin`, so the analyser loaded the pre-fix detector.

**Root cause:** the handoff command `./gradlew :refactoring:publishToMavenLocal
:plugin:publishToMavenLocal` is partial. Changes in any other module
(`recipes`, `adapters`, `core`, `annotations`, `claude-review`, `mcp`)
won't land in mavenLocal. Fixed by switching the handoff to the root
`./gradlew publishToMavenLocal` task and by `scripts/nightly-compare.sh`
which enforces this.

Next 4-way run should show HARNESS rejected count drop to ~0, as originally
predicted.

### 4. Total cost rises 10–20% — **DISCONFIRMED**

(Morning costs below corrected from the initial "$3.17 for all" placeholder
once `scripts/compare-runs.py` extracted the real per-variant numbers.)

| Variant | morning cost | tonight cost | Δ |
|---|---:|---:|---:|
| VANILLA | $3.46 | $4.63 | **+34%** |
| MCP_GRADLE_ONLY | $2.98 | $5.40 | **+81%** |
| MCP_RECIPES | $3.17 | $6.15 | **+94%** |
| HARNESS | $3.17 | $3.53 | **+11%** |

Only HARNESS stayed inside the predicted 10–20% band. The other three are
2–5× over the predicted cost increase.

Why: the retry pass is a **second `claude -p` invocation**. Each invocation
pays full cache-creation on its prompt (~125–245k cache-creation tokens per
variant, per the Cost table). Two invocations means two cache-creation
events, which roughly doubles the prompt-processing cost share. The output
token counts went up too (variants stored more reasoning across two turns).

HARNESS is the outlier because the agent did less work in total (44 turns
vs 60–68 for others) — the deterministic recipe pass stripped enough
findings upfront that the retry had much smaller residuals to churn on,
keeping the second invocation cheap.

### 5. Final findings drops to 15–20 from 23–24 — **CONFIRMED AND SLIGHTLY EXCEEDED**

| Variant | morning | tonight |
|---|---:|---:|
| VANILLA | 24 | 18 |
| MCP_GRADLE_ONLY | 23 | 16 |
| MCP_RECIPES | 23 | 15 |
| HARNESS | 23 | 14 |

All variants landed in or below the predicted 15–20 range. HARNESS at 14 is
below the predicted floor. Net improvement on the batch: ~33% fewer final
findings versus this morning, at ~55% more total cost.

## Secondary observations

**HARNESS is now the clear winner.** Best on every axis — findings (14),
cost ($3.53), wall (12:41), turns (44), $/action ($0.15). This morning
HARNESS tied MCP_RECIPES on cost and didn't dominate quality. Tonight the
combination of deterministic recipes + retry pass made it the cheapest *and*
highest-quality variant.

**MCP_RECIPES got the worst cost/quality ratio tonight.** $6.15 for 15
final findings is $0.41 per finding-removed (43-15=28), versus $0.12 per
finding-removed for HARNESS. The agent's freedom to choose MCP tools in
MCP_RECIPES means the retry pass amplifies that exploration cost.

**F2 recipe didn't fire on any file.** None of HARNESS's eight HarnessRecipePass
entries list `ReturnInsteadOfMutateArgRecipe`. `UserAccountService.validate`
is the known target (morning run flagged it clearly); the recipe either
doesn't match the current pattern or isn't wired into the default recipe
set. Needs inspection.

**G18 detector fix did not land effectively.** See hypothesis 3 above. The
baseline G18 findings on methods that mutate unqualified instance fields
are unchanged.

## Recommended next steps (roughly ordered by leverage)

1. **Fix `InappropriateStaticRecipe` to actually catch unqualified
   postfix/prefix writes and unqualified `field.method(...)` invocations.**
   Add a soak test with `parseRow`/`execute`/`dispatchUrgent` shapes so
   regressions are obvious.
2. **Investigate why `ReturnInsteadOfMutateArgRecipe` didn't match
   `UserAccountService.validate`.** Check the visitor's method-body
   predicate — it likely requires the body to *only* call `.add()` but
   `validate` probably also has conditional bodies around the add-calls.
3. **Reduce retry-pass cost.** Two options:
   - Reuse the first session's cache by passing the residual-findings as a
     *continuation* rather than a fresh prompt (would need Claude Code CLI
     support for session resume, which today's `claude -p` does not).
   - Skip the retry when the introduced count is 0 — no-op savings, but
     cheap to implement. Even better: skip when introduced ≤ N cheap
     findings (like pure constant-extraction) the retry probably won't fix.
4. **Run once more at `maxRetries=2`** on the best-value variant (HARNESS)
   to see if a second retry pushes findings below 14 at tolerable cost.
   Morning's plan listed this as a maxRetries=1-vs-2 decision.
5. **Capture introduced-findings breakdown by code per variant** (noted in
   morning's backlog). Without per-code tallies we can't tell which
   patterns the feedback loop actually fixes vs which it leaves.

## Files

- Raw report: `docs/sessions/2026-04-19-four-way-comparison-run2-raw.md`
- Log: `/tmp/cleanclaude/rework-4way-20260419-132013.log` (local only)
- Gradle build took 1h 2m 32s wall start-to-finish (BST 13:20–14:22).
