package com.sqlcompat.verify.model;

import java.util.Objects;

/**
 * 一条用例、两侧结果，以及判定。
 */
public final class CaseOutcome {

    private final SqlCase sqlCase;
    private final Status status;
    private final String detail;
    private final QueryOutcome mysql;
    private final QueryOutcome postgres;

    public CaseOutcome(SqlCase sqlCase,
                       Status status,
                       String detail,
                       QueryOutcome mysql,
                       QueryOutcome postgres) {
        this.sqlCase = sqlCase;
        this.status = status;
        this.detail = detail;
        this.mysql = mysql;
        this.postgres = postgres;
    }

    public static CaseOutcome error(SqlCase sqlCase, String detail, QueryOutcome mysql, QueryOutcome postgres) {
        return new CaseOutcome(sqlCase, Status.ERROR, detail, mysql, postgres);
    }

    /** 用例定义。 */
    public SqlCase sqlCase() {
        return sqlCase;
    }

    /** 判定结果。 */
    public Status status() {
        return status;
    }

    /** 可读原因；DIFFERENT 时说明第一处不一致的位置。 */
    public String detail() {
        return detail;
    }

    /** MySQL 侧结果。 */
    public QueryOutcome mysql() {
        return mysql;
    }

    /** PostgreSQL 侧结果。 */
    public QueryOutcome postgres() {
        return postgres;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaseOutcome)) {
            return false;
        }
        CaseOutcome that = (CaseOutcome) o;
        return Objects.equals(sqlCase, that.sqlCase)
                && status == that.status
                && Objects.equals(detail, that.detail)
                && Objects.equals(mysql, that.mysql)
                && Objects.equals(postgres, that.postgres);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sqlCase, status, detail, mysql, postgres);
    }

    @Override
    public String toString() {
        return (sqlCase == null ? "<no case>" : sqlCase.id()) + " -> " + status;
    }
}
