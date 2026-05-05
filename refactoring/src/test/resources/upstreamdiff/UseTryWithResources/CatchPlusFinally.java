package com.example;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
public class CatchPlusFinally {
    public byte[] read(Path path) {
        InputStream in;
        try {
            in = Files.newInputStream(path);
        } catch (IOException e) {
            return new byte[0];
        }
        try {
            return in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        } finally {
            try { in.close(); } catch (IOException ignored) {}
        }
    }
}