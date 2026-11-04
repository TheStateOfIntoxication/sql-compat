package com.sqlcompat.core.rule;

import com.sqlcompat.core.fix.FixSafety;
import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.model.SourceLocation;
import com.sqlcompat.core.model.SqlStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import com.sqlcompat.core.util.J8;

/**
 * 规则接口的**默认契约**。
 *
 * <p>这三个 default 方法是"不实现就安全"的兜底：默认不可改写、默认等级为需确认、默认原样返回。
 * 一旦有人改动默认值，会让一批本来不该被改写的规则悄悄进入改写链，所以单独钉住。
 */
class DialectRuleDefaultsTest {

    /** 只实现必修方法的规则，其余走默认值。 */
    private static final class MinimalRule implements DialectRule {
        @Override
        public String id() {
            return "R900";
        }

        @Override
        public String name() {
            return "最小实现";
        }

        @Override
        public Severity defaultSeverity() {
            return Severity.MINOR;
        }

        @Override
        public String description() {
            return "只用于验证默认值";
        }

        @Override
        public List<Issue> check(RuleContext context) {
            return J8.list();
        }
    }

    @Test
    @DisplayName("默认不可自动改写")
    void defaultsToNotFixable() {
        assertThat(new MinimalRule().fixable()).isFalse();
    }

    @Test
    @DisplayName("默认可改写等级是 REVIEW 而不是 SAFE —— 宁可少改，不可误改")
    void defaultsToReviewSafety() {
        assertThat(new MinimalRule().fixSafety()).isEqualTo(FixSafety.REVIEW);
    }

    @Test
    @DisplayName("默认 fix 原样返回，不会把文本改坏")
    void defaultFixIsIdentity() {
        String sql = "SELECT IFNULL(a, 1) FROM t";
        assertThat(new MinimalRule().fix(sql)).isEqualTo(sql);
    }

    @Test
    @DisplayName("默认 suggestion 为空串而不是 null，渲染层不必做空判断")
    void defaultSuggestionIsEmptyString() {
        assertThat(new MinimalRule().suggestion()).isEmpty();
    }

    @Test
    @DisplayName("check 的入参是只读上下文，默认实现不会用到 AST")
    void defaultCheckIgnoresAst() {
        SqlStatement statement = new SqlStatement("ns", "id", "SELECT 1", SourceLocation.of("f.xml", 1));
        RuleContext context = new RuleContext(statement, null);

        assertThat(context.hasAst()).isFalse();
        assertThat(new MinimalRule().check(context)).isEmpty();
    }
}
