package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.Set;

record SuppressionFields(Set<HeuristicCode> codes, String reason, String until) {

    boolean hasCodes() {
        return !codes.isEmpty();
    }
}
