package org.fiftieshousewife.cleancode.core;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class SuppressionIndex {

    private static final String OTHER_FILE_METADATA_KEY = "otherFile";

    private final List<Suppression> suppressions;
    private final List<Finding> metaFindings;

    private SuppressionIndex(final List<Suppression> suppressions, final List<Finding> metaFindings) {
        this.suppressions = suppressions;
        this.metaFindings = metaFindings;
    }

    public static SuppressionIndex build(final Path sourceRoot) {
        final SuppressionParser.ParsedSuppressions parsed = new SuppressionParser(sourceRoot).parse();
        return new SuppressionIndex(parsed.suppressions(), parsed.metaFindings());
    }

    public boolean isSuppressed(final Finding finding) {
        if (finding.sourceFile() == null) {
            return false;
        }
        for (final Suppression s : suppressions) {
            if (matches(s, finding)) {
                return true;
            }
        }
        return false;
    }

    public List<Finding> metaFindings() {
        return Collections.unmodifiableList(metaFindings);
    }

    private static boolean matches(final Suppression s, final Finding finding) {
        if (!s.codes().contains(finding.code()) || s.isExpired()) {
            return false;
        }
        if (s.isPackageScoped()) {
            return matchesPackage(finding.sourceFile(), s.packagePath())
                    || matchesPackage(otherFile(finding), s.packagePath());
        }
        return matchesFile(finding.sourceFile(), s.sourceFile())
                && finding.startLine() >= s.startLine()
                && finding.startLine() <= s.endLine();
    }

    private static boolean matchesPackage(final String path, final String packagePath) {
        return path != null && path.contains(packagePath + "/");
    }

    private static boolean matchesFile(final String findingFile, final String suppressionFile) {
        return findingFile.equals(suppressionFile)
                || findingFile.endsWith(suppressionFile)
                || suppressionFile.endsWith(findingFile);
    }

    private static String otherFile(final Finding finding) {
        return finding.metadata() == null ? null : finding.metadata().get(OTHER_FILE_METADATA_KEY);
    }
}
