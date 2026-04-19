package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class DeleteMumblingLogRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new DeleteMumblingLogRecipe())
                .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void deletesLogCallWithSingleStringLiteralArgument() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        public class Service {
                            private static final Logger log = LoggerFactory.getLogger(Service.class);
                            public void start() {
                                log.info("starting up");
                                doWork();
                            }
                            private void doWork() { }
                        }
                        """,
                        """
                        package com.example;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        public class Service {
                            private static final Logger log = LoggerFactory.getLogger(Service.class);
                            public void start() {
                                doWork();
                            }
                            private void doWork() { }
                        }
                        """));
    }

    @Test
    void deletesEveryStandardLevelMumble() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        public class Service {
                            private static final Logger log = LoggerFactory.getLogger(Service.class);
                            public void noisy() {
                                log.trace("traced");
                                log.debug("debugged");
                                log.info("informed");
                                log.warn("warned");
                                log.error("errored");
                                int x = 1;
                            }
                        }
                        """,
                        """
                        package com.example;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        public class Service {
                            private static final Logger log = LoggerFactory.getLogger(Service.class);
                            public void noisy() {
                                int x = 1;
                            }
                        }
                        """));
    }

    @Test
    void leavesLogCallWithFormatPlaceholderAndArgument() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        public class Service {
                            private static final Logger log = LoggerFactory.getLogger(Service.class);
                            public void start(String userId) {
                                log.info("user {} starting up", userId);
                            }
                        }
                        """));
    }

    @Test
    void leavesLogCallWithStringConcatenationContainingVariable() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        public class Service {
                            private static final Logger log = LoggerFactory.getLogger(Service.class);
                            public void start(int count) {
                                log.info("count: " + count);
                            }
                        }
                        """));
    }

    @Test
    void deletesConcatenationOfOnlyLiterals() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        public class Service {
                            private static final Logger log = LoggerFactory.getLogger(Service.class);
                            public void start() {
                                log.info("starting" + " up");
                                int x = 1;
                            }
                        }
                        """,
                        """
                        package com.example;
                        import org.slf4j.Logger;
                        import org.slf4j.LoggerFactory;
                        public class Service {
                            private static final Logger log = LoggerFactory.getLogger(Service.class);
                            public void start() {
                                int x = 1;
                            }
                        }
                        """));
    }

    @Test
    void leavesNonLoggerInfoCallAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Service {
                            public void info(String msg) { }
                            public void wrap() {
                                info("hello");
                            }
                        }
                        """));
    }
}
