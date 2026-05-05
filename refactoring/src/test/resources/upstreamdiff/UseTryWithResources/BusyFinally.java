package com.example;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
public class BusyFinally {
    public byte[] read(Path path) throws Exception {
        InputStream in = Files.newInputStream(path);
        try {
            return in.readAllBytes();
        } finally {
            System.out.println("done");
            in.close();
        }
    }
}