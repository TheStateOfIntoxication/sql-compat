package com.sqlcompat.core.engine;

import com.sqlcompat.core.model.Report;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.model.SqlStatement;
import com.sqlcompat.core.report.HtmlReportRenderer;
import com.sqlcompat.core.report.MarkdownReportRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Paths;

class CompatEngineTest {

    private Path fixtures() throws URISyntaxException {
        return Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource("fixtures")).toURI());
    }

    @Test
    @DisplayName("能解析 Mapper XML 并取出语句")
    void collectsStatementsFromMapper() throws IOException, URISyntaxException {
        List<SqlStatement> statements = new CompatEngine().collectStatements(fixtures());

        assertThat(statements).isNotEmpty();
        assertThat(statements)
                .anySatisfy(s -> assertThat(s.qualifiedId()).isEqualTo("demo.mapper.AssetMapper.selectAssetById"))
                .allSatisfy(s -> assertThat(s.sqlText()).isNotBlank());
    }

    @Test
    @DisplayName("在 fixture 里能扫出 BLOCKER 级问题")
    void findsIssues() throws IOException, URISyntaxException {
        Report report = new CompatEngine().scan(fixtures(), "MySQL", "Kingbase/PostgreSQL");

        assertThat(report.statementCount()).isGreaterThan(5);
        assertThat(report.issueCount()).isGreaterThan(0);
        assertThat(report.issueCountBySeverity().get(Severity.BLOCKER)).isGreaterThan(0);
        assertThat(report.worstSeverity()).isEqualTo(Severity.BLOCKER);
    }

    @Test
    @DisplayName("可移植的语句保持干净")
    void portableStatementIsClean() throws IOException, URISyntaxException {
        Report report = new CompatEngine().scan(fixtures(), "MySQL", "Kingbase/PostgreSQL");

        assertThat(report.results())
                .filteredOn(r -> r.statement().id().equals("selectPortableAssets"))
                .singleElement()
                .satisfies(r -> assertThat(r.issues()).isEmpty());
    }

    @Test
    @DisplayName("两种渲染器都能产出内容")
    void rendersReports() throws IOException, URISyntaxException {
        Report report = new CompatEngine().scan(fixtures(), "MySQL", "Kingbase/PostgreSQL");

        String markdown = new MarkdownReportRenderer().render(report);
        String html = new HtmlReportRenderer().render(report);

        assertThat(markdown).contains("# sql-compat 兼容性报告").contains("R001");
        assertThat(html).contains("<!DOCTYPE html>").contains("selectAssetById");
    }
}
