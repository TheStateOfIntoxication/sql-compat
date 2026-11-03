package com.sqlcompat.core.model;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 从 Mapper XML（或纯 SQL 文件）中抽取出来的一条语句。
 *
 * @param namespace   mapper 的 namespace；纯 SQL 文件为空
 * @param id          mapper 内的语句 id，例如 {@code selectUserById}
 * @param sqlText     语句正文，动态标签已展平
 * @param location    来源文件与行号
 * @param dynamicTags 语句内出现过的 MyBatis 动态标签名（如 {@code foreach}、{@code if}），已转小写
 */
public record SqlStatement(
        String namespace,
        String id,
        String sqlText,
        SourceLocation location,
        Set<String> dynamicTags) {

    public SqlStatement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sqlText, "sqlText");
        if (location == null) {
            location = SourceLocation.UNKNOWN;
        }
        dynamicTags = dynamicTags == null ? Set.of() : Set.copyOf(dynamicTags);
    }

    /** 纯 SQL 语句的便捷构造器：不存在动态标签。 */
    public SqlStatement(String namespace, String id, String sqlText, SourceLocation location) {
        this(namespace, id, sqlText, location, Set.of());
    }

    public String qualifiedId() {
        return namespace == null || namespace.isBlank() ? id : namespace + "." + id;
    }

    public boolean hasDynamicTag(String tag) {
        return dynamicTags.contains(tag.toLowerCase(Locale.ROOT));
    }
}
