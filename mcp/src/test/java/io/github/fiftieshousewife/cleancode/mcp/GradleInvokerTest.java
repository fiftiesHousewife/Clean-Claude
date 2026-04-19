package io.github.fiftieshousewife.cleancode.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradleInvokerTest {

    @Test
    void invokeAlwaysIncludesSandboxOptInFlag() throws Exception {
        final AtomicReference<List<String>> captured = new AtomicReference<>();
        final GradleInvoker invoker = new GradleInvoker(
                (args, timeout) -> {
                    captured.set(args);
                    return new GradleInvoker.Result(0, "");
                });

        invoker.invoke(":sandbox:compileJava");

        final List<String> args = captured.get();
        assertAll(
                () -> assertTrue(args.contains("-PcleanCodeSelfApply=true"),
                        "flag must be passed so settings.gradle.kts includes :sandbox — args=" + args),
                () -> assertTrue(args.contains(":sandbox:compileJava"),
                        "caller's task arg survives"),
                () -> assertTrue(args.contains("--console=plain"),
                        "baseline gradle args still present"));
    }
}
