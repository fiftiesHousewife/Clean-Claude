package com.example;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
public class MisaligningClose {
    public byte[] read(Path path, InputStream other) throws Exception {
        InputStream in = Files.newInputStream(path);
        try {
            return in.readAllBytes();
        } finally {
            other.close();
        }
    }
}