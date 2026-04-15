# Clean Code Heuristics Reference

A complete reference for every heuristic from Robert C. Martin's *Clean Code: A Handbook of Agile Software Craftsmanship* (Prentice Hall, 2008). Each entry includes Martin's original name, the chapter and page reference, what the heuristic means, how this plugin detects it, and what to do when it's found.

---

## Comments

### C1: Inappropriate Information
*Ch.17 p.286*

Comments should be reserved for technical notes about the code and its design. Anything that belongs in another system — changelogs, author attributions, issue tracker references, dates, revision histories — is inappropriate information that clutters the source and drifts out of date. Meta-data like this belongs in source control, not in comments.

**Detection:** Manual review only. No automated detection.

---

### C2: Obsolete Comment
*Ch.17 p.286*

A comment that has become inaccurate is worse than no comment at all. It actively misleads. If the code has changed and the comment hasn't, delete it or rewrite it. Floating, forgotten comments are a form of lying. Old comments tend to migrate away from the code they once described, becoming orphans that confuse readers.

**Detection:** Manual review only. No automated detection.

---

### C3: Redundant Comment
*Ch.17 p.286*

A comment that merely restates the code is clutter. `i++ // increment i` teaches us nothing. If the code is so unclear that it needs a comment to explain what it does, the real solution is to make the code clearer — not to add a redundant comment on top of unclear code. Comments should say things the code cannot say for itself.

**Detection:** [MumblingCommentRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/MumblingCommentRecipe.java) — detects comments that restate the method name or parameter names.
**Skill file:** [comments-and-clutter.md](.claude/skills/comments-and-clutter.md)

---

### C4: Poorly Written Comment
*Ch.17 p.287*

If you are going to write a comment, take the time to make sure it is the best comment you can write. Choose your words carefully. Use correct grammar and punctuation. Don't ramble. Don't state the obvious. Make it brief. A sloppy comment is a sign that the author doesn't care — and that carelessness infects the code around it.

**Detection:** Manual review only. No automated detection.

---

### C5: Commented-Out Code
*Ch.17 p.287*

Commented-out code rots. Others who see it won't have the courage to delete it — they'll assume it's there for a reason. Over time, commented-out code accumulates like sediment, obscuring the code that actually matters. Delete it. Source control remembers everything; you don't need to.

**Detection:** [CommentedCodeRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/CommentedCodeRecipe.java) — detects commented-out code blocks.
**Skill file:** [comments-and-clutter.md](.claude/skills/comments-and-clutter.md)

---

## Environment

### E1: Build Requires More Than One Step
*Ch.17 p.287*

You should be able to build the system with a single trivial command. You should not have to search around for dependencies, scripts, or obscure commands. Outdated dependencies make that one-step build fragile — every stale library is a silent accumulation of risk, incompatibility, and unfixed vulnerabilities.

**Detection:** [DependencyUpdatesFindingSource](adapters/src/main/java/org/fiftieshousewife/cleancode/adapters/DependencyUpdatesFindingSource.java) — parses Ben-Manes dependency update reports for outdated minor versions.

---

### E2: Tests Require More Than One Step
*Ch.17 p.287*

You should be able to run all the unit tests with a single trivial command. Being able to run tests quickly, easily, and without fuss is so fundamental that a failure here poisons the entire development experience.

**Detection:** Manual review only. No automated detection.

---

## Functions

### F1: Too Many Arguments
*Ch.17 p.288*

Functions should have a small number of arguments. The ideal number is zero. Next comes one, followed closely by two. Three arguments should be avoided where possible. More than three requires very special justification — and then shouldn't be used anyway. Arguments are hard to understand. They take conceptual power and force you to know details that aren't important at that point.

**Detection:** Checkstyle `ParameterNumber` check (threshold: 4).
**Skill file:** [functions.md](.claude/skills/functions.md)

---

### F2: Output Arguments
*Ch.17 p.288*

