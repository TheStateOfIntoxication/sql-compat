package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.rule.RuleTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DateFormatAndLimitRuleTest {

    @Test
    @DisplayName("R002 检出 DATE_FORMAT")
    void detectsDateFormat() {
        List<Issue> issues = RuleTestSupport.check(new DateFormatRule(),
                "SELECT DATE_FORMAT(a.created_at, '%Y-%m-%d') FROM demo_asset a");

        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().ruleId()).isEqualTo("R002");
        assertThat(issues.getFirst().suggestion()).contains("TO_CHAR");
    }

    @Test
    @DisplayName("R003 检出 DELETE ... LIMIT")
    void detectsDeleteLimit() {
        List<Issue> issues = RuleTestSupport.check(new DeleteUpdateLimitRule(),
                "DELETE FROM demo_task WHERE created_at < '2024-01-01' LIMIT 1000");

        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().ruleId()).isEqualTo("R003");
    }

    @Test
    @DisplayName("R003 检出 UPDATE ... LIMIT")
    void detectsUpdateLimit() {
        List<Issue> issues = RuleTestSupport.check(new DeleteUpdateLimitRule(),
                "UPDATE demo_task SET state = 'x' WHERE state = 'y' LIMIT 10");

        assertThat(issues).hasSize(1);
    }

    @Test
    @DisplayName("R003 对 SELECT ... LIMIT 不应误报")
    void silentOnSelectLimit() {
        List<Issue> issues = RuleTestSupport.check(new DeleteUpdateLimitRule(),
                "SELECT a.id FROM demo_asset a ORDER BY a.id DESC LIMIT 10");

        assertThat(issues).isEmpty();
    }
}
