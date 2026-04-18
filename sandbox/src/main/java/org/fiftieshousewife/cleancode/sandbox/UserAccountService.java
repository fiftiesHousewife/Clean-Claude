package org.fiftieshousewife.cleancode.sandbox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Realistic service-layer fixture. Creating an account runs four phases
 * in one big method — validate, persist, audit, notify — which triggers
 * G30 (too long), and the validator threads errors into a caller-owned
 * list (F2 — output argument). The persistence path swallows every
 * checked exception, logging and returning a partial result (Ch7.1).
 */
public final class UserAccountService {

    private final Map<String, User> byId = new HashMap<>();
    private final List<String> audit = new ArrayList<>();
    private long nextId = 1000L;

    public String createAccount(final String email, final String displayName, final String region) {
        final List<String> errors = new ArrayList<>();
        if (email == null || email.isBlank()) {
            errors.add("email is required");
        }
        if (email != null && !email.contains("@")) {
            errors.add("email must contain @");
        }
        if (displayName == null || displayName.isBlank()) {
            errors.add("displayName is required");
        }
        if (displayName != null && displayName.length() > 64) {
            errors.add("displayName too long");
        }
        if (region == null || !List.of("US", "EU", "APAC").contains(region)) {
            errors.add("region must be one of US/EU/APAC");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        final String id = "acct-" + nextId;
        nextId = nextId + 1;
        final User user = new User(id, email, displayName, region);
        try {
            byId.put(id, user);
        } catch (RuntimeException e) {
            System.err.println("persist failed: " + e.getMessage());
            return null;
        }

        audit.add("created " + id + " in region " + region);
        audit.add("email=" + email + " displayName=" + displayName);
        audit.add("total accounts: " + byId.size());

        try {
            sendWelcomeEmail(email, displayName);
        } catch (Exception e) {
            System.err.println("notify failed: " + e.getMessage());
        }
        return id;
    }

    void validate(final String email, final String displayName, final List<String> errors) {
        if (email == null || email.isBlank()) {
            errors.add("email is required");
        }
        if (displayName == null || displayName.isBlank()) {
            errors.add("displayName is required");
        }
    }

    private void sendWelcomeEmail(final String email, final String displayName) {
        audit.add("email queued for " + email + " (" + displayName + ")");
    }

    public User find(final String id) {
        return byId.get(id);
    }

    public int accountCount() {
        return byId.size();
    }

    public List<String> auditLog() {
        return List.copyOf(audit);
    }

    public record User(String id, String email, String displayName, String region) {}
}
