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
 * Batch 1 (functions): R008 GROUP_CONCAT, R012 REGEXP, R013 JSON_EXTRACT.
 *
 * <p>Every rule gets at least one positive case and one negative case. The negative case
 * matters more: a rule that cries wolf gets switched off within a week.
 */
class FunctionRulesTest {

    @Nested
    @DisplayName("R008 GROUP_CONCAT")
    class GroupConcat {

        private final DialectRule rule = new GroupConcatRule();

        @Test
        @DisplayName("detects GROUP_CONCAT")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT GROUP_CONCAT(a.name) FROM demo_asset a WHERE a.owner_id = 1");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R008");
            assertThat(issues.getFirst().suggestion()).contains("STRING_AGG");
        }

        @Test
        @DisplayName("mentions the separator argument when SEPARATOR is used")
        void mentionsSeparator() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT GROUP_CONCAT(a.name SEPARATOR '|') FROM demo_asset a");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().message()).contains("SEPARATOR");
        }

        @Test
        @DisplayName("stays silent on STRING_AGG")
        void silentOnStringAgg() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT STRING_AGG(a.name, ',') FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R012 REGEXP / RLIKE")
    class Regexp {

        private final DialectRule rule = new RegexpOperatorRule();

        @Test
        @DisplayName("detects REGEXP")
        void detectsRegexp() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.code REGEXP '^AB[0-9]+$'");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R012");
            assertThat(issues.getFirst().suggestion()).contains("~");
        }

        @Test
        @DisplayName("detects RLIKE")
        void detectsRlike() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.code RLIKE '^AB'");

            assertThat(issues).hasSize(1);
        }

        @Test
        @DisplayName("stays silent on the ~ operator")
        void silentOnTilde() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.code ~ '^AB[0-9]+$'");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R013 JSON_EXTRACT")
    class JsonExtract {

        private final DialectRule rule = new JsonExtractRule();

        @Test
        @DisplayName("detects JSON_EXTRACT")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT JSON_EXTRACT(a.attrs, '$.owner') FROM demo_asset a");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R013");
            assertThat(issues.getFirst().message()).contains("->>");
        }

        @Test
        @DisplayName("stays silent on the arrow operators")
        void silentOnArrow() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.attrs ->> 'owner' FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }
}
