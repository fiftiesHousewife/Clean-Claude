package io.github.fiftieshousewife.cleancode.adapters;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathUtilsTest {

    @Test
    void relativisesCorrectly() {
        String result = PathUtils.relativise("/project/src/main/Foo.java", Path.of("/project"));
        assertEquals("src/main/Foo.java", result);
    }

    @Test
    void handlesTrailingSlash() {
        String result = PathUtils.relativise("/project/src/Foo.java", Path.of("/project"));
        assertEquals("src/Foo.java", result);
    }

    @Test
    void returnsOriginalIfNoPrefixMatch() {
        String result = PathUtils.relativise("/other/src/Foo.java", Path.of("/project"));
        assertEquals("/other/src/Foo.java", result);
    }
}
