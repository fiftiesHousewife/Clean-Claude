package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceStringBuilderWithTextBlockRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new ReplaceStringBuilderWithTextBlockRecipe());
    }

    @Test
    void rewritesAllLiteralMultilineBuilder() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Report {
                            public String render() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("<table>\\n");
                                sb.append("  <tr><td>row</td></tr>\\n");
                                sb.append("</table>\\n");
                                return sb.toString();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Report {
                            public String render() {
                                return \"\"\"
                                        <table>
                                          <tr><td>row</td></tr>
                                        </table>
                                        \"\"\";
                            }
                        }
                        """
                )
        );
    }

    @Test
    void rewritesInterpolatedMultilineBuilder() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Report {
                            public String render(String name) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("<table>\\n");
                                sb.append("  <tr><td>").append(name).append("</td></tr>\\n");
                                sb.append("</table>\\n");
                                return sb.toString();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Report {
                            public String render(String name) {
                                return \"\"\"
                                        <table>
                                          <tr><td>%s</td></tr>
                                        </table>
                                        \"\"\".formatted(name);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void escapesPercentInLiteralsWhenFormattedIsUsed() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Progress {
                            public String render(int percent) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("status: 100%\\n");
                                sb.append("current: ").append(percent).append("%\\n");
                                return sb.toString();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Progress {
                            public String render(int percent) {
                                return \"\"\"
                                        status: 100%%
                                        current: %s%%
                                        \"\"\".formatted(percent);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesSingleLineBuilderAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class OneLine {
                            public String render(String a, String b) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("a=").append(a);
                                sb.append(",b=").append(b);
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesBuilderWithCapacityArgAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Sized {
                            public String render() {
                                StringBuilder sb = new StringBuilder(1024);
                                sb.append("line1\\n");
                                sb.append("line2\\n");
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesBuilderWithSeedStringAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Seeded {
                            public String render() {
                                StringBuilder sb = new StringBuilder("prefix:");
                                sb.append("line1\\n");
                                sb.append("line2\\n");
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesBuilderWithLoopAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.List;
                        public class Looped {
                            public String render(List<String> items) {
                                StringBuilder sb = new StringBuilder();
                                for (String item : items) {
                                    sb.append(item).append("\\n");
                                }
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesBuilderWithBranchingAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Branchy {
                            public String render(String name, boolean verbose) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("name=").append(name).append("\\n");
                                if (verbose) {
                                    sb.append("verbose\\n");
                                }
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesBuilderWithExtraStatementsAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Extra {
                            public String render() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("line1\\n");
                                sb.append("line2\\n");
                                int n = 5;
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesBuilderWithReassignedToStringAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Trimmed {
                            public String render() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("line1\\n");
                                sb.append("line2\\n");
                                return sb.toString().trim();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesBuilderWithTabLiteralAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Tabby {
                            public String render() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("col1\\tcol2\\n");
                                sb.append("a\\tb\\n");
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesContentThatDoesNotEndWithNewlineAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class NoTrail {
                            public String render() {
                                StringBuilder sb = new StringBuilder();
                                sb.append("line1\\n");
                                sb.append("line2");
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }

    @Test
    void handlesLongChainedAppendInSingleStatement() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class Chain {
                            public String render(String a, String b) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("<x>").append(a).append("</x>\\n").append("<y>").append(b).append("</y>\\n");
                                return sb.toString();
                            }
                        }
                        """,
                        """
                        package com.example;
                        public class Chain {
                            public String render(String a, String b) {
                                return \"\"\"
                                        <x>%s</x>
                                        <y>%s</y>
                                        \"\"\".formatted(a, b);
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesNonStringBuilderBuilderAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        public class WithBuffer {
                            public String render() {
                                StringBuffer sb = new StringBuffer();
                                sb.append("line1\\n");
                                sb.append("line2\\n");
                                return sb.toString();
                            }
                        }
                        """
                )
        );
    }
}
