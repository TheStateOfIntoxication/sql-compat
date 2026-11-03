package com.sqlcompat.verify;

import com.sqlcompat.verify.model.SqlCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads cases from a directory of {@code .sql} files.
 *
 * <p>One file is one case; the id is the file name without extension. A case is treated as
 * order-sensitive when the statement contains {@code ORDER BY}. Keeping the metadata in the file
 * name instead of a comment directive means the case list is obvious in a pull request diff and
 * there is no parser to get wrong.
 */
public final class CaseLoader {

    public List<SqlCase> load(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            throw new IOException("Cases directory not found: " + directory);
        }
        List<SqlCase> cases = new ArrayList<>();
        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".sql"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(p -> {
                        try {
                            SqlCase loaded = readCase(p);
                            if (loaded != null) {
                                cases.add(loaded);
                            }
                        } catch (IOException e) {
                            throw new IllegalStateException("Failed to read case: " + p, e);
                        }
                    });
        }
        return cases;
    }

    private SqlCase readCase(Path file) throws IOException {
        String sql = stripLineComments(Files.readString(file, StandardCharsets.UTF_8))
                .replaceAll(";\\s*$", "")
                .strip();
        if (sql.isEmpty()) {
            return null;
        }
        String id = file.getFileName().toString()
                .replaceAll("(?i)\\.sql$", "");
        boolean ordered = sql.toLowerCase(java.util.Locale.ROOT).contains("order by");
        return new SqlCase(id, sql, ordered);
    }

    private String stripLineComments(String sql) {
        StringBuilder sb = new StringBuilder(sql.length());
        for (String line : sql.split("\\R")) {
            if (line.stripLeading().startsWith("--")) {
                continue;
            }
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
}
