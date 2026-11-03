package com.sqlcompat.core.rule;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.SourceLocation;
import com.sqlcompat.core.model.SqlStatement;

import java.util.List;

/**
 * 测试辅助类：对一段纯 SQL 单独跑一条规则。
 */
public final class RuleTestSupport {

    private RuleTestSupport() {
    }

    public static List<Issue> check(DialectRule rule, String sql) {
        SqlStatement statement = new SqlStatement("demo.mapper", "case", sql, SourceLocation.of("case.xml", 1));
        return rule.check(new RuleContext(statement, null));
    }
}
