package io.github.fiftieshousewife.cleancode.adapters;

import io.github.fiftieshousewife.cleancode.core.FindingSourceException;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Path;

public final class XmlReportParser {

    private XmlReportParser() {}

    public static Document parse(Path file) throws FindingSourceException {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            final DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
            return builder.parse(file.toFile());
        } catch (Exception e) {
            throw new FindingSourceException("Failed to parse XML report: " + file, e);
        }
    }
}
