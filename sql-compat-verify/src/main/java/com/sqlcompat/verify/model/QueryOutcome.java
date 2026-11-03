package com.sqlcompat.verify.model;

import java.util.List;

/**
 * What one database returned for one case, plus how long it took.
 *
 * @param rows  normalized rows; every cell is already a canonical string
 * @param millis median wall time of the measured iterations, in milliseconds
 * @param error  failure message, or {@code null} when the statement succeeded
 */
public record QueryOutcome(List<List<String>> rows, long millis, String error) {

    public QueryOutcome {
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public boolean failed() {
        return error != null;
    }

    public static QueryOutcome failure(String error) {
        return new QueryOutcome(List.of(), 0L, error);
    }

    public int rowCount() {
        return rows.size();
    }
}
