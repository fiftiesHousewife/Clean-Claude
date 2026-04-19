@NullMarked
@SuppressCleanCode(
        value = { HeuristicCode.G5, HeuristicCode.Ch7_2 },
        reason = "OpenRewrite visitor pattern produces structurally similar scanners across recipes "
                + "and relies on null returns to signal no-change; both are API-imposed, not design flaws"
)
package io.github.fiftieshousewife.cleancode.recipes;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;
import io.github.fiftieshousewife.cleancode.annotations.SuppressCleanCode;
import org.jspecify.annotations.NullMarked;
