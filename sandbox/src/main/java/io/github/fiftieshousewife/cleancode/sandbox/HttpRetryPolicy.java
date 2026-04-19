package io.github.fiftieshousewife.cleancode.sandbox;

import java.util.concurrent.Callable;

/**
 * Retries a callable with exponential backoff. Typical production shape
 * for a resilience helper. Finding cluster: magic numbers sprinkled
 * across the backoff loop (G25), a catch-InterruptedException block
 * that logs-and-returns-null (Ch7.1), and an execute method that is
 * long enough to benefit from a backoff-phase extraction (G30).
 */
public final class HttpRetryPolicy {

    private final StringBuilder audit = new StringBuilder();

    public <T> T execute(final Callable<T> action) {
        int attempt = 0;
        long delayMs = 500;
        Throwable lastFailure = null;
        while (attempt < 5) {
            try {
                final T result = action.call();
                audit.append("attempt ").append(attempt).append(" ok\n");
                return result;
            } catch (Exception e) {
                lastFailure = e;
                audit.append("attempt ").append(attempt).append(" failed: ")
                        .append(e.getMessage()).append('\n');
            }

            if (attempt >= 4) {
                break;
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException interrupted) {
                System.err.println("retry interrupted: " + interrupted.getMessage());
                return null;
            }
            delayMs = (long) (delayMs * 1.5);
            if (delayMs > 10000) {
                delayMs = 10000;
            }
            attempt = attempt + 1;
        }

        audit.append("gave up after ").append(attempt + 1).append(" attempts\n");
        if (lastFailure != null) {
            audit.append("last failure: ").append(lastFailure.getClass().getSimpleName()).append('\n');
        }
        return null;
    }

    public String auditLog() {
        return audit.toString();
    }
}