Output arguments are counterintuitive. Readers expect arguments to be inputs to a function, not things the function writes to. When you see `appendFooter(report)`, do you expect it to append something to report, or to append report to something else? If a function must transform something, let it transform the state of the owning object, or return the result.

**Detection:** [OutputArgumentRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/OutputArgumentRecipe.java) — detects methods that mutate collection arguments.
**Skill file:** [functions.md](.claude/skills/functions.md)

---

### F3: Flag Arguments
*Ch.17 p.288*

Boolean arguments loudly declare that the function does more than one thing. It does one thing if the flag is true and another if the flag is false. The function should be split into two: one for each path. `render(true)` tells the reader nothing — `renderForSuite()` and `renderForSingleTest()` tell them everything.

**Detection:** [FlagArgumentRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/FlagArgumentRecipe.java) — detects boolean parameters on non-private methods.
**Skill file:** [functions.md](.claude/skills/functions.md)

---

### F4: Dead Function
*Ch.17 p.288*

Methods that are never called are dead code. They clog the class, confuse the reader, and add maintenance burden. Delete them without hesitation — your source control system remembers them if you ever need them back.

**Detection:** PMD `UnusedPrivateMethod` rule.
**Skill file:** [functions.md](.claude/skills/functions.md)

---

## General

### G1: Multiple Languages in One Source File
*Ch.17 p.288*

A source file should contain one, and only one, language. The confusion that arises from a single file containing multiple languages is enormous. Inline SQL, JavaScript in Java, XML in code — all are violations.

**Detection:** Manual review only. No automated detection.

---

### G2: Obvious Behaviour Is Unimplemented
*Ch.17 p.288*

Following the Principle of Least Surprise, any function or class should implement the behaviours that another programmer could reasonably expect. When an obvious behaviour is not implemented, readers and users of the code can no longer depend on their intuition about function names.

**Detection:** Manual review only. Narrative stub generated in CLAUDE.md for team annotation.

---

### G3: Incorrect Behaviour at the Boundaries
*Ch.17 p.289*

It seems obvious to say that code should behave correctly. The devil is in the details. Don't rely on your intuition. Look for every boundary condition and write a test for it.

**Detection:** Manual review only. Narrative stub generated in CLAUDE.md for team annotation.

---

### G4: Overridden Safeties
*Ch.17 p.289*

Don't override safeties. Turning off warnings, ignoring failing tests, catching and discarding exceptions — these are all the same mistake. Safeties exist because someone, at some point, decided they were important enough to create. Don't silence them; fix the underlying problem.

**Detection:** [UncheckedCastRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/UncheckedCastRecipe.java) — detects `@SuppressWarnings("unchecked")`. Also PMD `EmptyCatchBlock` and SpotBugs `DE_MIGHT_IGNORE`.
**Skill file:** [java-idioms.md](.claude/skills/java-idioms.md)

---

### G5: Duplication
*Ch.17 p.289*

Duplication is the root of all evil in software. Every duplication represents a missed opportunity for abstraction. When you see duplicated code, it almost always means there is a concept crying out to be a subroutine or a class. Find it and eliminate the duplication.

**Detection:** CPD (Copy/Paste Detector) token-based duplication detection.
**Skill file:** [functions.md](.claude/skills/functions.md)

---

### G6: Code at Wrong Level of Abstraction
*Ch.17 p.290*

It is important to create abstractions that separate higher-level general concepts from lower-level detailed concepts. When we find code at the wrong level of abstraction — low-level implementation in a high-level interface, or high-level orchestration in a utility class — the separation has failed.

**Detection:** Manual review only. Narrative stub generated in CLAUDE.md for team annotation.

---

### G7: Base Classes Depending on Their Derivatives
*Ch.17 p.291*

In general, base classes should know nothing about their derivatives. There are exceptions, but for the most part, when a base class references a subclass, something is wrong. The dependency should point from derivative to base, never the reverse.

**Detection:** Not yet implemented. Planned: detect `instanceof SubClass` in a parent class.

---

