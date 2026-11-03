package com.sqlcompat.core.parser;

import com.sqlcompat.core.model.SourceLocation;
import com.sqlcompat.core.model.SqlStatement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts statements from MyBatis mapper XML files.
 *
 * <p>Dynamic tags ({@code <if>}, {@code <where>}, {@code <foreach>}, ...) are flattened:
 * their text content is kept, the tag itself is dropped. That is enough for most textual
 * dialect checks and avoids needing a full MyBatis dynamic-SQL simulator.
 *
 * <p>The parser never touches the network: DTD loading is disabled, so a missing
 * {@code mybatis-3-mapper.dtd} will not fail the scan.
 */
public final class MyBatisXmlParser {

    private static final Set<String> STATEMENT_TAGS = Set.of("select", "insert", "update", "delete");

    public List<SqlStatement> parse(Path xmlFile) throws IOException {
        String raw = Files.readString(xmlFile, StandardCharsets.UTF_8);
        Document document = readDocument(xmlFile);
        Element root = document.getDocumentElement();
        String namespace = root.getAttribute("namespace");

        List<SqlStatement> statements = new ArrayList<>();
        for (String tag : STATEMENT_TAGS) {
            NodeList nodes = document.getElementsByTagName(tag);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                String id = element.getAttribute("id");
                if (id == null || id.isBlank()) {
                    continue;
                }
                String sql = flatten(element);
                if (sql.isBlank()) {
                    continue;
                }
                statements.add(new SqlStatement(
                        namespace,
                        id,
                        sql,
                        new SourceLocation(xmlFile.getFileName().toString(), findLine(raw, id))));
            }
        }
        return statements;
    }

    /**
     * Flattens an element into plain SQL text: child tags are dropped, their text kept.
     */
    private String flatten(Node node) {
        StringBuilder sb = new StringBuilder();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                sb.append(' ').append(flatten(child)).append(' ');
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /**
     * Best-effort line lookup: finds the line containing {@code id="<id>"}.
     * DOM does not expose line numbers, and this is accurate enough for reports.
     */
    private int findLine(String raw, String id) {
        String[] lines = raw.split("\\R", -1);
        Pattern pattern = Pattern.compile("id\\s*=\\s*\"" + Pattern.quote(id) + "\"");
        for (int i = 0; i < lines.length; i++) {
            if (pattern.matcher(lines[i]).find()) {
                return i + 1;
            }
        }
        return 0;
    }

    private Document readDocument(Path xmlFile) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Swallow DTD fetch errors: mapper files reference a DTD we do not ship.
            builder.setEntityResolver((publicId, systemId) -> new org.xml.sax.InputSource(new java.io.StringReader("")));
            return builder.parse(xmlFile.toFile());
        } catch (ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IOException("Failed to parse mapper XML: " + xmlFile, e);
        }
    }
}
