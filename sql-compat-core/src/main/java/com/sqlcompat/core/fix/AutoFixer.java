package com.sqlcompat.core.fix;

import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 按规则集对文件做自动改写。
 *
 * <p>设计上不做多轮收敛：一轮改完就交出去，因为**多轮改写会互相影响**（先改 A 再改 B 可能又生成 A）。
 * 如果一轮之后仍有问题，报告会如实告诉用户"剩下的需要人工决策"。
 */
public final class AutoFixer {

    /** 一份文件的改写结果。 */
    public record FixResult(Path source, String original, String fixed, List<String> appliedRuleIds) {

        public boolean changed() {
            return !original.equals(fixed);
        }
    }

    private final RuleRegistry registry;
    private final FixMode mode;

    public AutoFixer(RuleRegistry registry, FixMode mode) {
        this.registry = registry;
        this.mode = mode;
    }

    /** 改写单个文件的内容；不落盘。 */
    public FixResult fixContent(Path source, String content) {
        String current = content;
        List<String> applied = new ArrayList<>();
        for (DialectRule rule : activeRules()) {
            String next = rule.fix(current);
            if (!next.equals(current)) {
                applied.add(rule.id());
                current = next;
            }
        }
        return new FixResult(source, content, current, applied);
    }

    /**
     * 把输入目录（或单个文件）中所有 Mapper XML / SQL 复制到输出目录后再改写。
     *
     * <p>默认不改原文件：迁移场景下"覆盖源文件"是不可接受的操作习惯。
     */
    public List<FixResult> fixTree(Path input, Path outputDir) throws IOException {
        List<Path> sources = collect(input);
        Path root = Files.isDirectory(input) ? input : input.getParent();
        List<FixResult> results = new ArrayList<>();
        for (Path source : sources) {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            FixResult result = fixContent(source, content);
            Path relative = root == null ? source.getFileName() : root.relativize(source);
            Path target = outputDir.resolve(relative);
            Files.createDirectories(target.getParent());
            Files.writeString(target, result.fixed(), StandardCharsets.UTF_8);
            results.add(result);
        }
        return results;
    }

    private List<Path> collect(Path input) throws IOException {
        if (Files.isRegularFile(input)) {
            return List.of(input);
        }
        try (Stream<Path> walk = Files.walk(input)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                        return name.endsWith(".xml") || name.endsWith(".sql");
                    })
                    .sorted()
                    .toList();
        }
    }

    private List<DialectRule> activeRules() {
        if (mode == FixMode.OFF) {
            return List.of();
        }
        return registry.rules().stream()
                .filter(DialectRule::fixable)
                .filter(rule -> mode == FixMode.INCLUDE_REVIEW
                        || rule.fixSafety() == FixSafety.SAFE)
                .toList();
    }
}
