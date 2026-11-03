package com.sqlcompat.core.rule.builtin;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;
import com.sqlcompat.core.rule.DialectRule;
import com.sqlcompat.core.rule.RuleContext;

import java.util.List;

/**
 * R011 - MyBatis {@code <foreach>} and dialect-specific collection binding.
 *
 * <p>This rule does not test the SQL body: it flags statements that <b>contain</b> a
 * {@code <foreach>}, because the generated {@code IN (...)} list behaves differently across
 * dialects in three ways worth a manual review:
 *
 * <ul>
 *   <li>an empty collection produces {@code IN ()} — a syntax error on both dialects, but MySQL
 *       tolerates it in a few contexts and PostgreSQL never does</li>
 *   <li>batch inserts built with {@code <foreach>} are exactly where {@code INSERT IGNORE} /
 *       {@code ON DUPLICATE KEY} tend to appear (see R005 / R006)</li>
 *   <li>PostgreSQL caps bound parameters at 65535 per statement — large batch inserts that are
 *       fine on MySQL can fail outright</li>
 * </ul>
 *
 * <p>Severity is MINOR because the rule cannot know the collection size or emptiness; it is a
 * "review this" marker, not a defect.
 */
public final class ForeachBindingRule implements DialectRule {

    @Override
    public String id() {
        return "R011";
    }

    @Override
    public String name() {
        return "<foreach> binding needs a manual review";
    }

    @Override
    public Severity defaultSeverity() {
        return Severity.MINOR;
    }

    @Override
    public String description() {
        return "Statements built with <foreach> need a size/emptiness review after migration.";
    }

    @Override
    public String suggestion() {
        return "guard empty lists (IN (NULL) or a <if> wrapper) and keep batch size < 65535 rows";
    }

    @Override
    public List<Issue> check(RuleContext context) {
        if (!context.statement().hasDynamicTag("foreach")) {
            return List.of();
        }
        return List.of(new Issue(
                id(),
                name(),
                defaultSeverity(),
                "This statement builds its list with <foreach>. Check the empty-collection case "
                        + "(IN () is a syntax error) and the bound-parameter limit (65535 per statement "
                        + "on PostgreSQL) for batch operations.",
                "<foreach>",
                suggestion(),
                context.location()));
    }
}
