package io.github.fiftieshousewife.cleancode.plugin.serve;

import java.util.List;

public record ApplyChangesRequest(List<PendingChange> changes) {
    public ApplyChangesRequest {
        changes = List.copyOf(changes);
    }
}
