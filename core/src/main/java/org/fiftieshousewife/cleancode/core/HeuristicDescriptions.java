package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HeuristicDescriptions {

    private HeuristicDescriptions() {}

    private static final List<Map<HeuristicCode, String>> NAME_SOURCES = List.of(
            CommentDescriptions.NAMES,
            EnvironmentDescriptions.NAMES,
            FunctionDescriptions.NAMES,
            GeneralStructureDescriptions.NAMES,
            GeneralExpressionDescriptions.NAMES,
            GeneralControlFlowDescriptions.NAMES,
            JavaDescriptions.NAMES,
            NamingDescriptions.NAMES,
            TestDescriptions.NAMES,
            ChapterDescriptions.NAMES
    );

    private static final List<Map<HeuristicCode, String>> GUIDANCE_SOURCES = List.of(
            CommentDescriptions.GUIDANCE,
            EnvironmentDescriptions.GUIDANCE,
            FunctionDescriptions.GUIDANCE,
            GeneralStructureDescriptions.GUIDANCE,
            GeneralExpressionDescriptions.GUIDANCE,
            GeneralControlFlowDescriptions.GUIDANCE,
            JavaDescriptions.GUIDANCE,
            NamingDescriptions.GUIDANCE,
            TestDescriptions.GUIDANCE,
            ChapterDescriptions.GUIDANCE
    );

    private static final List<Map<HeuristicCode, String>> REFERENCE_SOURCES = List.of(
            CommentDescriptions.REFERENCES,
            EnvironmentDescriptions.REFERENCES,
            FunctionDescriptions.REFERENCES,
            GeneralStructureDescriptions.REFERENCES,
            GeneralExpressionDescriptions.REFERENCES,
            GeneralControlFlowDescriptions.REFERENCES,
            JavaDescriptions.REFERENCES,
            NamingDescriptions.REFERENCES,
            TestDescriptions.REFERENCES,
            ChapterDescriptions.REFERENCES
    );

    private static final List<Map<HeuristicCode, String>> SUMMARY_SOURCES = List.of(
            CommentDescriptions.SUMMARIES,
            EnvironmentDescriptions.SUMMARIES,
            FunctionDescriptions.SUMMARIES,
            GeneralStructureDescriptions.SUMMARIES,
            GeneralExpressionDescriptions.SUMMARIES,
            GeneralControlFlowDescriptions.SUMMARIES,
            JavaDescriptions.SUMMARIES,
            NamingDescriptions.SUMMARIES,
            TestDescriptions.SUMMARIES,
            ChapterDescriptions.SUMMARIES
    );

    private static final Map<HeuristicCode, String> NAMES = merge(NAME_SOURCES);
    private static final Map<HeuristicCode, String> GUIDANCE = merge(GUIDANCE_SOURCES);
    private static final Map<HeuristicCode, String> REFERENCES = merge(REFERENCE_SOURCES);
    private static final Map<HeuristicCode, String> SUMMARIES = merge(SUMMARY_SOURCES);

    private static Map<HeuristicCode, String> merge(final List<Map<HeuristicCode, String>> sources) {
        final Map<HeuristicCode, String> combined = new HashMap<>();
        sources.forEach(combined::putAll);
        return Map.copyOf(combined);
    }

    public static String name(final HeuristicCode code) {
        return NAMES.getOrDefault(code, code.name());
    }

    public static String summary(final HeuristicCode code) {
        return SUMMARIES.get(code);
    }

    public static String guidance(final HeuristicCode code) {
        return GUIDANCE.get(code);
    }

    public static String reference(final HeuristicCode code) {
        return REFERENCES.get(code);
    }
}
