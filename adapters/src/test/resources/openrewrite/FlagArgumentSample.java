package com.example;

public class FlagArgumentSample {

    public void publicWithFlag(String name, boolean verbose) {
        // F3 violation — boolean param on public method
    }

    void packagePrivateWithFlag(int count, boolean dryRun) {
        // F3 violation — boolean param on package-private method
    }

    private void privateWithFlag(boolean hidden) {
        // NOT a violation — private method
    }

    public FlagArgumentSample(boolean init) {
        // NOT a violation — constructor
    }

    public void noFlag(String name, int count) {
        // NOT a violation — no boolean params
    }

    protected void protectedWithFlag(boolean enabled) {
        // F3 violation — boolean param on protected method
    }
}
