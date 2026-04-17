package org.fiftieshousewife.cleancode.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

final class JsonObjectFile {

    private JsonObjectFile() {}

    static Map<String, Object> read(final Path file) throws IOException {
        final String json = Files.readString(file);
        return new Gson().fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
    }
}
