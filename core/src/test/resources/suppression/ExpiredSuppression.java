package com.example;

import io.github.fiftieshousewife.cleancode.annotations.SuppressCleanCode;
import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

public class ExpiredSuppression {

    @SuppressCleanCode(value = HeuristicCode.F1, reason = "Temporary", until = "2024-01-01")
    public void expiredMethod() {
        // line 10
    }

    @SuppressCleanCode(value = HeuristicCode.F1, reason = "Still valid", until = "2099-12-31")
    public void futureMethod() {
        // line 15
    }
}
