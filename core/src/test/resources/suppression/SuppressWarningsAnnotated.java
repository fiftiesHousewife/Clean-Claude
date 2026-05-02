package com.example;

public class SuppressWarningsAnnotated {

    @SuppressWarnings("CleanCode:G30")
    public void singleSuppressed() {
        int x = 1;
    }

    @SuppressWarnings({"CleanCode:N5", "CleanCode:Ch7_2"})
    public void arraySuppressed() {
        int y = 2;
    }

    public void unsuppressedMethod() {
        int z = 3;
    }
}
