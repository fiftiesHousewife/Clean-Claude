package org.fiftieshousewife.cleancode.core;

enum AnnotateMarker {
    OPEN_PREFIX("<!-- ANNOTATE:"),
    CLOSE_PREFIX("<!-- /ANNOTATE"),
    OPEN_SUFFIX("-->");

    private final String token;

    AnnotateMarker(final String token) {
        this.token = token;
    }

    String token() {
        return token;
    }

    int length() {
        return token.length();
    }
}
