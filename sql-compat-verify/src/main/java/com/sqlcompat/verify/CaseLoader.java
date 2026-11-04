package com.sqlcompat.verify;

import com.sqlcompat.verify.model.SqlCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import com.sqlcompat.core.util.Io;

/**
 * 从 {@code .sql} 文件目录加载用例。
 *
 * <p>一个文件就是一条用例，编号取文件名（去掉扩展名）；语句含 {@code ORDER BY} 时视为对行序敏感。
 * 把元信息放在文件名而不是注释指令里，好处是拉 PR 时用例清单一眼可见，也没有解析器可写错。
 */
public final class CaseLoader {

    public List<SqlCase> load(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("用例目录不存在：" + directory);
        }
        List<SqlCase> cases = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".sql"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(p -> {
                        try {
                            SqlCase loaded = readCase(p);
                            if (loaded != null) {
                                cases.add(loaded);
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException("读取用例失败：" + p, e);
                        }
                    });
        }
        return cases;
    }

    private SqlCase readCase(Path file) throws IOException {
        String sql = stripLineComments(Io.readString(file, StandardCharsets.UTF_8))
                .replaceAll(";\\s*$", "")
                .trim();
        if (sql.isEmpty()) {
            return null;
        }
        String id = file.getFileName().toString()
                .replaceAll("(?i)\\.sql$", "");
        boolean ordered = sql.toLowerCase(Locale.ROOT).contains("order by");
        return new SqlCase(id, sql, ordered);
    }

    private String stripLineComments(String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        for (String line : sql.split("\\R")) {
            if (line.trim().startsWith("--")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
