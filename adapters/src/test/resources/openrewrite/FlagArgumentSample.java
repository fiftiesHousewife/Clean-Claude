package com.example;

public class FlagArgumentSample {

    public void publicWithFlag(final String name, final boolean verbose) {
        // F3 violation — boolean param on public method
    }

    void packagePrivateWithFlag(final int count, final boolean dryRun) {
        // F3 violation — boolean param on package-private method
    }

    private void privateWithFlag(final boolean hidden) {
        // NOT a violation — private method
    }

    public FlagArgumentSample(final boolean init) {
        // NOT a violation — constructor
    }

    public void noFlag(final String name, final int count) {
        // NOT a violation — no boolean params
    }

    protected void protectedWithFlag(final boolean enabled) {
        // F3 violation — boolean param on protected method
    }
}
