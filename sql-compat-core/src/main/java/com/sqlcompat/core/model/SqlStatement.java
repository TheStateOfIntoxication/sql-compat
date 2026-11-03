package com.sqlcompat.core.model;

import java.util.Objects;
import java.util.Set;

/**
 * One SQL statement extracted from a MyBatis mapper (or a plain .sql file).
 *
 * @param namespace    mapper namespace, may be blank for plain SQL files
 * @param id           statement id inside the mapper, e.g. {@code selectUserById}
 * @param sqlText      statement body with dynamic tags flattened
 * @param location     source file and line
 * @param dynamicTags  MyBatis dynamic tag names found inside the statement
 *                     (e.g. {@code foreach}, {@code if}), lower-cased
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

    /** Convenience constructor for plain SQL statements, which have no dynamic tags. */
    public SqlStatement(String namespace, String id, String sqlText, SourceLocation location) {
        this(namespace, id, sqlText, location, Set.of());
    }

    public String qualifiedId() {
        return namespace == null || namespace.isBlank() ? id : namespace + "." + id;
    }

    public boolean hasDynamicTag(String tag) {
        return dynamicTags.contains(tag.toLowerCase(java.util.Locale.ROOT));
    }
}
