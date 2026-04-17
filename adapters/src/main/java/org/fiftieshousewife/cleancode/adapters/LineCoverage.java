package org.fiftieshousewife.cleancode.adapters;

import org.w3c.dom.Element;

record LineCoverage(int missed, int covered) {

    private static final double PERCENT_MULTIPLIER = 100.0;

    static LineCoverage from(Element counter) {
        return new LineCoverage(
                Integer.parseInt(counter.getAttribute("missed")),
                Integer.parseInt(counter.getAttribute("covered")));
    }

    int total() {
        return missed + covered;
    }

    double percentage() {
        return total() > 0 ? (covered * PERCENT_MULTIPLIER) / total() : 0;
    }
}
