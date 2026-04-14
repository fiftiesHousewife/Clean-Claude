package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;

public final class XmlReportParser {

    private XmlReportParser() {}

    public static Document parse(Path file) throws FindingSourceException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(file.toFile());
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse XML report: " + file, e);
        }
    }
}
