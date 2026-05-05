package com.example;
public class MissingRestore {
    public void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // swallow
        }
    }
}
