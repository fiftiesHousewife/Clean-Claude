package org.fiftieshousewife.cleancode.adapters;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RelativePathTest {

    @Test
    void relativisesCorrectly() {
        String result = RelativePath.of("/project/src/main/Foo.java", Path.of("/project"));
        assertEquals("src/main/Foo.java", result);
    }

    @Test
    void handlesTrailingSlash() {
        String result = RelativePath.of("/project/src/Foo.java", Path.of("/project"));
        assertEquals("src/Foo.java", result);
    }

    @Test
    void returnsOriginalIfNoPrefixMatch() {
        String result = RelativePath.of("/other/src/Foo.java", Path.of("/project"));
        assertEquals("/other/src/Foo.java", result);
    }
}
