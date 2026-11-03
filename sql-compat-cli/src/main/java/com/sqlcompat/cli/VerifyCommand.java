package com.sqlcompat.cli;

import com.sqlcompat.verify.CaseLoader;
import com.sqlcompat.verify.DualDbComparator;
import com.sqlcompat.verify.db.DbEndpoint;
import com.sqlcompat.verify.model.ComparisonReport;
import com.sqlcompat.verify.model.SqlCase;
import com.sqlcompat.verify.report.ComparisonReportRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * {@code sql-compat verify --mysql-url ... --pg-url ... -c ./cases}
 *
 * <p>The second layer of the tool: static rules find syntax problems, this finds the cases that
 * compile on both sides but return different answers.
 *
 * <p>Credentials should come from environment variables in CI, not from the command line — process
 * arguments end up in shell history and in build logs.
 */
@Command(
        name = "verify",
        mixinStandardHelpOptions = true,
        description = "Run the same SQL cases against MySQL and PostgreSQL and report consistency.")
public final class VerifyCommand implements Callable<Integer> {

    @Option(names = {"--mysql-url"}, description = "JDBC URL for MySQL. Env: SQLCOMPAT_MYSQL_URL")
    String mysqlUrl;

    @Option(names = {"--mysql-user"}, defaultValue = "root", description = "MySQL user")
    String mysqlUser;

    @Option(names = {"--mysql-password"}, description = "MySQL password. Env: SQLCOMPAT_MYSQL_PASSWORD")
    String mysqlPassword;

    @Option(names = {"--pg-url"}, description = "JDBC URL for PostgreSQL. Env: SQLCOMPAT_PG_URL")
    String pgUrl;

    @Option(names = {"--pg-user"}, defaultValue = "postgres", description = "PostgreSQL user")
    String pgUser;

    @Option(names = {"--pg-password"}, description = "PostgreSQL password. Env: SQLCOMPAT_PG_PASSWORD")
    String pgPassword;

    @Option(names = {"-c", "--cases"}, required = true, description = "Directory of *.sql case files")
    Path casesDir;

    @Option(names = {"-s", "--setup"}, description = "Optional setup script applied to both databases")
    Path setupScript;

    @Option(names = {"-o", "--output"}, description = "Report file. Default: sql-compat-verify.md")
    Path output;

    @Option(names = {"--fail-under"}, description = "Exit with code 1 when the consistency rate "
            + "is below this percentage, e.g. 95")
    Double failUnder;

    @Override
    public Integer call() throws Exception {
        String myUrl = firstNonBlank(mysqlUrl, System.getenv("SQLCOMPAT_MYSQL_URL"));
        String pgUrlResolved = firstNonBlank(pgUrl, System.getenv("SQLCOMPAT_PG_URL"));
        if (myUrl == null || pgUrlResolved == null) {
            System.err.println("Both --mysql-url and --pg-url (or their env vars) are required.");
            return 2;
        }
        String myPassword = firstNonBlank(mysqlPassword, System.getenv("SQLCOMPAT_MYSQL_PASSWORD"));
        String pgPasswordResolved = firstNonBlank(pgPassword, System.getenv("SQLCOMPAT_PG_PASSWORD"));

        List<SqlCase> cases = new CaseLoader().load(casesDir);
        if (cases.isEmpty()) {
            System.err.println("No .sql cases found in " + casesDir);
            return 2;
        }

        DbEndpoint mysql = new DbEndpoint("mysql", myUrl, mysqlUser, nullToEmpty(myPassword));
        DbEndpoint postgres = new DbEndpoint("postgres", pgUrlResolved, pgUser, nullToEmpty(pgPasswordResolved));

        ComparisonReport report = new DualDbComparator(mysql, postgres).compare(setupScript, cases);

        ComparisonReportRenderer renderer = new ComparisonReportRenderer();
        Path target = output != null ? output : Path.of("sql-compat-verify.md");
        Files.writeString(target, renderer.render(report), StandardCharsets.UTF_8);

        System.out.println(renderer.summaryLine(report));
        System.out.println("report written to " + target.toAbsolutePath());

        if (failUnder != null && report.consistencyRate() < failUnder) {
            System.err.printf("FAILED: consistency %.1f%% is below the %.1f%% threshold%n",
                    report.consistencyRate(), failUnder);
            return 1;
        }
        return 0;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
