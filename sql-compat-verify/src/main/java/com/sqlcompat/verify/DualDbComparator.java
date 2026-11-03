package com.sqlcompat.verify;

import com.sqlcompat.verify.db.DbEndpoint;
import com.sqlcompat.verify.db.DbProbe;
import com.sqlcompat.verify.model.CaseOutcome;
import com.sqlcompat.verify.model.ComparisonReport;
import com.sqlcompat.verify.model.QueryOutcome;
import com.sqlcompat.verify.model.SqlCase;
import com.sqlcompat.verify.model.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Runs a case set against MySQL and PostgreSQL and reports how much of it agrees.
 *
 * <p>Setup scripts are applied to both databases before the cases run, so a fresh CI container
 * and a developer's local schema behave the same way.
 */
public final class DualDbComparator {

    private final DbEndpoint mysql;
    private final DbEndpoint postgres;
    private final ResultSetDiffer differ = new ResultSetDiffer();

    public DualDbComparator(DbEndpoint mysql, DbEndpoint postgres) {
        this.mysql = mysql;
        this.postgres = postgres;
    }

    /**
     * @param setupSql  optional DDL + seed statements applied to both sides first
     * @param cases     cases to run
     */
    public ComparisonReport compare(Path setupSql, List<SqlCase> cases) throws IOException, SQLException {
        try (DbProbe mysqlProbe = new DbProbe(mysql);
             DbProbe postgresProbe = new DbProbe(postgres)) {

            if (setupSql != null) {
                applySetup(mysqlProbe, postgresProbe, setupSql);
            }

            List<CaseOutcome> outcomes = new ArrayList<>();
            for (SqlCase sqlCase : cases) {
                outcomes.add(runCase(mysqlProbe, postgresProbe, sqlCase));
            }
            return new ComparisonReport(mysql.label(), postgres.label(), outcomes);
        }
    }

    private void applySetup(DbProbe mysqlProbe, DbProbe postgresProbe, Path setupSql)
            throws IOException, SQLException {
        String script = Files.readString(setupSql, StandardCharsets.UTF_8);
        for (String statement : script.split(";")) {
            String sql = stripComments(statement).strip();
            if (sql.isEmpty()) {
                continue;
            }
            QueryOutcome left = mysqlProbe.runOnce(sql);
            if (left.failed()) {
                throw new SQLException("Setup failed on " + mysql.label() + ": " + left.error()
                        + "\n  while running: " + sql);
            }
            QueryOutcome right = postgresProbe.runOnce(sql);
            if (right.failed()) {
                throw new SQLException("Setup failed on " + postgres.label() + ": " + right.error()
                        + "\n  while running: " + sql);
            }
        }
    }

    private CaseOutcome runCase(DbProbe mysqlProbe, DbProbe postgresProbe, SqlCase sqlCase) {
        QueryOutcome left = mysqlProbe.run(sqlCase.sql());
        QueryOutcome right = postgresProbe.run(sqlCase.sql());

        if (left.failed() || right.failed()) {
            StringBuilder detail = new StringBuilder();
            if (left.failed()) {
                detail.append("mysql: ").append(left.error());
            }
            if (right.failed()) {
                if (!detail.isEmpty()) {
                    detail.append(" | ");
                }
                detail.append("postgres: ").append(right.error());
            }
            return CaseOutcome.error(sqlCase, detail.toString(), left, right);
        }

        String diff = differ.diff(sqlCase, left.rows(), right.rows());
        Status status = diff == null ? Status.CONSISTENT : Status.DIFFERENT;
        return new CaseOutcome(sqlCase, status, diff, left, right);
    }

    private String stripComments(String chunk) {
        StringBuilder sb = new StringBuilder(chunk.length());
        for (String line : chunk.split("\\R")) {
            if (line.stripLeading().startsWith("--")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
