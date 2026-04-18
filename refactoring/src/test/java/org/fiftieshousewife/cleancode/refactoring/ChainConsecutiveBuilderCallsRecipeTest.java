package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChainConsecutiveBuilderCallsRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new ChainConsecutiveBuilderCallsRecipe());
    }

    @Test
    void chainsTwoStringBuilderAppends() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Builder {
                            public String build(String name) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("hello ");
                                sb.append(name);
                                return sb.toString();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Builder {
                            public String build(String name) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("hello ").append(name);
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void chainsFourConsecutiveAppends() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Many {
                            public String build() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("a");
                                sb.append("b");
                                sb.append("c");
                                sb.append("d");
                                return sb.toString();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Many {
                            public String build() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("a").append("b").append("c").append("d");
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesSingleAppendAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Alone {
                            public String build() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("x");
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesSeparatedAppendsAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Sep {
                            public String build(String a, String b) {
                                StringBuilder sb = new StringBuilder();
                                sb.append(a);
                                if (b != null) {
                                    sb.append(b);
                                }
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesCallsOnDifferentReceiversAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Two {
                            public String build(String a, String b) {
                                StringBuilder one = new StringBuilder();
                                StringBuilder two = new StringBuilder();
                                one.append(a);
                                two.append(b);
                                return one.toString() + two.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesNonFluentReceiverAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public class NotFluent {
                            public List<String> build() {
                                List<String> list = new ArrayList<>();
                                list.add("a");
                                list.add("b");
                                return list;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void chainsAppendsWithChainedReceiverFirstCall() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Mix {
                            public String build(int n) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("n=").append(n);
                                sb.append(",done");
                                return sb.toString();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Mix {
                            public String build(int n) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("n=").append(n).append(",done");
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }
}
