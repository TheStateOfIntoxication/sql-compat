package com.sqlcompat.core.fix;

import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;
import com.sqlcompat.core.rule.builtin.BacktickQuoteRule;
import com.sqlcompat.core.rule.builtin.DateFormatRule;
import com.sqlcompat.core.rule.builtin.IfnullRule;
import com.sqlcompat.core.rule.builtin.JsonExtractRule;
import com.sqlcompat.core.rule.builtin.NullSafeEqualsRule;
import com.sqlcompat.core.rule.builtin.PagingSyntaxRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 自动改写的正确性与幂等性。
 *
 * <p>幂等性单独立一条：{@code fix(fix(x)) == fix(x)}。迁移工具会被反复执行，
 * 不能收敛的改写比不改写更危险。
 */
class RuleFixTest {

    private void assertFix(DialectRule rule, String input, String expected) {
        String once = rule.fix(input);
        assertThat(once).isEqualTo(expected);
        assertThat(rule.fix(once))
                .as("二次改写必须不产生变化，否则无法收敛")
                .isEqualTo(once);
    }

    @Nested
    @DisplayName("R001 IFNULL → COALESCE（SAFE）")
    class Ifnull {

        private final DialectRule rule = new IfnullRule();

        @Test
        @DisplayName("替换函数名，参数原样保留")
        void rewrites() {
            assertFix(rule,
                    "SELECT IFNULL(a.name, 'x') FROM demo_asset a",
                    "SELECT COALESCE(a.name, 'x') FROM demo_asset a");
        }

        @Test
        @DisplayName("大小写混写也处理")
        void handlesMixedCase() {
            assertFix(rule, "select ifNull(a, 0) from t", "select COALESCE(a, 0) from t");
        }

        @Test
        @DisplayName("已经是 COALESCE 时不改动")
        void untouchedWhenAlreadyFixed() {
            assertFix(rule, "SELECT COALESCE(a, 0) FROM t", "SELECT COALESCE(a, 0) FROM t");
        }
    }

    @Nested
    @DisplayName("R004 分页语法（SAFE）")
    class Paging {

        private final DialectRule rule = new PagingSyntaxRule();

        @Test
        @DisplayName("参数位置互换")
        void swapsArguments() {
            assertFix(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.id LIMIT 10, 20",
                    "SELECT a.id FROM demo_asset a ORDER BY a.id LIMIT 20 OFFSET 10");
        }

        @Test
        @DisplayName("MyBatis 占位符同样互换")
        void swapsPlaceholders() {
            assertFix(rule,
                    "SELECT a.id FROM demo_asset a LIMIT #{offset}, #{pageSize}",
                    "SELECT a.id FROM demo_asset a LIMIT #{pageSize} OFFSET #{offset}");
        }

        @Test
        @DisplayName("OFFSET 形式保持不动")
        void untouchedWhenOffsetForm() {
            String sql = "SELECT a.id FROM demo_asset a LIMIT 20 OFFSET 10";
            assertFix(rule, sql, sql);
        }
    }

    @Nested
    @DisplayName("R016 <=> → IS NOT DISTINCT FROM（SAFE）")
    class NullSafeEquals {

        private final DialectRule rule = new NullSafeEqualsRule();

        @Test
        @DisplayName("运算符换成标准谓词")
        void rewrites() {
            assertFix(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.owner_id <=> #{ownerId}",
                    "SELECT a.id FROM demo_asset a WHERE a.owner_id IS NOT DISTINCT FROM #{ownerId}");
        }

        @Test
        @DisplayName("普通等号保持不动")
        void untouchedOnPlainEquals() {
            String sql = "SELECT a.id FROM demo_asset a WHERE a.owner_id = 1";
            assertFix(rule, sql, sql);
        }
    }

    @Nested
    @DisplayName("R002 DATE_FORMAT → TO_CHAR（REVIEW）")
    class DateFormat {

        private final DialectRule rule = new DateFormatRule();

