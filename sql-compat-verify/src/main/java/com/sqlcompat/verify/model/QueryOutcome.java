package com.sqlcompat.verify.model;

import java.util.List;

/**
 * 单个数据库对单条用例的返回结果与耗时。
 *
 * @param rows   归一化后的行；每个单元格都已是规范字符串
 * @param millis 测量轮次的中位耗时，单位毫秒
 * @param error  失败原因；执行成功为 {@code null}
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
