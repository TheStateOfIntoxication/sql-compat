package com.sqlcompat.core.rule;

import com.sqlcompat.core.engine.CompatEngine;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.Severity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import com.sqlcompat.core.util.J8;
import com.sqlcompat.core.util.Io;

class RuleRegistryTest {

    @Test
    @DisplayName("默认注册表包含全部 20 条内置规则，并按编号排序")
    void defaultsContainAllBuiltins() {
        RuleRegistry registry = RuleRegistry.defaults();

        assertThat(registry.size()).isEqualTo(20);
        assertThat(registry.rules()).extracting(DialectRule::id)
                .isSorted()
                .startsWith("R001")
                .endsWith("R020");
    }

    @Test
    @DisplayName("excluding 返回新实例，不影响原注册表")
    void excludingReturnsNewRegistry() {
        RuleRegistry defaults = RuleRegistry.defaults();

        RuleRegistry filtered = defaults.excluding(J8.list("R011", "r010"));

        assertThat(filtered.size()).isEqualTo(18);
        assertThat(filtered.rules()).extracting(DialectRule::id).doesNotContain("R011", "R010");
        assertThat(defaults.size()).as("原注册表不得被修改").isEqualTo(20);
    }

    @Test
    @DisplayName("excluding 传空集合时原样返回")
    void excludingWithNothingIsNoOp() {
        RuleRegistry defaults = RuleRegistry.defaults();

        assertThat(defaults.excluding(J8.list())).isSameAs(defaults);
        assertThat(defaults.excluding(null)).isSameAs(defaults);
    }

    @Test
    @DisplayName("规则编号重复时拒绝注册")
    void rejectsDuplicateIds() {
        RuleRegistry registry = RuleRegistry.empty().register(new StubRule("R900"));
        DialectRule duplicate = new StubRule("R900");

        assertThat(registry.size()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registry.register(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("R900");
    }

    @Test
    @DisplayName("解析失败会作为 PARSE 级 INFO 报出来，且可被忽略")
    void parseFailuresAreVisibleAndIgnorable(@TempDir Path dir) throws IOException {
        Path mapper = dir.resolve("Broken.xml");
        Io.writeString(mapper, String.join("\n",
                "<mapper namespace=\"demo\">",
                "    <select id=\"oracleOnly\">",
                "        SELECT CONNECT_BY_ROOT(o.id) FROM demo_org o",
                "    </select>",
                "</mapper>"));

        Report reported = new CompatEngine().scan(mapper, "MySQL", "Kingbase/PostgreSQL");
        assertThat(reported.parseErrorCount()).isEqualTo(1);
        assertThat(reported.issueCountBySeverity().get(Severity.INFO)).isEqualTo(1);
        assertThat(reported.results().get(0).issues())
                .anySatisfy(i -> assertThat(i.ruleId()).isEqualTo(CompatEngine.PARSE_ISSUE_RULE_ID));

        Report silenced = new CompatEngine(RuleRegistry.defaults(), J8.set("PARSE"))
                .scan(mapper, "MySQL", "Kingbase/PostgreSQL");
        assertThat(silenced.issueCount()).isZero();
        assertThat(silenced.parseErrorCount()).as("统计数字仍保留").isEqualTo(1);
    }

    /** 只用于验证注册表行为，不参与真实扫描。 */
    private static final class StubRule implements DialectRule {

        private final String id;

        StubRule(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public String name() {
            return "桩规则";
        }

        @Override
        public Severity defaultSeverity() {
            return Severity.MINOR;
        }

        @Override
        public String description() {
            return "测试用";
        }

        @Override
        public List<com.sqlcompat.core.model.Issue> check(RuleContext context) {
            return J8.list();
        }
    }
}
