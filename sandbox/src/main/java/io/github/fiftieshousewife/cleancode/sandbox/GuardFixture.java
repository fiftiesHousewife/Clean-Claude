package io.github.fiftieshousewife.cleancode.sandbox;

/**
 * Phase A fixture. {@link #handle} opens with an early-return guard
 * block — a {@code null} / empty check that causes the method to
 * bail. The right extraction is the guard alone, turning it into a
 * boolean-returning {@code isInvalid(…)} helper. The surrounding
 * method stays void; the call site becomes
 * {@code if (isInvalid(event)) return;}.
 */
public final class GuardFixture {

    public void handle(final String event, final StringBuilder log) {
        if (event == null) {
            log.append("drop: null event\n");
            log.append("---\n");
            return;
        }
        if (event.isBlank()) {
            log.append("drop: blank event\n");
            log.append("---\n");
            return;
        }
        log.append("processed: ").append(event.strip()).append('\n');
    }
}
