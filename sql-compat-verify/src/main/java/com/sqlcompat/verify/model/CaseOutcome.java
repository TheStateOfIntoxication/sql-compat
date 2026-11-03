package com.sqlcompat.verify.model;

import java.util.List;

/**
 * One case, both sides, and the verdict.
 *
 * @param sqlCase case definition
 * @param status  verdict
 * @param detail  human readable reason; for DIFFERENT it explains the first mismatching row
 * @param mysql   MySQL outcome
 * @param postgres PostgreSQL outcome
 */
public record CaseOutcome(
        SqlCase sqlCase,
        Status status,
        String detail,
        QueryOutcome mysql,
        QueryOutcome postgres) {

    public static CaseOutcome error(SqlCase sqlCase, String detail, QueryOutcome mysql, QueryOutcome postgres) {
        return new CaseOutcome(sqlCase, Status.ERROR, detail, mysql, postgres);
    }
}
