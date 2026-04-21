package io.github.fiftieshousewife.cleancode.refactoring.support;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperCallScannerTest {

    @Test
    void findsSuperMethodInvocations() {
        final String source = """
                public class Child extends Parent {
                    public String describe() {
                        return super.describe() + " extra";
                    }
                    public int compute() {
                        return super.compute() * 2;
                    }
                }
                """;
        assertEquals(Set.of("describe", "compute"), SuperCallScanner.scanSource(source));
    }

    @Test
    void ignoresPlainIdentifierCalls() {
        final String source = """
                public class A {
                    public void run() {
                        compute();
                        this.compute();
                    }
                    public int compute() { return 1; }
                }
                """;
        assertTrue(SuperCallScanner.scanSource(source).isEmpty());
    }

    @Test
    void findsSuperCallAcrossLines() {
        final String source = """
                public class Wrap {
                    public int delegate() {
                        return super
                                .compute();
                    }
                }
                """;
        assertEquals(Set.of("compute"), SuperCallScanner.scanSource(source));
    }

    @Test
    void collectsNamesAcrossMultipleSources() {
        final Set<String> names = SuperCallScanner.scanSource(
                "class X { void a() { super.foo(); } }"
                        + "class Y { void b() { super.bar(1, 2); } }");
        assertEquals(Set.of("foo", "bar"), names);
    }
}
