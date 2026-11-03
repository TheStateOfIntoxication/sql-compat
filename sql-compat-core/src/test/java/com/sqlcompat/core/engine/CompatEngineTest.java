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

class CompatEngineTest {

    private Path fixtures() throws URISyntaxException {
        return Path.of(Objects.requireNonNull(
                getClass().getClassLoader().getResource("fixtures")).toURI());
    }

    @Test
    @DisplayName("parses a mapper XML and finds the statements")
    void collectsStatementsFromMapper() throws IOException, URISyntaxException {
        List<SqlStatement> statements = new CompatEngine().collectStatements(fixtures());

        assertThat(statements).isNotEmpty();
        assertThat(statements)
                .anySatisfy(s -> assertThat(s.qualifiedId()).isEqualTo("demo.mapper.AssetMapper.selectAssetById"))
                .allSatisfy(s -> assertThat(s.sqlText()).isNotBlank());
    }

    @Test
    @DisplayName("finds blocker issues in the fixture mappers")
    void findsIssues() throws IOException, URISyntaxException {
        Report report = new CompatEngine().scan(fixtures(), "MySQL", "Kingbase/PostgreSQL");

        assertThat(report.statementCount()).isGreaterThan(5);
        assertThat(report.issueCount()).isGreaterThan(0);
        assertThat(report.issueCountBySeverity().get(Severity.BLOCKER)).isGreaterThan(0);
        assertThat(report.worstSeverity()).isEqualTo(Severity.BLOCKER);
    }

    @Test
    @DisplayName("portable statement stays clean")
    void portableStatementIsClean() throws IOException, URISyntaxException {
        Report report = new CompatEngine().scan(fixtures(), "MySQL", "Kingbase/PostgreSQL");

        assertThat(report.results())
                .filteredOn(r -> r.statement().id().equals("selectPortableAssets"))
                .singleElement()
                .satisfies(r -> assertThat(r.issues()).isEmpty());
    }

    @Test
    @DisplayName("both renderers produce non-empty output")
    void rendersReports() throws IOException, URISyntaxException {
        Report report = new CompatEngine().scan(fixtures(), "MySQL", "Kingbase/PostgreSQL");

        String markdown = new MarkdownReportRenderer().render(report);
        String html = new HtmlReportRenderer().render(report);

        assertThat(markdown).contains("# sql-compat report").contains("R001");
        assertThat(html).contains("<!DOCTYPE html>").contains("selectAssetById");
    }
}
