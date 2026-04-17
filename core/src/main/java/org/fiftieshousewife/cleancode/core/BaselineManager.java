package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    private static final Set<String> KNOWN_CODES = EnumSet.allOf(HeuristicCode.class).stream()
            .map(Enum::name)
            .collect(Collectors.toUnmodifiableSet());

    private BaselineManager() {}

    public record Delta(int baseline, int current, int change) {}

    private record BaselineSnapshot(Map<String, Integer> counts) {
        BaselineSnapshot {
            counts = counts == null ? Map.of() : counts;
        }
    }

    public static void writeBaseline(final AggregatedReport report, final Path baselineFile) throws IOException {
        final Map<String, Integer> counts = report.findings().stream()
                .collect(Collectors.groupingBy(
                        f -> f.code().name(),
                        TreeMap::new,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)));

        final BaselineSnapshot snapshot = new BaselineSnapshot(counts);
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        final Path parent = baselineFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(baselineFile, gson.toJson(snapshot));
    }

    public static Map<HeuristicCode, Integer> readBaseline(final Path baselineFile) throws IOException {
        if (!Files.exists(baselineFile)) {
            return Map.of();
        }

        final String json = Files.readString(baselineFile);
        final BaselineSnapshot snapshot = new Gson().fromJson(json, BaselineSnapshot.class);

        final Map<HeuristicCode, Integer> result = new EnumMap<>(HeuristicCode.class);
        snapshot.counts().entrySet().stream()
                .filter(entry -> KNOWN_CODES.contains(entry.getKey()))
                .forEach(entry -> result.put(HeuristicCode.valueOf(entry.getKey()), entry.getValue()));
        return result;
    }

    public static Map<HeuristicCode, Delta> computeDeltas(final AggregatedReport report,
                                                            final Path baselineFile) throws IOException {
        final Map<HeuristicCode, Integer> baselineCounts = readBaseline(baselineFile);

        final Map<HeuristicCode, Long> currentCounts = report.findings().stream()
                .collect(Collectors.groupingBy(Finding::code, Collectors.counting()));

        final Set<HeuristicCode> allCodes = EnumSet.noneOf(HeuristicCode.class);
        allCodes.addAll(baselineCounts.keySet());
        allCodes.addAll(currentCounts.keySet());

        final Map<HeuristicCode, Delta> deltas = new TreeMap<>();
        for (final HeuristicCode code : allCodes) {
            final int baseline = baselineCounts.getOrDefault(code, 0);
            final int current = currentCounts.getOrDefault(code, 0L).intValue();
            deltas.put(code, new Delta(baseline, current, current - baseline));
        }
        return deltas;
    }
}
