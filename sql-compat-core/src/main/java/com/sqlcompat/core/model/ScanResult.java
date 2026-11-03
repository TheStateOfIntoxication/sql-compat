package com.sqlcompat.core.model;

import java.util.List;

/**
 * 单条语句的扫描结果。
 *
 * @param statement  被扫描的语句
 * @param issues     发现的问题；为空表示这条语句是干净的
 * @param parseError 无法解析成 AST 时的错误信息，正常为 {@code null}
 */
public record ScanResult(SqlStatement statement, List<Issue> issues, String parseError) {

    public ScanResult {
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean clean() {
        return issues.isEmpty();
    }
}
