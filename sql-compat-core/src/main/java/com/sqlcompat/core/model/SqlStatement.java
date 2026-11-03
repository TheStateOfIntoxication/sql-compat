package com.sqlcompat.core.model;

import java.util.Objects;

/**
 * One SQL statement extracted from a MyBatis mapper (or a plain .sql file).
 *
 * @param namespace mapper namespace, may be blank for plain SQL files
 * @param id        statement id inside the mapper, e.g. {@code selectUserById}
 * @param sqlText   statement body with dynamic tags flattened
 * @param location  source file and line
 */
public record SqlStatement(String namespace, String id, String sqlText, SourceLocation location) {

    public SqlStatement {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sqlText, "sqlText");
        if (location == null) {
            location = SourceLocation.UNKNOWN;
        }
    }

    public String qualifiedId() {
        return namespace == null || namespace.isBlank() ? id : namespace + "." + id;
    }
}
