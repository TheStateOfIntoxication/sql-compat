package com.sqlcompat.core.rule;

import com.sqlcompat.core.model.SourceLocation;
import com.sqlcompat.core.model.SqlStatement;

import java.util.Locale;

/**
 * 规则看到的单条语句上下文。
 *
 * <p>原始 SQL 始终可用；只有解析器认得出这条语句时才能拿到 AST（见 {@link #hasAst()}）。
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

    /** 已转小写的 SQL，避免每条规则各自转换分配。 */
    public String lowerCaseSql() {
        return lowerCaseSql;
    }

    public SourceLocation location() {
        return statement.location();
    }

    /** AST 节点，类型声明为 {@code Object}，这样规则类不必依赖 jsqlparser。 */
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
