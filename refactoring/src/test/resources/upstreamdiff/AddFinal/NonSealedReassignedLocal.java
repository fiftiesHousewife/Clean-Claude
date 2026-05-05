package com.example;
public class NonSealedReassignedLocal {
    public int compute() {
        int x = 5;
        x = x + 1;
        return x;
    }
}
