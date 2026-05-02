package io.github.fiftieshousewife.cleancode.core;

import java.util.List;

/**
 * Per-source diagnostic record so the report can show every configured
 * tool — including tools that ran but produced no findings, and tools
 * whose backing report file wasn't found.
 *
 * <p>Without this, the "Sources" section only listed tools that produced
 * &gt;0 findings, leaving users guessing whether silent tools were broken
 * or simply clean. The {@link Status} discriminates the three cases.
 */
public record SourceState(String id, String displayName, Status status, int findingCount) {

    public enum Status {
        PRODUCED_FINDINGS,
        RAN_NO_FINDINGS,
        NOT_AVAILABLE
    }

    public static SourceState produced(final String id, final String displayName, final int count) {
        return new SourceState(id, displayName, Status.PRODUCED_FINDINGS, count);
    }

    public static SourceState ranNoFindings(final String id, final String displayName) {
        return new SourceState(id, displayName, Status.RAN_NO_FINDINGS, 0);
    }

    public static SourceState notAvailable(final String id, final String displayName) {
        return new SourceState(id, displayName, Status.NOT_AVAILABLE, 0);
    }

    public static SourceState classify(final String id, final String displayName,
                                        final boolean wasAvailable, final List<Finding> findings) {
        if (!wasAvailable) {
            return notAvailable(id, displayName);
        }
        if (findings.isEmpty()) {
            return ranNoFindings(id, displayName);
        }
        return produced(id, displayName, findings.size());
    }
}
