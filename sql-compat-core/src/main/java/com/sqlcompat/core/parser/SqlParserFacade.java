package com.sqlcompat.core.parser;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import java.util.Locale;

/**
 * 对 JSQLParser 的薄封装，保证**永不抛异常**。
 *
 * <p>解析不了的语句不算错误：记到结果的 {@code parseError} 上，纯文本规则照常跑。
 */
public final class SqlParserFacade {

    /** 解析结果：成功给 AST，失败给原因，二者必有一个为 null。 */
    public static final class ParseOutcome {

        private final Statement ast;
        private final String error;

        public ParseOutcome(Statement ast, String error) {
            this.ast = ast;
            this.error = error;
        }

        /** 解析出的语句，失败为 {@code null}。 */
        public Statement ast() {
            return ast;
        }

        /** 失败原因，成功为 {@code null}。 */
        public String error() {
            return error;
        }

        public boolean ok() {
            return ast != null;
        }
    }

    public ParseOutcome parse(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return new ParseOutcome(null, "空语句");
        }
        try {
            return new ParseOutcome(CCJSqlParserUtil.parse(sanitize(sql)), null);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return new ParseOutcome(null, msg.split("\r?\n", 2)[0]);
        }
    }

    /**
     * 把 MyBatis 占位符替换成解析器能接受的字面量，保持 SQL 结构不变；
     * 顺便剥掉 PL/SQL 的 {@code BEGIN ... END} 包裹。
     *
     * <p>剥 BEGIN/END 是安全的：AST 只用于分析、不用于回写。而 Oracle 风格的匿名块
     * JSQLParser 根本不认 —— 与其整句丢掉，不如把它里面的 DML 解析出来。
     *
     * <p><b>相邻的多个拼接占位符要合成一个</b>：{@code ORDER BY ${col} ${sort}} 这类写法
     * 把两个 {@code ${}} 都换成标识符会得到 {@code ORDER BY t t}，直接不是合法 SQL。
     * 这是从真实项目审计出来的：一个项目里 3 条“分页 + 动态排序”语句因此全部解析失败。
     */
    private String sanitize(String sql) {
        String text = sql
                // 动态排序：ORDER BY 后面接多少个拼接占位符都直接换成常量列。
                // 形态一：ORDER BY ${col} ${sort}；形态二：ORDER BY "${col}" ${sort}
                // 不换的话会得到 ORDER BY t t —— 两个标识符相邻，不是合法 SQL。
                .replaceAll("(?i)(order\\s+by)\\s+(?:(?:\"\\s*\\$\\{[^}]*}\\s*\"|\\$\\{[^}]*})\\s*)+", "$1 1")
                // ORDER BY <表达式> ${sort} 里的 ${sort} 是排序方向，换成标识符会变成
                // ORDER BY INET_ATON(ip) t —— 函数调用后面跟标识符，也不是合法 SQL。
                .replaceAll("(?i)(order\\s+by\\s+[;\\n]*?\\S+?)\\s+\\$\\{[^}]*}\\s*(?=lim\\w*\\b|\\)|$|;)", "$1 ASC")
                // 包在双引号里的拼接占位符（其它位置），避免变成 "t"
                .replaceAll("\"[^\"]*\\$\\{[^}]*}[^\"]*\"", "t")
                // 相邻（中间只有空白）的多个 ${}，整体折叠成一个标识符
                  // MySQL 的 NULL 安全等于 <=>：JSQLParser 4.x 的词法表里没有这个运算符，
                  // 会在解析阶段直接报错。AST 只用于统计解析成功率、不参与改写，
                  // 所以先换成普通等号再解析，避免 R016 这类语句每条都多一条无意义的 PARSE 记录。
                  .replace("<" + "=>", "=")
                .replaceAll("(?:\\$\\{[^}]*\\}\\s*){2,}", "t ")
                .replaceAll("#\\{[^}]*}", "?")
                .replaceAll("\\$\\{[^}]*}", "t");
        String stripped = text.trim();
        if (stripped.regionMatches(true, 0, "begin", 0, 5)
                && (stripped.length() == 5 || !Character.isLetterOrDigit(stripped.charAt(5)))) {
            stripped = stripped.substring(5).trim();
            if (stripped.toLowerCase(java.util.Locale.ROOT).endsWith(" end")) {
                stripped = stripped.substring(0, stripped.length() - 4).trim();
            } else if (stripped.toLowerCase(java.util.Locale.ROOT).endsWith("\nend")) {
                stripped = stripped.substring(0, stripped.length() - 4).trim();
            }
        }
        return stripped;
    }
}
