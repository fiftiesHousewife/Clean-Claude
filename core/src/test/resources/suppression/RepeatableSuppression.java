package com.example;

import io.github.fiftieshousewife.cleancode.annotations.SuppressCleanCode;
import io.github.fiftieshousewife.cleancode.annotations.HeuristicCode;

public class RepeatableSuppression {

    @SuppressCleanCode(value = HeuristicCode.F1, reason = "Legacy API")
    @SuppressCleanCode(value = HeuristicCode.G28, reason = "Complex condition unavoidable")
    public void multiSuppressed() {
        // line 11
    }
}
