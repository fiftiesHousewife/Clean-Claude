package io.github.fiftieshousewife.cleancode.core;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class FrameworkRegistry {

    private static final Map<String, String> PREFIX_TO_FRAMEWORK = Map.ofEntries(
            Map.entry("org.springframework.boot", "Spring Boot"),
            Map.entry("org.openrewrite", "OpenRewrite"),
            Map.entry("com.google.code.gson", "Gson"),
            Map.entry("io.projectreactor", "Project Reactor"),
            Map.entry("org.junit.jupiter", "JUnit 5"),
            Map.entry("com.github.javaparser", "JavaParser"),
            Map.entry("org.projectlombok", "Lombok"),
            Map.entry("io.quarkus", "Quarkus"),
            Map.entry("io.micronaut", "Micronaut"),
            Map.entry("jakarta.", "Jakarta EE"),
            Map.entry("javax.", "Java EE (legacy)"),
            Map.entry("org.apache.kafka", "Kafka"),
            Map.entry("io.grpc", "gRPC"),
            Map.entry("com.squareup.okhttp3", "OkHttp"),
            Map.entry("com.fasterxml.jackson", "Jackson")
    );

    private FrameworkRegistry() {}

    public static List<String> detect(final List<String> dependencyCoordinates) {
        final TreeSet<String> detected = dependencyCoordinates.stream()
                .flatMap(coord -> PREFIX_TO_FRAMEWORK.entrySet().stream()
                        .filter(entry -> coord.startsWith(entry.getKey()))
                        .map(Map.Entry::getValue))
                .collect(Collectors.toCollection(TreeSet::new));
        return List.copyOf(detected);
    }
}
