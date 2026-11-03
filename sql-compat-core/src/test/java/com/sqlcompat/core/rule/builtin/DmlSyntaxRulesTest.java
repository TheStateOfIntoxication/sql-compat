package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 批 2（DML / DDL 语法）：R004 分页、R005 INSERT IGNORE、R006 upsert、
 * R007 AUTO_INCREMENT、R019 REPLACE INTO。
 */
class DmlSyntaxRulesTest {

    @Nested
    @DisplayName("R004 LIMIT offset, size")
    class Paging {

        private final DialectRule rule = new PagingSyntaxRule();

        @Test
        @DisplayName("建议里把两个参数位置换过来")
        void swapsArguments() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.id LIMIT 10, 20");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R004");
            assertThat(issues.getFirst().suggestion()).isEqualTo("LIMIT 20 OFFSET 10");
        }

        @Test
        @DisplayName("能处理 MyBatis 占位符")
        void handlesPlaceholders() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a LIMIT #{offset}, #{pageSize}");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().suggestion()).isEqualTo("LIMIT #{pageSize} OFFSET #{offset}");
        }

        @Test
        @DisplayName("LIMIT size OFFSET offset 不应误报")
        void silentOnOffsetForm() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a LIMIT 20 OFFSET 10");

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("单参数 LIMIT 不应误报")
        void silentOnSingleArg() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a LIMIT 20");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R005 INSERT IGNORE")
    class InsertIgnore {

        private final DialectRule rule = new InsertIgnoreRule();

        @Test
        @DisplayName("检出 INSERT IGNORE，并提示语义差异")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "INSERT IGNORE INTO demo_asset (id, name) VALUES (1, 'a')");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R005");
            assertThat(issues.getFirst().message()).contains("数据错误");
        }

        @Test
        @DisplayName("普通 INSERT 不应误报")
        void silentOnPlainInsert() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "INSERT INTO demo_asset (id, name) VALUES (1, 'a')");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R006 ON DUPLICATE KEY UPDATE")
    class Upsert {

        private final DialectRule rule = new UpsertRule();

        @Test
        @DisplayName("检出 upsert，并点名 VALUES() 要改成 EXCLUDED")
        void detectsWithValuesRef() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "INSERT INTO demo_asset (id, name) VALUES (1, 'a') "
                            + "ON DUPLICATE KEY UPDATE name = VALUES(name)");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R006");
            assertThat(issues.getFirst().message()).contains("EXCLUDED.name");
        }

        @Test
        @DisplayName("没有 VALUES() 时提示冲突目标列是强制的")
        void detectsWithoutValuesRef() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "INSERT INTO demo_asset (id, name) VALUES (1, 'a') "
                            + "ON DUPLICATE KEY UPDATE name = 'b'");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().message()).contains("强制");
        }

        @Test
        @DisplayName("ON CONFLICT 不应误报")
        void silentOnConflict() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "INSERT INTO demo_asset (id, name) VALUES (1, 'a') "
                            + "ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R007 AUTO_INCREMENT")
    class AutoIncrement {

        private final DialectRule rule = new AutoIncrementRule();

        @Test
        @DisplayName("DDL 中检出 AUTO_INCREMENT，并提示序列要调到最大 id 之后")
        void detectsInDdl() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64))");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R007");
            assertThat(issues.getFirst().message()).contains("序列");
        }

        @Test
        @DisplayName("自增列写法不应误报")
        void silentOnIdentity() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY)");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R019 REPLACE INTO")
    class ReplaceInto {

        private final DialectRule rule = new ReplaceIntoRule();

        @Test
        @DisplayName("检出 REPLACE INTO，并说明先删后插的语义")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "REPLACE INTO demo_asset (id, name) VALUES (1, 'a')");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R019");
            assertThat(issues.getFirst().message()).contains("先删后插");
        }

        @Test
        @DisplayName("REPLACE() 字符串函数不应误报")
        void silentOnReplaceFunction() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT REPLACE(a.name, '-', '') FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }
}
