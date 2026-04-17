package org.fiftieshousewife.cleancode.claudereview;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCacheTest {

    @Test
    void roundTripStoreAndLookup(@TempDir Path tempDir) throws Exception {
        final ReviewCache cache = ReviewCache.load(tempDir);
        final List<ReviewCache.CachedFinding> findings = List.of(
                new ReviewCache.CachedFinding("G6", "Foo.java", 10, 15, "wrong level"));

        cache.store("abc123", findings);
        cache.save();

        final ReviewCache reloaded = ReviewCache.load(tempDir);
        final var result = reloaded.lookup("abc123");

        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(1, result.get().size()),
                () -> assertEquals("G6", result.get().getFirst().code()),
                () -> assertEquals("wrong level", result.get().getFirst().message())
        );
    }

    @Test
    void cacheMissReturnsEmpty(@TempDir Path tempDir) {
        final ReviewCache cache = ReviewCache.load(tempDir);

        assertTrue(cache.lookup("nonexistent").isEmpty());
    }

    @Test
    void differentHashDoesNotMatchExisting(@TempDir Path tempDir) throws Exception {
        final ReviewCache cache = ReviewCache.load(tempDir);
        cache.store("hash1", List.of(
                new ReviewCache.CachedFinding("G6", "Foo.java", 1, 1, "msg")));
        cache.save();

        final ReviewCache reloaded = ReviewCache.load(tempDir);

        assertAll(
                () -> assertTrue(reloaded.lookup("hash1").isPresent()),
                () -> assertTrue(reloaded.lookup("hash2").isEmpty())
        );
    }

    @Test
    void hashProducesDeterministicOutput() {
        final String hash1 = ReviewCache.hash("content", "G6,G20");
        final String hash2 = ReviewCache.hash("content", "G6,G20");

        assertEquals(hash1, hash2);
    }

    @Test
    void hashDiffersForDifferentCodes() {
        final String hash1 = ReviewCache.hash("content", "G6,G20");
        final String hash2 = ReviewCache.hash("content", "G6,G31");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void loadsGracefullyFromMissingDirectory(@TempDir Path tempDir) {
        final Path nonExistent = tempDir.resolve("missing");
        final ReviewCache cache = ReviewCache.load(nonExistent);

        assertTrue(cache.lookup("anything").isEmpty());
    }
}
