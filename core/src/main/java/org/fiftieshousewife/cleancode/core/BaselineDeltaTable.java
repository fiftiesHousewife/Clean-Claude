package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

final class BaselineDeltaTable {

    private final AggregatedReport report;

    BaselineDeltaTable(final AggregatedReport report) {
        this.report = report;
    }

    void appendTo(final StringBuilder sb, final Path baselineFile) throws IOException {
        if (baselineFile == null || !Files.exists(baselineFile)) {
            return;
        }

        final Map<String, Double> baselineCounts = readBaselineCounts(baselineFile);
        if (baselineCounts == null) {
            return;
        }

        final Map<HeuristicCode, Long> currentCounts = report.findings().stream()
                .collect(Collectors.groupingBy(Finding::code, Collectors.counting()));

        final Set<String> allCodes = new TreeSet<>();
        allCodes.addAll(baselineCounts.keySet());
        currentCounts.keySet().forEach(c -> allCodes.add(c.name()));

        sb.append("## Current standing vs baseline\n\n");
        sb.append("| Category | Baseline | Current | Delta |\n");
        sb.append("|---|---|---|---|\n");
        allCodes.forEach(codeName -> appendRow(sb, codeName, baselineCounts, currentCounts));
        sb.append('\n');
    }

    private static Map<String, Double> readBaselineCounts(final Path baselineFile) throws IOException {
        final String json = Files.readString(baselineFile);
        final Gson gson = new Gson();
        final Map<String, Object> raw = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        return castToStringDoubleMap(raw.get("counts"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Double> castToStringDoubleMap(final Object value) {
        return (Map<String, Double>) value;
    }

    private static void appendRow(final StringBuilder sb, final String codeName,
                                  final Map<String, Double> baselineCounts,
                                  final Map<HeuristicCode, Long> currentCounts) {
        final int baseline = baselineCounts.containsKey(codeName)
                ? baselineCounts.get(codeName).intValue() : 0;
        final long current = currentForCode(codeName, currentCounts);
        final long delta = current - baseline;
        sb.append(String.format("| %s | %d | %d | %s |%n", codeName, baseline, current, formatDelta(delta)));
    }

    private static long currentForCode(final String codeName, final Map<HeuristicCode, Long> currentCounts) {
        try {
            return currentCounts.getOrDefault(HeuristicCode.valueOf(codeName), 0L);
        } catch (final IllegalArgumentException ignored) {
            return 0;
        }
    }

    private static String formatDelta(final long delta) {
        if (delta == 0) {
            return "0";
        }
        if (delta > 0) {
            return "+" + delta + " \u26A0";
        }
        return delta + " \u2713";
    }
}
