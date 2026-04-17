package org.fiftieshousewife.cleancode.plugin;

record SummaryCounts(int errors, int warnings, int info) {

    static SummaryCounts zero() {
        return new SummaryCounts(0, 0, 0);
    }

    SummaryCounts plus(final SummaryCounts other) {
        return new SummaryCounts(errors + other.errors, warnings + other.warnings, info + other.info);
    }

    int total() {
        return errors + warnings + info;
    }
}
