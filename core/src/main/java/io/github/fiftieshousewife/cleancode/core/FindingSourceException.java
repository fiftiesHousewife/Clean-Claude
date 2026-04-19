package io.github.fiftieshousewife.cleancode.core;

public class FindingSourceException extends Exception {
    private static final long serialVersionUID = 1L;

    public FindingSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public FindingSourceException(String message) {
        super(message);
    }
}
