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
     * 把 MyBatis 占位符替换成解析器能接受的字面量，保持 SQL 结构不变；
     * 顺便剥掉 PL/SQL 的 {@code BEGIN ... END} 包裹。
     *
     * <p>剥 BEGIN/END 是安全的：AST 只用于分析、不用于回写。而 Oracle 风格的匿名块
     * JSQLParser 根本不认 —— 与其整句丢掉，不如把它里面的 DML 解析出来。
     */
    private String sanitize(String sql) {
        String text = sql.replaceAll("#\\{[^}]*}", "?")
                .replaceAll("\\$\\{[^}]*}", "t");
        String stripped = text.strip();
        if (stripped.regionMatches(true, 0, "begin", 0, 5)
                && (stripped.length() == 5 || !Character.isLetterOrDigit(stripped.charAt(5)))) {
            stripped = stripped.substring(5).strip();
            if (stripped.toLowerCase(java.util.Locale.ROOT).endsWith(" end")) {
                stripped = stripped.substring(0, stripped.length() - 4).strip();
            } else if (stripped.toLowerCase(java.util.Locale.ROOT).endsWith("\nend")) {
                stripped = stripped.substring(0, stripped.length() - 4).strip();
            }
        }
        return stripped;
    }
}
