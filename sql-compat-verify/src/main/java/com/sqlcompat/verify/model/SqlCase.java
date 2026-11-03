package com.sqlcompat.verify.model;

import java.util.Objects;

/**
 * One portable SQL case that should produce the same answer on both databases.
 *
 * @param id      case id, derived from the file name so it is stable and reviewable in a diff
 * @param sql     the statement to execute
 * @param ordered when {@code true} the row order is part of the contract (the case has ORDER BY)
 */
public record SqlCase(String id, String sql, boolean ordered) {

    public SqlCase {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sql, "sql");
    }
}
