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

final class DeltaTableWriter {

    private DeltaTableWriter() {}

    static void append(final StringBuilder sb, final AggregatedReport report,
                       final Path baselineFile) throws IOException {
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

        for (final String codeName : allCodes) {
            final int baseline = baselineCounts.containsKey(codeName)
                    ? baselineCounts.get(codeName).intValue() : 0;
            long current = 0;
            try {
                current = currentCounts.getOrDefault(HeuristicCode.valueOf(codeName), 0L);
            } catch (final IllegalArgumentException ignored) {
                // Unknown code in baseline — not a code we recognise
            }
            final long delta = current - baseline;
            final String deltaStr = formatDelta(delta);
            sb.append(String.format("| %s | %d | %d | %s |%n", codeName, baseline, current, deltaStr));
        }
        sb.append('\n');
    }

    private static Map<String, Double> readBaselineCounts(final Path baselineFile) throws IOException {
        final String json = Files.readString(baselineFile);
        final Gson gson = new Gson();
        final Map<String, Object> raw = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
        @SuppressWarnings("unchecked")
        final Map<String, Double> baselineCounts = (Map<String, Double>) raw.get("counts");
        return baselineCounts;
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
