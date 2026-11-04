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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import com.sqlcompat.core.util.J8;
import com.sqlcompat.core.util.Io;

/**
 * 从 MyBatis Mapper XML 中抽取语句。
 *
 * <p>动态标签（{@code <if>}、{@code <where>}、{@code <foreach>} 等）做展平处理：保留其文本内容、
 * 丢掉标签本身。这足够做绝大多数文本级方言检查，也避免了实现一个完整的 MyBatis 动态 SQL 模拟器。
 * 标签名仍会记录到语句上，因为个别规则（R011）需要知道语句里有没有 {@code <foreach>}。
 *
 * <p>解析过程不访问网络：外部 DTD 加载已关闭，因此缺 {@code mybatis-3-mapper.dtd} 也不会失败。
 */
public final class MyBatisXmlParser {

    private static final Set<String> STATEMENT_TAGS = J8.set("select", "insert", "update", "delete");

    /** 值得记录的标签，其余标签直接丢弃不留痕迹。 */
    private static final Set<String> TRACKED_TAGS = J8.set(
            "if", "foreach", "choose", "when", "otherwise", "where", "set", "trim", "bind", "include");

    public List<SqlStatement> parse(Path xmlFile) throws IOException {
        String raw = Io.readString(xmlFile, StandardCharsets.UTF_8);
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
                if (id == null || id.trim().isEmpty()) {
                    continue;
                }
                Set<String> dynamicTags = new LinkedHashSet<>();
                String sql = flatten(element, dynamicTags);
                if (sql.trim().isEmpty()) {
                    continue;
                }
                statements.add(new SqlStatement(
                        namespace,
                        id,
                        sql,
                        new SourceLocation(xmlFile.getFileName().toString(), findLine(raw, id)),
                        dynamicTags));
            }
        }
        return statements;
    }

    /**
     * 把元素展平成纯 SQL 文本：丢掉子标签、保留其文本，同时把需要跟踪的标签名收进 {@code dynamicTags}。
     *
     * <p><b>两个标签不能只丢不管</b>：{@code <where>} 与 {@code <set>} 自带关键字生成职责，
     * 丢了标签就等于丢了 {@code WHERE} / {@code SET}，展平后直接不是合法 SQL。
     * 同时 MyBatis 会擦掉 {@code <where>} 开头的 AND/OR、{@code <set>} 末尾的多余逗号，这里一并模拟。
     */
    private String flatten(Node node, Set<String> dynamicTags) {
        StringBuilder sb = new StringBuilder();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                String tagName = child.getNodeName().toLowerCase(Locale.ROOT);
                if (TRACKED_TAGS.contains(tagName)) {
                    dynamicTags.add(tagName);
                }
                String inner = flatten(child, dynamicTags);
                // 用 if-else 而不是 switch 表达式：switch 表达式是 Java 14 的语法，
                // 而本工具要能在 JDK 8 的 Maven 里运行。
                if ("where".equals(tagName)) {
                    sb.append(' ').append(renderWhere(inner)).append(' ');
                } else if ("set".equals(tagName)) {
                    sb.append(' ').append(renderSet(inner)).append(' ');
                } else if ("choose".equals(tagName)) {
                    sb.append(' ').append(renderChooseFirstBranch(child, dynamicTags)).append(' ');
                } else {
                    sb.append(' ').append(inner).append(' ');
                }
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /** {@code <where>}：生成 WHERE，并抹掉内层开头的 AND / OR（MyBatis 就是这么干的）。 */
    private String renderWhere(String inner) {
        String body = inner.trim().replaceFirst("(?i)^(and|or)\\s+", "");
        return body.isEmpty() ? "" : "WHERE " + body;
    }

    /** {@code <set>}：生成 SET，并抹掉末尾多余逗号。 */
    private String renderSet(String inner) {
        String body = inner.trim().replaceFirst(",+$", "");
        return body.isEmpty() ? "" : "SET " + body;
    }

    /**
     * {@code <choose>}：运行期只会走其中**一个**分支，所以展平时也只能取一个。
     *
     * <p>早期版本把 when / otherwise 全部拼在一起，结果一条语句里出现两个
     * {@code ORDER BY}，必然解析失败 —— 这是真实项目上暴露的。
     * 静态检查不需要穷举所有分支，取第一个分支（优先 {@code <when>}）即可。
     */
    private String renderChooseFirstBranch(Node choose, Set<String> dynamicTags) {
        NodeList children = choose.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            return flatten(child, dynamicTags);
        }
        return "";
    }

    /**
     * 行号的尽力回溯：找包含 {@code id="<id>"} 的那一行。
     * DOM 不暴露行号，这个精度对报告来说够用。
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
            // mapper 引用了一个我们并不分发的 DTD，这里直接吞掉 DTD 拉取。
            builder.setEntityResolver((publicId, systemId) ->
                    new org.xml.sax.InputSource(new java.io.StringReader("")));
            return builder.parse(xmlFile.toFile());
        } catch (ParserConfigurationException | org.xml.sax.SAXException e) {
            throw new IOException("解析 Mapper XML 失败：" + xmlFile, e);
        }
    }
}
