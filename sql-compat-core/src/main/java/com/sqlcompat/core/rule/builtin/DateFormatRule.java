package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.fix.FixSafety;
import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sqlcompat.core.util.J8;
import java.util.Locale;

/**
 * R002 —— MySQL 的 {@code DATE_FORMAT(col, '%Y-%m-%d')}，需要改写为 {@code TO_CHAR}。
 *
 * <p>注意函数名要换，格式串也要换（{@code %Y} → {@code YYYY}、{@code %i} → {@code MI}），
 * 这是迁移里最容易漏的一步。
 *
 * <p>自动改写只覆盖常见格式符。遇到 {@code %W} / {@code %M} 这类依赖 locale 的写法时**原样保留**，
 * 交给人工 —— 猜错了就是上线后日期显示成英文。
 */
public final class DateFormatRule implements DialectRule {

    private static final Pattern PATTERN = Pattern.compile("\\bdate_format\\s*\\(", Pattern.CASE_INSENSITIVE);
    private static final Pattern CALL = Pattern.compile("\\bdate_format\\s*\\(", Pattern.CASE_INSENSITIVE);

    /** 常见的 MySQL 格式符 → PostgreSQL 格式符。顺序无关，替换时逐字符扫描。 */
    private static final List<String[]> SPECIFIERS = J8.list(
            new String[]{"%Y", "YYYY"},
            new String[]{"%y", "YY"},
            new String[]{"%m", "MM"},
            new String[]{"%c", "FMMM"},
            new String[]{"%d", "DD"},
            new String[]{"%e", "FMDD"},
            new String[]{"%H", "HH24"},
            new String[]{"%k", "FMHH24"},
            new String[]{"%h", "HH12"},
            new String[]{"%I", "HH12"},
            new String[]{"%l", "FMHH12"},
            new String[]{"%i", "MI"},
            new String[]{"%s", "SS"},
            new String[]{"%S", "SS"},
            new String[]{"%f", "US"},
            new String[]{"%p", "AM"},
            new String[]{"%T", "HH24:MI:SS"},
            new String[]{"%r", "HH12:MI:SS AM"},
            new String[]{"%%", "%"}
    );

    @Override
    public String id() {
        return "R002";
    }

    @Override
    public String name() {
        return "DATE_FORMAT 需改写为 TO_CHAR";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.BLOCKER;
    }

    @Override
    public String description() {
        return "MySQL 的 DATE_FORMAT(col, fmt) 在 Kingbase/PostgreSQL 上不存在。";
    }

    @Override
    public String suggestion() {
        return "DATE_FORMAT(col, '%Y-%m-%d') -> TO_CHAR(col, 'YYYY-MM-DD')";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        String sql = context.sql();
        Matcher matcher = PATTERN.matcher(sql);
        List<Issue> issues = new ArrayList<>();
        while (matcher.find()) {
            int end = Math.min(sql.length(), matcher.end() + 45);
            issues.add(new Issue(
                    id(),
                    name(),
                    defaultSeverity(),
                    "DATE_FORMAT 是 MySQL 专有函数，且格式串语法也需一并改写。",
                    sql.substring(matcher.start(), end).replaceAll("\\s+", " ").trim(),
                    suggestion(),
                    context.location()));
        }
        return issues;
    }

    @Override
    public boolean fixable() {
        return true;
    }

    @Override
    public FixSafety fixSafety() {
        // 格式串映射表覆盖不到的写法会原样保留，但仍然属于"改写依赖默认值"，需要显式开启。
        return FixSafety.REVIEW;
    }

    @Override
    public String fix(String sql) {
        StringBuilder out = new StringBuilder();
        Matcher matcher = CALL.matcher(sql);
        int last = 0;
        while (matcher.find()) {
            int openParen = matcher.end() - 1;
            int closeParen = matchingParen(sql, openParen);
            if (closeParen < 0) {
                continue;
            }
            String args = sql.substring(openParen + 1, closeParen);
            List<String> parts = splitTopLevel(args);
            if (parts.size() != 2) {
                continue;
            }
            String format = parts.get(1).trim();
            String converted = convertFormatLiteral(format);
            if (converted == null) {
                // 不认识的格式符：整段保留，让规则继续报出来。
                continue;
            }
            out.append(sql, last, matcher.start());
            out.append("TO_CHAR(").append(parts.get(0).trim()).append(", ").append(converted).append(')');
            last = closeParen + 1;
        }
        if (last == 0) {
            return sql;
        }
        out.append(sql.substring(last));
        return out.toString();
    }

    /**
     * 把单引号格式串转成 PostgreSQL 写法。
     *
     * @return 转换结果；含未知格式符或不是单引号字面量时返回 {@code null}
     */
    private String convertFormatLiteral(String literal) {
        if (literal.length() < 2 || literal.charAt(0) != '\'' || literal.charAt(literal.length() - 1) != '\'') {
            return null;
        }
        String body = literal.substring(1, literal.length() - 1);
        StringBuilder sb = new StringBuilder("'");
        int i = 0;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (c != '%') {
                sb.append(c);
                i++;
                continue;
            }
            String mapped = null;
            for (String[] pair : SPECIFIERS) {
                if (body.startsWith(pair[0], i)) {
                    mapped = pair[1];
                    i += pair[0].length();
                    break;
                }
            }
            if (mapped == null) {
                return null;
            }
            sb.append(mapped);
        }
        sb.append('\'');
        return sb.toString();
    }

    private int matchingParen(String sql, int openParen) {
        int depth = 0;
        char quote = 0;
        for (int i = openParen; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private List<String> splitTopLevel(String args) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        char quote = 0;
        int start = 0;
        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                }
                continue;
            }
            if (c == '\'' || c == '"') {
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                parts.add(args.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(args.substring(start));
        return parts;
    }
}
