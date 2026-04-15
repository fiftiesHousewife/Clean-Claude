package org.fiftieshousewife.cleancode.claudereview;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class ReviewCache {

    private static final String CACHE_FILE = "claude-review-cache.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CACHE_TYPE = new TypeToken<Map<String, List<CachedFinding>>>() {}.getType();

    record CachedFinding(String code, String sourceFile, int startLine, int endLine, String message) {}

    private final Map<String, List<CachedFinding>> entries;

    private ReviewCache(Map<String, List<CachedFinding>> entries) {
        this.entries = entries;
    }

    static ReviewCache load(Path cacheDir) {
        final Path cacheFile = cacheDir.resolve(CACHE_FILE);
        if (!Files.exists(cacheFile)) {
            return new ReviewCache(new ConcurrentHashMap<>());
        }
        try {
            final String json = Files.readString(cacheFile, StandardCharsets.UTF_8);
            final Map<String, List<CachedFinding>> loaded = GSON.fromJson(json, CACHE_TYPE);
            return new ReviewCache(new ConcurrentHashMap<>(loaded != null ? loaded : Map.of()));
        } catch (IOException e) {
            return new ReviewCache(new ConcurrentHashMap<>());
        }
    }

    Optional<List<CachedFinding>> lookup(String hash) {
        return Optional.ofNullable(entries.get(hash));
    }

    void store(String hash, List<CachedFinding> findings) {
        entries.put(hash, List.copyOf(findings));
    }

    void save(Path cacheDir) throws IOException {
        Files.createDirectories(cacheDir);
        final Path cacheFile = cacheDir.resolve(CACHE_FILE);
        Files.writeString(cacheFile, GSON.toJson(entries, CACHE_TYPE), StandardCharsets.UTF_8);
    }

    static String hash(String fileContent, String enabledCodesKey) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(fileContent.getBytes(StandardCharsets.UTF_8));
            digest.update(enabledCodesKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
