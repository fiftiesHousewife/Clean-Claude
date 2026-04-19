package io.github.fiftieshousewife.cleancode.refactoring;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

class MergeInlineValidationRecipeTest implements RewriteTest {

    @Override
    public void defaults(final RecipeSpec spec) {
        spec.recipe(new MergeInlineValidationRecipe())
                .typeValidationOptions(TypeValidation.none());
    }

    @Test
    void replacesInlineValidationWithCallWhenBlocksMatchValidateExactly() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public final class UserAccountService {
                            public String createAccount(final String email, final String displayName) {
                                final List<String> errors = new ArrayList<>();
                                if (email == null || email.isBlank()) {
                                    errors.add("email is required");
                                }
                                if (displayName == null || displayName.isBlank()) {
                                    errors.add("displayName is required");
                                }
                                if (!errors.isEmpty()) {
                                    throw new IllegalArgumentException(String.join("; ", errors));
                                }
                                return "acct-" + email;
                            }
                            void validate(final String email, final String displayName, final List<String> errors) {
                                if (email == null || email.isBlank()) {
                                    errors.add("email is required");
                                }
                                if (displayName == null || displayName.isBlank()) {
                                    errors.add("displayName is required");
                                }
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public final class UserAccountService {
                            public String createAccount(final String email, final String displayName) {
                                final List<String> errors = new ArrayList<>();
                                validate(email, displayName, errors);
                                if (!errors.isEmpty()) {
                                    throw new IllegalArgumentException(String.join("; ", errors));
                                }
                                return "acct-" + email;
                            }
                            void validate(final String email, final String displayName, final List<String> errors) {
                                if (email == null || email.isBlank()) {
                                    errors.add("email is required");
                                }
                                if (displayName == null || displayName.isBlank()) {
                                    errors.add("displayName is required");
                                }
                            }
                        }
                        """));
    }

    @Test
    void mergesSupersetInlineBlocksIntoValidateAndExpandsParams() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public final class UserAccountService {
                            public String createAccount(final String email, final String displayName, final String region) {
                                final List<String> errors = new ArrayList<>();
                                if (email == null || email.isBlank()) {
                                    errors.add("email is required");
                                }
                                if (displayName == null || displayName.isBlank()) {
                                    errors.add("displayName is required");
                                }
                                if (region == null || region.isBlank()) {
                                    errors.add("region is required");
                                }
                                if (!errors.isEmpty()) {
                                    throw new IllegalArgumentException(String.join("; ", errors));
                                }
                                return "acct-" + email;
                            }
                            void validate(final String email, final String displayName, final List<String> errors) {
                                if (email == null || email.isBlank()) {
                                    errors.add("email is required");
                                }
                                if (displayName == null || displayName.isBlank()) {
                                    errors.add("displayName is required");
                                }
                            }
                        }
                        """,
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public final class UserAccountService {
                            public String createAccount(final String email, final String displayName, final String region) {
                                final List<String> errors = new ArrayList<>();
                                validate(email, displayName, region, errors);
                                if (!errors.isEmpty()) {
                                    throw new IllegalArgumentException(String.join("; ", errors));
                                }
                                return "acct-" + email;
                            }
                            void validate(final String email, final String displayName, final String region, final List<String> errors) {
                                if (email == null || email.isBlank()) {
                                    errors.add("email is required");
                                }
                                if (displayName == null || displayName.isBlank()) {
                                    errors.add("displayName is required");
                                }
                                if (region == null || region.isBlank()) {
                                    errors.add("region is required");
                                }
                            }
                        }
                        """));
    }

    @Test
    void leavesMethodAloneWhenNoValidateMethodExists() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public final class UserAccountService {
                            public String createAccount(final String email) {
                                final List<String> errors = new ArrayList<>();
                                if (email == null) {
                                    errors.add("email is required");
                                }
                                if (!errors.isEmpty()) {
                                    throw new IllegalArgumentException(String.join("; ", errors));
                                }
                                return email;
                            }
                        }
                        """));
    }

    @Test
    void leavesMethodAloneWhenInlineBlockTooSmall() {
        rewriteRun(
                java(
                        """
                        package com.example;
                        import java.util.ArrayList;
                        import java.util.List;
                        public final class UserAccountService {
                            public String createAccount(final String email) {
                                final List<String> errors = new ArrayList<>();
                                if (email == null) {
                                    errors.add("email is required");
                                }
                                if (!errors.isEmpty()) {
                                    throw new IllegalArgumentException(String.join("; ", errors));
                                }
                                return email;
                            }
                            void validate(final String email, final List<String> errors) {
                                if (email == null) {
                                    errors.add("email is required");
                                }
                            }
                        }
                        """));
    }
}
