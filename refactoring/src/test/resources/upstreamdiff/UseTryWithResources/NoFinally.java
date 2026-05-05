package com.example;
public class NoFinally {
    public void run() {
        String s = "hi";
        try {
            System.out.println(s);
        } catch (RuntimeException e) {
            // ignore
        }
    }
}