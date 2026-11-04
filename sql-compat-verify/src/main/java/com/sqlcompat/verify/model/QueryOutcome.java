package com.sqlcompat.verify.model;

import com.sqlcompat.core.util.J8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 单个数据库对单条用例的返回结果与耗时。
 */
public final class QueryOutcome {

    private final List<List<String>> rows;
    private final long millis;
    private final String error;

    public QueryOutcome(List<List<String>> rows, long millis, String error) {
        this.rows = rows == null
                ? J8.<List<String>>list()
                : Collections.unmodifiableList(new ArrayList<>(rows));
        this.millis = millis;
        this.error = error;
    }

    /** 归一化后的行；每个单元格都已是规范字符串。 */
    public List<List<String>> rows() {
        return rows;
    }

    /** 测量轮次的中位耗时，单位毫秒。 */
    public long millis() {
        return millis;
    }

    /** 失败原因；执行成功为 {@code null}。 */
    public String error() {
        return error;
    }

    public boolean failed() {
        return error != null;
    }

    public static QueryOutcome failure(String error) {
        return new QueryOutcome(J8.<List<String>>list(), 0L, error);
    }

    public int rowCount() {
        return rows.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof QueryOutcome)) {
            return false;
        }
        QueryOutcome that = (QueryOutcome) o;
        return millis == that.millis
                && rows.equals(that.rows)
                && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rows, millis, error);
    }

    @Override
    public String toString() {
        return "QueryOutcome[" + rows.size() + " rows, " + millis + " ms"
                + (error == null ? "" : ", error=" + error) + "]";
    }
}
