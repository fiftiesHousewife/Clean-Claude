package io.github.fiftieshousewife.cleancode.sandbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-extract-method fixture. {@link #run} is a long orchestrator
 * method carrying five distinct phases, each cohesive and of extractable
 * size; the class holds instance state (so G18 doesn't fire) and the
 * method has a void return (so F2 / mutation-vs-return doesn't fire).
 * There are no fully-qualified references, no magic string literals,
 * no section comments — so G12 / G25 / G34 don't fire either. The only
 * live findings should be G30 (method too long) and Ch10.1 (class too
 * long), which is exactly the shape extract_method was built for.
 */
public final class PipelineFixture {

    private final StringBuilder audit = new StringBuilder();
    private final List<String> warnings = new ArrayList<>();
    private int eventsProcessed = 0;
    private int eventsSkipped = 0;

    public void run(final List<String> events, final int threshold) {
        audit.append("pipeline start\n");
        audit.append("input size: ").append(events.size()).append('\n');
        audit.append("threshold: ").append(threshold).append('\n');
        audit.append("warnings reset\n");
        warnings.clear();

        final List<String> validated = new ArrayList<>();
        for (final String event : events) {
            if (event == null || event.isBlank()) {
                warnings.add("skipped blank event");
                eventsSkipped++;
                continue;
            }
            validated.add(event.trim());
        }
        audit.append("validated: ").append(validated.size()).append('\n');

        final List<String> normalized = new ArrayList<>();
        for (final String event : validated) {
            final String lower = event.toLowerCase();
            final String collapsed = lower.replaceAll("\\s+", " ");
            final String stripped = collapsed.replaceAll("[^a-z0-9 ]", "");
            normalized.add(stripped);
        }
        audit.append("normalized: ").append(normalized.size()).append('\n');

        int accepted = 0;
        int rejected = 0;
        for (final String event : normalized) {
            if (event.length() < threshold) {
                rejected++;
                warnings.add("rejected short event: " + event);
                continue;
            }
            accepted++;
            eventsProcessed++;
        }
        audit.append("accepted: ").append(accepted).append('\n');
        audit.append("rejected: ").append(rejected).append('\n');

        audit.append("pipeline end\n");
        audit.append("warnings: ").append(warnings.size()).append('\n');
        audit.append("processed (cumulative): ").append(eventsProcessed).append('\n');
        audit.append("skipped (cumulative): ").append(eventsSkipped).append('\n');
    }

    public int eventsProcessed() {
        return eventsProcessed;
    }

    public int eventsSkipped() {
        return eventsSkipped;
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }

    public String audit() {
        return audit.toString();
    }
}
