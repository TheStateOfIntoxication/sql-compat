package com.sqlcompat.verify.model;

import java.util.Objects;

/**
 * 一条应当在两个数据库上得到相同答案的可移植 SQL 用例。
 */
public final class SqlCase {

    private final String id;
    private final String sql;
    private final boolean ordered;

    public SqlCase(String id, String sql, boolean ordered) {
        this.id = Objects.requireNonNull(id, "id");
        this.sql = Objects.requireNonNull(sql, "sql");
        this.ordered = ordered;
    }

    /** 用例编号，取自文件名，稳定且便于在 diff 中审阅。 */
    public String id() {
        return id;
    }

    /** 要执行的语句。 */
    public String sql() {
        return sql;
    }

    /** 为 {@code true} 时行序属于契约的一部分（语句含 ORDER BY）。 */
    public boolean ordered() {
        return ordered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SqlCase)) {
            return false;
        }
        SqlCase that = (SqlCase) o;
        return ordered == that.ordered && id.equals(that.id) && sql.equals(that.sql);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sql, ordered);
    }

    @Override
    public String toString() {
        return id;
    }
}
