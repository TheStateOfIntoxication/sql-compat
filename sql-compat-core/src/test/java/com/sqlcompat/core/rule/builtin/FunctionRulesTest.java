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
 * 批 1（函数类）：R008 GROUP_CONCAT、R012 REGEXP、R013 JSON_EXTRACT。
 *
 * <p>每条规则至少一个正例和一个反例。反例更重要：一条爱喊狼来了的规则，一周之内就会被关掉。
 */
class FunctionRulesTest {

    @Nested
    @DisplayName("R008 GROUP_CONCAT")
    class GroupConcat {

        private final DialectRule rule = new GroupConcatRule();

        @Test
        @DisplayName("检出 GROUP_CONCAT")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT GROUP_CONCAT(a.name) FROM demo_asset a WHERE a.owner_id = 1");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R008");
            assertThat(issues.getFirst().suggestion()).contains("STRING_AGG");
        }

        @Test
        @DisplayName("用了 SEPARATOR 时在说明里点出来")
        void mentionsSeparator() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT GROUP_CONCAT(a.name SEPARATOR '|') FROM demo_asset a");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().message()).contains("SEPARATOR");
        }

        @Test
        @DisplayName("STRING_AGG 不应误报")
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
        @DisplayName("检出 REGEXP")
        void detectsRegexp() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.code REGEXP '^AB[0-9]+$'");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R012");
            assertThat(issues.getFirst().suggestion()).contains("~");
        }

        @Test
        @DisplayName("检出 RLIKE")
        void detectsRlike() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.code RLIKE '^AB'");

            assertThat(issues).hasSize(1);
        }

        @Test
        @DisplayName("~ 运算符不应误报")
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
        @DisplayName("检出 JSON_EXTRACT，并提示 ->> 的作用")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT JSON_EXTRACT(a.attrs, '$.owner') FROM demo_asset a");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R013");
            assertThat(issues.getFirst().message()).contains("->>");
        }

        @Test
        @DisplayName("箭头运算符不应误报")
        void silentOnArrow() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.attrs ->> 'owner' FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }
}
