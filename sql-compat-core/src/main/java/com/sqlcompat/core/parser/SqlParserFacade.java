package com.sqlcompat.core.parser;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;

/**
 * 对 JSQLParser 的薄封装，保证**永不抛异常**。
 *
 * <p>解析不了的语句不算错误：记到结果的 {@code parseError} 上，纯文本规则照常跑。
 */
public final class SqlParserFacade {

    /**
     * @param ast   解析出的语句，失败为 {@code null}
     * @param error 失败原因，成功为 {@code null}
     */
    public record ParseOutcome(Statement ast, String error) {
        public boolean ok() {
            return ast != null;
        }
    }

    public ParseOutcome parse(String sql) {
        if (sql == null || sql.isBlank()) {
            return new ParseOutcome(null, "空语句");
        }
        try {
            return new ParseOutcome(CCJSqlParserUtil.parse(sanitize(sql)), null);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ParseOutcome(null, msg.lines().findFirst().orElse(msg));
        }
    }

    /**
     * 把 MyBatis 的 {@code #{...}} / {@code ${...}} 占位符替换成解析器能接受的字面量，
     * 保持 SQL 结构不变。
     */
    private String sanitize(String sql) {
        return sql.replaceAll("#\\{[^}]*}", "?")
                .replaceAll("\\$\\{[^}]*}", "t");
    }
}
