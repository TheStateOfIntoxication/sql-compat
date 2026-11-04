package com.sqlcompat.verify;

import com.sqlcompat.verify.db.DbEndpoint;
import com.sqlcompat.verify.model.ComparisonReport;
import com.sqlcompat.verify.model.SqlCase;
import com.sqlcompat.verify.model.Status;
import com.sqlcompat.verify.report.ComparisonReportRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Paths;

/**
 * 针对两个活库的集成测试。
 *
 * <p>未设置 {@code SQLCOMPAT_MYSQL_URL} / {@code SQLCOMPAT_PG_URL} 时整类跳过，
 * 因此在没有 Docker 的机器上 {@code mvn verify} 依然能通过。CI 用 service 容器注入这两个变量。
 */
@EnabledIfEnvironmentVariable(named = "SQLCOMPAT_MYSQL_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SQLCOMPAT_PG_URL", matches = ".+")
class DualDbComparisonIT {

    private Path resource(String name) {
        try {
            return Paths.get(Objects.requireNonNull(getClass().getClassLoader().getResource(name)).toURI());
        } catch (Exception e) {
            throw new IllegalStateException("缺少测试资源：" + name, e);
        }
    }

    private ComparisonReport run() throws Exception {
        DbEndpoint mysql = new DbEndpoint("mysql",
                System.getenv("SQLCOMPAT_MYSQL_URL"),
                envOr("SQLCOMPAT_MYSQL_USER", "root"),
                envOr("SQLCOMPAT_MYSQL_PASSWORD", ""));
        DbEndpoint postgres = new DbEndpoint("postgres",
                System.getenv("SQLCOMPAT_PG_URL"),
                envOr("SQLCOMPAT_PG_USER", "postgres"),
                envOr("SQLCOMPAT_PG_PASSWORD", ""));

        List<SqlCase> cases = new CaseLoader().load(resource("verify/cases"));
        return new DualDbComparator(mysql, postgres).compare(resource("verify/setup.sql"), cases);
    }

    private String envOr(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    @Test
    @DisplayName("可移植的用例在两个库上结果一致")
    void portableCasesAgree() throws Exception {
        ComparisonReport report = run();

        assertThat(report.caseCount()).isGreaterThanOrEqualTo(17);
        assertThat(report.errorCount()).isZero();
        assertThat(report.consistencyRate()).isGreaterThanOrEqualTo(80.0);
    }

    @Test
    @DisplayName("已知的方言陷阱确实被检出，而不是静默通过")
    void knownTrapsAreDetected() throws Exception {
        ComparisonReport report = run();

        assertThat(report.outcomes())
                .filteredOn(o -> o.status() == Status.DIFFERENT)
                .extracting(o -> o.sqlCase().id())
                .contains("90_known_div_integer", "91_known_concat_operator", "92_known_order_null_desc");
    }

    @Test
    @DisplayName("Markdown 报告带一致率与差异说明")
    void rendersReport() throws Exception {
        ComparisonReport report = run();
        String markdown = new ComparisonReportRenderer().render(report);

        assertThat(markdown).contains("# sql-compat 一致性报告")
                .contains("一致率")
                .contains(report.consistencyRateText());
        assertThat(report.outcomes()).allSatisfy(o -> assertThat(o.detail()).isNotNull());
    }
}
