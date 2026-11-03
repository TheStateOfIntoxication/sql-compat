package com.sqlcompat.core.rule;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;

import java.util.List;

/**
 * A pluggable compatibility rule.
 *
 * <p>Implementations must be stateless and thread-safe: the engine may reuse a single
 * instance across statements.
 *
 * <p>Implement this interface and register the class in
 * {@code META-INF/services/com.sqlcompat.core.rule.DialectRule} to have it discovered
 * automatically (see {@code RuleRegistry#loadFromServiceLoader()}).
 */
public interface DialectRule {

    /** Stable identifier, never reused or renumbered. e.g. {@code R001}. */
    String id();

    /** Short human readable name shown in reports. */
    String name();

    /** Default severity; a rule may still emit a different severity per finding. */
    Severity defaultSeverity();

    /** One line explaining what the rule looks for. Shown in the rules doc. */
    String description();

    /** Expected rewrite on the target dialect, shown in reports. */
    default String suggestion() {
        return "";
    }

    /**
     * Inspect a single statement and return findings.
     *
     * @return findings, never {@code null}; return {@link List#of()} when clean
     */
    List<Issue> check(RuleContext context);
}
