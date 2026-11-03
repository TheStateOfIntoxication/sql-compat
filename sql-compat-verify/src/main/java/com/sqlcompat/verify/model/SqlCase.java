package com.sqlcompat.verify.model;

import java.util.Objects;

/**
 * 一条应当在两个数据库上得到相同答案的可移植 SQL 用例。
 *
 * @param id      用例编号，取自文件名，稳定且便于在 diff 中审阅
 * @param sql     要执行的语句
 * @param ordered 为 {@code true} 时行序属于契约的一部分（语句含 ORDER BY）
 */
public record SqlCase(String id, String sql, boolean ordered) {

    public SqlCase {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sql, "sql");
    }
}
