package org.fiftieshousewife.cleancode.core;

public class FindingSourceException extends Exception {
    public FindingSourceException(String message, Throwable cause) {
        super(message, cause);
    }

    public FindingSourceException(String message) {
        super(message);
    }
}
