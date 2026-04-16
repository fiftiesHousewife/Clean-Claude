package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class FindingAggregator {

    private FindingAggregator() {}

    public static AggregatedReport aggregate(List<FindingSource> sources,
                                              ProjectContext context) throws FindingSourceException {
        final List<Finding> allFindings = new ArrayList<>();
        final Set<HeuristicCode> coveredCodes = EnumSet.noneOf(HeuristicCode.class);

        for (FindingSource source : sources) {
            coveredCodes.addAll(source.coveredCodes());

            if (!source.isAvailable(context)) {
                continue;
            }

            allFindings.addAll(source.collectFindings(context));
        }

        return new AggregatedReport(
                List.copyOf(allFindings),
                Collections.unmodifiableSet(coveredCodes),
                Instant.now(),
                context.projectName(),
                context.projectVersion());
    }
}
