package com.sqlcompat.core.fix;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 函数调用的简易解析器：找出 {@code name(...)} 并把括号内参数按顶层逗号切开。
 *
 * <p>只做改写需要的那么多事：正确跳过括号与单/双引号即可，不构造 AST。
 * 这样规则改写不必依赖 jsqlparser 的版本差异。
 */
final class SqlCalls {

    /** 一次函数调用的位置与参数。{@code end} 指向右括号之后。 */
    record Call(int start, int end, String name, List<String> args) {
    }

    private SqlCalls() {
    }

    static List<Call> parse(String sql, Set<String> names) {
        String alternation = String.join("|", names);
        Pattern p = Pattern.compile("\\b(" + alternation + ")\\s*\\(", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        List<Call> calls = new ArrayList<>();
        while (m.find()) {
            int openParen = m.end() - 1;
            int closeParen = matchingParen(sql, openParen);
            if (closeParen < 0) {
                continue;
            }
            String inside = sql.substring(openParen + 1, closeParen);
            calls.add(new Call(m.start(), closeParen + 1,
                    m.group(1).toLowerCase(java.util.Locale.ROOT), splitArgs(inside)));
        }
        return calls;
    }

    /** 从 {@code openParen} 开始找配对的右括号；遇到引号会整体跳过。 */
    private static int matchingParen(String sql, int openParen) {
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

    /** 按顶层逗号切参数，括号与引号内部的逗号不算。 */
    static List<String> splitArgs(String inside) {
        List<String> args = new ArrayList<>();
        int depth = 0;
        char quote = 0;
        int start = 0;
        for (int i = 0; i < inside.length(); i++) {
            char c = inside.charAt(i);
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
                args.add(inside.substring(start, i).strip());
                start = i + 1;
            }
        }
        args.add(inside.substring(start).strip());
        return args;
    }

    /** 把若干次调用一次性替换掉，从后往前做，避免位置偏移。 */
    static String replaceAll(String sql, List<Call> calls, java.util.function.Function<Call, String> replacement) {
        StringBuilder sb = new StringBuilder(sql);
        List<Call> reversed = new ArrayList<>(calls);
        reversed.sort((a, b) -> Integer.compare(b.start(), a.start()));
        for (Call call : reversed) {
            String text = replacement.apply(call);
            if (text == null) {
                continue;
            }
            sb.replace(call.start(), call.end(), text);
        }
        return sb.toString();
    }

    /** 去重并排序，用于报告"本次改写了哪些规则"。 */
    static List<String> distinctSorted(List<String> values) {
        Set<String> set = new LinkedHashSet<>(values);
        List<String> sorted = new ArrayList<>(set);
        sorted.sort(String::compareTo);
        return sorted;
    }
}
