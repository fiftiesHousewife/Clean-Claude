package io.github.fiftieshousewife.cleancode.plugin.serve;

public record FeedbackResponse(boolean success, String savedTo, String error) {

    public static FeedbackResponse ok(final String savedTo) {
        return new FeedbackResponse(true, savedTo, null);
    }

    public static FeedbackResponse failed(final String error) {
        return new FeedbackResponse(false, null, error);
    }
}
