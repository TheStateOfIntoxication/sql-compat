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
 * 批 4（语义 / 边界）：R010 分页无排序、R016 NULL 安全等于、R017 NULL 排序、R020 日期函数。
 *
 * <p>这四条属于"不报错但结果不对"那一类，是迁移里代价最高的问题：没人发现，直到业务方说数字不对。
 */
class SemanticRulesTest {

    @Nested
    @DisplayName("R010 LIMIT 无 ORDER BY")
    class LimitWithoutOrder {

        private final DialectRule rule = new LimitWithoutOrderByRule();

        @Test
        @DisplayName("有 LIMIT 没有 ORDER BY 时触发")
        void firesWithoutOrderBy() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id, a.name FROM demo_asset a LIMIT 20");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).ruleId()).isEqualTo("R010");
            assertThat(issues.get(0).message()).contains("未定义");
        }

        @Test
        @DisplayName("有 ORDER BY 时不触发")
        void silentWithOrderBy() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.id LIMIT 20");

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("没有 LIMIT 时不触发")
        void silentWithoutLimit() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R016 <=> NULL 安全等于")
    class NullSafeEquals {

        private final DialectRule rule = new NullSafeEqualsRule();

        @Test
        @DisplayName("检出 <=>，建议改为 IS NOT DISTINCT FROM")
        void detects() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a WHERE a.owner_id <=> #{ownerId}");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).ruleId()).isEqualTo("R016");
            assertThat(issues.get(0).suggestion()).contains("IS NOT DISTINCT FROM");
        }

        @Test
        @DisplayName("普通等号不应误报")
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
        @DisplayName("DESC + LIMIT 时触发，建议显式 NULLS LAST")
        void firesOnDescWithLimit() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.created_at DESC LIMIT 20");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).ruleId()).isEqualTo("R017");
            assertThat(issues.get(0).suggestion()).contains("NULLS LAST");
        }

        @Test
        @DisplayName("没有 LIMIT 时不触发")
        void silentWithoutLimit() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.created_at DESC");

            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("ASC 排序不触发")
        void silentOnAsc() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.id FROM demo_asset a ORDER BY a.created_at ASC LIMIT 20");

            assertThat(issues).isEmpty();
        }
    }

    @Nested
    @DisplayName("R020 日期函数")
    class DateFunctions {

        private final DialectRule rule = new DateFunctionRule();

        @Test
        @DisplayName("检出 DATE_ADD，给出 INTERVAL 改写")
        void detectsDateAdd() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT DATE_ADD(a.expired_at, INTERVAL 30 DAY) FROM demo_asset a");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).ruleId()).isEqualTo("R020");
            assertThat(issues.get(0).suggestion()).contains("INTERVAL '1 day'");
        }

        @Test
        @DisplayName("检出 STR_TO_DATE，给出 TO_DATE 改写")
        void detectsStrToDate() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT STR_TO_DATE(a.day, '%Y-%m-%d') FROM demo_asset a");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).suggestion())
                    .isEqualTo("STR_TO_DATE(s, '%Y-%m-%d') -> TO_DATE(s, 'YYYY-MM-DD')");
        }

        @Test
        @DisplayName("可移植的 INTERVAL 写法不应误报")
        void silentOnInterval() {
            List<Issue> issues = RuleTestSupport.check(rule,
                    "SELECT a.expired_at + INTERVAL '30 day' FROM demo_asset a");

            assertThat(issues).isEmpty();
        }
    }
}
