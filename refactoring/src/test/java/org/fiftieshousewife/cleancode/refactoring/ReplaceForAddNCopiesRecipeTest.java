package org.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ReplaceForAddNCopiesRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new ReplaceForAddNCopiesRecipe());
    }

    @Test
    void replacesForLoopThatAppendsSameValue() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public class Copies {
                            public List<String> echo(String value, int n) {
                                List<String> out = new ArrayList<>();
                                for (int i = 0; i < n; i++) {
                                    out.add(value);
                                }
                                return out;
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.Collections;
                        import java.util.List;

                        public class Copies {
                            public List<String> echo(String value, int n) {
                                List<String> out = new ArrayList<>();
                                out.addAll(Collections.nCopies(n, value));
                                return out;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesForLoopThatReferencesCounterInBody() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public class Indexed {
                            public List<Integer> numbers(int n) {
                                List<Integer> out = new ArrayList<>();
                                for (int i = 0; i < n; i++) {
                                    out.add(i);
                                }
                                return out;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesForLoopThatCallsSomethingOtherThanAdd() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public class Other {
                            public List<String> log(String v, int n) {
                                List<String> out = new ArrayList<>();
                                for (int i = 0; i < n; i++) {
                                    System.out.println(v);
                                }
                                return out;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesForLoopWithMultipleStatementsInBodyAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public class Many {
                            public List<String> run(String v, int n) {
                                List<String> out = new ArrayList<>();
                                for (int i = 0; i < n; i++) {
                                    System.out.println("adding");
                                    out.add(v);
                                }
                                return out;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void replacesForLoopWithLiteralCount() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public class Literal {
                            public List<String> threeTimes(String v) {
                                List<String> out = new ArrayList<>();
                                for (int i = 0; i < 3; i++) {
                                    out.add(v);
                                }
                                return out;
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.Collections;
                        import java.util.List;

                        public class Literal {
                            public List<String> threeTimes(String v) {
                                List<String> out = new ArrayList<>();
                                out.addAll(Collections.nCopies(3, v));
                                return out;
                            }
                        }
                        """
                )
        );
    }

    @Test
    void leavesForLoopThatStartsNotAtZeroAlone() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public class NotZero {
                            public List<String> run(String v, int n) {
                                List<String> out = new ArrayList<>();
                                for (int i = 1; i < n; i++) {
                                    out.add(v);
                                }
                                return out;
                            }
                        }
                        """
                )
        );
    }
}
