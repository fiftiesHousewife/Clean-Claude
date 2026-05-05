package com.example;
import java.io.InputStream;
public class NoInitializer {
    public void read(InputStream src) throws Exception {
        InputStream in;
        in = src;
        try {
            in.readAllBytes();
        } finally {
            in.close();
        }
    }
}
