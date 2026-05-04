package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FindingAggregator {

    public record Result(AggregatedReport report, List<SourceState> sourceStates) {}

    private FindingAggregator() {}

    public static AggregatedReport aggregate(final List<FindingSource> sources,
                                              final ProjectContext context) throws FindingSourceException {
        return aggregateWithStates(sources, context).report();
    }

    public static Result aggregateWithStates(final List<FindingSource> sources,
                                              final ProjectContext context) throws FindingSourceException {
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
                deduplicate(allFindings),
                Collections.unmodifiableSet(coveredCodes),
                Instant.now(),
                context.projectName(),
                context.projectVersion());
        return new Result(report, List.copyOf(states));
    }

    /**
     * Drops findings that share the same heuristic code, source file, line,
     * and message text. Two real cases this collapses:
     *
     * <ul>
     *   <li>SpotBugs reports {@code EI_EXPOSE_REP} on every accessor of a
     *       record's mutable fields. The synthetic getters all map back to
     *       the record's class declaration line, so a 4-field record
     *       produces 4 identical findings — eight if you count both
     *       message variants ("returning reference" vs "incorporating
     *       reference"). After dedup the user sees one row per variant.
     *   <li>Two finding sources occasionally surface the same issue at
     *       the same line through different rule paths — keeping just
     *       one row avoids a bogus inflation of the count.
     * </ul>
     *
     * <p>Order of the first occurrence is preserved so the report still
     * reads in source-collection order.
     */
    private static List<Finding> deduplicate(final List<Finding> findings) {
        final Set<String> seen = new HashSet<>();
        final List<Finding> kept = new ArrayList<>(findings.size());
        for (final Finding f : findings) {
            final String key = f.code().name() + '|'
                    + f.sourceFile() + '|'
                    + f.startLine() + '|'
                    + f.message();
            if (seen.add(key)) {
                kept.add(f);
            }
        }
        return List.copyOf(kept);
    }
}
