package com.sqlcompat.verify;

import com.sqlcompat.verify.db.DbEndpoint;
import com.sqlcompat.verify.model.CaseOutcome;
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

/**
 * Integration test against two live databases.
 *
 * <p>Skipped unless {@code SQLCOMPAT_MYSQL_URL} and {@code SQLCOMPAT_PG_URL} are set, so
 * {@code mvn verify} stays green on a machine without Docker. CI sets both from service containers.
 */
@EnabledIfEnvironmentVariable(named = "SQLCOMPAT_MYSQL_URL", matches = ".+")
@EnabledIfEnvironmentVariable(named = "SQLCOMPAT_PG_URL", matches = ".+")
class DualDbComparisonIT {

    private Path resource(String name) {
        try {
            return Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource(name)).toURI());
        } catch (Exception e) {
            throw new IllegalStateException("Missing test resource: " + name, e);
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
        return value == null || value.isBlank() ? fallback : value;
    }

    @Test
    @DisplayName("portable cases agree across both databases")
    void portableCasesAgree() throws Exception {
        ComparisonReport report = run();

        assertThat(report.caseCount()).isGreaterThanOrEqualTo(17);
        assertThat(report.errorCount()).isZero();
        assertThat(report.consistencyRate()).isGreaterThanOrEqualTo(80.0);
    }

    @Test
    @DisplayName("the known dialect traps are actually detected, not silently passed")
    void knownTrapsAreDetected() throws Exception {
        ComparisonReport report = run();

        assertThat(report.outcomes())
                .filteredOn(o -> o.status() == Status.DIFFERENT)
                .extracting(o -> o.sqlCase().id())
                .contains("90_known_div_integer", "91_known_concat_operator", "92_known_order_null_desc");
    }

    @Test
    @DisplayName("the markdown report carries the rate and the difference detail")
    void rendersReport() throws Exception {
        ComparisonReport report = run();
        String markdown = new ComparisonReportRenderer().render(report);

        assertThat(markdown).contains("# sql-compat consistency report")
                .contains("consistency rate")
                .contains(report.consistencyRateText());
        assertThat(report.outcomes()).allSatisfy(o -> assertThat(o.detail()).isNotNull());
    }
}
