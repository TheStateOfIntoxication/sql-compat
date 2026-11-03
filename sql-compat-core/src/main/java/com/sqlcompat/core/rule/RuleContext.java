package com.sqlcompat.core.rule;

import com.sqlcompat.core.model.SourceLocation;
import com.sqlcompat.core.model.SqlStatement;

import java.util.Locale;

/**
 * Everything a rule needs about one statement.
 *
 * <p>The raw SQL is always available; the AST is available only when the parser
 * understood the statement ({@link #hasAst()}).
 */
public final class RuleContext {

    private final SqlStatement statement;
    private final Object ast;
    private final String lowerCaseSql;

    public RuleContext(SqlStatement statement, Object ast) {
        this.statement = statement;
        this.ast = ast;
        this.lowerCaseSql = statement.sqlText().toLowerCase(Locale.ROOT);
    }

    public SqlStatement statement() {
        return statement;
    }

    public String sql() {
        return statement.sqlText();
    }

    /** Lower-cased SQL, for case-insensitive matching without allocating in every rule. */
    public String lowerCaseSql() {
        return lowerCaseSql;
    }

    public SourceLocation location() {
        return statement.location();
    }

    /** AST node, typed as {@code Object} so rules do not need jsqlparser on their classpath. */
    public Object ast() {
        return ast;
    }

    public boolean hasAst() {
        return ast != null;
    }

    public boolean containsIgnoreCase(String fragment) {
        return lowerCaseSql.contains(fragment.toLowerCase(Locale.ROOT));
    }
}
