package io.github.fiftieshousewife.cleancode.sandbox;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Session cache with expiry. Exhibits the null-return pattern that
 * Ch7.2 targets, negative-conditional nesting (G29), and a lookup
 * method whose core predicate would be clearer as an explanatory
 * variable (G19). The expiry arithmetic is also a G25 candidate
 * (hardcoded 30L * 60L * 1000L).
 */
public final class SessionStore {

    private final Map<String, Session> sessions = new HashMap<>();

    public String userIdFor(final String token) {
        if (token == null) {
            return null;
        }
        if (token.isBlank()) {
            return null;
        }
        final Session session = sessions.get(token);
        if (session == null) {
            return null;
        }
        if (!session.active) {
            return null;
        }
        if (Instant.now().toEpochMilli() - session.createdAtMs > 30L * 60L * 1000L) {
            return null;
        }
        return session.userId;
    }

    public Session lookupOrNull(final String token) {
        if (!sessions.containsKey(token)) {
            return null;
        }
        final Session session = sessions.get(token);
        if (!session.active) {
            return null;
        }
        return session;
    }

    public void open(final String token, final String userId) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("token is required");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        sessions.put(token, new Session(userId, Instant.now().toEpochMilli(), true));
    }

    public void close(final String token) {
        final Session existing = sessions.get(token);
        if (existing == null) {
            return;
        }
        sessions.put(token, new Session(existing.userId, existing.createdAtMs, false));
    }

    public int activeSessionCount() {
        int count = 0;
        for (final Session session : sessions.values()) {
            if (session.active) {
                count = count + 1;
            }
        }
        return count;
    }

    public static final class Session {
        public final String userId;
        public final long createdAtMs;
        public final boolean active;

        public Session(final String userId, final long createdAtMs, final boolean active) {
            this.userId = userId;
            this.createdAtMs = createdAtMs;
            this.active = active;
        }
    }
}
