package io.github.fiftieshousewife.cleancode.core;

import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.List;
import java.util.Set;

public interface FindingSource {
    String id();
    String displayName();

    List<Finding> collectFindings(ProjectContext context) throws FindingSourceException;

    Set<HeuristicCode> coveredCodes();

    default boolean isAvailable(final ProjectContext context) {
        return true;
    }
}