### G8: Too Much Information
*Ch.17 p.291*

Well-defined modules have very small interfaces that allow you to do a lot with a little. A class with too many public methods, too many fields, or too many dependencies has exposed too much of itself. Keep interfaces tight. Fewer things to know means fewer things to go wrong. Hide your data. Hide your utility functions. Hide your constants and your temporaries.

**Detection:** [VisibilityReductionRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/VisibilityReductionRecipe.java) — detects public mutable fields. Also PMD `ExcessivePublicCount`, `CouplingBetweenObjects`, `TooManyFields`.
**Skill file:** [classes.md](.claude/skills/classes.md)

---

### G9: Dead Code
*Ch.17 p.292*

Dead code is code that isn't executed. It's the body of an if statement that checks for a condition that can't happen. It's the catch block for an exception that is never thrown. It's the utility method that is never called. Dead code is not completely updated when designs change. It rots. Delete it.

**Detection:** PMD `UnusedLocalVariable`. SpotBugs `UUF_UNUSED_FIELD`.
**Skill file:** [comments-and-clutter.md](.claude/skills/comments-and-clutter.md)

---

### G10: Vertical Separation
*Ch.17 p.292*

Local variables should be declared just before their first use. Vertical separation between declaration and use makes the reader hold a mental placeholder — 'what was that variable for again?' — while reading unrelated code. Declare variables at the point of need, not at the top of the method.

**Detection:** [VerticalSeparationRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/VerticalSeparationRecipe.java) — detects declarations more than 10 lines from first use (configurable).
**Skill file:** [comments-and-clutter.md](.claude/skills/comments-and-clutter.md)

---

### G11: Inconsistency
*Ch.17 p.292*

If you do something a certain way, do all similar things the same way. If you name one method `fetchUsers`, don't name a similar method `getOrders` and another `retrieveProducts`. Inconsistency breeds confusion — the reader must wonder whether the different names signify different semantics when they don't.

**Detection:** [InconsistentNamingRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/InconsistentNamingRecipe.java) — detects mixed verb prefixes (get/fetch/retrieve, create/make/build, remove/delete) in the same class.
**Skill file:** [naming.md](.claude/skills/naming.md)

---

### G12: Clutter
*Ch.17 p.293*

Clutter is anything that adds noise without adding value: unused variables, never-called functions, redundant imports, purposeless comments. Keep your source files clean. A lean source file is easier to read, easier to understand, and easier to change.

**Detection:** PMD `UnusedImports`. Checkstyle `UnusedImports`, `RedundantImport`.
**Skill file:** [comments-and-clutter.md](.claude/skills/comments-and-clutter.md)

---

### G13: Artificial Coupling
*Ch.17 p.293*

Things that don't depend upon each other should not be artificially coupled. Artificial coupling is a coupling between two modules that serves no direct purpose. It is a result of putting a variable, constant, or function in a temporarily convenient, though inappropriate, location.

**Detection:** Manual review only. Narrative stub generated in CLAUDE.md for team annotation.

---

### G14: Feature Envy
*Ch.17 p.293*

A method that calls six methods on another object but only one on its own class has Feature Envy — it clearly wants to be over there, not here. Move the method to the class whose data it is actually manipulating. Methods should operate on the data of their own class, not reach across into another.

**Detection:** [FeatureEnvyRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/FeatureEnvyRecipe.java) — detects methods where external calls exceed double the self calls.
**Skill file:** [classes.md](.claude/skills/classes.md)

---

### G15: Selector Arguments
*Ch.17 p.294*

There is hardly anything more abominable than a dangling false argument at the end of a function call. Selector arguments are just a lazy way to avoid splitting a large function into several smaller functions.

**Detection:** See F3 (Flag Arguments). Selector arguments are a generalisation of flag arguments.

---

### G16: Obscured Intent
*Ch.17 p.295*

