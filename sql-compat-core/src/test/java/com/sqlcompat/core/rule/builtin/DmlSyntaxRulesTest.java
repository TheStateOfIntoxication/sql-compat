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
 * Batch 2 (DML / DDL syntax): R004 paging, R005 INSERT IGNORE, R006 upsert,
 * R007 AUTO_INCREMENT, R019 REPLACE INTO.
 */
class DmlSyntaxRulesTest {

    @Nested
    @DisplayName("R004 LIMIT offset, size")
    class Paging {

        private final DialectRule rule = new PagingSyntaxRule();

        @Test
        @DisplayName("swaps the arguments in the suggestion")
        void swapsArguments() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.id LIMIT 10, 20");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R004");
            assertThat(issues.getFirst().suggestion()).isEqualTo("LIMIT 20 OFFSET 10");
        }

        @Test
        @DisplayName("handles MyBatis placeholders")
        void handlesPlaceholders() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a LIMIT #{offset}, #{pageSize}");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().suggestion()).isEqualTo("LIMIT #{pageSize} OFFSET #{offset}");
        }

        @Test
        @DisplayName("stays silent on LIMIT size OFFSET offset")
        void silentOnOffsetForm() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a LIMIT 20 OFFSET 10");

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("stays silent on a plain single-argument LIMIT")
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
        @DisplayName("detects INSERT IGNORE and warns about the semantic gap")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "INSERT IGNORE INTO demo_asset (id, name) VALUES (1, 'a')");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R005");
            assertThat(issues.getFirst().message()).contains("data errors");
        }

        @Test
        @DisplayName("stays silent on a plain INSERT")
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
        @DisplayName("detects the clause and names VALUES() refs")
        void detectsWithValuesRef() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "INSERT INTO demo_asset (id, name) VALUES (1, 'a') "
                            + "ON DUPLICATE KEY UPDATE name = VALUES(name)");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R006");
            assertThat(issues.getFirst().message()).contains("EXCLUDED.name");
        }

        @Test
        @DisplayName("detects the clause without VALUES() refs")
        void detectsWithoutValuesRef() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "INSERT INTO demo_asset (id, name) VALUES (1, 'a') "
                            + "ON DUPLICATE KEY UPDATE name = 'b'");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().message()).contains("mandatory");
        }

        @Test
        @DisplayName("stays silent on ON CONFLICT")
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
        @DisplayName("detects AUTO_INCREMENT in DDL and mentions sequence repositioning")
        void detectsInDdl() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64))");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R007");
            assertThat(issues.getFirst().message()).contains("repositioned");
        }

        @Test
        @DisplayName("stays silent on an identity column")
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
        @DisplayName("detects REPLACE INTO and explains the delete-then-insert semantics")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "REPLACE INTO demo_asset (id, name) VALUES (1, 'a')");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R019");
            assertThat(issues.getFirst().message()).contains("delete-then-insert");
        }

        @Test
        @DisplayName("does not fire on REPLACE() the string function")
        void silentOnReplaceFunction() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT REPLACE(a.name, '-', '') FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }
}
