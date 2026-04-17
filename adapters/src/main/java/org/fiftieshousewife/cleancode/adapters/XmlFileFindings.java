package org.fiftieshousewife.cleancode.adapters;

import org.fiftieshousewife.cleancode.core.Finding;
import org.fiftieshousewife.cleancode.core.FindingSourceException;
import org.fiftieshousewife.cleancode.core.ProjectContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

final class XmlFileFindings {

    private XmlFileFindings() {}

    static List<Finding> collect(
            final Path report,
            final ProjectContext context,
            final BiConsumer<FileContext, List<Finding>> perFile) throws FindingSourceException {
        final Document doc = XmlReportParser.parse(report);
        final List<Finding> findings = new ArrayList<>();
        final NodeList fileNodes = doc.getElementsByTagName("file");
        for (int i = 0; i < fileNodes.getLength(); i++) {
            final Element fileElement = (Element) fileNodes.item(i);
            final String relativePath = RelativePath.of(
                    fileElement.getAttribute("name"), context.projectRoot());
            perFile.accept(new FileContext(fileElement, relativePath), findings);
        }
        return findings;
    }

    record FileContext(Element fileElement, String relativePath) {}
}