Obscured intent is the opposite of expressiveness. Code that uses nested ternaries, overly terse variable names, or magic expressions is prioritising brevity over clarity. The reader shouldn't need a debugger to understand what a line of code does. If you're impressed by how cleverly terse your code is, that's a bad sign.

**Detection:** [NestedTernaryRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/NestedTernaryRecipe.java) — detects ternary expressions nested inside other ternaries.
**Skill file:** [naming.md](.claude/skills/naming.md)

---

### G17: Misplaced Responsibility
*Ch.17 p.295*

One of the most important decisions a developer makes is where to put code. A data class is a class with public fields and no real behaviour — it exposes its internals and delegates all responsibility elsewhere.

**Detection:** PMD `DataClass`.
**Skill file:** [classes.md](.claude/skills/classes.md)

---

### G18: Inappropriate Static
*Ch.17 p.296*

In general you should prefer non-static methods to static methods. If you really want a function to be static, make sure there is no chance that you'll want it to behave polymorphically. Static methods cannot be overridden and cannot participate in dependency injection.

**Detection:** PMD analysis. Mapped to G18 via adapter.
**Skill file:** [classes.md](.claude/skills/classes.md)

---

### G19: Use Explanatory Variables
*Ch.17 p.296*

Complex expressions should be broken into intermediate variables with explanatory names. `wasPressed` is better than `event.getTarget().isButton() && event.getState() == CLICKED`. The intermediate variable documents the programmer's intent and makes the code read like a well-written paragraph.

**Detection:** [MissingExplanatoryVariableRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/MissingExplanatoryVariableRecipe.java) — detects complex inline expressions (chain depth >= 3 or 4+ binary operators).
**Skill file:** [conditionals-and-expressions.md](.claude/skills/conditionals-and-expressions.md)

---

### G20: Function Names Should Say What They Do
*Ch.17 p.297*

If you have to look at the implementation of a function to know what it does, then you should work to find a better name, or rearrange the functionality so that it can be placed in a function with a better name. The name of a function should tell you exactly what it does, unambiguously, without surprises.

