package com.sqlcompat.core.model;

import java.util.Objects;

/**
 * 规则产出的单条兼容性发现。
 *
 * @param ruleId     稳定的规则编号，如 {@code R001}
 * @param ruleName   规则名，会出现在报告里
 * @param severity   严重级别
 * @param message    问题说明
 * @param snippet    语句中出问题的片段
 * @param suggestion 在目标方言下应改写成什么
 * @param location   在源文件中的位置
 */
public record Issue(
        String ruleId,
        String ruleName,
        Severity severity,
        String message,
        String snippet,
        String suggestion,
        SourceLocation location) {

    public Issue {
        Objects.requireNonNull(ruleId, "ruleId");
        Objects.requireNonNull(severity, "severity");
        if (location == null) {
            location = SourceLocation.UNKNOWN;
        }
    }
}
