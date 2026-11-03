package com.sqlcompat.verify.model;

/**
 * 一条用例、两侧结果，以及判定。
 *
 * @param sqlCase  用例定义
 * @param status   判定结果
 * @param detail   可读原因；DIFFERENT 时说明第一处不一致的位置
 * @param mysql    MySQL 侧结果
 * @param postgres PostgreSQL 侧结果
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
