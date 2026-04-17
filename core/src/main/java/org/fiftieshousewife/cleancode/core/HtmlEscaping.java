package org.fiftieshousewife.cleancode.core;

final class HtmlEscaping {

    private HtmlEscaping() {}

    static String escape(final String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
