package com.sqlcompat.core.parser;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

/**
 * Thin wrapper around JSQLParser that never throws.
 *
 * <p>A statement the parser cannot understand is not an error: it is recorded as
 * {@code parseError} on the result and the textual rules still run.
 */
public final class SqlParserFacade {

    /**
     * @param ast   parsed statement, or {@code null}
     * @param error error message, or {@code null} on success
     */
    public record ParseOutcome(Statement ast, String error) {
        public boolean ok() {
            return ast != null;
        }
    }

    public ParseOutcome parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ParseOutcome(null, "empty statement");
        }
        try {
            return new ParseOutcome(CCJSqlParserUtil.parse(sanitize(sql)), null);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ParseOutcome(null, msg.lines().findFirst().orElse(msg));
        }
    }

    /**
     * Replaces MyBatis {@code #{...}} / {@code ${...}} placeholders with literals the
     * parser accepts, keeping the SQL shape intact.
     */
    private String sanitize(String sql) {
        return sql.replaceAll("#\\{[^}]*}", "?")
                .replaceAll("\\$\\{[^}]*}", "t");
    }
}
