package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.core.FindingSourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlReportParserTest {

    @Test
    void parsesValidXml(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("test.xml");
        Files.writeString(file, "<?xml version=\"1.0\"?><root><child/></root>");

        Document doc = XmlReportParser.parse(file);
        assertEquals("root", doc.getDocumentElement().getTagName());
    }

    @Test
    void throwsOnMissingFile(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("nonexistent.xml");
        assertThrows(FindingSourceException.class, () -> XmlReportParser.parse(missing));
    }

    @Test
    void throwsOnMalformedXml(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bad.xml");
        Files.writeString(file, "this is not xml");
        assertThrows(FindingSourceException.class, () -> XmlReportParser.parse(file));
    }
}
