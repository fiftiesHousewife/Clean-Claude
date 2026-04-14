package com.example;

import org.fiftieshousewife.cleancode.annotations.SuppressCleanCode;
import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

public class MethodSuppressed {

    @SuppressCleanCode(value = HeuristicCode.G30, reason = "Legacy method")
    public void complexMethod() {
        // lines 9-11
        int x = 1;
    }

    public void unsuppressedMethod() {
        // lines 14-16
        int y = 2;
    }
}
