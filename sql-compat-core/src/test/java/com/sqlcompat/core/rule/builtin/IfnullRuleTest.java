package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.rule.RuleTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IfnullRuleTest {

    private final IfnullRule rule = new IfnullRule();

    @Test
    @DisplayName("detects IFNULL and reports a BLOCKER")
    void detectsIfnull() {
        List<Issue> issues = RuleTestSupport.check(rule,
                "SELECT IFNULL(a.name, 'x') FROM demo_asset a WHERE a.id = 1");

        assertThat(issues).hasSize(1);
        assertThat(issues.getFirst().ruleId()).isEqualTo("R001");
        assertThat(issues.getFirst().severity().name()).isEqualTo("BLOCKER");
        assertThat(issues.getFirst().suggestion()).contains("COALESCE");
    }

    @Test
    @DisplayName("does not report COALESCE")
    void cleanOnCoalesce() {
        List<Issue> issues = RuleTestSupport.check(rule,
                "SELECT COALESCE(a.name, 'x') FROM demo_asset a");

        assertThat(issues).isEmpty();
    }

    @Test
    @DisplayName("is case insensitive")
    void caseInsensitive() {
        List<Issue> issues = RuleTestSupport.check(rule, "select ifnull(a, b) from t");

        assertThat(issues).hasSize(1);
    }

    @Test
    @DisplayName("reports every occurrence")
    void reportsAllOccurrences() {
        List<Issue> issues = RuleTestSupport.check(rule,
                "SELECT IFNULL(a, 0), IFNULL(b, 0) FROM t");

        assertThat(issues).hasSize(2);
    }
}
