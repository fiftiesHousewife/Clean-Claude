# ExtractMethodRecipe — usage

First-cut port of IntelliJ IDEA Community's `ExtractMethodProcessor`,
living at `refactoring/src/main/java/org/fiftieshousewife/cleancode/refactoring/extractmethod/`.
Full class-level Javadoc on `ExtractMethodRecipe` describes the supported
extraction shapes and the known rejection reasons.

## What it does

Given `(file, startLine, endLine, newMethodName)`, finds the method that
contains those lines, extracts the contiguous run of top-level statements
between them into a new `private` method (or `static` if the enclosing
method is static), and replaces the original range with a call to the
new method.

Three extraction shapes are supported today:

1. **No control-flow escape, no output.** Inputs are read-only outer
   locals or parameters; extracted method returns `void`; call site is
   `newMethod(args);`.
2. **Internal-local output.** One local declared inside the range is used
   after it; extracted method returns its type; call site is
   `Type x = newMethod(args);`.
3. **Outer-local reassignment.** One local declared before the range is
   written inside it and read after; extracted method returns the new
   value; call site is `x = newMethod(args);` — no redeclaration.
4. **Void conditional-exit.** Range contains bare `return;` in a void
   enclosing method; extracted method returns `boolean`; each `return;`
   inside the body becomes `return true;`; call site is
   `if (newMethod(args)) return;`.

The recipe **rejects** (no-op) otherwise — for example when the range
contains `break`, `continue`, `throw`, a non-bare `return`, more than
one output value, `var`-typed output, or a generic enclosing method.
These are documented in the `ExitMode` and `ExtractionAnalysis` javadoc
and are candidates for follow-up phases.

## Three ways to invoke it

### 1. From the command line (agent / human)

The refactoring module ships a `extractMethod` Gradle task that wraps
the recipe's CLI entry point:

```
./gradlew :refactoring:extractMethod \
    -Pfile=<path-relative-or-absolute-to-target.java> \
    -PstartLine=<first-line-1-indexed> \
    -PendLine=<last-line-1-indexed> \
    -PnewMethodName=<identifier>
```

The task parses the target file, runs the recipe, and writes the result
back to the same path. If the recipe rejects the range, the task prints
why and leaves the file unchanged.

Example:

```
./gradlew :refactoring:extractMethod \
    -Pfile=core/src/main/java/.../HeuristicDescriptions.java \
    -PstartLine=80 -PendLine=120 \
    -PnewMethodName=nameForClassHeuristic
```

Commit the result only after running the module's tests; the recipe is
correct-by-construction for the rejection cases it knows about, but its
line-range finder and regex-based read/write detection are conservative
text scans, not a full dataflow analysis.

### 2. Programmatically, inside an OpenRewrite recipe chain

When you want several fixes applied together — e.g. extract method,
then shorten FQNs, then invert a negative conditional — construct the
recipe in code and add it to a `RecipeList`:

```java
final var extract = new ExtractMethodRecipe(
        "HeuristicDescriptions.java", 80, 120, "nameForClassHeuristic");
final var shortenFqn = new ShortenFullyQualifiedReferencesRecipe();
final var recipeList = List.of(extract, shortenFqn);
// then drive via LargeSourceSet → recipe.run(...)
```

See `ExtractMethodRecipeTest.runRecipe` for the minimal setup.

### 3. Agent workflow

For a G30 / Ch10.1 fix brief where the skill tells the agent to extract
a method, the agent:

1. Reads the method and picks a contiguous statement range plus a name.
2. Invokes `./gradlew :refactoring:extractMethod -Pfile=… -PstartLine=…`.
3. Runs `./gradlew :<module>:test` to confirm behaviour preserved.
4. Commits with `fix: <File> (G30:extract)`.

If the task's output begins with `no extraction performed`, the agent
reads the recipe's rejection javadoc, picks a different range or
unblocks the constraint manually (e.g. renaming a clashing identifier),
then retries.

## Known limits (v1)

- Break/continue/non-bare return → reject. A real control-flow graph
  (upstream: `ControlFlowWrapper`) is needed to distinguish escapes
  from loop-internal jumps.
- Reference-type conditional-exit (`return expr;`) → reject. Phase C.
- `var`-typed output locals → reject. Phase D (resolve via JavaType).
- Generic enclosing methods → reject. Phase E.
- Range must be a contiguous run of top-level statements — no
  expression-level or cross-block selections.
