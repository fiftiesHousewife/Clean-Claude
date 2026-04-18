@NullMarked
@SuppressCleanCode(
        value = { HeuristicCode.Ch7_2 },
        reason = "OpenRewrite relies on null returns to signal `no change`; API-imposed, not a design flaw. "
                + "G5 used to be suppressed here too on the grounds that visitor scaffolding repeats across "
                + "recipes, but CPD (post-d5c2b0d audit) found the real duplications are in the scan+rewrite "
                + "bodies we authored — D14 tracks the fixes; anything genuinely visitor-pattern-imposed gets "
                + "a narrow per-class suppression rather than this blanket one."
)
package org.fiftieshousewife.cleancode.refactoring;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;
import org.fiftieshousewife.cleancode.annotations.SuppressCleanCode;
import org.jspecify.annotations.NullMarked;