**Detection:** PMD `UseLocaleWithCaseConversions` (functions that don't say what locale they use). Narrative stub in CLAUDE.md for broader cases.

---

### G21: Understand the Algorithm
*Ch.17 p.297*

Before you consider yourself to be done with a function, make sure you understand how it works. It is not good enough that it passes all the tests. You must know that the solution is correct. The best way to gain this knowledge is to refactor the function into something so clean and expressive that it is obvious how it works.

**Detection:** Manual review only. Narrative stub generated in CLAUDE.md for team annotation.

---

### G22: Make Logical Dependencies Physical
*Ch.17 p.298*

If something in your code can logically be constant — a variable that's never reassigned, an object that's never swapped — then declare it final. This communicates intent to every future reader: 'this value is set once and never changes.' It prevents accidental reassignment and signals a design where fewer things are moving at once.

**Detection:** Checkstyle `FinalLocalVariable`.
**Skill file:** [comments-and-clutter.md](.claude/skills/comments-and-clutter.md)

---

### G23: Prefer Polymorphism to If/Else or Switch/Case
*Ch.17 p.299*

When you see code that tests for a type to decide what behaviour to invoke, consider replacing it with polymorphism. 'One switch' is a reasonable rule of thumb — if you find yourself writing the same switch in multiple places, the switch is telling you that there's a class hierarchy hiding in your code, waiting to be discovered.

**Detection:** [SwitchOnTypeRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/SwitchOnTypeRecipe.java) — detects `instanceof` chains. [StringSwitchRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/StringSwitchRecipe.java) — detects String switch with 3+ cases.
**Skill file:** [conditionals-and-expressions.md](.claude/skills/conditionals-and-expressions.md)

---

### G24: Follow Standard Conventions
*Ch.17 p.299*

Every team should follow a coding standard. The standard should specify things like where to declare instance variables, consistent naming, brace style, and so forth. The standard should not need a document to describe it because the code itself should be the exemplar.

**Detection:** Checkstyle `LeftCurly`, `RightCurly`, `LineLength`.
**Skill file:** [comments-and-clutter.md](.claude/skills/comments-and-clutter.md)

---

### G25: Replace Magic Numbers with Named Constants
*Ch.17 p.300*

In general it is a bad idea to have raw numbers in your code. Numbers like 42 or 86400 are magic — they have no context, no meaning, no documentation. Hide them behind well-named constants. `SECONDS_PER_DAY` is immediately clear; 86400 is not. The term 'Magic Number' does not apply only to numbers — it applies to any token that has a value that is not self-describing.

**Detection:** [MagicStringRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/MagicStringRecipe.java) — detects string literals appearing 2+ times (configurable).
**Skill file:** [java-idioms.md](.claude/skills/java-idioms.md)

---

### G26: Be Precise
*Ch.17 p.301*

Ambiguity in code is a sign of insufficient care. When you make a decision in code, make it precisely. Know why you've made it and how you will deal with any exceptions. Don't be lazy about the precision of your decisions. If you decide to call a function that might return null, make sure you check for null.

**Detection:** SpotBugs `DM_BOXED_PRIMITIVE_FOR_COMPARE`.
**Skill file:** [java-idioms.md](.claude/skills/java-idioms.md)

---

### G27: Structure over Convention
*Ch.17 p.301*

Enforce design decisions with structure over convention. Naming conventions are good, but they are inferior to structures that force compliance. Switch/case statements with nicely named enumerations are inferior to base classes with abstract methods.

**Detection:** Manual review only. No automated detection.

---

### G28: Encapsulate Conditionals
*Ch.17 p.301*

Boolean logic is hard enough to understand without reading it in the context of an if or while statement. Extract functions that explain the intent of the conditional. `if (shouldBeDeleted(timer))` is vastly preferable to `if (timer.hasExpired() && !timer.isRecurrent())`.

**Detection:** [EncapsulateConditionalRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/EncapsulateConditionalRecipe.java) — detects conditions with 2+ logical operators.
**Skill file:** [conditionals-and-expressions.md](.claude/skills/conditionals-and-expressions.md)

---

### G29: Avoid Negative Conditionals
*Ch.17 p.302*

Negatives are slightly harder to understand than positives. So, when possible, conditionals should be expressed as positives. `if (buffer.shouldCompact())` is preferable to `if (!buffer.shouldNotCompact())`. Double negation forces the reader to do mental gymnastics that add nothing.

**Detection:** [NegativeConditionalRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/NegativeConditionalRecipe.java) — detects double negation patterns.
**Skill file:** [conditionals-and-expressions.md](.claude/skills/conditionals-and-expressions.md)

---

### G30: Functions Should Do One Thing
*Ch.17 p.302*

Functions should do one thing. They should do it well. They should do it only. A function that is too long is probably doing too many things. If you can extract another function from it with a name that is not merely a restatement of its implementation, then the original is doing more than one thing.

**Detection:** [WhitespaceSplitMethodRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/WhitespaceSplitMethodRecipe.java) — detects methods with 4+ blank-line sections (configurable). [ImperativeLoopRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/ImperativeLoopRecipe.java) — detects for-loops replaceable with streams.
**Skill file:** [functions.md](.claude/skills/functions.md)

---

### G31: Hidden Temporal Couplings
*Ch.17 p.302*

Temporal couplings are often necessary, but you should not hide the coupling. Structure the arguments of your functions such that the order in which they should be called is obvious. When one function must be called before another, make it impossible to call them out of order.

**Detection:** Manual review only. Narrative stub generated in CLAUDE.md for team annotation.

---

### G32: Don't Be Arbitrary
*Ch.17 p.303*

Have a reason for the way you structure your code, and make sure that reason is communicated by the structure of the code. If a structure appears arbitrary, others will feel empowered to change it. If a structure appears consistently throughout the system, others will use it and preserve the convention.

**Detection:** Manual review only. Narrative stub generated in CLAUDE.md for team annotation.

---

### G33: Encapsulate Boundary Conditions
*Ch.17 p.304*

Boundary conditions are hard to keep track of. Put the processing for them in one place. Don't let them leak all over the code. `array.length - 1` scattered through your code is a bug waiting to happen. Extract it: `final int lastIndex = array.length - 1;` — the name documents the intent and the adjustment happens in exactly one place.

**Detection:** [BoundaryConditionRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/BoundaryConditionRecipe.java) — detects raw `+1`/`-1` adjustments on method calls.
**Skill file:** [conditionals-and-expressions.md](.claude/skills/conditionals-and-expressions.md)

---

### G34: Functions Should Descend Only One Level of Abstraction
*Ch.17 p.304*

Mixing levels of abstraction within a function is always confusing. Section comments — `// initialisation`, `// processing`, `// cleanup` — are a dead giveaway. Each section is at a different level of abstraction. Extract each section into its own well-named function. The sections become the function calls, and the comment headers become function names.

**Detection:** [SectionCommentRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/SectionCommentRecipe.java) — detects section comment banners inside methods.
**Skill file:** [functions.md](.claude/skills/functions.md)

---

### G35: Keep Configurable Data at High Levels
*Ch.17 p.306*

If you have a constant such as a default or configuration value that is known and expected at a high level of abstraction, do not bury it in a low-level function. Expose it as a parameter of the high-level function.

**Detection:** Not yet implemented.

---

### G36: Avoid Transitive Navigation
*Ch.17 p.306*

Write shy code — modules that don't reveal anything unnecessary and that don't rely on others' implementations. The Law of Demeter says a method `f` of class `C` should only call methods on `C` itself, objects created by `f`, objects passed as arguments to `f`, and objects held in instance variables. `a.getB().getC().doSomething()` is a train wreck — it couples you to the entire chain.

**Detection:** [LawOfDemeterRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/LawOfDemeterRecipe.java) — detects method chains of depth >= 3 (configurable, fluent APIs excluded).
**Skill file:** [functions.md](.claude/skills/functions.md)

---

## Java

### J1: Avoid Long Import Lists by Using Wildcards
*Ch.17 p.307*

Long lists of imports are daunting to the reader. If you're importing from many different packages, it's worth asking whether this class has too many responsibilities. Wildcard imports can reduce clutter, but the real fix is often to split the class.

**Detection:** Checkstyle `AvoidStarImport`.
**Skill file:** [java-idioms.md](.claude/skills/java-idioms.md)

---

### J2: Don't Inherit Constants
*Ch.17 p.307*

Implementing an interface just to gain convenient access to its constants is a terrible practice. The constants become part of your class's public API, polluting the namespace. Use static imports to access constants, or place constants in a class or enum where they naturally belong.

**Detection:** [InheritConstantsRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/InheritConstantsRecipe.java) — detects classes implementing constant-only interfaces.
**Skill file:** [java-idioms.md](.claude/skills/java-idioms.md)

---

### J3: Constants versus Enums
*Ch.17 p.308*

Now that Java has enums, use them. The old pattern of `public static final int` or `String` constants as an enumeration is obsolete. Enums provide type safety, can have methods and fields, and make switch statements exhaustive. Don't cling to the pre-Java 5 idiom.

**Detection:** [EnumForConstantsRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/EnumForConstantsRecipe.java) — detects groups of static final fields that share a prefix.
**Skill file:** [java-idioms.md](.claude/skills/java-idioms.md)

---

## Names

### N1: Choose Descriptive Names
*Ch.17 p.309*

Choose names that reveal intention. The name of a variable, function, or class should answer the big questions: why it exists, what it does, and how it is used. If a name requires a comment, then the name is not revealing its intent.

**Detection:** Checkstyle `LocalVariableName`, `MethodName`, `TypeName`.
**Skill file:** [naming.md](.claude/skills/naming.md)

---

### N2: Choose Names at the Appropriate Level of Abstraction
*Ch.17 p.311*

Don't pick names that communicate implementation; pick names that reflect the level of abstraction of the class or function you are working in. This is a difficult thing to do, but it separates the professionals from the amateurs.

**Detection:** Manual review only. No automated detection.

---

### N3: Use Standard Nomenclature Where Possible
*Ch.17 p.311*

Names are easier to understand if they are based on existing convention or usage. For example, if you are using the Decorator pattern, you should use the word `Decorator` in the names of the decorating classes.

**Detection:** Manual review only. No automated detection.

---

### N4: Unambiguous Names
*Ch.17 p.312*

Choose names that make the workings of a function or variable unambiguous. If a name requires the reader to look at its implementation or search for its definition, the name is ambiguous.

**Detection:** Manual review only. No automated detection.

---

### N5: Use Long Names for Long Scopes
*Ch.17 p.312*

The length of a name should correspond to the size of its scope. A variable named `i` is fine in a three-line for-loop. But a variable named `s` in a fifty-line method is a riddle. If the scope is long, the name should be long enough to be found, remembered, and understood without scrolling.

**Detection:** [ShortVariableNameRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/ShortVariableNameRecipe.java) — detects single-letter names outside loops and lambdas (configurable).
**Skill file:** [naming.md](.claude/skills/naming.md)

---

### N6: Avoid Encodings
*Ch.17 p.312*

In the days of early C and Fortran, encoding type information in names was necessary. Today, with modern languages and IDEs, Hungarian notation and type prefixes (`strName`, `iCount`, `m_field`) are nothing but noise. They make names harder to read and harder to change. Let the type system do its job.

**Detection:** [EncodingNamingRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/EncodingNamingRecipe.java) — detects Hungarian notation and type prefixes.
**Skill file:** [naming.md](.claude/skills/naming.md)

---

### N7: Names Should Describe Side-Effects
*Ch.17 p.313*

If a method has side effects, the name should describe them. A method named `getPassword` that also initialises the session is lying. It should be called `getPasswordAndInitialiseSession` — or better, split into two methods. If a reader must look at the implementation to discover a side effect, the name has broken its promise.

**Detection:** [SideEffectNamingRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/SideEffectNamingRecipe.java) — detects methods named get/is/has that contain assignments.
**Skill file:** [naming.md](.claude/skills/naming.md)

---

## Tests

### T1: Insufficient Tests
*Ch.17 p.313*

A test suite is insufficient so long as there are conditions that have not been explored by tests, or calculations that have not been validated. Coverage tools report gaps in your testing strategy — they tell you where untested code lurks, waiting to surprise you in production.

**Detection:** JaCoCo line coverage analysis.

---

### T2: Use a Coverage Tool
*Ch.17 p.313*

A coverage tool makes it easy to find modules, classes, and functions that are insufficiently tested. Most IDEs give you visual coverage, making it quick to find if and catch statements whose bodies haven't been tested. Use this tool. It's not optional.

**Detection:** JaCoCo report presence check.

---

### T3: Don't Skip Trivial Tests
*Ch.17 p.313*

A disabled test is a question about an ambiguity. The code is there, the test is there, but someone decided it shouldn't run. Why? Is the test wrong, or is the code wrong? Find out and fix it. Don't leave disabled tests lingering — they are a form of lying about the state of your system.

**Detection:** [DisabledTestRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/DisabledTestRecipe.java) — detects `@Disabled`/`@Ignore` without meaningful reason.

---

### T4: An Ignored Test Is a Question about an Ambiguity
*Ch.17 p.313*

Sometimes we know that a test is failing because we also know that the requirements are ambiguous. It's tempting to disable it 'until we figure it out.' But that disabled test is a reminder that something is unresolved — and unresolved ambiguities are bugs waiting to surface.

**Detection:** [DisabledTestRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/DisabledTestRecipe.java).

---

### T5: Test Boundary Conditions
*Ch.17 p.314*

Take special care to test boundary conditions. We often get the middle of an algorithm right but misjudge the boundaries.

**Detection:** Manual review only. No automated detection.

---

### T6: Exhaustively Test Near Bugs
*Ch.17 p.314*

Bugs tend to congregate. When you find a bug in a function, it is wise to do an exhaustive test of that function. You'll probably find that the bug was not alone.

**Detection:** Manual review only. No automated detection.

---

### T7: Patterns of Failure Are Revealing
*Ch.17 p.314*

Sometimes you can diagnose a problem by finding patterns in the way the test cases fail. This is another argument for making the test cases as complete as possible. Complete test cases, ordered in a reasonable way, expose patterns.

**Detection:** Manual review only. No automated detection.

---

### T8: Test Coverage Patterns Can Be Revealing
*Ch.17 p.314*

Looking at the code that is or is not executed by the passing tests gives clues to why the failing tests fail.

**Detection:** JaCoCo per-class coverage analysis.

---

### T9: Tests Should Be Fast
*Ch.17 p.314*

A slow test is a test that won't get run. When things get tight, the slow tests are the ones that get dropped from the suite. Keep your tests fast. Ruthlessly refactor tests that take too long to run.

**Detection:** Surefire test timing analysis.

---

## Chapter-Specific Patterns

### Ch3.1: Small Functions
*Ch.3 p.34*

The first rule of functions is that they should be small. The second rule is that they should be smaller than that. Functions should hardly ever be 20 lines long. Each function should tell a story, and each line should lead you naturally to the next in a compelling order.

**Detection:** [PrivateMethodTestabilityRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/PrivateMethodTestabilityRecipe.java) — detects non-trivial private methods that should be package-private for testing.
**Skill file:** [functions.md](.claude/skills/functions.md)

---

### Ch7.1: Use Exceptions Rather Than Return Codes
*Ch.7 p.103*

Exceptions are for exceptional circumstances. When you catch an exception and merely log it — or worse, leave the catch block empty — you've told the calling code that everything is fine when it isn't. The caller makes decisions based on a lie. Either handle the exception meaningfully, or let it propagate to someone who can.

**Detection:** [CatchLogContinueRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/CatchLogContinueRecipe.java) — detects catch blocks that only log or are empty.
**Skill file:** [exception-handling.md](.claude/skills/exception-handling.md)

---

### Ch7.2: Don't Return Null
*Ch.7 p.110*

When you return null, you are creating work for your callers. Every caller must check for null, and if even one forgets, the application blows up with a NullPointerException at some unexpected point. Code peppered with null checks is noisy, hard to read, and fragile. Use Optional, throw an exception, or return a Special Case object instead.

**Detection:** [NullDensityRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/NullDensityRecipe.java) — detects methods with 3+ null checks (configurable). Also SpotBugs redundant null check detection.
**Skill file:** [null-handling.md](.claude/skills/null-handling.md)

---

### Ch10.1: Classes Should Be Small
*Ch.10 p.136*

The first rule of classes is that they should be small. The second rule is that they should be smaller than that. With functions, we measured size by counting physical lines. With classes, we use a different measure: responsibilities. A class should have one, and only one, reason to change. If you need the word 'and' to describe what a class does, it's too big.

**Detection:** [ClassLineLengthRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/ClassLineLengthRecipe.java) — detects classes exceeding 150 lines (configurable).
**Skill file:** [classes.md](.claude/skills/classes.md)

---

### Ch10.2: The Single Responsibility Principle
*Ch.10 p.138*

A record or data structure with many components is a signal that it's carrying too many responsibilities, or that it's a data clump — a group of fields that always travel together and should be extracted into their own meaningful type. Six is a reasonable upper bound; beyond that, ask what smaller structures are hiding inside.

**Detection:** [LargeRecordRecipe](recipes/src/main/java/org/fiftieshousewife/cleancode/recipes/LargeRecordRecipe.java) — detects records with 6+ components (configurable).
**Skill file:** [classes.md](.claude/skills/classes.md)