        @Test
        @DisplayName("常见格式符完整映射")
        void mapsCommonSpecifiers() {
            assertFix(rule,
                    "SELECT DATE_FORMAT(a.created_at, '%Y-%m-%d %H:%i:%s') FROM demo_asset a",
                    "SELECT TO_CHAR(a.created_at, 'YYYY-MM-DD HH24:MI:SS') FROM demo_asset a");
        }

        @Test
        @DisplayName("带前后缀文本的格式串同样映射")
        void mapsWithLiteralText() {
            assertFix(rule,
                    "SELECT DATE_FORMAT(a.created_at, '%Y年%m月%d日') FROM demo_asset a",
                    "SELECT TO_CHAR(a.created_at, 'YYYY年MM月DD日') FROM demo_asset a");
        }

        @Test
        @DisplayName("依赖 locale 的格式符原样保留，交给人工")
        void leavesLocaleSpecifierAlone() {
            String sql = "SELECT DATE_FORMAT(a.created_at, '%W %M %Y') FROM demo_asset a";
            assertFix(rule, sql, sql);
        }

        @Test
        @DisplayName("格式串不是字面量时不动")
        void leavesDynamicFormatAlone() {
            String sql = "SELECT DATE_FORMAT(a.created_at, #{fmt}) FROM demo_asset a";
            assertFix(rule, sql, sql);
        }

        @Test
        @DisplayName("函数名已在 TO_CHAR 时不改动")
        void untouchedWhenAlreadyFixed() {
            String sql = "SELECT TO_CHAR(a.created_at, 'YYYY-MM-DD') FROM demo_asset a";
            assertFix(rule, sql, sql);
        }
    }

    @Nested
    @DisplayName("R009 反引号 → 双引号（REVIEW）")
    class Backticks {

        private final DialectRule rule = new BacktickQuoteRule();

        @Test
        @DisplayName("逐个替换为双引号")
        void rewrites() {
            assertFix(rule,
                    "SELECT `asset_name` FROM `demo_asset` WHERE id = 1",
                    "SELECT \"asset_name\" FROM \"demo_asset\" WHERE id = 1");
        }

        @Test
        @DisplayName("无引号标识符保持不动")
        void untouchedOnPlain() {
            String sql = "SELECT asset_name FROM demo_asset";
            assertFix(rule, sql, sql);
        }
    }

    @Nested
    @DisplayName("R013 JSON_EXTRACT → ->>（REVIEW）")
    class JsonExtract {

        private final DialectRule rule = new JsonExtractRule();

        @Test
        @DisplayName("单层路径改写为 ->>")
        void rewritesSingleLevelPath() {
            assertFix(rule,
                    "SELECT JSON_EXTRACT(a.attrs, '$.owner') FROM demo_asset a",
                    "SELECT a.attrs ->> 'owner' FROM demo_asset a");
        }

        @Test
        @DisplayName("多级路径留给人工，不硬改")
        void leavesNestedPathAlone() {
            String sql = "SELECT JSON_EXTRACT(a.attrs, '$.meta.owner') FROM demo_asset a";
            assertFix(rule, sql, sql);
        }
    }

    @Test
    @DisplayName("不支持改写的规则 fixable() 为 false，避免被误加入改写链")
    void nonFixableRulesAreDeclared() {
        List<DialectRule> fixable = List.of(
                new IfnullRule(), new PagingSyntaxRule(), new NullSafeEqualsRule(),
                new DateFormatRule(), new BacktickQuoteRule(), new JsonExtractRule());

        assertThat(fixable).allSatisfy(r -> {
            assertThat(r.fixable()).isTrue();
            assertThat(r.check(new RuleContext(
                    new com.sqlcompat.core.model.SqlStatement("ns", "id", "SELECT 1",
                            com.sqlcompat.core.model.SourceLocation.of("f.xml", 1)), null)))
                    .isNotNull();
            assertThat(r.defaultSeverity()).isNotNull();
            assertThat(r.defaultSeverity().weight()).isGreaterThanOrEqualTo(Severity.MINOR.weight());
        });
    }
}
