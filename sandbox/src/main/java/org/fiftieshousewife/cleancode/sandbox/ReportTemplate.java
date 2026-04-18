package org.fiftieshousewife.cleancode.sandbox;

import java.util.List;
import java.util.Map;

/**
 * StringBuilder-threading fixture. The private helpers take a
 * StringBuilder named `sb` and mutate it — the F2 + N1 pattern
 * identified in the manual-1 audit as endemic. The renderReport
 * method is also long enough to benefit from phase extraction (G30).
 * HTML literal is deliberately inline so the agent sees a G1
 * (multiple-languages) hint as well.
 */
public final class ReportTemplate {

    public String renderReport(final String title, final List<String> sections,
                               final Map<String, Integer> totals) {
        final StringBuilder sb = new StringBuilder();
        sb.append("<html><head><title>").append(title).append("</title></head><body>");
        sb.append("<h1>").append(title).append("</h1>");
        sb.append("<p>generated report with ").append(sections.size()).append(" sections</p>");

        sb.append("<div class=\"sections\">");
        for (final String section : sections) {
            appendSection(sb, section);
        }
        sb.append("</div>");

        sb.append("<div class=\"totals\">");
        sb.append("<h2>Totals</h2>");
        sb.append("<ul>");
        for (final Map.Entry<String, Integer> entry : totals.entrySet()) {
            appendTotalRow(sb, entry.getKey(), entry.getValue());
        }
        sb.append("</ul>");
        sb.append("</div>");

        sb.append("</body></html>");
        return sb.toString();
    }

    private void appendSection(final StringBuilder sb, final String section) {
        sb.append("<section>");
        sb.append("<h2>").append(section).append("</h2>");
        sb.append("<p>content for ").append(section).append("</p>");
        sb.append("</section>");
    }

    private void appendTotalRow(final StringBuilder sb, final String key, final int value) {
        sb.append("<li><span class=\"key\">").append(key).append("</span>");
        sb.append("<span class=\"value\">").append(value).append("</span></li>");
    }
}
