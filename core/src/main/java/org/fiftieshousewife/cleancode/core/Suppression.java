package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.time.LocalDate;
import java.util.Set;

record Suppression(Scope scope, Coverage coverage) {

    record Scope(String sourceFile, int startLine, int endLine, String packagePath) {
    }

    record Coverage(Set<HeuristicCode> codes, String reason, String until) {
    }

    boolean isPackageScoped() {
        return scope.packagePath() != null;
    }

    boolean isExpired() {
        if (coverage.until().isEmpty()) {
            return false;
        }
        return LocalDate.parse(coverage.until()).isBefore(LocalDate.now());
    }

    boolean covers(final Finding finding) {
        if (!coverage.codes().contains(finding.code())) {
            return false;
        }
        if (isExpired()) {
            return false;
        }
        if (isPackageScoped()) {
            return matchesPackage(finding.sourceFile()) || matchesPackage(otherFile(finding));
        }
        return matchesFile(finding.sourceFile())
                && finding.startLine() >= scope.startLine()
                && finding.startLine() <= scope.endLine();
    }

    private boolean matchesPackage(final String path) {
        return path != null && path.contains(scope.packagePath() + "/");
    }

    private boolean matchesFile(final String findingFile) {
        return findingFile.equals(scope.sourceFile())
                || findingFile.endsWith(scope.sourceFile())
                || scope.sourceFile().endsWith(findingFile);
    }

    private static String otherFile(final Finding finding) {
        return finding.metadata() == null ? null : finding.metadata().get("otherFile");
    }
}
