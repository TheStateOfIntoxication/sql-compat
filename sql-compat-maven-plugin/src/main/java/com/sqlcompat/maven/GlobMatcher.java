package com.sqlcompat.maven;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import com.sqlcompat.core.util.J8;
import java.util.regex.Matcher;

/**
 * 把 Maven 里的 include 通配符（`**&#47;*.xml` 这一类）翻成正则。
 *
 * <p>只实现工具需要的三个语义：{@code **} 跨目录、{@code *} 单层内任意字符、{@code ?} 单字符。
 * 不引第三方 glob 库 —— 为一个几十行的需求加依赖不划算。
 */
final class GlobMatcher {

    private final List<Pattern> patterns;

    private GlobMatcher(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    static GlobMatcher of(List<String> globs) {
        if (globs == null || globs.isEmpty()) {
            return new GlobMatcher(J8.list());
        }
        return new GlobMatcher(globs.stream()
                .map(String::trim)
                .filter(g -> !g.isEmpty())
                .map(GlobMatcher::toPattern)
                .collect(java.util.stream.Collectors.toList()));
    }

    boolean matches(Path path) {
        if (patterns.isEmpty()) {
            return true;
        }
        // 统一成正斜杠再比，避免 Windows 下 `src\main\resources` 匹配不上
        String normalized = path.toString().replace('\\', '/');
        return patterns.stream().anyMatch(p -> p.matcher(normalized).matches());
    }

    private static Pattern toPattern(String glob) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            // 用 if-else 而不是 switch 表达式：switch 表达式是 Java 14 的语法，
            // 而本插件要能在 JDK 8 的 Maven 里被加载。
            if (c == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    // `**/` 可以匹配零层目录，`**` 单独出现则跨任意字符
                    if (i + 2 < glob.length() && glob.charAt(i + 2) == '/') {
                        regex.append("(?:.*/)?");
                        i += 2;
                    } else {
                        regex.append(".*");
                        i += 1;
                    }
                } else {
                    regex.append("[^/]*");
                }
            } else if (c == '?') {
                regex.append("[^/]");
            } else if (isRegexMeta(c)) {
                regex.append('\\').append(c);
            } else {
                regex.append(c);
            }
        }
        return Pattern.compile(regex.toString());
    }

    /** glob 里要转义的字符，交给正则前先加反斜杠。 */
    private static boolean isRegexMeta(char c) {
        return c == '.' || c == '(' || c == ')' || c == '[' || c == ']' || c == '{'
                || c == '}' || c == '+' || c == '|' || c == '^' || c == '$' || c == '\\';
    }
}
