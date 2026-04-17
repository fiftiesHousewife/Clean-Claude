package org.fiftieshousewife.cleancode.core;

final class HtmlFindingLocation {

    private static final String SRC_MAIN_JAVA = "src/main/java/";
    private static final String SRC_PREFIX = "src/";
    private static final String PROJECT_LEVEL_LOCATION = "(project)";

    private HtmlFindingLocation() {}

    static String format(final Finding finding) {
        if (isProjectLevel(finding)) {
            return PROJECT_LEVEL_LOCATION;
        }
        final String file = shortenPath(finding.sourceFile());
        if (hasLineNumber(finding)) {
            return file + ":" + finding.startLine();
        }
        return file;
    }

    static String toHtml(final Finding finding, final String location, final String repositoryUrl) {
        if (cannotLinkToSource(finding, repositoryUrl)) {
            return HtmlEscaping.escape(location);
        }
        final String baseUrl = stripTrailingSlash(repositoryUrl);
        final String relativePath = relativiseSourceFile(finding.sourceFile());
        final String fileUrl = baseUrl + "/" + relativePath;
        final String linkedUrl = hasLineNumber(finding) ? fileUrl + "#L" + finding.startLine() : fileUrl;
        final String anchorHtml = "<a href=\"" + HtmlEscaping.escape(linkedUrl) + "\">"
                + HtmlEscaping.escape(location) + "</a>";
        return anchorHtml;
    }

    private static boolean isProjectLevel(final Finding finding) {
        return finding.sourceFile() == null;
    }

    private static boolean hasLineNumber(final Finding finding) {
        return finding.startLine() > 0;
    }

    private static boolean cannotLinkToSource(final Finding finding, final String repositoryUrl) {
        return isProjectLevel(finding) || repositoryUrl == null || repositoryUrl.isBlank();
    }

    private static String stripTrailingSlash(final String url) {
        final int lastCharIndex = url.length() - 1;
        return url.endsWith("/") ? url.substring(0, lastCharIndex) : url;
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
