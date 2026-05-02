package io.github.fiftieshousewife.cleancode.plugin.serve;

import java.util.List;

public record ApplyChangesResponse(boolean success, int applied, List<String> errors) {
    public ApplyChangesResponse {
        errors = List.copyOf(errors);
    }

    public static ApplyChangesResponse ok(final int applied) {
        return new ApplyChangesResponse(true, applied, List.of());
    }

    public static ApplyChangesResponse failed(final List<String> errors) {
        return new ApplyChangesResponse(false, 0, errors);
    }
}
