package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.SourceLocation;
import com.sqlcompat.core.model.SqlStatement;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;
import com.sqlcompat.core.rule.RuleTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import com.sqlcompat.core.util.J8;

/**
 * 批 3（标识符 / MyBatis / 类型）：R009 反引号、R011 foreach、
 * R014 TINYINT(1)、R015 ON UPDATE、R018 索引前缀。
 */
class IdentifierAndTypeRulesTest {

    @Nested
    @DisplayName("R009 反引号标识符")
    class Backticks {

        private final DialectRule rule = new BacktickQuoteRule();

        @Test
        @DisplayName("检出反引号，并给出双引号改写")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT `asset_name` FROM `demo_asset` WHERE id = 1");

            assertThat(issues).hasSize(2);
            assertThat(issues).allSatisfy(i -> assertThat(i.ruleId()).isEqualTo("R009"));
            assertThat(issues.get(0).suggestion()).isEqualTo("`asset_name` -> \"asset_name\"");
        }

        @Test
        @DisplayName("普通标识符不应误报")
        void silentOnPlain() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT asset_name FROM demo_asset WHERE id = 1");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R011 <foreach> 拼接")
    class Foreach {

        private final DialectRule rule = new ForeachBindingRule();

        @Test
        @DisplayName("语句含 foreach 标签时触发，并提示 65535 参数上限")
        void firesWithForeach() {
            SqlStatement statement = new SqlStatement("demo.mapper", "byOwners",
                    "SELECT a.id FROM demo_asset a WHERE a.owner_id IN ( )",
                    SourceLocation.of("case.xml", 1), J8.set("foreach", "if"));
            List<Issue> issues = rule.check(new RuleContext(statement, null));

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).ruleId()).isEqualTo("R011");
            assertThat(issues.get(0).message()).contains("65535");
        }

        @Test
        @DisplayName("没有 foreach 标签时不触发")
        void silentWithoutForeach() {
            SqlStatement statement = new SqlStatement("demo.mapper", "byId",
                    "SELECT a.id FROM demo_asset a WHERE a.id = 1",
                    SourceLocation.of("case.xml", 2), J8.set("if"));
            List<Issue> issues = rule.check(new RuleContext(statement, null));

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R014 TINYINT(1)")
    class TinyInt {

        private final DialectRule rule = new TinyIntBooleanRule();

        @Test
        @DisplayName("检出 TINYINT(1)，并提示不要映射成 SMALLINT")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, enabled TINYINT(1) DEFAULT 0)");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).ruleId()).isEqualTo("R014");
            assertThat(issues.get(0).message()).contains("SMALLINT");
        }

        @Test
        @DisplayName("TINYINT(4) 不应误报")
        void silentOnTinyInt4() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, level TINYINT(4))");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R015 ON UPDATE CURRENT_TIMESTAMP")
    class OnUpdate {

        private final DialectRule rule = new OnUpdateTimestampRule();

        @Test
        @DisplayName("检出 ON UPDATE CURRENT_TIMESTAMP")
        void detectsCurrentTimestamp() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, updated_at DATETIME "
                            + "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).ruleId()).isEqualTo("R015");
            assertThat(issues.get(0).message()).contains("停止更新");
        }

        @Test
        @DisplayName("检出 ON UPDATE NOW()")
        void detectsNow() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "ALTER TABLE demo_asset MODIFY updated_at DATETIME ON UPDATE NOW()");

            assertThat(issues).hasSize(1);
        }

        @Test
        @DisplayName("只有默认值、没有 ON UPDATE 时不应误报")
        void silentOnDefaultOnly() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R018 索引前缀长度")
    class IndexPrefix {

        private final DialectRule rule = new IndexPrefixLengthRule();

        @Test
        @DisplayName("检出前缀索引，并给出 left() 的建索引语句")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, name VARCHAR(200), KEY idx_name (name(10)))");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).ruleId()).isEqualTo("R018");
            assertThat(issues.get(0).suggestion()).contains("left(name, 10)");
        }

        @Test
        @DisplayName("整列索引不应误报")
        void silentOnFullIndex() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, name VARCHAR(200), KEY idx_name (name))");

            assertThat(issues).isEmpty();
        }
    }
}
