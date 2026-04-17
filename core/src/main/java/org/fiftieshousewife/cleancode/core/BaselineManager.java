package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class BaselineManager {

    private BaselineManager() {}

    public record Delta(int baseline, int current, int change) {}

    public static void writeBaseline(AggregatedReport report, Path baselineFile) throws IOException {
        final Map<String, Integer> counts = report.findings().stream()
                .collect(Collectors.groupingBy(
                        f -> f.code().name(),
                        TreeMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        final Map<String, Object> wrapper = Map.of("counts", counts);
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, gson.toJson(wrapper));
    }

    public static Map<HeuristicCode, Integer> readBaseline(Path baselineFile) throws IOException {
        if (!Files.exists(baselineFile)) {
            return Map.of();
        }

        final String json = Files.readString(baselineFile);
        final Gson gson = new Gson();
        final Map<String, Object> raw = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());

        @SuppressWarnings("unchecked")
        final Map<String, Double> rawCounts = (Map<String, Double>) raw.get("counts");
        if (rawCounts == null) {
            return Map.of();
        }

        final Map<HeuristicCode, Integer> result = new EnumMap<>(HeuristicCode.class);
        rawCounts.forEach((key, value) -> {
            try {
                result.put(HeuristicCode.valueOf(key), value.intValue());
            } catch (IllegalArgumentException ignored) {
                // Skip unknown codes
            }
        });
        return result;
    }

    public static Map<HeuristicCode, Delta> computeDeltas(AggregatedReport report,
                                                            Path baselineFile) throws IOException {
        final Map<HeuristicCode, Integer> baselineCounts = readBaseline(baselineFile);

        final Map<HeuristicCode, Long> currentCounts = report.findings().stream()
                .collect(Collectors.groupingBy(Finding::code, Collectors.counting()));

        final Set<HeuristicCode> allCodes = EnumSet.noneOf(HeuristicCode.class);
        allCodes.addAll(baselineCounts.keySet());
        allCodes.addAll(currentCounts.keySet());

        final Map<HeuristicCode, Delta> deltas = new TreeMap<>();
        for (HeuristicCode code : allCodes) {
            final int baseline = baselineCounts.getOrDefault(code, 0);
            final int current = currentCounts.getOrDefault(code, 0L).intValue();
            deltas.put(code, new Delta(baseline, current, current - baseline));
        }
        return deltas;
    }
}
