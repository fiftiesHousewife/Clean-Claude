package org.fiftieshousewife.cleancode.sandbox;

import java.util.ArrayList;
import java.util.List;

/**
 * Stylistic-heavy fixture. dispatch switches on a channel-kind string
 * via an if/else-if ladder (G23 — polymorphism would be cleaner) and
 * takes a boolean "urgent" flag that selects an entirely different
 * code path per branch (F3 — split into two methods). Also a G25
 * opportunity on the retry-count literal.
 */
public final class NotificationDispatcher {

    private final List<String> dispatched = new ArrayList<>();

    public void dispatch(final String channel, final String recipient, final String body,
                         final boolean urgent) {
        if (channel.equals("email")) {
            if (urgent) {
                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
                    dispatched.add("email(urgent) -> " + recipient + ": " + body);
                }
            } else {
                dispatched.add("email -> " + recipient + ": " + body);
            }
        } else if (channel.equals("sms")) {
            if (urgent) {
                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
                    dispatched.add("sms(urgent) -> " + recipient + ": " + body);
                }
            } else {
                dispatched.add("sms -> " + recipient + ": " + body);
            }
        } else if (channel.equals("push")) {
            if (urgent) {
                for (int attempt = 0; attempt < 3; attempt = attempt + 1) {
                    dispatched.add("push(urgent) -> " + recipient + ": " + body);
                }
            } else {
                dispatched.add("push -> " + recipient + ": " + body);
            }
        } else if (channel.equals("webhook")) {
            dispatched.add("webhook -> " + recipient + ": " + body);
        } else {
            throw new IllegalArgumentException("unknown channel: " + channel);
        }
    }

    public int dispatchedCount() {
        return dispatched.size();
    }

    public List<String> dispatched() {
        return List.copyOf(dispatched);
    }
}
