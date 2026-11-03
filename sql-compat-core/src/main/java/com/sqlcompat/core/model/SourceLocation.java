package com.sqlcompat.core.model;

/**
 * 问题所在位置。{@code line} 从 1 开始，未知时为 {@code 0}。
 */
public record SourceLocation(String file, int line) {

    public static final SourceLocation UNKNOWN = new SourceLocation("<unknown>", 0);

    public static SourceLocation of(String file, int line) {
        return new SourceLocation(file, line);
    }

    @Override
    public String toString() {
        return line > 0 ? file + ":" + line : file;
    }
}
