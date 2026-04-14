package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Map;

public final class HeuristicDescriptions {

    private HeuristicDescriptions() {}

    private static final Map<HeuristicCode, String> NAMES = Map.ofEntries(
            Map.entry(HeuristicCode.C1, "Inappropriate Information"),
            Map.entry(HeuristicCode.C2, "Obsolete Comment"),
            Map.entry(HeuristicCode.C3, "Redundant Comment"),
            Map.entry(HeuristicCode.C4, "Poorly Written Comment"),
            Map.entry(HeuristicCode.C5, "Commented-Out Code"),
            Map.entry(HeuristicCode.E1, "Build Requires More Than One Step"),
            Map.entry(HeuristicCode.E2, "Tests Require More Than One Step"),
            Map.entry(HeuristicCode.F1, "Too Many Arguments"),
            Map.entry(HeuristicCode.F2, "Output Arguments"),
            Map.entry(HeuristicCode.F3, "Flag Arguments"),
            Map.entry(HeuristicCode.F4, "Dead Function"),
            Map.entry(HeuristicCode.G1, "Multiple Languages in One Source File"),
            Map.entry(HeuristicCode.G2, "Obvious Behaviour Is Unimplemented"),
            Map.entry(HeuristicCode.G3, "Incorrect Behaviour at the Boundaries"),
            Map.entry(HeuristicCode.G4, "Overridden Safeties"),
            Map.entry(HeuristicCode.G5, "Duplication"),
            Map.entry(HeuristicCode.G6, "Code at Wrong Level of Abstraction"),
            Map.entry(HeuristicCode.G7, "Base Classes Depending on Their Derivatives"),
            Map.entry(HeuristicCode.G8, "Too Much Information"),
            Map.entry(HeuristicCode.G9, "Dead Code"),
            Map.entry(HeuristicCode.G10, "Vertical Separation"),
            Map.entry(HeuristicCode.G11, "Inconsistency"),
            Map.entry(HeuristicCode.G12, "Clutter"),
            Map.entry(HeuristicCode.G13, "Artificial Coupling"),
            Map.entry(HeuristicCode.G14, "Feature Envy"),
            Map.entry(HeuristicCode.G15, "Selector Arguments"),
            Map.entry(HeuristicCode.G16, "Obscured Intent"),
            Map.entry(HeuristicCode.G17, "Misplaced Responsibility"),
            Map.entry(HeuristicCode.G18, "Inappropriate Static"),
            Map.entry(HeuristicCode.G19, "Use Explanatory Variables"),
            Map.entry(HeuristicCode.G20, "Function Names Should Say What They Do"),
            Map.entry(HeuristicCode.G21, "Understand the Algorithm"),
            Map.entry(HeuristicCode.G22, "Make Logical Dependencies Physical"),
            Map.entry(HeuristicCode.G23, "Prefer Polymorphism to If/Else or Switch/Case"),
            Map.entry(HeuristicCode.G24, "Follow Standard Conventions"),
            Map.entry(HeuristicCode.G25, "Replace Magic Numbers with Named Constants"),
            Map.entry(HeuristicCode.G26, "Be Precise"),
            Map.entry(HeuristicCode.G27, "Structure over Convention"),
            Map.entry(HeuristicCode.G28, "Encapsulate Conditionals"),
            Map.entry(HeuristicCode.G29, "Avoid Negative Conditionals"),
            Map.entry(HeuristicCode.G30, "Functions Should Do One Thing"),
            Map.entry(HeuristicCode.G31, "Hidden Temporal Couplings"),
            Map.entry(HeuristicCode.G32, "Don't Be Arbitrary"),
            Map.entry(HeuristicCode.G33, "Encapsulate Boundary Conditions"),
            Map.entry(HeuristicCode.G34, "Functions Should Descend Only One Level of Abstraction"),
            Map.entry(HeuristicCode.G35, "Keep Configurable Data at High Levels"),
            Map.entry(HeuristicCode.G36, "Avoid Transitive Navigation"),
            Map.entry(HeuristicCode.J1, "Avoid Long Import Lists by Using Wildcards"),
            Map.entry(HeuristicCode.J2, "Don't Inherit Constants"),
            Map.entry(HeuristicCode.J3, "Constants versus Enums"),
            Map.entry(HeuristicCode.N1, "Choose Descriptive Names"),
            Map.entry(HeuristicCode.N2, "Choose Names at the Appropriate Level of Abstraction"),
            Map.entry(HeuristicCode.N3, "Use Standard Nomenclature Where Possible"),
            Map.entry(HeuristicCode.N4, "Unambiguous Names"),
            Map.entry(HeuristicCode.N5, "Use Long Names for Long Scopes"),
            Map.entry(HeuristicCode.N6, "Avoid Encodings"),
            Map.entry(HeuristicCode.N7, "Names Should Describe Side-Effects"),
            Map.entry(HeuristicCode.T1, "Insufficient Tests"),
            Map.entry(HeuristicCode.T2, "Use a Coverage Tool"),
            Map.entry(HeuristicCode.T3, "Don't Skip Trivial Tests"),
            Map.entry(HeuristicCode.T4, "An Ignored Test Is a Question about an Ambiguity"),
            Map.entry(HeuristicCode.T5, "Test Boundary Conditions"),
            Map.entry(HeuristicCode.T6, "Exhaustively Test Near Bugs"),
            Map.entry(HeuristicCode.T7, "Patterns of Failure Are Revealing"),
            Map.entry(HeuristicCode.T8, "Test Coverage Patterns Can Be Revealing"),
            Map.entry(HeuristicCode.T9, "Tests Should Be Fast"),
            Map.entry(HeuristicCode.Ch3_1, "Small Functions"),
            Map.entry(HeuristicCode.Ch3_2, "Do One Thing"),
            Map.entry(HeuristicCode.Ch3_3, "One Level of Abstraction per Function"),
            Map.entry(HeuristicCode.Ch6_1, "Data/Object Anti-Symmetry"),
            Map.entry(HeuristicCode.Ch7_1, "Use Exceptions Rather Than Return Codes"),
            Map.entry(HeuristicCode.Ch7_2, "Don't Return Null"),
            Map.entry(HeuristicCode.Ch10_1, "Classes Should Be Small"),
            Map.entry(HeuristicCode.Ch10_2, "The Single Responsibility Principle")
    );

