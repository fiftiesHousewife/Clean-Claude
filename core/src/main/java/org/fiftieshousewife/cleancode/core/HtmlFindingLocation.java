package org.fiftieshousewife.cleancode.core;

final class HtmlFindingLocation {

    private static final String SRC_MAIN_JAVA = "src/main/java/";
    private static final String SRC_PREFIX = "src/";

    private HtmlFindingLocation() {}

    static String format(final Finding finding) {
        if (finding.sourceFile() == null) {
            return "(project)";
        }
        final String file = shortenPath(finding.sourceFile());
        if (finding.startLine() > 0) {
            return file + ":" + finding.startLine();
        }
        return file;
    }

    static String toHtml(final Finding finding, final String location, final String repositoryUrl) {
        if (finding.sourceFile() == null || repositoryUrl == null || repositoryUrl.isBlank()) {
            return HtmlEscaping.escape(location);
        }
        final String baseUrl = stripTrailingSlash(repositoryUrl);
        final String relativePath = relativiseSourceFile(finding.sourceFile());
        final String fileUrl = baseUrl + "/" + relativePath;
        final String linkedUrl = finding.startLine() > 0
                ? fileUrl + "#L" + finding.startLine() : fileUrl;
        return "<a href=\"" + HtmlEscaping.escape(linkedUrl) + "\">"
                + HtmlEscaping.escape(location) + "</a>";
    }

    private static String stripTrailingSlash(final String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String relativiseSourceFile(final String sourceFile) {
        final int srcIdx = sourceFile.indexOf(SRC_PREFIX);
        if (srcIdx > 0) {
            return sourceFile.substring(srcIdx);
        }
        return sourceFile;
    }

    private static String shortenPath(final String path) {
        final int srcIdx = path.indexOf(SRC_MAIN_JAVA);
        if (srcIdx >= 0) {
            return path.substring(srcIdx + SRC_MAIN_JAVA.length());
        }
        return path;
    }
}
