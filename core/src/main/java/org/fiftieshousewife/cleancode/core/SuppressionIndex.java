package org.fiftieshousewife.cleancode.core;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class SuppressionIndex {

    private final List<Suppression> suppressions;
    private final List<Finding> metaFindings;

    private SuppressionIndex(final List<Suppression> suppressions, final List<Finding> metaFindings) {
        this.suppressions = suppressions;
        this.metaFindings = metaFindings;
    }

    public static SuppressionIndex build(final Path sourceRoot) {
        final SuppressionParser.ParseOutcome outcome = SuppressionParser.parse(sourceRoot);
        return new SuppressionIndex(outcome.suppressions(), outcome.metaFindings());
    }

    public boolean isSuppressed(final Finding finding) {
        if (finding.sourceFile() == null) {
            return false;
        }
        return suppressions.stream().anyMatch(s -> s.covers(finding));
    }

    public List<Finding> metaFindings() {
        return Collections.unmodifiableList(metaFindings);
    }
}
