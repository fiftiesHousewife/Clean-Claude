package com.example;
import java.io.IOException;
public class OtherException {
    public void run() {
        try {
            throw new IOException("oops");
        } catch (IOException e) {
            // swallow
        }
    }
}
