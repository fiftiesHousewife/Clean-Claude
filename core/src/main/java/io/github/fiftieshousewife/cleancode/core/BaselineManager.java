package io.github.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public final class BaselineManager {

    private BaselineManager() {}

    public record Delta(int baseline, int current, int change) {}

    public static void writeBaseline(AggregatedReport report, Path baselineFile) throws IOException {
        Map<String, Integer> counts = report.findings().stream()
                .collect(Collectors.groupingBy(
                        f -> f.code().name(),
                        TreeMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        Map<String, Object> wrapper = Map.of("counts", counts);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        Files.createDirectories(baselineFile.getParent());
        Files.writeString(baselineFile, gson.toJson(wrapper));
    }

    public static Map<HeuristicCode, Integer> readBaseline(Path baselineFile) throws IOException {
        if (!Files.exists(baselineFile)) {
            return Map.of();
        }

        String json = Files.readString(baselineFile);
        Gson gson = new Gson();
        Map<String, Object> raw = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());

        @SuppressWarnings("unchecked")
        Map<String, Double> rawCounts = (Map<String, Double>) raw.get("counts");
        if (rawCounts == null) {
            return Map.of();
        }

        Map<HeuristicCode, Integer> result = new EnumMap<>(HeuristicCode.class);
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
        Map<HeuristicCode, Integer> baselineCounts = readBaseline(baselineFile);

        Map<HeuristicCode, Long> currentCounts = report.findings().stream()
                .collect(Collectors.groupingBy(Finding::code, Collectors.counting()));

        Set<HeuristicCode> allCodes = EnumSet.noneOf(HeuristicCode.class);
        allCodes.addAll(baselineCounts.keySet());
        allCodes.addAll(currentCounts.keySet());

        Map<HeuristicCode, Delta> deltas = new TreeMap<>();
        for (HeuristicCode code : allCodes) {
            int baseline = baselineCounts.getOrDefault(code, 0);
            int current = currentCounts.getOrDefault(code, 0L).intValue();
            deltas.put(code, new Delta(baseline, current, current - baseline));
        }
        return deltas;
    }
}
