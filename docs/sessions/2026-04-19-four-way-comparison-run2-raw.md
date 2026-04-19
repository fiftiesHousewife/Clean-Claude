# Rework comparison

Target files:
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
  - /Users/pippanewbold/CleanClaude/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java

## Cost

| | vanilla | mcp gradle only | mcp + recipes | harness + agent |
|---|---:|---:|---:|---:|
| input tokens | 78 | 86 | 96 | 52 |
| cache creation | 186864 | 237190 | 243960 | 124240 |
| cache read | 3698693 | 3705439 | 4651337 | 2424549 |
| total input | 3885635 | 3942715 | 4895393 | 2548841 |
| output tokens | 64264 | 82428 | 91748 | 61418 |
| cache hit rate | 95.2% | 94.0% | 95.0% | 95.1% |
| turns | 65 | 60 | 68 | 44 |
| duration (s) | 802.9 | 978.0 | 1172.4 | 760.7 |
| cost (USD) | 4.6298 | 5.4021 | 6.1514 | 3.5306 |
| actions | 17 | 16 | 16 | 23 |
| rejected | 5 | 0 | 1 | 6 |
| cost per action | 0.2723 | 0.3376 | 0.3845 | 0.1535 |

## Findings

| | vanilla | mcp gradle only | mcp + recipes | harness + agent |
|---|---:|---:|---:|---:|
| baseline | 43 | 43 | 43 | 43 |
| fixed | 30 | 33 | 33 | 34 |
| introduced | 5 | 6 | 5 | 5 |
| final | 18 | 16 | 15 | 14 |

## vanilla — commit message body

