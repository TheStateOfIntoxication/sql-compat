package com.sqlcompat.core.model;

import com.sqlcompat.core.util.J8;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * 从 Mapper XML（或纯 SQL 文件）中抽取出来的一条语句。
 */
public final class SqlStatement {

    private final String namespace;
    private final String id;
    private final String sqlText;
    private final SourceLocation location;
    private final Set<String> dynamicTags;

    public SqlStatement(String namespace,
                        String id,
                        String sqlText,
                        SourceLocation location,
                        Set<String> dynamicTags) {
        this.namespace = namespace;
        this.id = Objects.requireNonNull(id, "id");
        this.sqlText = Objects.requireNonNull(sqlText, "sqlText");
        this.location = location == null ? SourceLocation.UNKNOWN : location;
        this.dynamicTags = dynamicTags == null
                ? J8.set()
                : Collections.unmodifiableSet(new LinkedHashSet<>(dynamicTags));
    }

    /** 纯 SQL 语句的便捷构造器：不存在动态标签。 */
    public SqlStatement(String namespace, String id, String sqlText, SourceLocation location) {
        this(namespace, id, sqlText, location, J8.<String>set());
    }

    public String namespace() {
        return namespace;
    }

    public String id() {
        return id;
    }

    public String sqlText() {
        return sqlText;
    }

    public SourceLocation location() {
        return location;
    }

    public Set<String> dynamicTags() {
        return dynamicTags;
    }

    public String qualifiedId() {
        return namespace == null || namespace.trim().isEmpty() ? id : namespace + "." + id;
    }

    public boolean hasDynamicTag(String tag) {
        return dynamicTags.contains(tag.toLowerCase(Locale.ROOT));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SqlStatement)) {
            return false;
        }
        SqlStatement that = (SqlStatement) o;
        return Objects.equals(namespace, that.namespace)
                && id.equals(that.id)
                && sqlText.equals(that.sqlText)
                && Objects.equals(location, that.location)
                && dynamicTags.equals(that.dynamicTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, id, sqlText, location, dynamicTags);
    }

    @Override
    public String toString() {
        return qualifiedId() + " @ " + location;
    }
}
