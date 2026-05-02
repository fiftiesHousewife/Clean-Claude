package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class FindingAggregator {

    public record Result(AggregatedReport report, List<SourceState> sourceStates) {}

    private FindingAggregator() {}

    public static AggregatedReport aggregate(List<FindingSource> sources,
                                              ProjectContext context) throws FindingSourceException {
        return aggregateWithStates(sources, context).report();
    }

    public static Result aggregateWithStates(List<FindingSource> sources,
                                              ProjectContext context) throws FindingSourceException {
        final List<Finding> allFindings = new ArrayList<>();
        final Set<HeuristicCode> coveredCodes = EnumSet.noneOf(HeuristicCode.class);
        final List<SourceState> states = new ArrayList<>();

        for (final FindingSource source : sources) {
            coveredCodes.addAll(source.coveredCodes());

            final boolean available = source.isAvailable(context);
            if (!available) {
                states.add(SourceState.notAvailable(source.id(), source.displayName()));
                continue;
            }

            final List<Finding> sourceFindings = source.collectFindings(context);
            allFindings.addAll(sourceFindings);
            states.add(SourceState.classify(source.id(), source.displayName(), true, sourceFindings));
        }

        final AggregatedReport report = new AggregatedReport(
                List.copyOf(allFindings),
                Collections.unmodifiableSet(coveredCodes),
                Instant.now(),
                context.projectName(),
                context.projectVersion());
        return new Result(report, List.copyOf(states));
    }
}
