package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.LocalDate;
import java.util.Set;

record Suppression(
        String sourceFile,
        int startLine,
        int endLine,
        Set<HeuristicCode> codes,
        String reason,
        String until,
        String packagePath
) {
    boolean isPackageScoped() {
        return packagePath != null;
    }

    boolean isExpired() {
        if (until.isEmpty()) {
            return false;
        }
        return LocalDate.parse(until).isBefore(LocalDate.now());
    }

    boolean covers(Finding finding) {
        if (!codes.contains(finding.code())) {
            return false;
        }
        if (isExpired()) {
            return false;
        }
        if (isPackageScoped()) {
            return matchesPackage(finding.sourceFile()) || matchesPackage(otherFile(finding));
        }
        return matchesFile(finding.sourceFile())
                && finding.startLine() >= startLine
                && finding.startLine() <= endLine;
    }

    private boolean matchesPackage(String path) {
        return path != null && path.contains(packagePath + "/");
    }

    private boolean matchesFile(String findingFile) {
        return findingFile.equals(sourceFile)
                || findingFile.endsWith(sourceFile)
                || sourceFile.endsWith(findingFile);
    }

    private static String otherFile(Finding finding) {
        return finding.metadata() == null ? null : finding.metadata().get("otherFile");
    }
}
