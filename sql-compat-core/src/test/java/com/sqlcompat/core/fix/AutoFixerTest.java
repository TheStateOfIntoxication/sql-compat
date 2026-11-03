package com.sqlcompat.core.fix;

import com.sqlcompat.core.engine.CompatEngine;
import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.rule.RuleRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutoFixerTest {

    private static final String MAPPER = """
            <?xml version="1.0" encoding="UTF-8"?>
            <mapper namespace="demo.mapper.AssetMapper">
                <select id="byId" resultType="map">
                    SELECT `asset_name`, IFNULL(a.owner_name, 'x') AS owner_name
                    FROM demo_asset a WHERE a.id = #{id}
                </select>
                <select id="paged" resultType="map">
                    SELECT a.id FROM demo_asset a ORDER BY a.id LIMIT 10, 20
                </select>
                <select id="nse" resultType="map">
                    SELECT a.id FROM demo_asset a WHERE a.owner_id &lt;=&gt; #{ownerId}
                </select>
            </mapper>
            """;

    private Path writeMapper(Path dir) throws IOException {
        Path mapper = dir.resolve("AssetMapper.xml");
        Files.writeString(mapper, MAPPER);
        return mapper;
    }

    private long issueCount(Path path) throws IOException {
        Report report = new CompatEngine(RuleRegistry.defaults()).scan(path, "MySQL", "Kingbase/PostgreSQL");
        return report.issueCount();
    }

    @Test
    @DisplayName("SAFE 模式只改等价改写，反引号与 IFNULL 各归各位")
    void safeModeRewritesOnlySafeRules(@TempDir Path dir) throws IOException {
        Path mapper = writeMapper(dir);
        Path out = dir.resolve("fixed");

        List<AutoFixer.FixResult> results = new AutoFixer(RuleRegistry.defaults(), FixMode.SAFE_ONLY)
                .fixTree(mapper, out);

        assertThat(results).hasSize(1);
        String fixed = Files.readString(out.resolve("AssetMapper.xml"));
        assertThat(fixed).contains("COALESCE(a.owner_name, 'x')");
        assertThat(fixed).contains("LIMIT 20 OFFSET 10");
        assertThat(fixed).contains(" IS NOT DISTINCT FROM ");
        // XML 里的 &lt;=&gt; 也必须被改掉，否则真实 Mapper 上是瞎的。
        assertThat(fixed).doesNotContain("&lt;=&gt;");
        // 反引号属于 REVIEW 级，SAFE 模式必须保留原样。
        assertThat(fixed).contains("`asset_name`");
        assertThat(results.getFirst().appliedRuleIds()).containsExactly("R001", "R004", "R016");
    }

    @Test
    @DisplayName("INCLUDE_REVIEW 模式连反引号一起改")
    void includeReviewAlsoRewritesQuotes(@TempDir Path dir) throws IOException {
        Path mapper = writeMapper(dir);
        Path out = dir.resolve("fixed-all");

        new AutoFixer(RuleRegistry.defaults(), FixMode.INCLUDE_REVIEW).fixTree(mapper, out);

        String fixed = Files.readString(out.resolve("AssetMapper.xml"));
        assertThat(fixed).contains("\"asset_name\"");
        assertThat(fixed).doesNotContain("`asset_name`");
    }

    @Test
    @DisplayName("改写后重新扫描，问题数必须下降 —— 这是改写唯一有意义的验收标准")
    void rescanShowsFewerIssues(@TempDir Path dir) throws IOException {
        Path mapper = writeMapper(dir);
        Path out = dir.resolve("fixed");
        long before = issueCount(mapper);

        new AutoFixer(RuleRegistry.defaults(), FixMode.SAFE_ONLY).fixTree(mapper, out);
        long after = issueCount(out);

        assertThat(before).isGreaterThan(0);
        assertThat(after).isLessThan(before);
    }

    @Test
    @DisplayName("不改源文件：改写结果写到输出目录")
    void neverTouchesSource(@TempDir Path dir) throws IOException {
        Path mapper = writeMapper(dir);
        String original = Files.readString(mapper);

        new AutoFixer(RuleRegistry.defaults(), FixMode.INCLUDE_REVIEW).fixTree(mapper, dir.resolve("out"));

        assertThat(Files.readString(mapper)).isEqualTo(original);
    }

    @Test
    @DisplayName("对已改写的目录再跑一次应为零变更，验证收敛")
    void secondRunIsNoOp(@TempDir Path dir) throws IOException {
        Path mapper = writeMapper(dir);
        Path first = dir.resolve("pass1");
        Path second = dir.resolve("pass2");

        new AutoFixer(RuleRegistry.defaults(), FixMode.INCLUDE_REVIEW).fixTree(mapper, first);
        List<AutoFixer.FixResult> results = new AutoFixer(RuleRegistry.defaults(), FixMode.INCLUDE_REVIEW)
                .fixTree(first, second);

        assertThat(results).allSatisfy(r -> assertThat(r.changed())
                .as("第二次改写不应再有变化，否则规则之间在互相打架")
                .isFalse());
        assertThat(Files.readString(first.resolve("AssetMapper.xml")))
                .isEqualTo(Files.readString(second.resolve("AssetMapper.xml")));
    }

    @Test
    @DisplayName("OFF 模式不做任何改写")
    void offModeDoesNothing(@TempDir Path dir) throws IOException {
        Path mapper = writeMapper(dir);
        Path out = dir.resolve("fixed-off");

        new AutoFixer(RuleRegistry.defaults(), FixMode.OFF).fixTree(mapper, out);

        assertThat(Files.readString(out.resolve("AssetMapper.xml"))).isEqualTo(MAPPER);
    }
}
