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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Batch 3 (identifiers / MyBatis / types): R009 backticks, R011 foreach,
 * R014 TINYINT(1), R015 ON UPDATE, R018 index prefix.
 */
class IdentifierAndTypeRulesTest {

    @Nested
    @DisplayName("R009 backtick identifiers")
    class Backticks {

        private final DialectRule rule = new BacktickQuoteRule();

        @Test
        @DisplayName("detects a backtick-quoted column and shows the double-quoted rewrite")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT `asset_name` FROM `demo_asset` WHERE id = 1");

            assertThat(issues).hasSize(2);
            assertThat(issues).allSatisfy(i -> assertThat(i.ruleId()).isEqualTo("R009"));
            assertThat(issues.getFirst().suggestion()).isEqualTo("`asset_name` -> \"asset_name\"");
        }

        @Test
        @DisplayName("stays silent on plain identifiers")
        void silentOnPlain() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT asset_name FROM demo_asset WHERE id = 1");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R011 <foreach> binding")
    class Foreach {

        private final DialectRule rule = new ForeachBindingRule();

        @Test
        @DisplayName("fires when the statement contains a foreach tag")
        void firesWithForeach() {
            SqlStatement statement = new SqlStatement("demo.mapper", "byOwners",
                    "SELECT a.id FROM demo_asset a WHERE a.owner_id IN ( )",
                    SourceLocation.of("case.xml", 1), Set.of("foreach", "if"));
            List<Issue> issues = rule.check(new RuleContext(statement, null));

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R011");
            assertThat(issues.getFirst().message()).contains("65535");
        }

        @Test
        @DisplayName("stays silent without a foreach tag")
        void silentWithoutForeach() {
            SqlStatement statement = new SqlStatement("demo.mapper", "byId",
                    "SELECT a.id FROM demo_asset a WHERE a.id = 1",
                    SourceLocation.of("case.xml", 2), Set.of("if"));
            List<Issue> issues = rule.check(new RuleContext(statement, null));

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R014 TINYINT(1)")
    class TinyInt {

        private final DialectRule rule = new TinyIntBooleanRule();

        @Test
        @DisplayName("detects TINYINT(1) and warns against mapping to SMALLINT")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, enabled TINYINT(1) DEFAULT 0)");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R014");
            assertThat(issues.getFirst().message()).contains("SMALLINT");
        }

        @Test
        @DisplayName("stays silent on TINYINT(4)")
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
        @DisplayName("detects ON UPDATE CURRENT_TIMESTAMP")
        void detectsCurrentTimestamp() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, updated_at DATETIME "
                            + "DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R015");
            assertThat(issues.getFirst().message()).contains("silently stops updating");
        }

        @Test
        @DisplayName("detects ON UPDATE NOW()")
        void detectsNow() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "ALTER TABLE demo_asset MODIFY updated_at DATETIME ON UPDATE NOW()");

            assertThat(issues).hasSize(1);
        }

        @Test
        @DisplayName("stays silent on a plain default timestamp")
        void silentOnDefaultOnly() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R018 index prefix length")
    class IndexPrefix {

        private final DialectRule rule = new IndexPrefixLengthRule();

        @Test
        @DisplayName("detects a prefix index and suggests left()")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, name VARCHAR(200), KEY idx_name (name(10)))");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R018");
            assertThat(issues.getFirst().suggestion()).contains("left(name, 10)");
        }

        @Test
        @DisplayName("stays silent on a full-column index")
        void silentOnFullIndex() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "CREATE TABLE demo_asset (id BIGINT, name VARCHAR(200), KEY idx_name (name))");

            assertThat(issues).isEmpty();
        }
    }
}
