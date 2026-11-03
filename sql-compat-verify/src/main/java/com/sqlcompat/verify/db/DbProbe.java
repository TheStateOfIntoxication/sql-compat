package com.sqlcompat.verify.db;

import com.sqlcompat.verify.model.QueryOutcome;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Runs cases against one endpoint and measures how long they take.
 *
 * <p>One connection is opened per endpoint and reused: opening a connection per case would put
 * TCP handshake and authentication inside the measured window, and the two drivers do not cost
 * the same there.
 *
 * <p>Timing is a median over a few iterations after a warm-up round. This is an indicative
 * cross-engine comparison, <b>not</b> a benchmark — different engines, different optimizers,
 * different default storage.
 */
public final class DbProbe implements AutoCloseable {

    private static final int WARMUP_ITERATIONS = 2;
    private static final int MEASURED_ITERATIONS = 5;
    private static final int QUERY_TIMEOUT_SECONDS = 30;

    private final DbEndpoint endpoint;
    private final Connection connection;

    public DbProbe(DbEndpoint endpoint) throws SQLException {
        this.endpoint = endpoint;
        this.connection = DriverManager.getConnection(
                endpoint.url(), endpoint.user(), endpoint.password());
        this.connection.setAutoCommit(true);
    }

    public DbEndpoint endpoint() {
        return endpoint;
    }

    /** Executes a statement once, without warm-up or measurement. Used for setup scripts. */
    public QueryOutcome runOnce(String sql) {
        long started = System.nanoTime();
        try {
            List<List<String>> rows = execute(sql);
            long elapsed = (System.nanoTime() - started) / 1_000_000L;
            return new QueryOutcome(rows, elapsed, null);
        } catch (SQLException e) {
            return QueryOutcome.failure(e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
        }
    }

    /** Executes a statement and returns normalized rows plus the median elapsed time. */
    public QueryOutcome run(String sql) {
        List<List<String>> rows = null;
        List<Long> samples = new ArrayList<>(MEASURED_ITERATIONS);
        try {
            for (int i = 0; i < WARMUP_ITERATIONS + MEASURED_ITERATIONS; i++) {
                long started = System.nanoTime();
                rows = execute(sql);
                long elapsed = (System.nanoTime() - started) / 1_000_000L;
                if (i >= WARMUP_ITERATIONS) {
                    samples.add(elapsed);
                }
            }
            return new QueryOutcome(rows, median(samples), null);
        } catch (SQLException e) {
            return QueryOutcome.failure(e.getClass().getSimpleName() + ": " + firstLine(e.getMessage()));
        }
    }

    private List<List<String>> execute(String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            boolean isResultSet = statement.execute(sql);
            if (isResultSet) {
                try (ResultSet rs = statement.getResultSet()) {
                    return readRows(rs);
                }
            }
            // DML: compare the affected row count instead of a result set.
            return List.of(List.of(String.valueOf(statement.getUpdateCount())));
        }
    }

    private List<List<String>> readRows(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        List<List<String>> rows = new ArrayList<>();
        while (rs.next()) {
            List<String> row = new ArrayList<>(columnCount);
            for (int c = 1; c <= columnCount; c++) {
                row.add(ValueNormalizer.normalize(rs.getObject(c)));
            }
            rows.add(List.copyOf(row));
        }
        return List.copyOf(rows);
    }

    private long median(List<Long> samples) {
        if (samples.isEmpty()) {
            return 0L;
        }
        long[] sorted = samples.stream().mapToLong(Long::longValue).sorted().toArray();
        return sorted[sorted.length / 2];
    }

    private String firstLine(String message) {
        if (message == null) {
            return "";
        }
        return Arrays.stream(message.split("\\R")).findFirst().orElse("").trim();
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
