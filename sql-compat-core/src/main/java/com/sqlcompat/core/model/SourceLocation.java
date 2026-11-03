package com.sqlcompat.core.model;

/**
 * Where an issue was found. {@code line} is 1-based, or {@code 0} when unknown.
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
