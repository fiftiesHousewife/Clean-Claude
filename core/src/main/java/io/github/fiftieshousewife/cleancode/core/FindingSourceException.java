package io.github.fiftieshousewife.cleancode.core;

public class FindingSourceException extends Exception {
    private static final long serialVersionUID = 1L;

    public FindingSourceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FindingSourceException(final String message) {
        super(message);
    }
}
