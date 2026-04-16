package org.fiftieshousewife.cleancode.core;

public final class HtmlEscaping {

    private HtmlEscaping() {}

    public static String escape(final String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