## Actions
- Edit(change=made computeScore static; replaced two guard-continues with stream filter + Math.min cap; extracted SCORE_CAP constant) — Addresses G18 (static) and G29 (merged guards into filter chain).
- Edit(change=extracted SEPARATOR constant; extracted dropReason() helper so handle() can be static) — Addresses G25 (constant) and G18 (static via pure helper).
- Edit(change=settleOrders now returns a new stock Map instead of mutating the arg; removed 6 section comments; added imports for LinkedHashSet/LinkedHashMap/HashMap/Collectors; made static; extracted RESERVATION_DISCOUNT/SEPARATOR; final audit.append calls chained) — Addresses F2, Ch7.2 (getOrDefault), G34, G30, G18, G31, G12.
- Edit(change=moved `fields` declaration next to its first use; extracted isEscapedQuote() helper from depth-3 condition; removed blank-line section breaks) — Addresses G10 (declaration locality), G28 (condition extraction), and G30 (fewer sections).
- Edit(change=extracted ATTEMPT_PREFIX/MAX_ATTEMPTS/INITIAL_DELAY_MS/MAX_DELAY_MS/BACKOFF_MULTIPLIER constants; replaced System.err.println with audit.append + Thread.interrupt; extracted recordFailure/sleepBeforeRetry/delayFor helpers; eliminated persistent delayMs state; wrapped nullable lastFailure with Optional.ofNullable) — Addresses G25, G4, G12 (System.err), G10 (delayMs gone), and the spirit of Ch7.2 on the lastFailure null check.
- Edit(change=all three methods now static and stream-based; balanceByRegion uses Map.merge; Map.Entry imported directly as Entry) — Addresses G18, G29, G30, G12.
- Edit(change=split dispatch into dispatch() and dispatchUrgent(); collapsed four if/else-if channel branches into a single Set-based validation + format(); IntStream replaces the retry for-loops; URGENT_RETRIES/RETRYING_CHANNELS/WEBHOOK constants) — Addresses F3, G15, G23, and G30 (all three loops).
- Edit(change=renderReport chains all StringBuilder appends; section/row helpers now return String instead of mutating a threaded sb; replaced Map.Entry loop with totals.forEach((k,v) -> ...)) — Addresses G31 (method chaining), F2 (no StringBuilder threading), and G12 (no Map.Entry reference).
- Edit(change=userIdFor combines its 5 guards into 2 using || and isExpired(); lookupOrNull condenses to one guard; open delegates to requireNonBlank() helper; activeSessionCount uses stream count; extracted SESSION_TTL_MS) — Addresses G29 on all three methods and G25 on the expiry literal.
- Edit(change=validate() now returns List<String> and absorbs the old inline validator; extracted EMAIL_REQUIRED/EMAIL_MUST_CONTAIN_AT/DISPLAY_NAME_REQUIRED/DISPLAY_NAME_TOO_LONG/REGION_INVALID/DISPLAY_NAME_MAX/VALID_REGIONS constants; removed the two catch-log-return-null blocks (HashMap.put + trivial sendWelcomeEmail neither throw); three audit.add calls merged into a single audit.addAll(List.of(...))) — Addresses F2 (output arg), Ch7.1 (both catches), Ch7.2 (via else-if chains), G25 (two duplicate messages), G4/G12 (System.err gone), G31 (single addAll).
- Edit(change=Collapsed two if-guard clauses in dropReason into a single Java 21 switch expression with null and pattern-guard arms) — Addresses G29 by simplifying the two guard clauses into one expression
- Edit(change=Extracted the price-discount loop into private static adjustedPrices(unique, prices); audit-logging for missing prices now runs as a separate stream in settleOrders) — Brings settleOrders under 50 lines (G30) without threading audit StringBuilder into the helper
- Edit(change=Extracted the character loop into private collectFields(line); parseRow now declares fields on the line it is used. Split isEscapedQuote into nextIndex/hasNextChar intermediate variables with an early return) — Fixes G10 (declaration close to use) and G19 (complex boolean expression broken into named variables)
- Edit(change=dispatchUrgent now delegates to a private static urgentMessages helper returning List<String>, and only dispatched.addAll is left as instance-state work. In format, extracted the channel+suffix concatenation into a local header variable) — Resolves G18 (extract non-instance logic) and G19 (complex expression extraction)
- Edit(change=Made renderReport static since it never touched instance state) — Addresses G18
- Edit(change=Extracted isUsable(session) from userIdFor's compound boolean. Changed requireNonBlank to return the validated value, and open now chains validToken/validUserId/session locals so the three former consecutive void calls have an explicit data dependency) — Fixes G28 (encapsulate conditional) and G31 (order of operations made explicit via data dependency)
- Edit(change=Replaced the five error-message String constants (including the three DISPLAY_* ones) with a private ValidationError enum that carries a message(). Renamed DISPLAY_NAME_MAX to DISPLAY_NAME_MAX_LENGTH. Made validate static.) — Addresses J3 (DISPLAY_* prefix cluster turned into enum) and G18 (validate had no instance-state usage)

## Not attempted
- Edit(finding=G18 parseRow does not use instance state) — parseRow reads instance fields `separator`/`quote` and mutates `rowsParsed`; making it static is incorrect.
- Edit(finding=G18 execute does not use instance state) — execute appends to the instance `audit` StringBuilder throughout; cannot be static.
- Edit(finding=Ch7.1 catch Exception) — Callable<T>.call() throws Exception, so narrower catches would leak checked exceptions — kept catch(Exception) with re-interrupt/audit handling.
- Edit(finding=G18 all five methods do not use instance state) — Every method reads or writes the `sessions` field; making them static is incorrect.
- Edit(finding=G12 session.userId fully-qualified type reference) — `session.userId` is field access, not a type reference; nothing to shorten.

## Suggestions
- G18 at L-1: Method 'computeScore' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'computeScore' has 2 guard clauses — simplify with early return or extract filter
- G25 at L-1: String "---
" appears 2 times — extract to a named constant
- G18 at L-1: Method 'handle' does not use instance state — consider making it static or extracting
- F2 at L-1: Method 'settleOrders' mutates its argument 'stock' (java.util.Map<java.lang.String, java.lang.Integer>) — return the result instead
- Ch7_2 at L-1: Method 'settleOrders' has 7 null checks
- G34 at L-1: Method 'settleOrders' has 6 section comments
- G30 at L-1: Loop in 'settleOrders' (for-each with filter-add) can be replaced with a stream operation
- G18 at L-1: Method 'settleOrders' does not use instance state — consider making it static or extracting
- G31 at L-1: Method 'settleOrders' has 3 consecutive void calls with no data dependency — make the order explicit
- G12 at L0: 5 inline fully-qualified type reference(s); first: java.util.LinkedHashSet — run ShortenFullyQualifiedReferencesRecipe
- G28 at L-1: Complex condition (depth 2) should be extracted
- G10 at L3: 'fields' is declared in parseRow() but not used until 20 lines later — move the declaration closer to line 23
- G30 at L-1: Method 'parseRow' has 6 blank-line sections across 41 lines — each section should be its own method
- G18 at L-1: Method 'parseRow' does not use instance state — consider making it static or extracting
- G10 at L4: 'delayMs' is declared in execute() but not used until 17 lines later — move the declaration closer to line 21
- G25 at L-1: String "attempt " appears 2 times — extract to a named constant
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G18 at L-1: Method 'execute' does not use instance state — consider making it static or extracting
- Ch7_1 at L-1: Method 'execute' catches Exception — catch specific exception types instead
- G12 at L0: 1 inline fully-qualified type reference(s); first: System.err — run ShortenFullyQualifiedReferencesRecipe
- G10 at L3: 'balances' is declared in balanceByRegion() but not used until 11 lines later — move the declaration closer to line 14
- G30 at L-1: Loop in 'overstockedRegions' (for-each with filter-add) can be replaced with a stream operation
- G18 at L-1: Method 'balanceByRegion' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'overstockedRegions' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'totalAcrossRegions' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'balanceByRegion' has 3 guard clauses — simplify with early return or extract filter
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- F3 at L-1: Method 'dispatch' takes boolean parameter 'urgent' — split into two methods instead
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G23 at L-1: Method 'dispatch' dispatches on String parameter 'channel' with 4 branches — use an enum or split into separate methods
- G15 at L-1: Method 'dispatch' uses boolean parameter 'urgent' to select behaviour — split into separate methods
- G31 at L-1: Method 'renderReport' has 4 consecutive void calls with no data dependency — make the order explicit
- G31 at L-1: Method 'appendSection' has 4 consecutive void calls with no data dependency — make the order explicit
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- G18 at L-1: Method 'userIdFor' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'lookupOrNull' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'open' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'close' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'activeSessionCount' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'userIdFor' has 5 guard clauses — simplify with early return or extract filter
- G29 at L-1: Method 'lookupOrNull' has 2 guard clauses — simplify with early return or extract filter
- G29 at L-1: Method 'open' has 2 guard clauses — simplify with early return or extract filter
- G12 at L0: 3 inline fully-qualified type reference(s); first: session.userId — run ShortenFullyQualifiedReferencesRecipe
- F2 at L-1: Method 'validate' mutates its argument 'errors' (java.util.List<java.lang.String>) — return the result instead
- Ch7_1 at L-1: Catch block in 'createAccount' only logs or is empty
- Ch7_2 at L-1: Method 'createAccount' has 5 null checks
- G25 at L-1: String "displayName is required" appears 2 times — extract to a named constant
- G25 at L-1: String "email is required" appears 2 times — extract to a named constant
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G31 at L-1: Method 'createAccount' has 3 consecutive void calls with no data dependency — make the order explicit
- Ch7_1 at L-1: Method 'createAccount' catches Exception — catch specific exception types instead
- F2 at L-1: Class has 1 methods returning collections and 1 void methods mutating collection params — pick one style
- G12 at L0: 2 inline fully-qualified type reference(s); first: System.err — run ShortenFullyQualifiedReferencesRecipe

## Agent usage
- input tokens : 78
- output tokens: 64264
- cache read   : 3698693
- cost (USD)   : 4.6298

## vanilla — diff

```diff
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
index 6567aad..9a68910 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
@@ -1,31 +1,18 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.List;
+import java.util.Objects;
 
-/**
- * Phase B + Phase G fixture. {@link #computeScore} declares a local
- * {@code total} before a for-each loop, the loop writes to
- * {@code total} from inside a {@code continue}-bearing body, and the
- * value of {@code total} is read after the loop. The right extraction
- * is the loop alone — an outer-local reassignment output (Phase B)
- * whose body contains a loop-internal continue (Phase G).
- */
 public final class AccumulatorFixture {
 
-    public int computeScore(final List<Integer> values) {
-        int total = 0;
-        for (final Integer value : values) {
-            if (value == null) {
-                continue;
-            }
-            if (value < 0) {
-                continue;
-            }
-            total = total + value;
-        }
-        if (total > 10000) {
-            total = 10000;
-        }
-        return total;
+    private static final int SCORE_CAP = 10_000;
+
+    public static int computeScore(final List<Integer> values) {
+        final int total = values.stream()
+                .filter(Objects::nonNull)
+                .filter(value -> value >= 0)
+                .mapToInt(Integer::intValue)
+                .sum();
+        return Math.min(total, SCORE_CAP);
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
index 63f8a43..3135bb0 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
@@ -26,16 +26,20 @@ public final class CsvParser {
     }
 
     public List<String> parseRow(final String line) {
+        final List<String> fields = collectFields(line);
+        rowsParsed = rowsParsed + 1;
+        return fields;
+    }
+
+    private List<String> collectFields(final String line) {
         final List<String> fields = new ArrayList<>();
         final StringBuilder current = new StringBuilder();
         boolean insideQuotes = false;
         int index = 0;
-
         while (index < line.length()) {
             final char ch = line.charAt(index);
-
             if (ch == quote) {
-                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == quote) {
+                if (isEscapedQuote(line, index, insideQuotes)) {
                     current.append(quote);
                     index = index + 2;
                     continue;
@@ -44,29 +48,33 @@ public final class CsvParser {
                 index = index + 1;
                 continue;
             }
-
             if (ch == separator && !insideQuotes) {
                 fields.add(current.toString());
                 current.setLength(0);
                 index = index + 1;
                 continue;
             }
-
             if (ch == '\\' && index + 1 < line.length()) {
                 current.append(line.charAt(index + 1));
                 index = index + 2;
                 continue;
             }
-
             current.append(ch);
             index = index + 1;
         }
-
         fields.add(current.toString());
-        rowsParsed = rowsParsed + 1;
         return fields;
     }
 
+    private boolean isEscapedQuote(final String line, final int index, final boolean insideQuotes) {
+        if (!insideQuotes) {
+            return false;
+        }
+        final int nextIndex = index + 1;
+        final boolean hasNextChar = nextIndex < line.length();
+        return hasNextChar && line.charAt(nextIndex) == quote;
+    }
+
     public int rowsParsed() {
         return rowsParsed;
     }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
index 43f0728..3be4b13 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
@@ -1,26 +1,23 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
-/**
- * Phase A fixture. {@link #handle} opens with an early-return guard
- * block — a {@code null} / empty check that causes the method to
- * bail. The right extraction is the guard alone, turning it into a
- * boolean-returning {@code isInvalid(…)} helper. The surrounding
- * method stays void; the call site becomes
- * {@code if (isInvalid(event)) return;}.
- */
 public final class GuardFixture {
 
-    public void handle(final String event, final StringBuilder log) {
-        if (event == null) {
-            log.append("drop: null event\n");
-            log.append("---\n");
-            return;
-        }
-        if (event.isBlank()) {
-            log.append("drop: blank event\n");
-            log.append("---\n");
+    private static final String SEPARATOR = "---\n";
+
+    public static void handle(final String event, final StringBuilder log) {
+        final String dropReason = dropReason(event);
+        if (dropReason != null) {
+            log.append("drop: ").append(dropReason).append('\n').append(SEPARATOR);
             return;
         }
         log.append("processed: ").append(event.strip()).append('\n');
     }
+
+    private static String dropReason(final String event) {
+        return switch (event) {
+            case null -> "null event";
+            case String s when s.isBlank() -> "blank event";
+            default -> null;
+        };
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
index fb1c60a..ac7b75c 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
@@ -1,57 +1,68 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
+import java.util.Optional;
 import java.util.concurrent.Callable;
 
-/**
- * Retries a callable with exponential backoff. Typical production shape
- * for a resilience helper. Finding cluster: magic numbers sprinkled
- * across the backoff loop (G25), a catch-InterruptedException block
- * that logs-and-returns-null (Ch7.1), and an execute method that is
- * long enough to benefit from a backoff-phase extraction (G30).
- */
 public final class HttpRetryPolicy {
 
+    private static final String ATTEMPT_PREFIX = "attempt ";
+    private static final int MAX_ATTEMPTS = 5;
+    private static final long INITIAL_DELAY_MS = 500L;
+    private static final long MAX_DELAY_MS = 10_000L;
+    private static final double BACKOFF_MULTIPLIER = 1.5;
+
     private final StringBuilder audit = new StringBuilder();
 
     public <T> T execute(final Callable<T> action) {
-        int attempt = 0;
-        long delayMs = 500;
         Throwable lastFailure = null;
-        while (attempt < 5) {
+        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt = attempt + 1) {
             try {
                 final T result = action.call();
-                audit.append("attempt ").append(attempt).append(" ok\n");
+                audit.append(ATTEMPT_PREFIX).append(attempt).append(" ok\n");
                 return result;
-            } catch (Exception e) {
-                lastFailure = e;
-                audit.append("attempt ").append(attempt).append(" failed: ")
-                        .append(e.getMessage()).append('\n');
+            } catch (Exception failure) {
+                lastFailure = failure;
+                recordFailure(attempt, failure);
             }
-
-            if (attempt >= 4) {
+            if (attempt >= MAX_ATTEMPTS - 1) {
                 break;
             }
-            try {
-                Thread.sleep(delayMs);
-            } catch (InterruptedException interrupted) {
-                System.err.println("retry interrupted: " + interrupted.getMessage());
+            if (!sleepBeforeRetry(attempt)) {
                 return null;
             }
-            delayMs = (long) (delayMs * 1.5);
-            if (delayMs > 10000) {
-                delayMs = 10000;
-            }
-            attempt = attempt + 1;
-        }
-
-        audit.append("gave up after ").append(attempt + 1).append(" attempts\n");
-        if (lastFailure != null) {
-            audit.append("last failure: ").append(lastFailure.getClass().getSimpleName()).append('\n');
         }
+        audit.append("gave up after ").append(MAX_ATTEMPTS).append(" attempts\n");
+        Optional.ofNullable(lastFailure)
+                .ifPresent(t -> audit.append("last failure: ")
+                        .append(t.getClass().getSimpleName()).append('\n'));
         return null;
     }
 
     public String auditLog() {
         return audit.toString();
     }
+
+    private void recordFailure(final int attempt, final Exception e) {
+        audit.append(ATTEMPT_PREFIX).append(attempt).append(" failed: ")
+                .append(e.getMessage()).append('\n');
+    }
+
+    private boolean sleepBeforeRetry(final int attempt) {
+        try {
+            Thread.sleep(delayFor(attempt));
+            return true;
+        } catch (InterruptedException interrupted) {
+            Thread.currentThread().interrupt();
+            audit.append("retry interrupted: ").append(interrupted.getMessage()).append('\n');
+            return false;
+        }
+    }
+
+    private static long delayFor(final int attempt) {
+        long delay = INITIAL_DELAY_MS;
+        for (int i = 0; i < attempt; i = i + 1) {
+            delay = Math.min(MAX_DELAY_MS, (long) (delay * BACKOFF_MULTIPLIER));
+        }
+        return delay;
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
index e972d68..cc2c806 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
@@ -1,59 +1,35 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
-import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
+import java.util.Map.Entry;
+import java.util.Objects;
 
-/**
- * Imperative-loop aggregator — classic candidate for stream conversion
- * (G30 when the loop is long). Each loop body also has nested
- * conditionals that could simplify via Map.computeIfAbsent. Deliberately
- * mutation-heavy to exercise the LLM's pattern-matching on stream
- * rewrites, where recipes have less leverage.
- */
 public final class InventoryBalancer {
 
-    public Map<String, Integer> balanceByRegion(final List<Lot> lots) {
+    public static Map<String, Integer> balanceByRegion(final List<Lot> lots) {
         final Map<String, Integer> balances = new HashMap<>();
-        for (final Lot lot : lots) {
-            if (lot == null) {
-                continue;
-            }
-            if (lot.region() == null || lot.region().isBlank()) {
-                continue;
-            }
-            if (lot.quantity() < 0) {
-                continue;
-            }
-            if (balances.containsKey(lot.region())) {
-                final int existing = balances.get(lot.region());
-                balances.put(lot.region(), existing + lot.quantity());
-            } else {
-                balances.put(lot.region(), lot.quantity());
-            }
-        }
+        lots.stream()
+                .filter(Objects::nonNull)
+                .filter(lot -> lot.region() != null && !lot.region().isBlank())
+                .filter(lot -> lot.quantity() >= 0)
+                .forEach(lot -> balances.merge(lot.region(), lot.quantity(), Integer::sum));
         return balances;
     }
 
-    public List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
-        final List<String> overstocked = new ArrayList<>();
-        for (final Map.Entry<String, Integer> entry : balances.entrySet()) {
-            if (entry.getValue() > threshold) {
-                overstocked.add(entry.getKey());
-            }
-        }
-        return overstocked;
+    public static List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
+        return balances.entrySet().stream()
+                .filter(entry -> entry.getValue() > threshold)
+                .map(Entry::getKey)
+                .toList();
     }
 
-    public int totalAcrossRegions(final Map<String, Integer> balances) {
-        int sum = 0;
-        for (final Integer value : balances.values()) {
-            if (value != null) {
-                sum = sum + value;
-            }
-        }
-        return sum;
+    public static int totalAcrossRegions(final Map<String, Integer> balances) {
+        return balances.values().stream()
+                .filter(Objects::nonNull)
+                .mapToInt(Integer::intValue)
+                .sum();
     }
 
     public record Lot(String region, String sku, int quantity) {}
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
index 04846e7..8fbac37 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
@@ -2,49 +2,35 @@ package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.ArrayList;
 import java.util.List;
+import java.util.Set;
+import java.util.stream.IntStream;
 
-/**
- * Stylistic-heavy fixture. dispatch switches on a channel-kind string
- * via an if/else-if ladder (G23 — polymorphism would be cleaner) and
- * takes a boolean "urgent" flag that selects an entirely different
- * code path per branch (F3 — split into two methods). Also a G25
- * opportunity on the retry-count literal.
- */
 public final class NotificationDispatcher {
 
+    private static final Set<String> RETRYING_CHANNELS = Set.of("email", "sms", "push");
+    private static final String WEBHOOK = "webhook";
+    private static final int URGENT_RETRIES = 3;
+
     private final List<String> dispatched = new ArrayList<>();
 
-    public void dispatch(final String channel, final String recipient, final String body,
-                         final boolean urgent) {
-        if (channel.equals("email")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("email(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("email -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("sms")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("sms(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("sms -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("push")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("push(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("push -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("webhook")) {
-            dispatched.add("webhook -> " + recipient + ": " + body);
-        } else {
-            throw new IllegalArgumentException("unknown channel: " + channel);
+    public void dispatch(final String channel, final String recipient, final String body) {
+        requireKnownChannel(channel);
+        dispatched.add(format(channel, recipient, body, false));
+    }
+
+    public void dispatchUrgent(final String channel, final String recipient, final String body) {
+        requireKnownChannel(channel);
+        dispatched.addAll(urgentMessages(channel, recipient, body));
+    }
+
+    private static List<String> urgentMessages(final String channel, final String recipient,
+                                                final String body) {
+        if (WEBHOOK.equals(channel)) {
+            return List.of(format(channel, recipient, body, false));
         }
+        return IntStream.range(0, URGENT_RETRIES)
+                .mapToObj(attempt -> format(channel, recipient, body, true))
+                .toList();
     }
 
     public int dispatchedCount() {
@@ -54,4 +40,17 @@ public final class NotificationDispatcher {
     public List<String> dispatched() {
         return List.copyOf(dispatched);
     }
+
+    private static String format(final String channel, final String recipient, final String body,
+                                 final boolean urgent) {
+        final String suffix = urgent ? "(urgent)" : "";
+        final String header = channel + suffix;
+        return header + " -> " + recipient + ": " + body;
+    }
+
+    private static void requireKnownChannel(final String channel) {
+        if (!RETRYING_CHANNELS.contains(channel) && !WEBHOOK.equals(channel)) {
+            throw new IllegalArgumentException("unknown channel: " + channel);
+        }
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
index 1eef21e..594fee7 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
@@ -1,69 +1,73 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
+import java.util.HashMap;
+import java.util.LinkedHashMap;
+import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
+import java.util.stream.Collectors;
 
-/**
- * G30-heavy fixture: a single orchestrator method strung together from
- * six distinct phases. Each phase is 8-10 lines, has a clear name in the
- * comments, and does not cross-reference other phases — i.e. the
- * textbook shape for ExtractMethodRecipe. No F2/G18/G12/G31 noise;
- * extraction is the obvious right fix.
- */
 public final class OrchestratorFixture {
 
-    public void settleOrders(final List<String> orderIds, final Map<String, Integer> stock,
-                             final Map<String, Double> prices, final StringBuilder audit) {
-        // Phase 1: validate input collections.
+    private static final double RESERVATION_DISCOUNT = 0.95;
+    private static final String SEPARATOR = "---\n";
+
+    public static Map<String, Integer> settleOrders(final List<String> orderIds,
+                                                    final Map<String, Integer> stock,
+                                                    final Map<String, Double> prices,
+                                                    final StringBuilder audit) {
         if (orderIds == null || orderIds.isEmpty()) {
             audit.append("no orders to settle\n");
-            return;
+            return stock == null ? new HashMap<>() : new HashMap<>(stock);
         }
         if (stock == null || prices == null) {
             audit.append("missing reference data\n");
-            return;
-        }
-        // Phase 2: dedupe the incoming ids into a clean working list.
-        final java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
-        for (final String id : orderIds) {
-            if (id != null && !id.isBlank()) {
-                unique.add(id.trim());
-            }
+            return new HashMap<>();
         }
+
+        final LinkedHashSet<String> unique = orderIds.stream()
+                .filter(id -> id != null && !id.isBlank())
+                .map(String::trim)
+                .collect(Collectors.toCollection(LinkedHashSet::new));
         audit.append("unique orders: ").append(unique.size()).append("\n");
-        // Phase 3: apply a flat reservation discount to each line price.
-        final java.util.LinkedHashMap<String, Double> adjusted = new java.util.LinkedHashMap<>();
-        for (final String id : unique) {
-            final Double base = prices.get(id);
-            if (base == null) {
-                audit.append("no price for ").append(id).append(" — skipping\n");
-                continue;
-            }
-            adjusted.put(id, base * 0.95);
-        }
-        // Phase 4: decrement the stock counter for every priced order.
+
+        final LinkedHashMap<String, Double> adjusted = adjustedPrices(unique, prices);
+        unique.stream()
+                .filter(id -> !adjusted.containsKey(id))
+                .forEach(id -> audit.append("no price for ").append(id).append(" — skipping\n"));
+
+        final Map<String, Integer> remainingStock = new HashMap<>(stock);
         int fulfilled = 0;
         for (final String id : adjusted.keySet()) {
-            final Integer available = stock.get(id);
-            if (available == null || available <= 0) {
+            final int available = remainingStock.getOrDefault(id, 0);
+            if (available <= 0) {
                 audit.append("out of stock: ").append(id).append("\n");
                 continue;
             }
-            stock.put(id, available - 1);
+            remainingStock.put(id, available - 1);
             fulfilled++;
         }
         audit.append("fulfilled: ").append(fulfilled).append("\n");
-        // Phase 5: compute the total revenue from the fulfilled orders.
-        double revenue = 0.0;
-        for (final Map.Entry<String, Double> entry : adjusted.entrySet()) {
-            final Integer available = stock.get(entry.getKey());
-            if (available != null) {
-                revenue += entry.getValue();
+
+        final double revenue = adjusted.entrySet().stream()
+                .filter(entry -> remainingStock.containsKey(entry.getKey()))
+                .mapToDouble(Map.Entry::getValue)
+                .sum();
+        audit.append("revenue: ").append(String.format("%.2f", revenue)).append("\n")
+                .append("settlement complete\n")
+                .append(SEPARATOR);
+        return remainingStock;
+    }
+
+    private static LinkedHashMap<String, Double> adjustedPrices(final LinkedHashSet<String> unique,
+                                                                final Map<String, Double> prices) {
+        final LinkedHashMap<String, Double> adjusted = new LinkedHashMap<>();
+        for (final String id : unique) {
+            final Double base = prices.get(id);
+            if (base != null) {
+                adjusted.put(id, base * RESERVATION_DISCOUNT);
             }
         }
-        audit.append("revenue: ").append(String.format("%.2f", revenue)).append("\n");
-        // Phase 6: sign off.
-        audit.append("settlement complete\n");
-        audit.append("---\n");
+        return adjusted;
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
index c6108b6..8f2458b 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
@@ -3,51 +3,40 @@ package org.fiftieshousewife.cleancode.sandbox;
 import java.util.List;
 import java.util.Map;
 
-/**
- * StringBuilder-threading fixture. The private helpers take a
- * StringBuilder named `sb` and mutate it — the F2 + N1 pattern
- * identified in the manual-1 audit as endemic. The renderReport
- * method is also long enough to benefit from phase extraction (G30).
- * HTML literal is deliberately inline so the agent sees a G1
- * (multiple-languages) hint as well.
- */
 public final class ReportTemplate {
 
-    public String renderReport(final String title, final List<String> sections,
-                               final Map<String, Integer> totals) {
-        final StringBuilder sb = new StringBuilder();
-        sb.append("<html><head><title>").append(title).append("</title></head><body>");
-        sb.append("<h1>").append(title).append("</h1>");
-        sb.append("<p>generated report with ").append(sections.size()).append(" sections</p>");
-
-        sb.append("<div class=\"sections\">");
-        for (final String section : sections) {
-            appendSection(sb, section);
-        }
-        sb.append("</div>");
-
-        sb.append("<div class=\"totals\">");
-        sb.append("<h2>Totals</h2>");
-        sb.append("<ul>");
-        for (final Map.Entry<String, Integer> entry : totals.entrySet()) {
-            appendTotalRow(sb, entry.getKey(), entry.getValue());
-        }
-        sb.append("</ul>");
-        sb.append("</div>");
-
-        sb.append("</body></html>");
-        return sb.toString();
+    public static String renderReport(final String title, final List<String> sections,
+                                      final Map<String, Integer> totals) {
+        final StringBuilder out = new StringBuilder()
+                .append("<html><head><title>").append(title).append("</title></head><body>")
+                .append("<h1>").append(title).append("</h1>")
+                .append("<p>generated report with ").append(sections.size()).append(" sections</p>")
+                .append("<div class=\"sections\">");
+        sections.forEach(section -> out.append(renderSection(section)));
+        out.append("</div>")
+                .append("<div class=\"totals\">")
+                .append("<h2>Totals</h2>")
+                .append("<ul>");
+        totals.forEach((key, value) -> out.append(renderTotalRow(key, value)));
+        out.append("</ul>")
+                .append("</div>")
+                .append("</body></html>");
+        return out.toString();
     }
 
-    private void appendSection(final StringBuilder sb, final String section) {
-        sb.append("<section>");
-        sb.append("<h2>").append(section).append("</h2>");
-        sb.append("<p>content for ").append(section).append("</p>");
-        sb.append("</section>");
+    private static String renderSection(final String section) {
+        return new StringBuilder()
+                .append("<section>")
+                .append("<h2>").append(section).append("</h2>")
+                .append("<p>content for ").append(section).append("</p>")
+                .append("</section>")
+                .toString();
     }
 
-    private void appendTotalRow(final StringBuilder sb, final String key, final int value) {
-        sb.append("<li><span class=\"key\">").append(key).append("</span>");
-        sb.append("<span class=\"value\">").append(value).append("</span></li>");
+    private static String renderTotalRow(final String key, final int value) {
+        return new StringBuilder()
+                .append("<li><span class=\"key\">").append(key).append("</span>")
+                .append("<span class=\"value\">").append(value).append("</span></li>")
+                .toString();
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
index de682a6..340754c 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
@@ -4,56 +4,40 @@ import java.time.Instant;
 import java.util.HashMap;
 import java.util.Map;
 
-/**
- * Session cache with expiry. Exhibits the null-return pattern that
- * Ch7.2 targets, negative-conditional nesting (G29), and a lookup
- * method whose core predicate would be clearer as an explanatory
- * variable (G19). The expiry arithmetic is also a G25 candidate
- * (hardcoded 30L * 60L * 1000L).
- */
 public final class SessionStore {
 
+    private static final long SESSION_TTL_MS = 30L * 60L * 1000L;
+
     private final Map<String, Session> sessions = new HashMap<>();
 
     public String userIdFor(final String token) {
-        if (token == null) {
-            return null;
-        }
-        if (token.isBlank()) {
+        if (token == null || token.isBlank()) {
             return null;
         }
         final Session session = sessions.get(token);
-        if (session == null) {
-            return null;
-        }
-        if (!session.active) {
-            return null;
-        }
-        if (Instant.now().toEpochMilli() - session.createdAtMs > 30L * 60L * 1000L) {
+        if (!isUsable(session)) {
             return null;
         }
         return session.userId;
     }
 
+    private static boolean isUsable(final Session session) {
+        return session != null && session.active && !isExpired(session);
+    }
+
     public Session lookupOrNull(final String token) {
-        if (!sessions.containsKey(token)) {
-            return null;
-        }
         final Session session = sessions.get(token);
-        if (!session.active) {
+        if (session == null || !session.active) {
             return null;
         }
         return session;
     }
 
     public void open(final String token, final String userId) {
-        if (token == null || token.isBlank()) {
-            throw new IllegalArgumentException("token is required");
-        }
-        if (userId == null || userId.isBlank()) {
-            throw new IllegalArgumentException("userId is required");
-        }
-        sessions.put(token, new Session(userId, Instant.now().toEpochMilli(), true));
+        final String validToken = requireNonBlank(token, "token");
+        final String validUserId = requireNonBlank(userId, "userId");
+        final Session session = new Session(validUserId, Instant.now().toEpochMilli(), true);
+        sessions.put(validToken, session);
     }
 
     public void close(final String token) {
@@ -65,13 +49,20 @@ public final class SessionStore {
     }
 
     public int activeSessionCount() {
-        int count = 0;
-        for (final Session session : sessions.values()) {
-            if (session.active) {
-                count = count + 1;
-            }
+        return (int) sessions.values().stream()
+                .filter(session -> session.active)
+                .count();
+    }
+
+    private static boolean isExpired(final Session session) {
+        return Instant.now().toEpochMilli() - session.createdAtMs > SESSION_TTL_MS;
+    }
+
+    private static String requireNonBlank(final String value, final String name) {
+        if (value == null || value.isBlank()) {
+            throw new IllegalArgumentException(name + " is required");
         }
-        return count;
+        return value;
     }
 
     public static final class Session {
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
index 8a24359..6d6f0bb 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
@@ -5,69 +5,66 @@ import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
-/**
- * Realistic service-layer fixture. Creating an account runs four phases
- * in one big method — validate, persist, audit, notify — which triggers
- * G30 (too long), and the validator threads errors into a caller-owned
- * list (F2 — output argument). The persistence path swallows every
- * checked exception, logging and returning a partial result (Ch7.1).
- */
 public final class UserAccountService {
 
+    private static final int DISPLAY_NAME_MAX_LENGTH = 64;
+    private static final List<String> VALID_REGIONS = List.of("US", "EU", "APAC");
+
+    private enum ValidationError {
+        EMAIL_REQUIRED("email is required"),
+        EMAIL_MUST_CONTAIN_AT("email must contain @"),
+        DISPLAY_NAME_REQUIRED("displayName is required"),
+        DISPLAY_NAME_TOO_LONG("displayName too long"),
+        REGION_INVALID("region must be one of US/EU/APAC");
+
+        private final String message;
+
+        ValidationError(final String message) {
+            this.message = message;
+        }
+
+        String message() {
+            return message;
+        }
+    }
+
     private final Map<String, User> byId = new HashMap<>();
     private final List<String> audit = new ArrayList<>();
     private long nextId = 1000L;
 
     public String createAccount(final String email, final String displayName, final String region) {
-        final List<String> errors = new ArrayList<>();
-        if (email == null || email.isBlank()) {
-            errors.add("email is required");
-        }
-        if (email != null && !email.contains("@")) {
-            errors.add("email must contain @");
-        }
-        if (displayName == null || displayName.isBlank()) {
-            errors.add("displayName is required");
-        }
-        if (displayName != null && displayName.length() > 64) {
-            errors.add("displayName too long");
-        }
-        if (region == null || !List.of("US", "EU", "APAC").contains(region)) {
-            errors.add("region must be one of US/EU/APAC");
-        }
+        final List<String> errors = validate(email, displayName, region);
         if (!errors.isEmpty()) {
             throw new IllegalArgumentException(String.join("; ", errors));
         }
-
         final String id = "acct-" + nextId;
         nextId = nextId + 1;
-        final User user = new User(id, email, displayName, region);
-        try {
-            byId.put(id, user);
-        } catch (RuntimeException e) {
-            System.err.println("persist failed: " + e.getMessage());
-            return null;
-        }
-
-        audit.add("created " + id + " in region " + region);
-        audit.add("email=" + email + " displayName=" + displayName);
-        audit.add("total accounts: " + byId.size());
-
-        try {
-            sendWelcomeEmail(email, displayName);
-        } catch (Exception e) {
-            System.err.println("notify failed: " + e.getMessage());
-        }
+        byId.put(id, new User(id, email, displayName, region));
+        audit.addAll(List.of(
+                "created " + id + " in region " + region,
+                "email=" + email + " displayName=" + displayName,
+                "total accounts: " + byId.size()
+        ));
+        sendWelcomeEmail(email, displayName);
         return id;
     }
 
-    void validate(final String email, final String displayName, final List<String> errors) {
+    static List<String> validate(final String email, final String displayName, final String region) {
+        final List<String> errors = new ArrayList<>();
         if (email == null || email.isBlank()) {
-            errors.add("email is required");
+            errors.add(ValidationError.EMAIL_REQUIRED.message());
+        } else if (!email.contains("@")) {
+            errors.add(ValidationError.EMAIL_MUST_CONTAIN_AT.message());
         }
         if (displayName == null || displayName.isBlank()) {
-            errors.add("displayName is required");
+            errors.add(ValidationError.DISPLAY_NAME_REQUIRED.message());
+        } else if (displayName.length() > DISPLAY_NAME_MAX_LENGTH) {
+            errors.add(ValidationError.DISPLAY_NAME_TOO_LONG.message());
+        }
+        if (region == null || !VALID_REGIONS.contains(region)) {
+            errors.add(ValidationError.REGION_INVALID.message());
         }
+        return errors;
     }
 
     private void sendWelcomeEmail(final String email, final String displayName) {

```

## mcp gradle only — commit message body

## Actions
- Edit() — Made computeScore static (G18), collapsed the two null/negative continues into one guard (G29), and extracted SCORE_CEILING.
- Edit() — Extracted SEPARATOR constant (G25), made handle static (G18), and pulled the null/blank predicate into a value-returning isInvalid helper (no StringBuilder threading).
- Edit() — Made settleOrders static, returning a Settlement record instead of mutating stock (F2); split six phases into named helpers that return values (G30/G34/G31), added imports (G12), and replaced null-control-flow with Map.getOrDefault/containsKey + streams (Ch7.2/G30).
- Edit() — Extracted pure parsing into static splitRow + isEscapedQuote helpers (G28/G18), moved fields declaration next to first use (G10), kept rowsParsed++ on the instance method.
- Edit() — Extracted ATTEMPT_PREFIX + backoff constants (G25/G12), inlined delayMs via backoffDelay helper (G10), replaced System.err with audit append and restored interrupt status (G4/Ch7.1).
- Edit() — Converted all three methods to static stream pipelines (G18/G30), extracted hasUsableRegion (G29), and imported Map.Entry to drop the inline qualifier (G12).
- Edit() — Split dispatch/dispatchUrgent (F3/G15), replaced the if/else-if ladder with a Channel enum (G23), and used Collections.nCopies for urgent retries (G30/G25).
- Edit() — Replaced StringBuilder-threaded helpers with pure string-returning phase methods (F2/G31), collapsed consecutive appends via Collectors.joining, imported Map.Entry (G12).
- Edit() — Converted Session to a record, replaced guard ladders with Optional chains + computeIfPresent (G29/Ch7.2), extracted SESSION_TTL_MS and requireNonBlank (G25/G18).
- Edit() — Replaced output-arg validate with per-field *Errors() helpers returning List<String> (F2), extracted EMAIL_REQUIRED/DISPLAY_NAME_REQUIRED/VALID_REGIONS (G25), removed catch-log-continue and System.err blocks (Ch7.1/G4), collapsed audit appends into one addAll (G31).
- Edit(extracted=[rejectionReason, buildAudit]) — Extract audit building into helper and collapse guards into an Optional-returning filter to fix G30 length and G29 duplicate guards.
- Edit(reordered_branches=true, named_vars=[hasNextChar, nextCharIsQuote]) — Reorder the separator branch first so fields.add sits next to the declaration (G10) and extract explanatory variables inside isEscapedQuote (G19).
- Edit(extracted=[buildUrgentLine, copyCount], renamed_ctor_params=true) — Extract line-building and copy-count into static helpers (G18) and rename Channel ctor params to drop this.wire/this.supportsUrgent (G12).
- Edit(named_vars=[pageHead, mainHeading, description, heading, content, keyCell, valueCell], constants=[TOTALS_OPEN, TOTALS_CLOSE]) — Extract explanatory variables in header/renderSection/renderTotalRow (G19) and pull totalsBlock HTML into named constants (G1).
- Edit(requireNonBlank_returns_value=true) — Return the validated value from requireNonBlank so open() expresses data flow instead of three independent void calls (G31).
- Edit(validate_made_static=true, collapsed_guards_with_ternary=[emailErrors, displayNameErrors]) — Make validate static since it uses no instance state (G18) and collapse the second guard in emailErrors/displayNameErrors to a ternary (G29).

## Suggestions
- G18 at L-1: Method 'computeScore' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'computeScore' has 2 guard clauses — simplify with early return or extract filter
- G25 at L-1: String "---
" appears 2 times — extract to a named constant
- G18 at L-1: Method 'handle' does not use instance state — consider making it static or extracting
- F2 at L-1: Method 'settleOrders' mutates its argument 'stock' (java.util.Map<java.lang.String, java.lang.Integer>) — return the result instead
- Ch7_2 at L-1: Method 'settleOrders' has 7 null checks
- G34 at L-1: Method 'settleOrders' has 6 section comments
- G30 at L-1: Loop in 'settleOrders' (for-each with filter-add) can be replaced with a stream operation
- G18 at L-1: Method 'settleOrders' does not use instance state — consider making it static or extracting
- G31 at L-1: Method 'settleOrders' has 3 consecutive void calls with no data dependency — make the order explicit
- G12 at L0: 5 inline fully-qualified type reference(s); first: java.util.LinkedHashSet — run ShortenFullyQualifiedReferencesRecipe
- G28 at L-1: Complex condition (depth 2) should be extracted
- G10 at L3: 'fields' is declared in parseRow() but not used until 20 lines later — move the declaration closer to line 23
- G30 at L-1: Method 'parseRow' has 6 blank-line sections across 41 lines — each section should be its own method
- G18 at L-1: Method 'parseRow' does not use instance state — consider making it static or extracting
- G10 at L4: 'delayMs' is declared in execute() but not used until 17 lines later — move the declaration closer to line 21
- G25 at L-1: String "attempt " appears 2 times — extract to a named constant
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G18 at L-1: Method 'execute' does not use instance state — consider making it static or extracting
- Ch7_1 at L-1: Method 'execute' catches Exception — catch specific exception types instead
- G12 at L0: 1 inline fully-qualified type reference(s); first: System.err — run ShortenFullyQualifiedReferencesRecipe
- G10 at L3: 'balances' is declared in balanceByRegion() but not used until 11 lines later — move the declaration closer to line 14
- G30 at L-1: Loop in 'overstockedRegions' (for-each with filter-add) can be replaced with a stream operation
- G18 at L-1: Method 'balanceByRegion' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'overstockedRegions' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'totalAcrossRegions' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'balanceByRegion' has 3 guard clauses — simplify with early return or extract filter
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- F3 at L-1: Method 'dispatch' takes boolean parameter 'urgent' — split into two methods instead
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G23 at L-1: Method 'dispatch' dispatches on String parameter 'channel' with 4 branches — use an enum or split into separate methods
- G15 at L-1: Method 'dispatch' uses boolean parameter 'urgent' to select behaviour — split into separate methods
- G31 at L-1: Method 'renderReport' has 4 consecutive void calls with no data dependency — make the order explicit
- G31 at L-1: Method 'appendSection' has 4 consecutive void calls with no data dependency — make the order explicit
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- G18 at L-1: Method 'userIdFor' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'lookupOrNull' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'open' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'close' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'activeSessionCount' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'userIdFor' has 5 guard clauses — simplify with early return or extract filter
- G29 at L-1: Method 'lookupOrNull' has 2 guard clauses — simplify with early return or extract filter
- G29 at L-1: Method 'open' has 2 guard clauses — simplify with early return or extract filter
- G12 at L0: 3 inline fully-qualified type reference(s); first: session.userId — run ShortenFullyQualifiedReferencesRecipe
- F2 at L-1: Method 'validate' mutates its argument 'errors' (java.util.List<java.lang.String>) — return the result instead
- Ch7_1 at L-1: Catch block in 'createAccount' only logs or is empty
- Ch7_2 at L-1: Method 'createAccount' has 5 null checks
- G25 at L-1: String "displayName is required" appears 2 times — extract to a named constant
- G25 at L-1: String "email is required" appears 2 times — extract to a named constant
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G31 at L-1: Method 'createAccount' has 3 consecutive void calls with no data dependency — make the order explicit
- Ch7_1 at L-1: Method 'createAccount' catches Exception — catch specific exception types instead
- F2 at L-1: Class has 1 methods returning collections and 1 void methods mutating collection params — pick one style
- G12 at L0: 2 inline fully-qualified type reference(s); first: System.err — run ShortenFullyQualifiedReferencesRecipe

## Agent usage
- input tokens : 86
- output tokens: 82428
- cache read   : 3705439
- cost (USD)   : 5.4021

## mcp gradle only — diff

```diff
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
index 6567aad..03c019e 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
@@ -12,20 +12,19 @@ import java.util.List;
  */
 public final class AccumulatorFixture {
 
-    public int computeScore(final List<Integer> values) {
+    private static final int SCORE_CEILING = 10000;
+
+    private AccumulatorFixture() {
+    }
+
+    public static int computeScore(final List<Integer> values) {
         int total = 0;
         for (final Integer value : values) {
-            if (value == null) {
-                continue;
-            }
-            if (value < 0) {
+            if (value == null || value < 0) {
                 continue;
             }
             total = total + value;
         }
-        if (total > 10000) {
-            total = 10000;
-        }
-        return total;
+        return Math.min(total, SCORE_CEILING);
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
index 63f8a43..23d363e 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
@@ -26,6 +26,12 @@ public final class CsvParser {
     }
 
     public List<String> parseRow(final String line) {
+        final List<String> fields = splitRow(line, separator, quote);
+        rowsParsed = rowsParsed + 1;
+        return fields;
+    }
+
+    static List<String> splitRow(final String line, final char separator, final char quote) {
         final List<String> fields = new ArrayList<>();
         final StringBuilder current = new StringBuilder();
         boolean insideQuotes = false;
@@ -34,8 +40,15 @@ public final class CsvParser {
         while (index < line.length()) {
             final char ch = line.charAt(index);
 
+            if (ch == separator && !insideQuotes) {
+                fields.add(current.toString());
+                current.setLength(0);
+                index = index + 1;
+                continue;
+            }
+
             if (ch == quote) {
-                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == quote) {
+                if (isEscapedQuote(line, index, insideQuotes, quote)) {
                     current.append(quote);
                     index = index + 2;
                     continue;
@@ -45,13 +58,6 @@ public final class CsvParser {
                 continue;
             }
 
-            if (ch == separator && !insideQuotes) {
-                fields.add(current.toString());
-                current.setLength(0);
-                index = index + 1;
-                continue;
-            }
-
             if (ch == '\\' && index + 1 < line.length()) {
                 current.append(line.charAt(index + 1));
                 index = index + 2;
@@ -63,10 +69,16 @@ public final class CsvParser {
         }
 
         fields.add(current.toString());
-        rowsParsed = rowsParsed + 1;
         return fields;
     }
 
+    private static boolean isEscapedQuote(final String line, final int index,
+                                          final boolean insideQuotes, final char quote) {
+        final boolean hasNextChar = index + 1 < line.length();
+        final boolean nextCharIsQuote = hasNextChar && line.charAt(index + 1) == quote;
+        return insideQuotes && nextCharIsQuote;
+    }
+
     public int rowsParsed() {
         return rowsParsed;
     }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
index 43f0728..1688d38 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
@@ -10,17 +10,24 @@ package org.fiftieshousewife.cleancode.sandbox;
  */
 public final class GuardFixture {
 
-    public void handle(final String event, final StringBuilder log) {
-        if (event == null) {
-            log.append("drop: null event\n");
-            log.append("---\n");
-            return;
-        }
-        if (event.isBlank()) {
-            log.append("drop: blank event\n");
-            log.append("---\n");
+    private static final String SEPARATOR = "---\n";
+
+    private GuardFixture() {
+    }
+
+    public static void handle(final String event, final StringBuilder log) {
+        if (isInvalid(event)) {
+            log.append(dropReason(event)).append(SEPARATOR);
             return;
         }
         log.append("processed: ").append(event.strip()).append('\n');
     }
+
+    private static boolean isInvalid(final String event) {
+        return event == null || event.isBlank();
+    }
+
+    private static String dropReason(final String event) {
+        return event == null ? "drop: null event\n" : "drop: blank event\n";
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
index fb1c60a..cc23667 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
@@ -11,36 +11,39 @@ import java.util.concurrent.Callable;
  */
 public final class HttpRetryPolicy {
 
+    private static final String ATTEMPT_PREFIX = "attempt ";
+    private static final int MAX_ATTEMPTS = 5;
+    private static final long INITIAL_DELAY_MS = 500L;
+    private static final long MAX_DELAY_MS = 10_000L;
+    private static final double BACKOFF_FACTOR = 1.5;
+
     private final StringBuilder audit = new StringBuilder();
 
     public <T> T execute(final Callable<T> action) {
         int attempt = 0;
-        long delayMs = 500;
         Throwable lastFailure = null;
-        while (attempt < 5) {
+        while (attempt < MAX_ATTEMPTS) {
             try {
                 final T result = action.call();
-                audit.append("attempt ").append(attempt).append(" ok\n");
+                audit.append(ATTEMPT_PREFIX).append(attempt).append(" ok\n");
                 return result;
             } catch (Exception e) {
                 lastFailure = e;
-                audit.append("attempt ").append(attempt).append(" failed: ")
+                audit.append(ATTEMPT_PREFIX).append(attempt).append(" failed: ")
                         .append(e.getMessage()).append('\n');
             }
 
-            if (attempt >= 4) {
+            if (attempt >= MAX_ATTEMPTS - 1) {
                 break;
             }
+            final long delayMs = backoffDelay(attempt);
             try {
                 Thread.sleep(delayMs);
             } catch (InterruptedException interrupted) {
-                System.err.println("retry interrupted: " + interrupted.getMessage());
+                Thread.currentThread().interrupt();
+                audit.append("retry interrupted: ").append(interrupted.getMessage()).append('\n');
                 return null;
             }
-            delayMs = (long) (delayMs * 1.5);
-            if (delayMs > 10000) {
-                delayMs = 10000;
-            }
             attempt = attempt + 1;
         }
 
@@ -51,6 +54,11 @@ public final class HttpRetryPolicy {
         return null;
     }
 
+    private static long backoffDelay(final int attempt) {
+        final long delay = (long) (INITIAL_DELAY_MS * Math.pow(BACKOFF_FACTOR, attempt));
+        return Math.min(delay, MAX_DELAY_MS);
+    }
+
     public String auditLog() {
         return audit.toString();
     }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
index e972d68..97bf060 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
@@ -1,9 +1,10 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
-import java.util.ArrayList;
-import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
+import java.util.Map.Entry;
+import java.util.Objects;
+import java.util.stream.Collectors;
 
 /**
  * Imperative-loop aggregator — classic candidate for stream conversion
@@ -14,46 +15,33 @@ import java.util.Map;
  */
 public final class InventoryBalancer {
 
-    public Map<String, Integer> balanceByRegion(final List<Lot> lots) {
-        final Map<String, Integer> balances = new HashMap<>();
-        for (final Lot lot : lots) {
-            if (lot == null) {
-                continue;
-            }
-            if (lot.region() == null || lot.region().isBlank()) {
-                continue;
-            }
-            if (lot.quantity() < 0) {
-                continue;
-            }
-            if (balances.containsKey(lot.region())) {
-                final int existing = balances.get(lot.region());
-                balances.put(lot.region(), existing + lot.quantity());
-            } else {
-                balances.put(lot.region(), lot.quantity());
-            }
-        }
-        return balances;
+    private InventoryBalancer() {
     }
 
-    public List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
-        final List<String> overstocked = new ArrayList<>();
-        for (final Map.Entry<String, Integer> entry : balances.entrySet()) {
-            if (entry.getValue() > threshold) {
-                overstocked.add(entry.getKey());
-            }
-        }
-        return overstocked;
+    public static Map<String, Integer> balanceByRegion(final List<Lot> lots) {
+        return lots.stream()
+                .filter(Objects::nonNull)
+                .filter(InventoryBalancer::hasUsableRegion)
+                .filter(lot -> lot.quantity() >= 0)
+                .collect(Collectors.toMap(Lot::region, Lot::quantity, Integer::sum));
     }
 
-    public int totalAcrossRegions(final Map<String, Integer> balances) {
-        int sum = 0;
-        for (final Integer value : balances.values()) {
-            if (value != null) {
-                sum = sum + value;
-            }
-        }
-        return sum;
+    private static boolean hasUsableRegion(final Lot lot) {
+        return lot.region() != null && !lot.region().isBlank();
+    }
+
+    public static List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
+        return balances.entrySet().stream()
+                .filter(entry -> entry.getValue() > threshold)
+                .map(Entry::getKey)
+                .toList();
+    }
+
+    public static int totalAcrossRegions(final Map<String, Integer> balances) {
+        return balances.values().stream()
+                .filter(Objects::nonNull)
+                .mapToInt(Integer::intValue)
+                .sum();
     }
 
     public record Lot(String region, String sku, int quantity) {}
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
index 04846e7..ff09013 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
@@ -1,6 +1,8 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.ArrayList;
+import java.util.Arrays;
+import java.util.Collections;
 import java.util.List;
 
 /**
@@ -12,39 +14,30 @@ import java.util.List;
  */
 public final class NotificationDispatcher {
 
+    private static final int URGENT_RETRIES = 3;
+
     private final List<String> dispatched = new ArrayList<>();
 
-    public void dispatch(final String channel, final String recipient, final String body,
-                         final boolean urgent) {
-        if (channel.equals("email")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("email(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("email -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("sms")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("sms(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("sms -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("push")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("push(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("push -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("webhook")) {
-            dispatched.add("webhook -> " + recipient + ": " + body);
-        } else {
-            throw new IllegalArgumentException("unknown channel: " + channel);
-        }
+    public void dispatch(final String channel, final String recipient, final String body) {
+        final Channel selected = Channel.of(channel);
+        dispatched.add(selected.wire() + " -> " + recipient + ": " + body);
+    }
+
+    public void dispatchUrgent(final String channel, final String recipient, final String body) {
+        final Channel selected = Channel.of(channel);
+        final String line = buildUrgentLine(selected, recipient, body);
+        final int copies = copyCount(selected);
+        dispatched.addAll(Collections.nCopies(copies, line));
+    }
+
+    private static String buildUrgentLine(final Channel channel, final String recipient, final String body) {
+        return channel.supportsUrgent()
+                ? channel.wire() + "(urgent) -> " + recipient + ": " + body
+                : channel.wire() + " -> " + recipient + ": " + body;
+    }
+
+    private static int copyCount(final Channel channel) {
+        return channel.supportsUrgent() ? URGENT_RETRIES : 1;
     }
 
     public int dispatchedCount() {
@@ -54,4 +47,34 @@ public final class NotificationDispatcher {
     public List<String> dispatched() {
         return List.copyOf(dispatched);
     }
+
+    public enum Channel {
+        EMAIL("email", true),
+        SMS("sms", true),
+        PUSH("push", true),
+        WEBHOOK("webhook", false);
+
+        private final String wire;
+        private final boolean supportsUrgent;
+
+        Channel(final String wireName, final boolean urgentCapable) {
+            wire = wireName;
+            supportsUrgent = urgentCapable;
+        }
+
+        public String wire() {
+            return wire;
+        }
+
+        public boolean supportsUrgent() {
+            return supportsUrgent;
+        }
+
+        public static Channel of(final String value) {
+            return Arrays.stream(values())
+                    .filter(channel -> channel.wire.equals(value))
+                    .findFirst()
+                    .orElseThrow(() -> new IllegalArgumentException("unknown channel: " + value));
+        }
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
index 1eef21e..1fd375e 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
@@ -1,7 +1,14 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
+import java.util.HashMap;
+import java.util.LinkedHashMap;
+import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
+import java.util.Objects;
+import java.util.Optional;
+import java.util.Set;
+import java.util.stream.Collectors;
 
 /**
  * G30-heavy fixture: a single orchestrator method strung together from
@@ -12,58 +19,110 @@ import java.util.Map;
  */
 public final class OrchestratorFixture {
 
-    public void settleOrders(final List<String> orderIds, final Map<String, Integer> stock,
-                             final Map<String, Double> prices, final StringBuilder audit) {
-        // Phase 1: validate input collections.
+    private static final String SEPARATOR = "---\n";
+    private static final double RESERVATION_DISCOUNT = 0.95;
+
+    private OrchestratorFixture() {
+    }
+
+    public static Settlement settleOrders(final List<String> orderIds,
+                                          final Map<String, Integer> stock,
+                                          final Map<String, Double> prices) {
+        final Optional<String> rejection = rejectionReason(orderIds, stock, prices);
+        if (rejection.isPresent()) {
+            return Settlement.rejected(stock, rejection.get());
+        }
+
+        final LinkedHashSet<String> unique = dedupe(orderIds);
+        final LinkedHashMap<String, Double> adjusted = applyDiscount(unique, prices);
+        final StockUpdate update = decrementStock(adjusted.keySet(), stock);
+        final double revenue = totalRevenue(adjusted, update.newStock());
+        final String audit = buildAudit(unique, adjusted, update, revenue);
+        return new Settlement(update.newStock(), audit);
+    }
+
+    private static Optional<String> rejectionReason(final List<String> orderIds,
+                                                    final Map<String, Integer> stock,
+                                                    final Map<String, Double> prices) {
         if (orderIds == null || orderIds.isEmpty()) {
-            audit.append("no orders to settle\n");
-            return;
+            return Optional.of("no orders to settle\n");
         }
         if (stock == null || prices == null) {
-            audit.append("missing reference data\n");
-            return;
+            return Optional.of("missing reference data\n");
         }
-        // Phase 2: dedupe the incoming ids into a clean working list.
-        final java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
-        for (final String id : orderIds) {
-            if (id != null && !id.isBlank()) {
-                unique.add(id.trim());
-            }
-        }
-        audit.append("unique orders: ").append(unique.size()).append("\n");
-        // Phase 3: apply a flat reservation discount to each line price.
-        final java.util.LinkedHashMap<String, Double> adjusted = new java.util.LinkedHashMap<>();
-        for (final String id : unique) {
-            final Double base = prices.get(id);
-            if (base == null) {
-                audit.append("no price for ").append(id).append(" — skipping\n");
-                continue;
-            }
-            adjusted.put(id, base * 0.95);
-        }
-        // Phase 4: decrement the stock counter for every priced order.
-        int fulfilled = 0;
-        for (final String id : adjusted.keySet()) {
-            final Integer available = stock.get(id);
-            if (available == null || available <= 0) {
-                audit.append("out of stock: ").append(id).append("\n");
-                continue;
+        return Optional.empty();
+    }
+
+    private static String buildAudit(final LinkedHashSet<String> unique,
+                                     final LinkedHashMap<String, Double> adjusted,
+                                     final StockUpdate update,
+                                     final double revenue) {
+        final StringBuilder audit = new StringBuilder();
+        audit.append("unique orders: ").append(unique.size()).append('\n');
+        unique.stream()
+                .filter(id -> !adjusted.containsKey(id))
+                .forEach(id -> audit.append("no price for ").append(id).append(" — skipping\n"));
+        update.outOfStock()
+                .forEach(id -> audit.append("out of stock: ").append(id).append('\n'));
+        audit.append("fulfilled: ").append(update.fulfilled().size()).append('\n');
+        audit.append("revenue: ").append(String.format("%.2f", revenue)).append('\n');
+        audit.append("settlement complete\n");
+        audit.append(SEPARATOR);
+        return audit.toString();
+    }
+
+    private static LinkedHashSet<String> dedupe(final List<String> orderIds) {
+        return orderIds.stream()
+                .filter(Objects::nonNull)
+                .filter(id -> !id.isBlank())
+                .map(String::trim)
+                .collect(Collectors.toCollection(LinkedHashSet::new));
+    }
+
+    private static LinkedHashMap<String, Double> applyDiscount(final Set<String> ids,
+                                                                final Map<String, Double> prices) {
+        return ids.stream()
+                .filter(prices::containsKey)
+                .collect(Collectors.toMap(
+                        id -> id,
+                        id -> prices.get(id) * RESERVATION_DISCOUNT,
+                        (left, right) -> left,
+                        LinkedHashMap::new));
+    }
+
+    private static StockUpdate decrementStock(final Set<String> ids, final Map<String, Integer> stock) {
+        final Map<String, Integer> newStock = new HashMap<>(stock);
+        final Set<String> fulfilled = new LinkedHashSet<>();
+        final Set<String> outOfStock = new LinkedHashSet<>();
+        for (final String id : ids) {
+            final int available = newStock.getOrDefault(id, 0);
+            if (available <= 0) {
+                outOfStock.add(id);
+            } else {
+                newStock.put(id, available - 1);
+                fulfilled.add(id);
             }
-            stock.put(id, available - 1);
-            fulfilled++;
         }
-        audit.append("fulfilled: ").append(fulfilled).append("\n");
-        // Phase 5: compute the total revenue from the fulfilled orders.
-        double revenue = 0.0;
-        for (final Map.Entry<String, Double> entry : adjusted.entrySet()) {
-            final Integer available = stock.get(entry.getKey());
-            if (available != null) {
-                revenue += entry.getValue();
-            }
+        return new StockUpdate(newStock, fulfilled, outOfStock);
+    }
+
+    private static double totalRevenue(final Map<String, Double> adjusted,
+                                       final Map<String, Integer> stock) {
+        return adjusted.entrySet().stream()
+                .filter(entry -> stock.containsKey(entry.getKey()))
+                .mapToDouble(Map.Entry::getValue)
+                .sum();
+    }
+
+    public record Settlement(Map<String, Integer> stock, String audit) {
+        static Settlement rejected(final Map<String, Integer> stock, final String message) {
+            final Map<String, Integer> copy = stock == null ? Map.of() : Map.copyOf(stock);
+            return new Settlement(copy, message);
         }
-        audit.append("revenue: ").append(String.format("%.2f", revenue)).append("\n");
-        // Phase 6: sign off.
-        audit.append("settlement complete\n");
-        audit.append("---\n");
+    }
+
+    private record StockUpdate(Map<String, Integer> newStock,
+                               Set<String> fulfilled,
+                               Set<String> outOfStock) {
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
index c6108b6..ab65e68 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
@@ -2,6 +2,8 @@ package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.List;
 import java.util.Map;
+import java.util.Map.Entry;
+import java.util.stream.Collectors;
 
 /**
  * StringBuilder-threading fixture. The private helpers take a
@@ -13,41 +15,49 @@ import java.util.Map;
  */
 public final class ReportTemplate {
 
-    public String renderReport(final String title, final List<String> sections,
-                               final Map<String, Integer> totals) {
-        final StringBuilder sb = new StringBuilder();
-        sb.append("<html><head><title>").append(title).append("</title></head><body>");
-        sb.append("<h1>").append(title).append("</h1>");
-        sb.append("<p>generated report with ").append(sections.size()).append(" sections</p>");
-
-        sb.append("<div class=\"sections\">");
-        for (final String section : sections) {
-            appendSection(sb, section);
-        }
-        sb.append("</div>");
-
-        sb.append("<div class=\"totals\">");
-        sb.append("<h2>Totals</h2>");
-        sb.append("<ul>");
-        for (final Map.Entry<String, Integer> entry : totals.entrySet()) {
-            appendTotalRow(sb, entry.getKey(), entry.getValue());
-        }
-        sb.append("</ul>");
-        sb.append("</div>");
-
-        sb.append("</body></html>");
-        return sb.toString();
+    private static final String TOTALS_OPEN = "<div class=\"totals\"><h2>Totals</h2><ul>";
+    private static final String TOTALS_CLOSE = "</ul></div>";
+
+    private ReportTemplate() {
+    }
+
+    public static String renderReport(final String title, final List<String> sections,
+                                      final Map<String, Integer> totals) {
+        return header(title, sections.size())
+                + sectionsBlock(sections)
+                + totalsBlock(totals)
+                + "</body></html>";
+    }
+
+    private static String header(final String title, final int sectionCount) {
+        final String pageHead = "<html><head><title>" + title + "</title></head><body>";
+        final String mainHeading = "<h1>" + title + "</h1>";
+        final String description = "<p>generated report with " + sectionCount + " sections</p>";
+        return pageHead + mainHeading + description;
+    }
+
+    private static String sectionsBlock(final List<String> sections) {
+        return sections.stream()
+                .map(ReportTemplate::renderSection)
+                .collect(Collectors.joining("", "<div class=\"sections\">", "</div>"));
+    }
+
+    private static String renderSection(final String section) {
+        final String heading = "<h2>" + section + "</h2>";
+        final String content = "<p>content for " + section + "</p>";
+        return "<section>" + heading + content + "</section>";
     }
 
-    private void appendSection(final StringBuilder sb, final String section) {
-        sb.append("<section>");
-        sb.append("<h2>").append(section).append("</h2>");
-        sb.append("<p>content for ").append(section).append("</p>");
-        sb.append("</section>");
+    private static String totalsBlock(final Map<String, Integer> totals) {
+        final String rows = totals.entrySet().stream()
+                .map(ReportTemplate::renderTotalRow)
+                .collect(Collectors.joining());
+        return TOTALS_OPEN + rows + TOTALS_CLOSE;
     }
 
-    private void appendTotalRow(final StringBuilder sb, final String key, final int value) {
-        sb.append("<li><span class=\"key\">").append(key).append("</span>");
-        sb.append("<span class=\"value\">").append(value).append("</span></li>");
+    private static String renderTotalRow(final Entry<String, Integer> entry) {
+        final String keyCell = "<span class=\"key\">" + entry.getKey() + "</span>";
+        final String valueCell = "<span class=\"value\">" + entry.getValue() + "</span>";
+        return "<li>" + keyCell + valueCell + "</li>";
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
index de682a6..1a67252 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
@@ -3,6 +3,7 @@ package org.fiftieshousewife.cleancode.sandbox;
 import java.time.Instant;
 import java.util.HashMap;
 import java.util.Map;
+import java.util.Optional;
 
 /**
  * Session cache with expiry. Exhibits the null-return pattern that
@@ -13,76 +14,52 @@ import java.util.Map;
  */
 public final class SessionStore {
 
+    private static final long SESSION_TTL_MS = 30L * 60L * 1000L;
+
     private final Map<String, Session> sessions = new HashMap<>();
 
     public String userIdFor(final String token) {
-        if (token == null) {
-            return null;
-        }
-        if (token.isBlank()) {
-            return null;
-        }
-        final Session session = sessions.get(token);
-        if (session == null) {
-            return null;
-        }
-        if (!session.active) {
-            return null;
-        }
-        if (Instant.now().toEpochMilli() - session.createdAtMs > 30L * 60L * 1000L) {
-            return null;
-        }
-        return session.userId;
+        return Optional.ofNullable(token)
+                .filter(candidate -> !candidate.isBlank())
+                .map(sessions::get)
+                .filter(Session::active)
+                .filter(SessionStore::isLive)
+                .map(Session::userId)
+                .orElse(null);
     }
 
     public Session lookupOrNull(final String token) {
-        if (!sessions.containsKey(token)) {
-            return null;
-        }
-        final Session session = sessions.get(token);
-        if (!session.active) {
-            return null;
-        }
-        return session;
+        return Optional.ofNullable(sessions.get(token))
+                .filter(Session::active)
+                .orElse(null);
     }
 
     public void open(final String token, final String userId) {
-        if (token == null || token.isBlank()) {
-            throw new IllegalArgumentException("token is required");
-        }
-        if (userId == null || userId.isBlank()) {
-            throw new IllegalArgumentException("userId is required");
-        }
-        sessions.put(token, new Session(userId, Instant.now().toEpochMilli(), true));
+        final String validatedToken = requireNonBlank(token, "token is required");
+        final String validatedUserId = requireNonBlank(userId, "userId is required");
+        sessions.put(validatedToken, new Session(validatedUserId, Instant.now().toEpochMilli(), true));
     }
 
     public void close(final String token) {
-        final Session existing = sessions.get(token);
-        if (existing == null) {
-            return;
-        }
-        sessions.put(token, new Session(existing.userId, existing.createdAtMs, false));
+        sessions.computeIfPresent(token,
+                (key, existing) -> new Session(existing.userId(), existing.createdAtMs(), false));
     }
 
     public int activeSessionCount() {
-        int count = 0;
-        for (final Session session : sessions.values()) {
-            if (session.active) {
-                count = count + 1;
-            }
-        }
-        return count;
+        return (int) sessions.values().stream().filter(Session::active).count();
     }
 
-    public static final class Session {
-        public final String userId;
-        public final long createdAtMs;
-        public final boolean active;
+    private static boolean isLive(final Session session) {
+        return Instant.now().toEpochMilli() - session.createdAtMs() <= SESSION_TTL_MS;
+    }
 
-        public Session(final String userId, final long createdAtMs, final boolean active) {
-            this.userId = userId;
-            this.createdAtMs = createdAtMs;
-            this.active = active;
+    private static String requireNonBlank(final String value, final String message) {
+        if (value == null || value.isBlank()) {
+            throw new IllegalArgumentException(message);
         }
+        return value;
+    }
+
+    public record Session(String userId, long createdAtMs, boolean active) {
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
index 8a24359..498bd30 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
@@ -14,64 +14,60 @@ import java.util.Map;
  */
 public final class UserAccountService {
 
+    private static final String EMAIL_REQUIRED = "email is required";
+    private static final String DISPLAY_NAME_REQUIRED = "displayName is required";
+    private static final int MAX_DISPLAY_NAME = 64;
+    private static final List<String> VALID_REGIONS = List.of("US", "EU", "APAC");
+
     private final Map<String, User> byId = new HashMap<>();
     private final List<String> audit = new ArrayList<>();
     private long nextId = 1000L;
 
     public String createAccount(final String email, final String displayName, final String region) {
-        final List<String> errors = new ArrayList<>();
-        if (email == null || email.isBlank()) {
-            errors.add("email is required");
-        }
-        if (email != null && !email.contains("@")) {
-            errors.add("email must contain @");
-        }
-        if (displayName == null || displayName.isBlank()) {
-            errors.add("displayName is required");
-        }
-        if (displayName != null && displayName.length() > 64) {
-            errors.add("displayName too long");
-        }
-        if (region == null || !List.of("US", "EU", "APAC").contains(region)) {
-            errors.add("region must be one of US/EU/APAC");
-        }
+        final List<String> errors = validate(email, displayName, region);
         if (!errors.isEmpty()) {
             throw new IllegalArgumentException(String.join("; ", errors));
         }
-
         final String id = "acct-" + nextId;
         nextId = nextId + 1;
-        final User user = new User(id, email, displayName, region);
-        try {
-            byId.put(id, user);
-        } catch (RuntimeException e) {
-            System.err.println("persist failed: " + e.getMessage());
-            return null;
-        }
-
-        audit.add("created " + id + " in region " + region);
-        audit.add("email=" + email + " displayName=" + displayName);
-        audit.add("total accounts: " + byId.size());
-
-        try {
-            sendWelcomeEmail(email, displayName);
-        } catch (Exception e) {
-            System.err.println("notify failed: " + e.getMessage());
-        }
+        byId.put(id, new User(id, email, displayName, region));
+        audit.addAll(List.of(
+                "created " + id + " in region " + region,
+                "email=" + email + " displayName=" + displayName,
+                "total accounts: " + byId.size(),
+                "email queued for " + email + " (" + displayName + ")"));
         return id;
     }
 
-    void validate(final String email, final String displayName, final List<String> errors) {
+    static List<String> validate(final String email, final String displayName, final String region) {
+        final List<String> errors = new ArrayList<>();
+        errors.addAll(emailErrors(email));
+        errors.addAll(displayNameErrors(displayName));
+        errors.addAll(regionErrors(region));
+        return List.copyOf(errors);
+    }
+
+    private static List<String> emailErrors(final String email) {
         if (email == null || email.isBlank()) {
-            errors.add("email is required");
+            return List.of(EMAIL_REQUIRED);
         }
+        return email.contains("@") ? List.of() : List.of("email must contain @");
+    }
+
+    private static List<String> displayNameErrors(final String displayName) {
         if (displayName == null || displayName.isBlank()) {
-            errors.add("displayName is required");
+            return List.of(DISPLAY_NAME_REQUIRED);
         }
+        return displayName.length() > MAX_DISPLAY_NAME
+                ? List.of("displayName too long")
+                : List.of();
     }
 
-    private void sendWelcomeEmail(final String email, final String displayName) {
-        audit.add("email queued for " + email + " (" + displayName + ")");
+    private static List<String> regionErrors(final String region) {
+        if (region == null || !VALID_REGIONS.contains(region)) {
+            return List.of("region must be one of US/EU/APAC");
+        }
+        return List.of();
     }
 
     public User find(final String id) {

```

## mcp + recipes — commit message body

## Actions
- Edit() — Made computeScore static and replaced imperative loop+guards with a filtered stream and Math.min cap.
- Edit() — Extracted END_MARKER constant, made handle static, and routed rejections through an Optional<String> helper.
- Edit() — Converted to static; returns updated stock; each phase is a static helper returning a record; removed FQN LinkedHashSet/LinkedHashMap refs; stream-based unique/revenue.
- Edit() — Extracted static parseFields and isEscapedQuote; moved fields declaration close to use; depth-2 quote condition pulled into helper.
- Edit() — Callable→Supplier removes checked Exception catch; extracted constants and static backoffDelayMs; replaced System.err with audit append; lastFailure via Optional.
- Edit() — Made all three methods static and stream-based with Collectors.groupingBy/summingInt; eliminated explicit guards and containsKey branching.
- Edit() — Split dispatch into dispatchStandard/dispatchUrgent; replaced String ladder with Channel enum; Collections.nCopies replaces retry loops; URGENT_RETRY_COUNT extracted.
- Edit() — Helpers now return Strings; renderReport composes via single chained StringBuilder and Collectors.joining — removes F2 threading and void-call sequences.
- Edit() — Session became a record; userIdFor/lookup return Optional via filter chain; close uses computeIfPresent; SESSION_TTL_MS constant extracted; activeSessionCount streams.
- Edit() — validate returns List<String>; removed swallow-Exception blocks and System.err calls; error messages and ALLOWED_REGIONS extracted to constants; audit log written via addAll.
- Edit(target=rejectionReason) — Replaced two guard-clause if-returns with a single ternary expression to satisfy G29.
- Edit(newHelper=buildAuditReport) — Extracted audit-write block into a String-returning static helper (not threading audit as F2 output param) to shrink settleOrders below 50 lines.
- Edit(target=isEscapedQuote) — Introduced hasNextChar and nextCharIsQuote named variables for G19 expression decomposition.
- Edit(newPolyMethod=Channel.isWebhook, namedVars=[header, payload]) — Added Channel.isWebhook() polymorphic accessor to drop the Channel.WEBHOOK qualified reference (G12), and decomposed formatMessage's concatenation into header/payload named vars (G19).
- Edit(madeStatic=renderReport, namedVars=[heading, paragraph, keySpan, valueSpan]) — Promoted renderReport to static (G18) and decomposed both renderSection and renderTotalRow concatenations into named substring variables (G19).
- Edit(newHelper=auditEntries) — Extracted the static audit-message-building list out of createAccount into the auditEntries static helper, addressing G18's 'or extracting' clause.

## Not attempted
- Edit(target=fields declaration) — G10 cannot be cleanly addressed: fields must persist across loop iterations, but its first use is inside the loop body where Java scoping forbids declaring it any closer; the variable is already the last local before the while loop.

## Suggestions
- G18 at L-1: Method 'computeScore' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'computeScore' has 2 guard clauses — simplify with early return or extract filter
- G25 at L-1: String "---
" appears 2 times — extract to a named constant
- G18 at L-1: Method 'handle' does not use instance state — consider making it static or extracting
- F2 at L-1: Method 'settleOrders' mutates its argument 'stock' (java.util.Map<java.lang.String, java.lang.Integer>) — return the result instead
- Ch7_2 at L-1: Method 'settleOrders' has 7 null checks
- G34 at L-1: Method 'settleOrders' has 6 section comments
- G30 at L-1: Loop in 'settleOrders' (for-each with filter-add) can be replaced with a stream operation
- G18 at L-1: Method 'settleOrders' does not use instance state — consider making it static or extracting
- G31 at L-1: Method 'settleOrders' has 3 consecutive void calls with no data dependency — make the order explicit
- G12 at L0: 5 inline fully-qualified type reference(s); first: java.util.LinkedHashSet — run ShortenFullyQualifiedReferencesRecipe
- G28 at L-1: Complex condition (depth 2) should be extracted
- G10 at L3: 'fields' is declared in parseRow() but not used until 20 lines later — move the declaration closer to line 23
- G30 at L-1: Method 'parseRow' has 6 blank-line sections across 41 lines — each section should be its own method
- G18 at L-1: Method 'parseRow' does not use instance state — consider making it static or extracting
- G10 at L4: 'delayMs' is declared in execute() but not used until 17 lines later — move the declaration closer to line 21
- G25 at L-1: String "attempt " appears 2 times — extract to a named constant
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G18 at L-1: Method 'execute' does not use instance state — consider making it static or extracting
- Ch7_1 at L-1: Method 'execute' catches Exception — catch specific exception types instead
- G12 at L0: 1 inline fully-qualified type reference(s); first: System.err — run ShortenFullyQualifiedReferencesRecipe
- G10 at L3: 'balances' is declared in balanceByRegion() but not used until 11 lines later — move the declaration closer to line 14
- G30 at L-1: Loop in 'overstockedRegions' (for-each with filter-add) can be replaced with a stream operation
- G18 at L-1: Method 'balanceByRegion' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'overstockedRegions' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'totalAcrossRegions' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'balanceByRegion' has 3 guard clauses — simplify with early return or extract filter
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- F3 at L-1: Method 'dispatch' takes boolean parameter 'urgent' — split into two methods instead
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G23 at L-1: Method 'dispatch' dispatches on String parameter 'channel' with 4 branches — use an enum or split into separate methods
- G15 at L-1: Method 'dispatch' uses boolean parameter 'urgent' to select behaviour — split into separate methods
- G31 at L-1: Method 'renderReport' has 4 consecutive void calls with no data dependency — make the order explicit
- G31 at L-1: Method 'appendSection' has 4 consecutive void calls with no data dependency — make the order explicit
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- G18 at L-1: Method 'userIdFor' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'lookupOrNull' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'open' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'close' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'activeSessionCount' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'userIdFor' has 5 guard clauses — simplify with early return or extract filter
- G29 at L-1: Method 'lookupOrNull' has 2 guard clauses — simplify with early return or extract filter
- G29 at L-1: Method 'open' has 2 guard clauses — simplify with early return or extract filter
- G12 at L0: 3 inline fully-qualified type reference(s); first: session.userId — run ShortenFullyQualifiedReferencesRecipe
- F2 at L-1: Method 'validate' mutates its argument 'errors' (java.util.List<java.lang.String>) — return the result instead
- Ch7_1 at L-1: Catch block in 'createAccount' only logs or is empty
- Ch7_2 at L-1: Method 'createAccount' has 5 null checks
- G25 at L-1: String "displayName is required" appears 2 times — extract to a named constant
- G25 at L-1: String "email is required" appears 2 times — extract to a named constant
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G31 at L-1: Method 'createAccount' has 3 consecutive void calls with no data dependency — make the order explicit
- Ch7_1 at L-1: Method 'createAccount' catches Exception — catch specific exception types instead
- F2 at L-1: Class has 1 methods returning collections and 1 void methods mutating collection params — pick one style
- G12 at L0: 2 inline fully-qualified type reference(s); first: System.err — run ShortenFullyQualifiedReferencesRecipe

## Agent usage
- input tokens : 96
- output tokens: 91748
- cache read   : 4651337
- cost (USD)   : 6.1514

## mcp + recipes — diff

```diff
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
index 6567aad..576d9d6 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
@@ -1,31 +1,22 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.List;
+import java.util.Objects;
 
-/**
- * Phase B + Phase G fixture. {@link #computeScore} declares a local
- * {@code total} before a for-each loop, the loop writes to
- * {@code total} from inside a {@code continue}-bearing body, and the
- * value of {@code total} is read after the loop. The right extraction
- * is the loop alone — an outer-local reassignment output (Phase B)
- * whose body contains a loop-internal continue (Phase G).
- */
 public final class AccumulatorFixture {
 
-    public int computeScore(final List<Integer> values) {
-        int total = 0;
-        for (final Integer value : values) {
-            if (value == null) {
-                continue;
-            }
-            if (value < 0) {
-                continue;
-            }
-            total = total + value;
-        }
-        if (total > 10000) {
-            total = 10000;
-        }
-        return total;
+    private static final int MAX_SCORE = 10000;
+
+    public static int computeScore(final List<Integer> values) {
+        Objects.requireNonNull(values, "values");
+        final int total = values.stream()
+                .filter(AccumulatorFixture::isValidContribution)
+                .mapToInt(Integer::intValue)
+                .sum();
+        return Math.min(total, MAX_SCORE);
+    }
+
+    private static boolean isValidContribution(final Integer value) {
+        return value != null && value >= 0;
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
index 63f8a43..37fbf81 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
@@ -3,13 +3,6 @@ package org.fiftieshousewife.cleancode.sandbox;
 import java.util.ArrayList;
 import java.util.List;
 
-/**
- * Minimal CSV row parser with quoted-field + escape handling. The
- * parseRow method carries a five-phase state machine — initialise,
- * advance through characters, handle quote toggles, handle field
- * separators, finalise — in one loop. Extract-method friendly: each
- * phase is a coherent block and none reassign the field list.
- */
 public final class CsvParser {
 
     private final char separator;
@@ -26,48 +19,54 @@ public final class CsvParser {
     }
 
     public List<String> parseRow(final String line) {
-        final List<String> fields = new ArrayList<>();
+        final List<String> fields = parseFields(line, separator, quote);
+        rowsParsed = rowsParsed + 1;
+        return fields;
+    }
+
+    public int rowsParsed() {
+        return rowsParsed;
+    }
+
+    static List<String> parseFields(final String line, final char separator, final char quote) {
         final StringBuilder current = new StringBuilder();
         boolean insideQuotes = false;
         int index = 0;
-
+        final List<String> fields = new ArrayList<>();
         while (index < line.length()) {
             final char ch = line.charAt(index);
-
             if (ch == quote) {
-                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == quote) {
+                if (isEscapedQuote(line, index, quote, insideQuotes)) {
                     current.append(quote);
                     index = index + 2;
-                    continue;
+                } else {
+                    insideQuotes = !insideQuotes;
+                    index = index + 1;
                 }
-                insideQuotes = !insideQuotes;
-                index = index + 1;
                 continue;
             }
-
             if (ch == separator && !insideQuotes) {
                 fields.add(current.toString());
                 current.setLength(0);
                 index = index + 1;
                 continue;
             }
-
             if (ch == '\\' && index + 1 < line.length()) {
                 current.append(line.charAt(index + 1));
                 index = index + 2;
                 continue;
             }
-
             current.append(ch);
             index = index + 1;
         }
-
         fields.add(current.toString());
-        rowsParsed = rowsParsed + 1;
         return fields;
     }
 
-    public int rowsParsed() {
-        return rowsParsed;
+    private static boolean isEscapedQuote(final String line, final int index,
+                                          final char quote, final boolean insideQuotes) {
+        final boolean hasNextChar = index + 1 < line.length();
+        final boolean nextCharIsQuote = hasNextChar && line.charAt(index + 1) == quote;
+        return insideQuotes && nextCharIsQuote;
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
index 43f0728..2c6c642 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
@@ -1,26 +1,25 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
-/**
- * Phase A fixture. {@link #handle} opens with an early-return guard
- * block — a {@code null} / empty check that causes the method to
- * bail. The right extraction is the guard alone, turning it into a
- * boolean-returning {@code isInvalid(…)} helper. The surrounding
- * method stays void; the call site becomes
- * {@code if (isInvalid(event)) return;}.
- */
+import java.util.Objects;
+import java.util.Optional;
+
 public final class GuardFixture {
 
-    public void handle(final String event, final StringBuilder log) {
-        if (event == null) {
-            log.append("drop: null event\n");
-            log.append("---\n");
-            return;
-        }
-        if (event.isBlank()) {
-            log.append("drop: blank event\n");
-            log.append("---\n");
+    private static final String END_MARKER = "---\n";
+
+    public static void handle(final String event, final StringBuilder log) {
+        Objects.requireNonNull(log, "log");
+        final Optional<String> rejection = rejectionReason(event);
+        if (rejection.isPresent()) {
+            log.append(rejection.get()).append('\n').append(END_MARKER);
             return;
         }
         log.append("processed: ").append(event.strip()).append('\n');
     }
+
+    private static Optional<String> rejectionReason(final String event) {
+        return event == null
+                ? Optional.of("drop: null event")
+                : event.isBlank() ? Optional.of("drop: blank event") : Optional.empty();
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
index fb1c60a..6c9a40f 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
@@ -1,57 +1,57 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
-import java.util.concurrent.Callable;
+import java.util.Optional;
+import java.util.function.Supplier;
 
-/**
- * Retries a callable with exponential backoff. Typical production shape
- * for a resilience helper. Finding cluster: magic numbers sprinkled
- * across the backoff loop (G25), a catch-InterruptedException block
- * that logs-and-returns-null (Ch7.1), and an execute method that is
- * long enough to benefit from a backoff-phase extraction (G30).
- */
 public final class HttpRetryPolicy {
 
+    private static final int MAX_ATTEMPTS = 5;
+    private static final long INITIAL_DELAY_MS = 500L;
+    private static final long MAX_DELAY_MS = 10000L;
+    private static final double BACKOFF_MULTIPLIER = 1.5;
+    private static final String ATTEMPT_PREFIX = "attempt ";
+
     private final StringBuilder audit = new StringBuilder();
 
-    public <T> T execute(final Callable<T> action) {
-        int attempt = 0;
-        long delayMs = 500;
-        Throwable lastFailure = null;
-        while (attempt < 5) {
+    public <T> T execute(final Supplier<T> action) {
+        Optional<Throwable> lastFailure = Optional.empty();
+        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt = attempt + 1) {
             try {
-                final T result = action.call();
-                audit.append("attempt ").append(attempt).append(" ok\n");
+                final T result = action.get();
+                audit.append(ATTEMPT_PREFIX).append(attempt).append(" ok\n");
                 return result;
-            } catch (Exception e) {
-                lastFailure = e;
-                audit.append("attempt ").append(attempt).append(" failed: ")
+            } catch (RuntimeException e) {
+                lastFailure = Optional.of(e);
+                audit.append(ATTEMPT_PREFIX).append(attempt).append(" failed: ")
                         .append(e.getMessage()).append('\n');
             }
-
-            if (attempt >= 4) {
+            if (attempt >= MAX_ATTEMPTS - 1) {
                 break;
             }
+            final long delayMs = backoffDelayMs(attempt);
             try {
                 Thread.sleep(delayMs);
             } catch (InterruptedException interrupted) {
-                System.err.println("retry interrupted: " + interrupted.getMessage());
+                Thread.currentThread().interrupt();
+                audit.append("retry interrupted: ").append(interrupted.getMessage()).append('\n');
                 return null;
             }
-            delayMs = (long) (delayMs * 1.5);
-            if (delayMs > 10000) {
-                delayMs = 10000;
-            }
-            attempt = attempt + 1;
-        }
-
-        audit.append("gave up after ").append(attempt + 1).append(" attempts\n");
-        if (lastFailure != null) {
-            audit.append("last failure: ").append(lastFailure.getClass().getSimpleName()).append('\n');
         }
+        audit.append("gave up after ").append(MAX_ATTEMPTS).append(" attempts\n");
+        lastFailure.ifPresent(failure ->
+                audit.append("last failure: ").append(failure.getClass().getSimpleName()).append('\n'));
         return null;
     }
 
     public String auditLog() {
         return audit.toString();
     }
+
+    private static long backoffDelayMs(final int attempt) {
+        long delay = INITIAL_DELAY_MS;
+        for (int i = 0; i < attempt; i = i + 1) {
+            delay = Math.min((long) (delay * BACKOFF_MULTIPLIER), MAX_DELAY_MS);
+        }
+        return delay;
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
index e972d68..d7b40a8 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
@@ -1,59 +1,37 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
-import java.util.ArrayList;
-import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
+import java.util.Objects;
+import java.util.stream.Collectors;
 
-/**
- * Imperative-loop aggregator — classic candidate for stream conversion
- * (G30 when the loop is long). Each loop body also has nested
- * conditionals that could simplify via Map.computeIfAbsent. Deliberately
- * mutation-heavy to exercise the LLM's pattern-matching on stream
- * rewrites, where recipes have less leverage.
- */
 public final class InventoryBalancer {
 
-    public Map<String, Integer> balanceByRegion(final List<Lot> lots) {
-        final Map<String, Integer> balances = new HashMap<>();
-        for (final Lot lot : lots) {
-            if (lot == null) {
-                continue;
-            }
-            if (lot.region() == null || lot.region().isBlank()) {
-                continue;
-            }
-            if (lot.quantity() < 0) {
-                continue;
-            }
-            if (balances.containsKey(lot.region())) {
-                final int existing = balances.get(lot.region());
-                balances.put(lot.region(), existing + lot.quantity());
-            } else {
-                balances.put(lot.region(), lot.quantity());
-            }
-        }
-        return balances;
+    public static Map<String, Integer> balanceByRegion(final List<Lot> lots) {
+        Objects.requireNonNull(lots, "lots");
+        return lots.stream()
+                .filter(Objects::nonNull)
+                .filter(InventoryBalancer::hasRegion)
+                .filter(lot -> lot.quantity() >= 0)
+                .collect(Collectors.groupingBy(Lot::region, Collectors.summingInt(Lot::quantity)));
     }
 
-    public List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
-        final List<String> overstocked = new ArrayList<>();
-        for (final Map.Entry<String, Integer> entry : balances.entrySet()) {
-            if (entry.getValue() > threshold) {
-                overstocked.add(entry.getKey());
-            }
-        }
-        return overstocked;
+    public static List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
+        return balances.entrySet().stream()
+                .filter(entry -> entry.getValue() > threshold)
+                .map(Map.Entry::getKey)
+                .toList();
     }
 
-    public int totalAcrossRegions(final Map<String, Integer> balances) {
-        int sum = 0;
-        for (final Integer value : balances.values()) {
-            if (value != null) {
-                sum = sum + value;
-            }
-        }
-        return sum;
+    public static int totalAcrossRegions(final Map<String, Integer> balances) {
+        return balances.values().stream()
+                .filter(Objects::nonNull)
+                .mapToInt(Integer::intValue)
+                .sum();
+    }
+
+    private static boolean hasRegion(final Lot lot) {
+        return lot.region() != null && !lot.region().isBlank();
     }
 
     public record Lot(String region, String sku, int quantity) {}
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
index 04846e7..0d5bcee 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
@@ -1,50 +1,30 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.ArrayList;
+import java.util.Collections;
 import java.util.List;
+import java.util.Objects;
 
-/**
- * Stylistic-heavy fixture. dispatch switches on a channel-kind string
- * via an if/else-if ladder (G23 — polymorphism would be cleaner) and
- * takes a boolean "urgent" flag that selects an entirely different
- * code path per branch (F3 — split into two methods). Also a G25
- * opportunity on the retry-count literal.
- */
 public final class NotificationDispatcher {
 
+    private static final int URGENT_RETRY_COUNT = 3;
+
     private final List<String> dispatched = new ArrayList<>();
 
-    public void dispatch(final String channel, final String recipient, final String body,
-                         final boolean urgent) {
-        if (channel.equals("email")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("email(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("email -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("sms")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("sms(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("sms -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("push")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("push(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("push -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("webhook")) {
-            dispatched.add("webhook -> " + recipient + ": " + body);
-        } else {
-            throw new IllegalArgumentException("unknown channel: " + channel);
+    public void dispatchStandard(final Channel channel, final String recipient, final String body) {
+        Objects.requireNonNull(channel, "channel");
+        dispatched.add(formatMessage(channel, "", recipient, body));
+    }
+
+    public void dispatchUrgent(final Channel channel, final String recipient, final String body) {
+        Objects.requireNonNull(channel, "channel");
+        if (channel.isWebhook()) {
+            dispatchStandard(channel, recipient, body);
+            return;
         }
+        dispatched.addAll(Collections.nCopies(
+                URGENT_RETRY_COUNT,
+                formatMessage(channel, "(urgent)", recipient, body)));
     }
 
     public int dispatchedCount() {
@@ -54,4 +34,32 @@ public final class NotificationDispatcher {
     public List<String> dispatched() {
         return List.copyOf(dispatched);
     }
+
+    private static String formatMessage(final Channel channel, final String qualifier,
+                                        final String recipient, final String body) {
+        final String header = channel.label() + qualifier;
+        final String payload = recipient + ": " + body;
+        return header + " -> " + payload;
+    }
+
+    public enum Channel {
+        EMAIL("email"),
+        SMS("sms"),
+        PUSH("push"),
+        WEBHOOK("webhook");
+
+        private final String label;
+
+        Channel(final String label) {
+            this.label = label;
+        }
+
+        public String label() {
+            return label;
+        }
+
+        public boolean isWebhook() {
+            return this == WEBHOOK;
+        }
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
index 1eef21e..a342607 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
@@ -1,69 +1,111 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
+import java.util.ArrayList;
+import java.util.LinkedHashMap;
+import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
+import java.util.Objects;
+import java.util.stream.Collectors;
 
-/**
- * G30-heavy fixture: a single orchestrator method strung together from
- * six distinct phases. Each phase is 8-10 lines, has a clear name in the
- * comments, and does not cross-reference other phases — i.e. the
- * textbook shape for ExtractMethodRecipe. No F2/G18/G12/G31 noise;
- * extraction is the obvious right fix.
- */
 public final class OrchestratorFixture {
 
-    public void settleOrders(final List<String> orderIds, final Map<String, Integer> stock,
-                             final Map<String, Double> prices, final StringBuilder audit) {
-        // Phase 1: validate input collections.
-        if (orderIds == null || orderIds.isEmpty()) {
+    private static final double RESERVATION_DISCOUNT = 0.95;
+    private static final String END_MARKER = "---\n";
+
+    public static Map<String, Integer> settleOrders(
+            final List<String> orderIds,
+            final Map<String, Integer> stock,
+            final Map<String, Double> prices,
+            final StringBuilder audit) {
+
+        Objects.requireNonNull(orderIds, "orderIds");
+        Objects.requireNonNull(stock, "stock");
+        Objects.requireNonNull(prices, "prices");
+        Objects.requireNonNull(audit, "audit");
+
+        if (orderIds.isEmpty()) {
             audit.append("no orders to settle\n");
-            return;
-        }
-        if (stock == null || prices == null) {
-            audit.append("missing reference data\n");
-            return;
-        }
-        // Phase 2: dedupe the incoming ids into a clean working list.
-        final java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
-        for (final String id : orderIds) {
-            if (id != null && !id.isBlank()) {
-                unique.add(id.trim());
-            }
+            return Map.copyOf(stock);
         }
-        audit.append("unique orders: ").append(unique.size()).append("\n");
-        // Phase 3: apply a flat reservation discount to each line price.
-        final java.util.LinkedHashMap<String, Double> adjusted = new java.util.LinkedHashMap<>();
-        for (final String id : unique) {
-            final Double base = prices.get(id);
-            if (base == null) {
-                audit.append("no price for ").append(id).append(" — skipping\n");
-                continue;
+
+        final LinkedHashSet<String> unique = uniqueIds(orderIds);
+        final PricingOutcome pricing = applyDiscount(unique, prices);
+        final FulfillmentOutcome fulfillment = fulfillOrders(pricing.adjusted().keySet(), stock);
+        final double revenue = computeRevenue(pricing.adjusted(), fulfillment.updatedStock());
+
+        audit.append(buildAuditReport(unique, pricing, fulfillment, revenue));
+        return fulfillment.updatedStock();
+    }
+
+    private static String buildAuditReport(
+            final LinkedHashSet<String> unique,
+            final PricingOutcome pricing,
+            final FulfillmentOutcome fulfillment,
+            final double revenue) {
+        final StringBuilder report = new StringBuilder();
+        report.append("unique orders: ").append(unique.size()).append("\n");
+        pricing.skipped().forEach(id ->
+                report.append("no price for ").append(id).append(" — skipping\n"));
+        fulfillment.outOfStock().forEach(id ->
+                report.append("out of stock: ").append(id).append("\n"));
+        report.append("fulfilled: ").append(fulfillment.fulfilled()).append("\n");
+        report.append("revenue: ").append(String.format("%.2f", revenue)).append("\n");
+        report.append("settlement complete\n");
+        report.append(END_MARKER);
+        return report.toString();
+    }
+
+    private static LinkedHashSet<String> uniqueIds(final List<String> orderIds) {
+        return orderIds.stream()
+                .filter(id -> id != null && !id.isBlank())
+                .map(String::trim)
+                .collect(Collectors.toCollection(LinkedHashSet::new));
+    }
+
+    private static PricingOutcome applyDiscount(
+            final LinkedHashSet<String> ids,
+            final Map<String, Double> prices) {
+        final LinkedHashMap<String, Double> adjusted = new LinkedHashMap<>();
+        final List<String> skipped = new ArrayList<>();
+        for (final String id : ids) {
+            if (prices.containsKey(id)) {
+                adjusted.put(id, prices.get(id) * RESERVATION_DISCOUNT);
+            } else {
+                skipped.add(id);
             }
-            adjusted.put(id, base * 0.95);
         }
-        // Phase 4: decrement the stock counter for every priced order.
+        return new PricingOutcome(adjusted, skipped);
+    }
+
+    private static FulfillmentOutcome fulfillOrders(
+            final Iterable<String> ids,
+            final Map<String, Integer> originalStock) {
+        final Map<String, Integer> updated = new LinkedHashMap<>(originalStock);
+        final List<String> outOfStock = new ArrayList<>();
         int fulfilled = 0;
-        for (final String id : adjusted.keySet()) {
-            final Integer available = stock.get(id);
-            if (available == null || available <= 0) {
-                audit.append("out of stock: ").append(id).append("\n");
-                continue;
+        for (final String id : ids) {
+            final int available = updated.getOrDefault(id, 0);
+            if (available <= 0) {
+                outOfStock.add(id);
+            } else {
+                updated.put(id, available - 1);
+                fulfilled = fulfilled + 1;
             }
-            stock.put(id, available - 1);
-            fulfilled++;
         }
-        audit.append("fulfilled: ").append(fulfilled).append("\n");
-        // Phase 5: compute the total revenue from the fulfilled orders.
-        double revenue = 0.0;
-        for (final Map.Entry<String, Double> entry : adjusted.entrySet()) {
-            final Integer available = stock.get(entry.getKey());
-            if (available != null) {
-                revenue += entry.getValue();
-            }
-        }
-        audit.append("revenue: ").append(String.format("%.2f", revenue)).append("\n");
-        // Phase 6: sign off.
-        audit.append("settlement complete\n");
-        audit.append("---\n");
+        return new FulfillmentOutcome(updated, outOfStock, fulfilled);
+    }
+
+    private static double computeRevenue(
+            final Map<String, Double> adjusted,
+            final Map<String, Integer> updatedStock) {
+        return adjusted.entrySet().stream()
+                .filter(entry -> updatedStock.containsKey(entry.getKey()))
+                .mapToDouble(Map.Entry::getValue)
+                .sum();
     }
+
+    private record PricingOutcome(LinkedHashMap<String, Double> adjusted, List<String> skipped) {}
+
+    private record FulfillmentOutcome(Map<String, Integer> updatedStock, List<String> outOfStock, int fulfilled) {}
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
index c6108b6..4d51273 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
@@ -2,52 +2,40 @@ package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.List;
 import java.util.Map;
+import java.util.stream.Collectors;
 
-/**
- * StringBuilder-threading fixture. The private helpers take a
- * StringBuilder named `sb` and mutate it — the F2 + N1 pattern
- * identified in the manual-1 audit as endemic. The renderReport
- * method is also long enough to benefit from phase extraction (G30).
- * HTML literal is deliberately inline so the agent sees a G1
- * (multiple-languages) hint as well.
- */
 public final class ReportTemplate {
 
-    public String renderReport(final String title, final List<String> sections,
-                               final Map<String, Integer> totals) {
-        final StringBuilder sb = new StringBuilder();
-        sb.append("<html><head><title>").append(title).append("</title></head><body>");
-        sb.append("<h1>").append(title).append("</h1>");
-        sb.append("<p>generated report with ").append(sections.size()).append(" sections</p>");
-
-        sb.append("<div class=\"sections\">");
-        for (final String section : sections) {
-            appendSection(sb, section);
-        }
-        sb.append("</div>");
-
-        sb.append("<div class=\"totals\">");
-        sb.append("<h2>Totals</h2>");
-        sb.append("<ul>");
-        for (final Map.Entry<String, Integer> entry : totals.entrySet()) {
-            appendTotalRow(sb, entry.getKey(), entry.getValue());
-        }
-        sb.append("</ul>");
-        sb.append("</div>");
-
-        sb.append("</body></html>");
-        return sb.toString();
+    public static String renderReport(final String title, final List<String> sections,
+                                      final Map<String, Integer> totals) {
+        final String sectionsHtml = sections.stream()
+                .map(ReportTemplate::renderSection)
+                .collect(Collectors.joining());
+        final String totalsHtml = totals.entrySet().stream()
+                .map(entry -> renderTotalRow(entry.getKey(), entry.getValue()))
+                .collect(Collectors.joining());
+        return new StringBuilder()
+                .append("<html><head><title>").append(title).append("</title></head><body>")
+                .append("<h1>").append(title).append("</h1>")
+                .append("<p>generated report with ").append(sections.size()).append(" sections</p>")
+                .append("<div class=\"sections\">").append(sectionsHtml).append("</div>")
+                .append("<div class=\"totals\">")
+                .append("<h2>Totals</h2>")
+                .append("<ul>").append(totalsHtml).append("</ul>")
+                .append("</div>")
+                .append("</body></html>")
+                .toString();
     }
 
-    private void appendSection(final StringBuilder sb, final String section) {
-        sb.append("<section>");
-        sb.append("<h2>").append(section).append("</h2>");
-        sb.append("<p>content for ").append(section).append("</p>");
-        sb.append("</section>");
+    private static String renderSection(final String section) {
+        final String heading = "<h2>" + section + "</h2>";
+        final String paragraph = "<p>content for " + section + "</p>";
+        return "<section>" + heading + paragraph + "</section>";
     }
 
-    private void appendTotalRow(final StringBuilder sb, final String key, final int value) {
-        sb.append("<li><span class=\"key\">").append(key).append("</span>");
-        sb.append("<span class=\"value\">").append(value).append("</span></li>");
+    private static String renderTotalRow(final String key, final int value) {
+        final String keySpan = "<span class=\"key\">" + key + "</span>";
+        final String valueSpan = "<span class=\"value\">" + value + "</span>";
+        return "<li>" + keySpan + valueSpan + "</li>";
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
index de682a6..0c279d8 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
@@ -3,86 +3,55 @@ package org.fiftieshousewife.cleancode.sandbox;
 import java.time.Instant;
 import java.util.HashMap;
 import java.util.Map;
+import java.util.Optional;
 
-/**
- * Session cache with expiry. Exhibits the null-return pattern that
- * Ch7.2 targets, negative-conditional nesting (G29), and a lookup
- * method whose core predicate would be clearer as an explanatory
- * variable (G19). The expiry arithmetic is also a G25 candidate
- * (hardcoded 30L * 60L * 1000L).
- */
 public final class SessionStore {
 
+    private static final long SESSION_TTL_MS = 30L * 60L * 1000L;
+
     private final Map<String, Session> sessions = new HashMap<>();
 
-    public String userIdFor(final String token) {
-        if (token == null) {
-            return null;
-        }
-        if (token.isBlank()) {
-            return null;
-        }
-        final Session session = sessions.get(token);
-        if (session == null) {
-            return null;
+    public Optional<String> userIdFor(final String token) {
+        if (isBlank(token)) {
+            return Optional.empty();
         }
-        if (!session.active) {
-            return null;
-        }
-        if (Instant.now().toEpochMilli() - session.createdAtMs > 30L * 60L * 1000L) {
-            return null;
-        }
-        return session.userId;
+        return Optional.ofNullable(sessions.get(token))
+                .filter(Session::active)
+                .filter(session -> !isExpired(session))
+                .map(Session::userId);
     }
 
-    public Session lookupOrNull(final String token) {
-        if (!sessions.containsKey(token)) {
-            return null;
-        }
-        final Session session = sessions.get(token);
-        if (!session.active) {
-            return null;
-        }
-        return session;
+    public Optional<Session> lookup(final String token) {
+        return Optional.ofNullable(sessions.get(token))
+                .filter(Session::active);
     }
 
     public void open(final String token, final String userId) {
-        if (token == null || token.isBlank()) {
+        if (isBlank(token)) {
             throw new IllegalArgumentException("token is required");
         }
-        if (userId == null || userId.isBlank()) {
+        if (isBlank(userId)) {
             throw new IllegalArgumentException("userId is required");
         }
         sessions.put(token, new Session(userId, Instant.now().toEpochMilli(), true));
     }
 
     public void close(final String token) {
-        final Session existing = sessions.get(token);
-        if (existing == null) {
-            return;
-        }
-        sessions.put(token, new Session(existing.userId, existing.createdAtMs, false));
+        sessions.computeIfPresent(token, (key, existing) ->
+                new Session(existing.userId(), existing.createdAtMs(), false));
     }
 
     public int activeSessionCount() {
-        int count = 0;
-        for (final Session session : sessions.values()) {
-            if (session.active) {
-                count = count + 1;
-            }
-        }
-        return count;
+        return (int) sessions.values().stream().filter(Session::active).count();
     }
 
-    public static final class Session {
-        public final String userId;
-        public final long createdAtMs;
-        public final boolean active;
+    private static boolean isBlank(final String value) {
+        return value == null || value.isBlank();
+    }
 
-        public Session(final String userId, final long createdAtMs, final boolean active) {
-            this.userId = userId;
-            this.createdAtMs = createdAtMs;
-            this.active = active;
-        }
+    private static boolean isExpired(final Session session) {
+        return Instant.now().toEpochMilli() - session.createdAtMs() > SESSION_TTL_MS;
     }
+
+    public record Session(String userId, long createdAtMs, boolean active) {}
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
index 8a24359..4ce383a 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
@@ -4,74 +4,60 @@ import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
+import java.util.Set;
 
-/**
- * Realistic service-layer fixture. Creating an account runs four phases
- * in one big method — validate, persist, audit, notify — which triggers
- * G30 (too long), and the validator threads errors into a caller-owned
- * list (F2 — output argument). The persistence path swallows every
- * checked exception, logging and returning a partial result (Ch7.1).
- */
 public final class UserAccountService {
 
+    private static final String EMAIL_REQUIRED = "email is required";
+    private static final String EMAIL_MISSING_AT = "email must contain @";
+    private static final String DISPLAY_NAME_REQUIRED = "displayName is required";
+    private static final String DISPLAY_NAME_TOO_LONG = "displayName too long";
+    private static final String INVALID_REGION = "region must be one of US/EU/APAC";
+    private static final Set<String> ALLOWED_REGIONS = Set.of("US", "EU", "APAC");
+    private static final int MAX_DISPLAY_NAME_LENGTH = 64;
+
     private final Map<String, User> byId = new HashMap<>();
     private final List<String> audit = new ArrayList<>();
     private long nextId = 1000L;
 
     public String createAccount(final String email, final String displayName, final String region) {
-        final List<String> errors = new ArrayList<>();
-        if (email == null || email.isBlank()) {
-            errors.add("email is required");
-        }
-        if (email != null && !email.contains("@")) {
-            errors.add("email must contain @");
-        }
-        if (displayName == null || displayName.isBlank()) {
-            errors.add("displayName is required");
-        }
-        if (displayName != null && displayName.length() > 64) {
-            errors.add("displayName too long");
-        }
-        if (region == null || !List.of("US", "EU", "APAC").contains(region)) {
-            errors.add("region must be one of US/EU/APAC");
-        }
+        final List<String> errors = validate(email, displayName, region);
         if (!errors.isEmpty()) {
             throw new IllegalArgumentException(String.join("; ", errors));
         }
-
         final String id = "acct-" + nextId;
         nextId = nextId + 1;
-        final User user = new User(id, email, displayName, region);
-        try {
-            byId.put(id, user);
-        } catch (RuntimeException e) {
-            System.err.println("persist failed: " + e.getMessage());
-            return null;
-        }
-
-        audit.add("created " + id + " in region " + region);
-        audit.add("email=" + email + " displayName=" + displayName);
-        audit.add("total accounts: " + byId.size());
-
-        try {
-            sendWelcomeEmail(email, displayName);
-        } catch (Exception e) {
-            System.err.println("notify failed: " + e.getMessage());
-        }
+        byId.put(id, new User(id, email, displayName, region));
+        audit.addAll(auditEntries(id, email, displayName, region, byId.size()));
         return id;
     }
 
-    void validate(final String email, final String displayName, final List<String> errors) {
-        if (email == null || email.isBlank()) {
-            errors.add("email is required");
-        }
-        if (displayName == null || displayName.isBlank()) {
-            errors.add("displayName is required");
-        }
+    static List<String> auditEntries(
+            final String id, final String email, final String displayName,
+            final String region, final int accountCount) {
+        return List.of(
+                "created " + id + " in region " + region,
+                "email=" + email + " displayName=" + displayName,
+                "total accounts: " + accountCount,
+                "email queued for " + email + " (" + displayName + ")");
     }
 
-    private void sendWelcomeEmail(final String email, final String displayName) {
-        audit.add("email queued for " + email + " (" + displayName + ")");
+    static List<String> validate(final String email, final String displayName, final String region) {
+        final List<String> errors = new ArrayList<>();
+        if (isBlank(email)) {
+            errors.add(EMAIL_REQUIRED);
+        } else if (!email.contains("@")) {
+            errors.add(EMAIL_MISSING_AT);
+        }
+        if (isBlank(displayName)) {
+            errors.add(DISPLAY_NAME_REQUIRED);
+        } else if (displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
+            errors.add(DISPLAY_NAME_TOO_LONG);
+        }
+        if (region == null || !ALLOWED_REGIONS.contains(region)) {
+            errors.add(INVALID_REGION);
+        }
+        return errors;
     }
 
     public User find(final String id) {
@@ -86,5 +72,9 @@ public final class UserAccountService {
         return List.copyOf(audit);
     }
 
+    private static boolean isBlank(final String value) {
+        return value == null || value.isBlank();
+    }
+
     public record User(String id, String email, String displayName, String region) {}
 }

```

## harness + agent — commit message body

## Actions
- HarnessRecipePass(file=AccumulatorFixture.java, recipes=MakeMethodStaticRecipe, MathMinCapRecipe, CollapseSiblingGuardsRecipe) — Harness applied MakeMethodStaticRecipe, MathMinCapRecipe, CollapseSiblingGuardsRecipe deterministically before handing off to the agent
- HarnessRecipePass(file=GuardFixture.java, recipes=MakeMethodStaticRecipe, ChainConsecutiveBuilderCallsRecipe) — Harness applied MakeMethodStaticRecipe, ChainConsecutiveBuilderCallsRecipe deterministically before handing off to the agent
- HarnessRecipePass(file=OrchestratorFixture.java, recipes=MakeMethodStaticRecipe, DeleteSectionCommentsRecipe, ChainConsecutiveBuilderCallsRecipe, ShortenFullyQualifiedReferencesRecipe) — Harness applied MakeMethodStaticRecipe, DeleteSectionCommentsRecipe, ChainConsecutiveBuilderCallsRecipe, ShortenFullyQualifiedReferencesRecipe deterministically before handing off to the agent
- HarnessRecipePass(file=HttpRetryPolicy.java, recipes=RestoreInterruptFlagRecipe, MathMinCapRecipe, AddFinalRecipe) — Harness applied RestoreInterruptFlagRecipe, MathMinCapRecipe, AddFinalRecipe deterministically before handing off to the agent
- HarnessRecipePass(file=InventoryBalancer.java, recipes=MakeMethodStaticRecipe, CollapseSiblingGuardsRecipe) — Harness applied MakeMethodStaticRecipe, CollapseSiblingGuardsRecipe deterministically before handing off to the agent
- HarnessRecipePass(file=ReportTemplate.java, recipes=MakeMethodStaticRecipe, ChainConsecutiveBuilderCallsRecipe) — Harness applied MakeMethodStaticRecipe, ChainConsecutiveBuilderCallsRecipe deterministically before handing off to the agent
- HarnessRecipePass(file=SessionStore.java, recipes=CollapseSiblingGuardsRecipe) — Harness applied CollapseSiblingGuardsRecipe deterministically before handing off to the agent
- HarnessRecipePass(file=UserAccountService.java, recipes=MakeMethodStaticRecipe, AddFinalRecipe) — Harness applied MakeMethodStaticRecipe, AddFinalRecipe deterministically before handing off to the agent
- Edit(constant=LOG_SEPARATOR) — Extract duplicated '---\n' literal into a named constant (G25).
- Edit(return_type=Map<String,Integer>, imports=[java.util.Map.Entry, java.util.stream.Collectors], constants=[PRICE_DISCOUNT, SETTLEMENT_FOOTER]) — Return a copied stock map instead of mutating the argument (F2), stream the unique/revenue loops (G30), and import Map.Entry (G12).
- Edit(extracted=isEscapedQuote) — Extract the depth-2 quote-escape predicate (G28) and collapse blank-line phase gaps inside parseRow (G30).
- Edit(constants=[ATTEMPT_PREFIX, MAX_ATTEMPTS, INITIAL_DELAY_MS, MAX_DELAY_MS, BACKOFF_MULTIPLIER]) — Replace System.err.println with audit append (G4, G12) and lift magic numbers/strings into named constants (G25).
- Edit(extracted=isInvalidLot, imports=[java.util.Map.Entry]) — Extract the depth-3 validity predicate (G28), rewrite the overstocked loop as a stream (G30), and import Map.Entry (G12).
- Edit(split=[dispatch, dispatchUrgent], enum=Channel, constants=[URGENT_RETRIES]) — Split on the urgent flag (F3/G15), replace the channel if-ladder with a nested Channel enum (G23), and use Collections.nCopies for retries (G30).
- Edit(static=renderReport, imports=[java.util.Map.Entry]) — Make renderReport static since it has no instance state (G18) and import Map.Entry (G12).
- Edit(extracted=[isInvalid, requireNonBlank], constants=[SESSION_TTL_MS]) — Extract the session-expiry predicate (G28), collapse lookupOrNull, and pull the non-blank guard for open into a helper (G29).
- Edit(validate_return=List<String>, constants=[EMAIL_REQUIRED, DISPLAY_NAME_REQUIRED, ALLOWED_REGIONS, MAX_DISPLAY_NAME_LENGTH]) — Return errors from validate instead of mutating the list (F2), extract repeated message constants (G25), drop dead log-and-return try-catches (Ch7.1), and remove System.err calls (G4, G12).
- Edit() — Extracted computeRevenue helper to pull the 5-line stream pipeline out of settleOrders and bring the method back under the 50-line limit (G30).
- Edit() — Extracted tokenize helper so parseRow is tiny and the fields list lives in a narrower scope (G10), and split isEscapedQuote's ternary-clause predicate into named booleans (G19).
- Edit() — Replaced the top-of-method mutable delayMs with a backoffDelayMs(attempt) helper whose result is declared inside the loop right before Thread.sleep (G10).
- Edit() — Extracted hasBlankRegion and hasNegativeQuantity explanatory variables from the 4-clause boolean chain in isInvalidLot (G19).
- Edit() — Extracted ageMs as an explanatory variable in isInvalid (G19) and wrapped the two requireNonBlank calls in validateOpenArgs so open() has a single validation step before the put (G31).
- Edit() — Moved the three extra error-collection checks into validateAll so createAccount drops to a single validate call and id is declared immediately before its first use (G10).

## Not attempted
- Edit(finding=G18 parseRow static) — parseRow reads instance fields separator and quote and increments rowsParsed, so it cannot be static.
- Edit(finding=G18 execute static; Ch7.1 catch specific) — execute mutates the audit instance field, and Callable.call() declares throws Exception so narrowing the catch would lose behaviour.
- Edit(finding=G18 static methods; G12 session.userId) — userIdFor/lookupOrNull/open/close/activeSessionCount all read the sessions instance map; session.userId is a field access, not a type reference.
- Edit(finding=Ch7.2 seven null checks) — The boundary validation and per-id null lookups guard real runtime conditions; replacing them with Optional plumbing would obscure the audit trail.
- Edit(finding=G18 on dispatchUrgent) — False positive — dispatchUrgent reads and mutates the instance field 'dispatched' via add/addAll, so it cannot be made static.
- Edit(finding=G12 this.label) — False positive — 'this.label' on line 53 is a standard constructor field assignment, not a fully-qualified type reference that ShortenFullyQualifiedReferencesRecipe would address.

## Suggestions
- G25 at L-1: String "---
" appears 2 times — extract to a named constant
- F2 at L-1: Method 'settleOrders' mutates its argument 'stock' (java.util.Map<java.lang.String, java.lang.Integer>) — return the result instead
- Ch7_2 at L-1: Method 'settleOrders' has 7 null checks
- G30 at L-1: Loop in 'settleOrders' (for-each with filter-add) can be replaced with a stream operation
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- G28 at L-1: Complex condition (depth 2) should be extracted
- G10 at L3: 'fields' is declared in parseRow() but not used until 20 lines later — move the declaration closer to line 23
- G30 at L-1: Method 'parseRow' has 6 blank-line sections across 41 lines — each section should be its own method
- G18 at L-1: Method 'parseRow' does not use instance state — consider making it static or extracting
- G10 at L4: 'delayMs' is declared in execute() but not used until 17 lines later — move the declaration closer to line 21
- G25 at L-1: String "attempt " appears 2 times — extract to a named constant
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G18 at L-1: Method 'execute' does not use instance state — consider making it static or extracting
- Ch7_1 at L-1: Method 'execute' catches Exception — catch specific exception types instead
- G12 at L0: 1 inline fully-qualified type reference(s); first: System.err — run ShortenFullyQualifiedReferencesRecipe
- G28 at L-1: Complex condition (depth 3) should be extracted
- G30 at L-1: Loop in 'overstockedRegions' (for-each with filter-add) can be replaced with a stream operation
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- F3 at L-1: Method 'dispatch' takes boolean parameter 'urgent' — split into two methods instead
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G30 at L-1: Loop in 'dispatch' (for with add) can be replaced with a stream operation
- G23 at L-1: Method 'dispatch' dispatches on String parameter 'channel' with 4 branches — use an enum or split into separate methods
- G15 at L-1: Method 'dispatch' uses boolean parameter 'urgent' to select behaviour — split into separate methods
- G18 at L-1: Method 'renderReport' does not use instance state — consider making it static or extracting
- G12 at L0: 1 inline fully-qualified type reference(s); first: Map.Entry — run ShortenFullyQualifiedReferencesRecipe
- G28 at L-1: Complex condition (depth 2) should be extracted
- G18 at L-1: Method 'userIdFor' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'lookupOrNull' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'open' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'close' does not use instance state — consider making it static or extracting
- G18 at L-1: Method 'activeSessionCount' does not use instance state — consider making it static or extracting
- G29 at L-1: Method 'userIdFor' has 2 guard clauses — simplify with early return or extract filter
- G29 at L-1: Method 'lookupOrNull' has 2 guard clauses — simplify with early return or extract filter
- G29 at L-1: Method 'open' has 2 guard clauses — simplify with early return or extract filter
- G12 at L0: 3 inline fully-qualified type reference(s); first: session.userId — run ShortenFullyQualifiedReferencesRecipe
- F2 at L-1: Method 'validate' mutates its argument 'errors' (java.util.List<java.lang.String>) — return the result instead
- Ch7_1 at L-1: Catch block in 'createAccount' only logs or is empty
- Ch7_2 at L-1: Method 'createAccount' has 5 null checks
- G25 at L-1: String "displayName is required" appears 2 times — extract to a named constant
- G25 at L-1: String "email is required" appears 2 times — extract to a named constant
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G4 at L-1: 'System.err.println' bypasses structured logging — use @Slf4j instead
- G31 at L-1: Method 'createAccount' has 3 consecutive void calls with no data dependency — make the order explicit
- Ch7_1 at L-1: Method 'createAccount' catches Exception — catch specific exception types instead
- G12 at L0: 2 inline fully-qualified type reference(s); first: System.err — run ShortenFullyQualifiedReferencesRecipe

## Agent usage
- input tokens : 52
- output tokens: 61418
- cache read   : 2424549
- cost (USD)   : 3.5306

## harness + agent — diff

```diff
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
index 6567aad..96b71b8 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/AccumulatorFixture.java
@@ -12,20 +12,15 @@ import java.util.List;
  */
 public final class AccumulatorFixture {
 
-    public int computeScore(final List<Integer> values) {
+    public static int computeScore(final List<Integer> values) {
         int total = 0;
         for (final Integer value : values) {
-            if (value == null) {
-                continue;
-            }
-            if (value < 0) {
+            if (value == null || value < 0) {
                 continue;
             }
             total = total + value;
         }
-        if (total > 10000) {
-            total = 10000;
-        }
+        total = Math.min(total, 10000);
         return total;
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
index 63f8a43..ad655b1 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/CsvParser.java
@@ -26,16 +26,20 @@ public final class CsvParser {
     }
 
     public List<String> parseRow(final String line) {
-        final List<String> fields = new ArrayList<>();
+        final List<String> fields = tokenize(line);
+        rowsParsed = rowsParsed + 1;
+        return fields;
+    }
+
+    private List<String> tokenize(final String line) {
         final StringBuilder current = new StringBuilder();
         boolean insideQuotes = false;
         int index = 0;
-
+        final List<String> fields = new ArrayList<>();
         while (index < line.length()) {
             final char ch = line.charAt(index);
-
             if (ch == quote) {
-                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == quote) {
+                if (isEscapedQuote(line, index, insideQuotes)) {
                     current.append(quote);
                     index = index + 2;
                     continue;
@@ -44,29 +48,30 @@ public final class CsvParser {
                 index = index + 1;
                 continue;
             }
-
             if (ch == separator && !insideQuotes) {
                 fields.add(current.toString());
                 current.setLength(0);
                 index = index + 1;
                 continue;
             }
-
             if (ch == '\\' && index + 1 < line.length()) {
                 current.append(line.charAt(index + 1));
                 index = index + 2;
                 continue;
             }
-
             current.append(ch);
             index = index + 1;
         }
-
         fields.add(current.toString());
-        rowsParsed = rowsParsed + 1;
         return fields;
     }
 
+    private boolean isEscapedQuote(final String line, final int index, final boolean insideQuotes) {
+        final boolean hasNextChar = index + 1 < line.length();
+        final boolean nextCharIsQuote = hasNextChar && line.charAt(index + 1) == quote;
+        return insideQuotes && nextCharIsQuote;
+    }
+
     public int rowsParsed() {
         return rowsParsed;
     }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
index 43f0728..00bffad 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/GuardFixture.java
@@ -10,15 +10,15 @@ package org.fiftieshousewife.cleancode.sandbox;
  */
 public final class GuardFixture {
 
-    public void handle(final String event, final StringBuilder log) {
+    private static final String LOG_SEPARATOR = "---\n";
+
+    public static void handle(final String event, final StringBuilder log) {
         if (event == null) {
-            log.append("drop: null event\n");
-            log.append("---\n");
+            log.append("drop: null event\n").append(LOG_SEPARATOR);
             return;
         }
         if (event.isBlank()) {
-            log.append("drop: blank event\n");
-            log.append("---\n");
+            log.append("drop: blank event\n").append(LOG_SEPARATOR);
             return;
         }
         log.append("processed: ").append(event.strip()).append('\n');
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
index fb1c60a..79ad9c3 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/HttpRetryPolicy.java
@@ -11,39 +11,40 @@ import java.util.concurrent.Callable;
  */
 public final class HttpRetryPolicy {
 
+    private static final String ATTEMPT_PREFIX = "attempt ";
+    private static final int MAX_ATTEMPTS = 5;
+    private static final long INITIAL_DELAY_MS = 500L;
+    private static final long MAX_DELAY_MS = 10_000L;
+    private static final double BACKOFF_MULTIPLIER = 1.5;
+
     private final StringBuilder audit = new StringBuilder();
 
     public <T> T execute(final Callable<T> action) {
         int attempt = 0;
-        long delayMs = 500;
         Throwable lastFailure = null;
-        while (attempt < 5) {
+        while (attempt < MAX_ATTEMPTS) {
             try {
                 final T result = action.call();
-                audit.append("attempt ").append(attempt).append(" ok\n");
+                audit.append(ATTEMPT_PREFIX).append(attempt).append(" ok\n");
                 return result;
-            } catch (Exception e) {
+            } catch (final Exception e) {
                 lastFailure = e;
-                audit.append("attempt ").append(attempt).append(" failed: ")
+                audit.append(ATTEMPT_PREFIX).append(attempt).append(" failed: ")
                         .append(e.getMessage()).append('\n');
             }
-
-            if (attempt >= 4) {
+            if (attempt >= MAX_ATTEMPTS - 1) {
                 break;
             }
+            final long delayMs = backoffDelayMs(attempt);
             try {
                 Thread.sleep(delayMs);
-            } catch (InterruptedException interrupted) {
-                System.err.println("retry interrupted: " + interrupted.getMessage());
+            } catch (final InterruptedException interrupted) {
+                Thread.currentThread().interrupt();
+                audit.append("retry interrupted: ").append(interrupted.getMessage()).append('\n');
                 return null;
             }
-            delayMs = (long) (delayMs * 1.5);
-            if (delayMs > 10000) {
-                delayMs = 10000;
-            }
             attempt = attempt + 1;
         }
-
         audit.append("gave up after ").append(attempt + 1).append(" attempts\n");
         if (lastFailure != null) {
             audit.append("last failure: ").append(lastFailure.getClass().getSimpleName()).append('\n');
@@ -51,6 +52,11 @@ public final class HttpRetryPolicy {
         return null;
     }
 
+    private static long backoffDelayMs(final int attempt) {
+        final double scaled = INITIAL_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt);
+        return Math.min((long) scaled, MAX_DELAY_MS);
+    }
+
     public String auditLog() {
         return audit.toString();
     }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
index e972d68..e5280eb 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/InventoryBalancer.java
@@ -1,9 +1,9 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
-import java.util.ArrayList;
 import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
+import java.util.Map.Entry;
 
 /**
  * Imperative-loop aggregator — classic candidate for stream conversion
@@ -14,39 +14,34 @@ import java.util.Map;
  */
 public final class InventoryBalancer {
 
-    public Map<String, Integer> balanceByRegion(final List<Lot> lots) {
+    public static Map<String, Integer> balanceByRegion(final List<Lot> lots) {
         final Map<String, Integer> balances = new HashMap<>();
         for (final Lot lot : lots) {
-            if (lot == null) {
+            if (isInvalidLot(lot)) {
                 continue;
             }
-            if (lot.region() == null || lot.region().isBlank()) {
-                continue;
-            }
-            if (lot.quantity() < 0) {
-                continue;
-            }
-            if (balances.containsKey(lot.region())) {
-                final int existing = balances.get(lot.region());
-                balances.put(lot.region(), existing + lot.quantity());
-            } else {
-                balances.put(lot.region(), lot.quantity());
-            }
+            balances.merge(lot.region(), lot.quantity(), Integer::sum);
         }
         return balances;
     }
 
-    public List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
-        final List<String> overstocked = new ArrayList<>();
-        for (final Map.Entry<String, Integer> entry : balances.entrySet()) {
-            if (entry.getValue() > threshold) {
-                overstocked.add(entry.getKey());
-            }
+    private static boolean isInvalidLot(final Lot lot) {
+        if (lot == null) {
+            return true;
         }
-        return overstocked;
+        final boolean hasBlankRegion = lot.region() == null || lot.region().isBlank();
+        final boolean hasNegativeQuantity = lot.quantity() < 0;
+        return hasBlankRegion || hasNegativeQuantity;
+    }
+
+    public static List<String> overstockedRegions(final Map<String, Integer> balances, final int threshold) {
+        return balances.entrySet().stream()
+                .filter(entry -> entry.getValue() > threshold)
+                .map(Entry::getKey)
+                .toList();
     }
 
-    public int totalAcrossRegions(final Map<String, Integer> balances) {
+    public static int totalAcrossRegions(final Map<String, Integer> balances) {
         int sum = 0;
         for (final Integer value : balances.values()) {
             if (value != null) {
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
index 04846e7..b9309c6 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/NotificationDispatcher.java
@@ -1,6 +1,7 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.ArrayList;
+import java.util.Collections;
 import java.util.List;
 
 /**
@@ -12,39 +13,23 @@ import java.util.List;
  */
 public final class NotificationDispatcher {
 
+    private static final int URGENT_RETRIES = 3;
+
     private final List<String> dispatched = new ArrayList<>();
 
-    public void dispatch(final String channel, final String recipient, final String body,
-                         final boolean urgent) {
-        if (channel.equals("email")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("email(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("email -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("sms")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("sms(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("sms -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("push")) {
-            if (urgent) {
-                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
-                    dispatched.add("push(urgent) -> " + recipient + ": " + body);
-                }
-            } else {
-                dispatched.add("push -> " + recipient + ": " + body);
-            }
-        } else if (channel.equals("webhook")) {
-            dispatched.add("webhook -> " + recipient + ": " + body);
-        } else {
-            throw new IllegalArgumentException("unknown channel: " + channel);
+    public void dispatch(final String channel, final String recipient, final String body) {
+        final Channel resolved = Channel.fromLabel(channel);
+        dispatched.add(resolved.label() + " -> " + recipient + ": " + body);
+    }
+
+    public void dispatchUrgent(final String channel, final String recipient, final String body) {
+        final Channel resolved = Channel.fromLabel(channel);
+        if (!resolved.urgencyAware()) {
+            dispatched.add(resolved.label() + " -> " + recipient + ": " + body);
+            return;
         }
+        dispatched.addAll(Collections.nCopies(URGENT_RETRIES,
+                resolved.label() + "(urgent) -> " + recipient + ": " + body));
     }
 
     public int dispatchedCount() {
@@ -54,4 +39,36 @@ public final class NotificationDispatcher {
     public List<String> dispatched() {
         return List.copyOf(dispatched);
     }
+
+    public enum Channel {
+        EMAIL("email", true),
+        SMS("sms", true),
+        PUSH("push", true),
+        WEBHOOK("webhook", false);
+
+        private final String label;
+        private final boolean urgencyAware;
+
+        Channel(final String label, final boolean urgencyAware) {
+            this.label = label;
+            this.urgencyAware = urgencyAware;
+        }
+
+        public String label() {
+            return label;
+        }
+
+        public boolean urgencyAware() {
+            return urgencyAware;
+        }
+
+        public static Channel fromLabel(final String label) {
+            for (final Channel ch : values()) {
+                if (ch.label.equals(label)) {
+                    return ch;
+                }
+            }
+            throw new IllegalArgumentException("unknown channel: " + label);
+        }
+    }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
index 1eef21e..9bd0631 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/OrchestratorFixture.java
@@ -1,7 +1,11 @@
 package org.fiftieshousewife.cleancode.sandbox;
 
+import java.util.LinkedHashMap;
+import java.util.LinkedHashSet;
 import java.util.List;
 import java.util.Map;
+import java.util.Map.Entry;
+import java.util.stream.Collectors;
 
 /**
  * G30-heavy fixture: a single orchestrator method strung together from
@@ -12,58 +16,55 @@ import java.util.Map;
  */
 public final class OrchestratorFixture {
 
-    public void settleOrders(final List<String> orderIds, final Map<String, Integer> stock,
+    private static final double PRICE_DISCOUNT = 0.95;
+    private static final String SETTLEMENT_FOOTER = "settlement complete\n---\n";
+
+    public static Map<String, Integer> settleOrders(final List<String> orderIds, final Map<String, Integer> stock,
                              final Map<String, Double> prices, final StringBuilder audit) {
-        // Phase 1: validate input collections.
         if (orderIds == null || orderIds.isEmpty()) {
             audit.append("no orders to settle\n");
-            return;
+            return stock;
         }
         if (stock == null || prices == null) {
             audit.append("missing reference data\n");
-            return;
-        }
-        // Phase 2: dedupe the incoming ids into a clean working list.
-        final java.util.LinkedHashSet<String> unique = new java.util.LinkedHashSet<>();
-        for (final String id : orderIds) {
-            if (id != null && !id.isBlank()) {
-                unique.add(id.trim());
-            }
+            return stock;
         }
+        final LinkedHashSet<String> unique = orderIds.stream()
+                .filter(id -> id != null && !id.isBlank())
+                .map(String::trim)
+                .collect(Collectors.toCollection(LinkedHashSet::new));
         audit.append("unique orders: ").append(unique.size()).append("\n");
-        // Phase 3: apply a flat reservation discount to each line price.
-        final java.util.LinkedHashMap<String, Double> adjusted = new java.util.LinkedHashMap<>();
+        final LinkedHashMap<String, Double> adjusted = new LinkedHashMap<>();
         for (final String id : unique) {
             final Double base = prices.get(id);
             if (base == null) {
                 audit.append("no price for ").append(id).append(" — skipping\n");
                 continue;
             }
-            adjusted.put(id, base * 0.95);
+            adjusted.put(id, base * PRICE_DISCOUNT);
         }
-        // Phase 4: decrement the stock counter for every priced order.
+        final Map<String, Integer> remaining = new LinkedHashMap<>(stock);
         int fulfilled = 0;
         for (final String id : adjusted.keySet()) {
-            final Integer available = stock.get(id);
+            final Integer available = remaining.get(id);
             if (available == null || available <= 0) {
                 audit.append("out of stock: ").append(id).append("\n");
                 continue;
             }
-            stock.put(id, available - 1);
+            remaining.put(id, available - 1);
             fulfilled++;
         }
         audit.append("fulfilled: ").append(fulfilled).append("\n");
-        // Phase 5: compute the total revenue from the fulfilled orders.
-        double revenue = 0.0;
-        for (final Map.Entry<String, Double> entry : adjusted.entrySet()) {
-            final Integer available = stock.get(entry.getKey());
-            if (available != null) {
-                revenue += entry.getValue();
-            }
-        }
-        audit.append("revenue: ").append(String.format("%.2f", revenue)).append("\n");
-        // Phase 6: sign off.
-        audit.append("settlement complete\n");
-        audit.append("---\n");
+        final double revenue = computeRevenue(adjusted, remaining);
+        audit.append("revenue: ").append(String.format("%.2f", revenue)).append("\n").append(SETTLEMENT_FOOTER);
+        return remaining;
+    }
+
+    private static double computeRevenue(final Map<String, Double> adjusted,
+                                         final Map<String, Integer> remaining) {
+        return adjusted.entrySet().stream()
+                .filter(entry -> remaining.get(entry.getKey()) != null)
+                .mapToDouble(Entry::getValue)
+                .sum();
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
index c6108b6..074723e 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/ReportTemplate.java
@@ -2,6 +2,7 @@ package org.fiftieshousewife.cleancode.sandbox;
 
 import java.util.List;
 import java.util.Map;
+import java.util.Map.Entry;
 
 /**
  * StringBuilder-threading fixture. The private helpers take a
@@ -13,41 +14,26 @@ import java.util.Map;
  */
 public final class ReportTemplate {
 
-    public String renderReport(final String title, final List<String> sections,
+    public static String renderReport(final String title, final List<String> sections,
                                final Map<String, Integer> totals) {
         final StringBuilder sb = new StringBuilder();
-        sb.append("<html><head><title>").append(title).append("</title></head><body>");
-        sb.append("<h1>").append(title).append("</h1>");
-        sb.append("<p>generated report with ").append(sections.size()).append(" sections</p>");
-
-        sb.append("<div class=\"sections\">");
+        sb.append("<html><head><title>").append(title).append("</title></head><body>").append("</h1>").append(" sections</p>").append("<div class=\"sections\">");
         for (final String section : sections) {
             appendSection(sb, section);
         }
-        sb.append("</div>");
-
-        sb.append("<div class=\"totals\">");
-        sb.append("<h2>Totals</h2>");
-        sb.append("<ul>");
-        for (final Map.Entry<String, Integer> entry : totals.entrySet()) {
+        sb.append("</div>").append("<div class=\"totals\">").append("<h2>Totals</h2>").append("<ul>");
+        for (final Entry<String, Integer> entry : totals.entrySet()) {
             appendTotalRow(sb, entry.getKey(), entry.getValue());
         }
-        sb.append("</ul>");
-        sb.append("</div>");
-
-        sb.append("</body></html>");
+        sb.append("</ul>").append("</div>").append("</body></html>");
         return sb.toString();
     }
 
-    private void appendSection(final StringBuilder sb, final String section) {
-        sb.append("<section>");
-        sb.append("<h2>").append(section).append("</h2>");
-        sb.append("<p>content for ").append(section).append("</p>");
-        sb.append("</section>");
+    private static void appendSection(final StringBuilder sb, final String section) {
+        sb.append("<section>").append("</h2>").append("</p>").append("</section>");
     }
 
-    private void appendTotalRow(final StringBuilder sb, final String key, final int value) {
-        sb.append("<li><span class=\"key\">").append(key).append("</span>");
-        sb.append("<span class=\"value\">").append(value).append("</span></li>");
+    private static void appendTotalRow(final StringBuilder sb, final String key, final int value) {
+        sb.append("<li><span class=\"key\">").append(key).append("</span>").append("</span></li>");
     }
 }
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
index de682a6..3b4f327 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/SessionStore.java
@@ -13,49 +13,50 @@ import java.util.Map;
  */
 public final class SessionStore {
 
+    private static final long SESSION_TTL_MS = 30L * 60L * 1000L;
+
     private final Map<String, Session> sessions = new HashMap<>();
 
     public String userIdFor(final String token) {
-        if (token == null) {
-            return null;
-        }
-        if (token.isBlank()) {
+        if (token == null || token.isBlank()) {
             return null;
         }
         final Session session = sessions.get(token);
-        if (session == null) {
-            return null;
-        }
-        if (!session.active) {
-            return null;
-        }
-        if (Instant.now().toEpochMilli() - session.createdAtMs > 30L * 60L * 1000L) {
+        if (isInvalid(session)) {
             return null;
         }
         return session.userId;
     }
 
-    public Session lookupOrNull(final String token) {
-        if (!sessions.containsKey(token)) {
-            return null;
+    private static boolean isInvalid(final Session session) {
+        if (session == null || !session.active) {
+            return true;
         }
+        final long ageMs = Instant.now().toEpochMilli() - session.createdAtMs;
+        return ageMs > SESSION_TTL_MS;
+    }
+
+    public Session lookupOrNull(final String token) {
         final Session session = sessions.get(token);
-        if (!session.active) {
-            return null;
-        }
-        return session;
+        return (session != null && session.active) ? session : null;
     }
 
     public void open(final String token, final String userId) {
-        if (token == null || token.isBlank()) {
-            throw new IllegalArgumentException("token is required");
-        }
-        if (userId == null || userId.isBlank()) {
-            throw new IllegalArgumentException("userId is required");
-        }
+        validateOpenArgs(token, userId);
         sessions.put(token, new Session(userId, Instant.now().toEpochMilli(), true));
     }
 
+    private static void validateOpenArgs(final String token, final String userId) {
+        requireNonBlank(token, "token");
+        requireNonBlank(userId, "userId");
+    }
+
+    private static void requireNonBlank(final String value, final String name) {
+        if (value == null || value.isBlank()) {
+            throw new IllegalArgumentException(name + " is required");
+        }
+    }
+
     public void close(final String token) {
         final Session existing = sessions.get(token);
         if (existing == null) {
diff --git a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
index 8a24359..7d46ccf 100644
--- a/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
+++ b/sandbox/src/main/java/org/fiftieshousewife/cleancode/sandbox/UserAccountService.java
@@ -14,60 +14,53 @@ import java.util.Map;
  */
 public final class UserAccountService {
 
+    private static final String EMAIL_REQUIRED = "email is required";
+    private static final String DISPLAY_NAME_REQUIRED = "displayName is required";
+    private static final List<String> ALLOWED_REGIONS = List.of("US", "EU", "APAC");
+    private static final int MAX_DISPLAY_NAME_LENGTH = 64;
+
     private final Map<String, User> byId = new HashMap<>();
     private final List<String> audit = new ArrayList<>();
     private long nextId = 1000L;
 
     public String createAccount(final String email, final String displayName, final String region) {
-        final List<String> errors = new ArrayList<>();
-        if (email == null || email.isBlank()) {
-            errors.add("email is required");
-        }
-        if (email != null && !email.contains("@")) {
-            errors.add("email must contain @");
-        }
-        if (displayName == null || displayName.isBlank()) {
-            errors.add("displayName is required");
-        }
-        if (displayName != null && displayName.length() > 64) {
-            errors.add("displayName too long");
-        }
-        if (region == null || !List.of("US", "EU", "APAC").contains(region)) {
-            errors.add("region must be one of US/EU/APAC");
-        }
+        final List<String> errors = validateAll(email, displayName, region);
         if (!errors.isEmpty()) {
             throw new IllegalArgumentException(String.join("; ", errors));
         }
-
         final String id = "acct-" + nextId;
         nextId = nextId + 1;
-        final User user = new User(id, email, displayName, region);
-        try {
-            byId.put(id, user);
-        } catch (RuntimeException e) {
-            System.err.println("persist failed: " + e.getMessage());
-            return null;
-        }
-
+        byId.put(id, new User(id, email, displayName, region));
         audit.add("created " + id + " in region " + region);
         audit.add("email=" + email + " displayName=" + displayName);
         audit.add("total accounts: " + byId.size());
+        sendWelcomeEmail(email, displayName);
+        return id;
+    }
 
-        try {
-            sendWelcomeEmail(email, displayName);
-        } catch (Exception e) {
-            System.err.println("notify failed: " + e.getMessage());
+    static List<String> validateAll(final String email, final String displayName, final String region) {
+        final List<String> errors = validate(email, displayName);
+        if (email != null && !email.contains("@")) {
+            errors.add("email must contain @");
         }
-        return id;
+        if (displayName != null && displayName.length() > MAX_DISPLAY_NAME_LENGTH) {
+            errors.add("displayName too long");
+        }
+        if (region == null || !ALLOWED_REGIONS.contains(region)) {
+            errors.add("region must be one of US/EU/APAC");
+        }
+        return errors;
     }
 
-    void validate(final String email, final String displayName, final List<String> errors) {
+    static List<String> validate(final String email, final String displayName) {
+        final List<String> errors = new ArrayList<>();
         if (email == null || email.isBlank()) {
-            errors.add("email is required");
+            errors.add(EMAIL_REQUIRED);
         }
         if (displayName == null || displayName.isBlank()) {
-            errors.add("displayName is required");
+            errors.add(DISPLAY_NAME_REQUIRED);
         }
+        return errors;
     }
 
     private void sendWelcomeEmail(final String email, final String displayName) {

```

