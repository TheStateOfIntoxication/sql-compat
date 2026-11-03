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
 * Batch 4 (semantics): R010 LIMIT without ORDER BY, R016 null-safe equals,
 * R017 NULL ordering, R020 date functions.
 *
 * <p>These four are the "no error, wrong answer" group — the most expensive kind of migration
 * bug, because nobody notices until a user complains about a number.
 */
class SemanticRulesTest {

    @Nested
    @DisplayName("R010 LIMIT without ORDER BY")
    class LimitWithoutOrder {

        private final DialectRule rule = new LimitWithoutOrderByRule();

        @Test
        @DisplayName("fires when a LIMIT statement has no ORDER BY")
        void firesWithoutOrderBy() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id, a.name FROM demo_asset a LIMIT 20");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R010");
            assertThat(issues.getFirst().message()).contains("undefined");
        }

        @Test
        @DisplayName("stays silent when ORDER BY is present")
        void silentWithOrderBy() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.id LIMIT 20");

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("stays silent when there is no LIMIT at all")
        void silentWithoutLimit() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R016 <=> null-safe equals")
    class NullSafeEquals {

        private final DialectRule rule = new NullSafeEqualsRule();

        @Test
        @DisplayName("detects <=> and suggests IS NOT DISTINCT FROM")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.owner_id <=> #{ownerId}");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R016");
            assertThat(issues.getFirst().suggestion()).contains("IS NOT DISTINCT FROM");
        }

        @Test
        @DisplayName("stays silent on plain equality")
        void silentOnEquals() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.owner_id = #{ownerId}");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R017 ORDER BY DESC + LIMIT")
    class NullOrdering {

        private final DialectRule rule = new NullOrderingRule();

        @Test
        @DisplayName("fires on DESC with LIMIT and suggests NULLS LAST")
        void firesOnDescWithLimit() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.created_at DESC LIMIT 20");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R017");
            assertThat(issues.getFirst().suggestion()).contains("NULLS LAST");
        }

        @Test
        @DisplayName("stays silent without LIMIT")
        void silentWithoutLimit() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.created_at DESC");

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("stays silent on ASC ordering")
        void silentOnAsc() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.created_at ASC LIMIT 20");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R020 date functions")
    class DateFunctions {

        private final DialectRule rule = new DateFunctionRule();

        @Test
        @DisplayName("detects DATE_ADD and gives an interval rewrite")
        void detectsDateAdd() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT DATE_ADD(a.expired_at, INTERVAL 30 DAY) FROM demo_asset a");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().ruleId()).isEqualTo("R020");
            assertThat(issues.getFirst().suggestion()).contains("INTERVAL '1 day'");
        }

        @Test
        @DisplayName("detects STR_TO_DATE and gives a TO_DATE rewrite")
        void detectsStrToDate() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT STR_TO_DATE(a.day, '%Y-%m-%d') FROM demo_asset a");

            assertThat(issues).hasSize(1);
            assertThat(issues.getFirst().suggestion()).isEqualTo("STR_TO_DATE(s, '%Y-%m-%d') -> TO_DATE(s, 'YYYY-MM-DD')");
        }

        @Test
        @DisplayName("stays silent on the portable interval form")
        void silentOnInterval() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.expired_at + INTERVAL '30 day' FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }
}
