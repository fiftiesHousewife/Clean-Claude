package com.example;
public sealed class SealedNonReassignedLocal permits SealedNonReassignedLocal.Sub {
    public int compute() {
        int x = 5;
        int y = 7;
        return x + y;
    }
    public static final class Sub extends SealedNonReassignedLocal {}
}