    private static final Map<HeuristicCode, String> GUIDANCE = Map.ofEntries(
            // Comments
            Map.entry(HeuristicCode.C1,
                    "Comments should be reserved for technical notes about the code and its " +
                    "design. Anything that belongs in another system — changelogs, author " +
                    "attributions, issue tracker references — is inappropriate information " +
                    "that clutters the source and drifts out of date."),
            Map.entry(HeuristicCode.C2,
                    "A comment that has become inaccurate is worse than no comment at all. " +
                    "It actively misleads. If the code has changed and the comment hasn't, " +
                    "delete it or rewrite it. Floating, forgotten comments are a form of lying."),
            Map.entry(HeuristicCode.C3,
                    "A comment that merely restates the code is clutter. 'i++ // increment i' " +
                    "teaches us nothing. If the code is so unclear that it needs a comment to " +
                    "explain what it does, the real solution is to make the code clearer — not " +
                    "to add a redundant comment on top of unclear code."),
            Map.entry(HeuristicCode.C4,
                    "If you are going to write a comment, take the time to make sure it is the " +
                    "best comment you can write. Choose your words carefully. Use correct grammar " +
                    "and punctuation. A sloppy comment is a sign that the author doesn't care — " +
                    "and that carelessness infects the code around it."),
            Map.entry(HeuristicCode.C5,
                    "Commented-out code rots. Others who see it won't have the courage to delete " +
                    "it — they'll assume it's there for a reason. Over time, commented-out code " +
                    "accumulates like sediment, obscuring the code that actually matters. Delete " +
                    "it. Source control remembers everything; you don't need to."),

            // Environment
            Map.entry(HeuristicCode.E1,
                    "You should be able to build the system with a single trivial command. You " +
                    "should not have to search around for dependencies, scripts, or obscure " +
                    "commands. Outdated dependencies make that one-step build fragile — every " +
                    "stale library is a silent accumulation of risk, incompatibility, and " +
                    "unfixed vulnerabilities."),
            Map.entry(HeuristicCode.E2,
                    "You should be able to run all the unit tests with a single trivial command. " +
                    "Being able to run tests quickly, easily, and without fuss is so fundamental " +
                    "that a failure here poisons the entire development experience."),

            // Functions
            Map.entry(HeuristicCode.F1,
                    "Functions should have a small number of arguments. The ideal number is zero. " +
                    "Next comes one, followed closely by two. Three arguments should be avoided " +
                    "where possible. More than three requires very special justification — and " +
                    "then shouldn't be used anyway."),
            Map.entry(HeuristicCode.F2,
                    "Output arguments are counterintuitive. Readers expect arguments to be inputs " +
                    "to a function, not things the function writes to. When you see " +
                    "appendFooter(report), do you expect it to append something to report, or " +
                    "to append report to something else? If a function must transform something, " +
                    "let it transform the state of the owning object, or return the result."),
            Map.entry(HeuristicCode.F3,
                    "Boolean arguments loudly declare that the function does more than one thing. " +
                    "It does one thing if the flag is true and another if the flag is false. The " +
                    "function should be split into two: one for each path. render(true) tells the " +
                    "reader nothing — renderForSuite() and renderForSingleTest() tell them " +
                    "everything."),
            Map.entry(HeuristicCode.F4,
                    "Methods that are never called are dead code. They clog the class, confuse " +
                    "the reader, and add maintenance burden. Delete them without hesitation — " +
                    "your source control system remembers them if you ever need them back."),

            // General
            Map.entry(HeuristicCode.G4,
                    "Don't override safeties. Turning off warnings, ignoring failing tests, " +
                    "catching and discarding exceptions — these are all the same mistake. " +
                    "Safeties exist because someone, at some point, decided they were important " +
                    "enough to create. Don't silence them; fix the underlying problem."),
            Map.entry(HeuristicCode.G5,
                    "Duplication is the root of all evil in software. Every duplication represents " +
                    "a missed opportunity for abstraction. When you see duplicated code, it almost " +
                    "always means there is a concept crying out to be a subroutine or a class. " +
                    "Find it and eliminate the duplication."),
            Map.entry(HeuristicCode.G8,
                    "Well-defined modules have very small interfaces that allow you to do a lot " +
                    "with a little. A class with too many public methods, too many fields, or too " +
                    "many dependencies has exposed too much of itself. Keep interfaces tight. " +
                    "Fewer things to know means fewer things to go wrong."),
            Map.entry(HeuristicCode.G9,
                    "Dead code is code that isn't executed. It's the body of an if statement that " +
                    "checks for a condition that can't happen. It's the catch block for an " +
                    "exception that is never thrown. It's the utility method that is never called. " +
                    "Dead code is not completely updated when designs change. It rots. Delete it."),
            Map.entry(HeuristicCode.G10,
                    "Local variables should be declared just before their first use. Vertical " +
                    "separation between declaration and use makes the reader hold a mental " +
                    "placeholder — 'what was that variable for again?' — while reading unrelated " +
                    "code. Declare variables at the point of need, not at the top of the method."),
            Map.entry(HeuristicCode.G12,
                    "Clutter is anything that adds noise without adding value: unused variables, " +
                    "never-called functions, redundant imports, purposeless comments. Keep your " +
                    "source files clean. A lean source file is easier to read, easier to " +
                    "understand, and easier to change."),
            Map.entry(HeuristicCode.G17,
                    "One of the most important decisions a developer makes is where to put code. " +
                    "A data class is a class with public fields and no real behaviour — it " +
                    "exposes its internals and delegates all responsibility elsewhere. Ask: " +
                    "does this behaviour belong here, or is it misplaced?"),
            Map.entry(HeuristicCode.G18,
                    "In general you should prefer non-static methods to static methods. If you " +
                    "really want a function to be static, make sure there is no chance that you'll " +
                    "want it to behave polymorphically. Static methods cannot be overridden and " +
                    "cannot participate in dependency injection."),
            Map.entry(HeuristicCode.G20,
                    "If you have to look at the implementation of a function to know what it does, " +
                    "then you should work to find a better name, or rearrange the functionality so " +
                    "that it can be placed in a function with a better name. The name of a function " +
                    "should tell you exactly what it does, unambiguously, without surprises."),
            Map.entry(HeuristicCode.G22,
                    "If something in your code can logically be constant — a variable that's never " +
                    "reassigned, an object that's never swapped — then declare it final. This isn't " +
                    "pedantry. It communicates intent to every future reader: 'this value is set " +
                    "once and never changes.' It prevents accidental reassignment and signals a " +
                    "design where fewer things are moving at once."),
            Map.entry(HeuristicCode.G23,
                    "When you see code that tests for a type to decide what behaviour to invoke, " +
                    "consider replacing it with polymorphism. 'One switch' is a reasonable rule of " +
                    "thumb — if you find yourself writing the same switch in multiple places, the " +
                    "switch is telling you that there's a class hierarchy hiding in your code, " +
                    "waiting to be discovered."),
            Map.entry(HeuristicCode.G24,
                    "Every team should follow a coding standard. The standard should specify things " +
                    "like where to declare instance variables, consistent naming, brace style, and " +
                    "so forth. The standard should not need a document to describe it because the " +
                    "code itself should be the exemplar."),
            Map.entry(HeuristicCode.G25,
                    "In general it is a bad idea to have raw numbers in your code. Numbers like 42 " +
                    "or 86400 are magic — they have no context, no meaning, no documentation. " +
                    "Hide them behind well-named constants. SECONDS_PER_DAY is immediately clear; " +
                    "86400 is not."),
            Map.entry(HeuristicCode.G26,
                    "Ambiguity in code is a sign of insufficient care. When you make a decision in " +
                    "code, make it precisely. Know why you've made it and how you will deal with " +
                    "any exceptions. Don't be lazy about the precision of your decisions."),
            Map.entry(HeuristicCode.G28,
                    "Boolean logic is hard enough to understand without reading it in the context " +
                    "of an if or while statement. Extract functions that explain the intent of the " +
                    "conditional. if (shouldBeDeleted(timer)) is vastly preferable to " +
                    "if (timer.hasExpired() && !timer.isRecurrent())."),
            Map.entry(HeuristicCode.G29,
                    "Negatives are slightly harder to understand than positives. So, when " +
                    "possible, conditionals should be expressed as positives. " +
                    "if (buffer.shouldCompact()) is preferable to if (!buffer.shouldNotCompact()). " +
                    "Double negation forces the reader to do mental gymnastics that add nothing."),
            Map.entry(HeuristicCode.G30,
                    "Functions should do one thing. They should do it well. They should do it only. " +
                    "A function that is too long is probably doing too many things. If you can " +
                    "extract another function from it with a name that is not merely a restatement " +
                    "of its implementation, then the original is doing more than one thing."),
            Map.entry(HeuristicCode.G34,
                    "Mixing levels of abstraction within a function is always confusing. Section " +
                    "comments — '// initialisation', '// processing', '// cleanup' — are a dead " +
                    "giveaway. Each section is at a different level of abstraction. Extract each " +
                    "section into its own well-named function. The sections become the function " +
                    "calls, and the comment headers become function names."),
            Map.entry(HeuristicCode.G36,
                    "Write shy code — modules that don't reveal anything unnecessary and that " +
                    "don't rely on others' implementations. The Law of Demeter says a method f " +
                    "of class C should only call methods on C itself, objects created by f, " +
                    "objects passed as arguments to f, and objects held in instance variables. " +
                    "a.getB().getC().doSomething() is a train wreck — it couples you to the " +
                    "entire chain."),

            // Java
            Map.entry(HeuristicCode.J1,
                    "Long lists of imports are daunting to the reader. If you're importing from " +
                    "many different packages, it's worth asking whether this class has too many " +
                    "responsibilities. Wildcard imports can reduce clutter, but the real fix is " +
                    "often to split the class."),
            Map.entry(HeuristicCode.J2,
                    "Implementing an interface just to gain convenient access to its constants " +
                    "is a terrible practice. The constants become part of your class's public API, " +
                    "polluting the namespace. Use static imports to access constants, or place " +
                    "constants in a class or enum where they naturally belong."),
            Map.entry(HeuristicCode.J3,
                    "Now that Java has enums, use them. The old pattern of public static final " +
                    "int or String constants as an enumeration is obsolete. Enums provide type " +
                    "safety, can have methods and fields, and make switch statements exhaustive. " +
                    "Don't cling to the pre-Java 5 idiom."),

            // Naming
            Map.entry(HeuristicCode.N1,
                    "Choose names that reveal intention. The name of a variable, function, or " +
                    "class should answer the big questions: why it exists, what it does, and " +
                    "how it is used. If a name requires a comment, then the name is not revealing " +
                    "its intent."),
            Map.entry(HeuristicCode.N6,
                    "In the days of early C and Fortran, encoding type information in names was " +
                    "necessary. Today, with modern languages and IDEs, Hungarian notation and " +
                    "type prefixes (strName, iCount, m_field) are nothing but noise. They make " +
                    "names harder to read and harder to change. Let the type system do its job."),

            // Tests
            Map.entry(HeuristicCode.T1,
                    "A test suite is insufficient so long as there are conditions that have not " +
                    "been explored by tests, or calculations that have not been validated. " +
                    "Coverage tools report gaps in your testing strategy — they tell you where " +
                    "untested code lurks, waiting to surprise you in production."),
            Map.entry(HeuristicCode.T2,
                    "A coverage tool makes it easy to find modules, classes, and functions that " +
                    "are insufficiently tested. Most IDEs give you visual coverage, making it " +
                    "quick to find if and catch statements whose bodies haven't been tested. " +
                    "Use this tool. It's not optional."),
            Map.entry(HeuristicCode.T3,
                    "A disabled test is a question about an ambiguity. The code is there, the " +
                    "test is there, but someone decided it shouldn't run. Why? Is the test wrong, " +
                    "or is the code wrong? Find out and fix it. Don't leave disabled tests " +
                    "lingering — they are a form of lying about the state of your system."),
            Map.entry(HeuristicCode.T4,
                    "Sometimes we know that a test is failing because we also know that the " +
                    "requirements are ambiguous. It's tempting to disable it 'until we figure it " +
                    "out.' But that disabled test is a reminder that something is unresolved — " +
                    "and unresolved ambiguities are bugs waiting to surface."),
            Map.entry(HeuristicCode.T9,
                    "A slow test is a test that won't get run. When things get tight, the slow " +
                    "tests are the ones that get dropped from the suite. Keep your tests fast. " +
                    "Ruthlessly refactor tests that take too long to run."),

            // Chapter codes
            Map.entry(HeuristicCode.Ch3_1,
                    "The first rule of functions is that they should be small. The second rule " +
                    "is that they should be smaller than that. Functions should hardly ever be " +
                    "20 lines long. Each function should tell a story, and each line should lead " +
                    "you naturally to the next in a compelling order."),
            Map.entry(HeuristicCode.Ch7_1,
                    "Exceptions are for exceptional circumstances. When you catch an exception " +
                    "and merely log it — or worse, leave the catch block empty — you've told the " +
                    "calling code that everything is fine when it isn't. The caller makes " +
                    "decisions based on a lie. Either handle the exception meaningfully, or let " +
                    "it propagate to someone who can."),
            Map.entry(HeuristicCode.Ch7_2,
                    "When you return null, you are creating work for your callers. Every caller " +
                    "must check for null, and if even one forgets, the application blows up with " +
                    "a NullPointerException at some unexpected point. Code peppered with null " +
                    "checks is noisy, hard to read, and fragile. Use Optional, throw an exception, " +
                    "or return a Special Case object instead."),
            Map.entry(HeuristicCode.Ch10_1,
                    "The first rule of classes is that they should be small. The second rule is " +
                    "that they should be smaller than that. With functions, we measured size by " +
                    "counting physical lines. With classes, we use a different measure: " +
                    "responsibilities. A class should have one, and only one, reason to change. " +
                    "If you need the word 'and' to describe what a class does, it's too big."),
            Map.entry(HeuristicCode.Ch10_2,
                    "A record or data structure with many components is a signal that it's carrying " +
                    "too many responsibilities, or that it's a data clump — a group of fields that " +
                    "always travel together and should be extracted into their own meaningful type. " +
                    "Five is a reasonable upper bound; beyond that, ask what smaller structures " +
                    "are hiding inside.")
    );

    public static String name(HeuristicCode code) {
        return NAMES.getOrDefault(code, code.name());
    }

    public static String guidance(HeuristicCode code) {
        return GUIDANCE.get(code);
    }
}
