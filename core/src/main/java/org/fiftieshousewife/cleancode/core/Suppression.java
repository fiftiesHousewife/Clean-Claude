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
}
