package com.sqlcompat.core.fix;

/**
 * 自动改写模式。
 */
public enum FixMode {

    /** 不改写。 */
    OFF,

    /** 只执行 {@link FixSafety#SAFE} 级的改写。 */
    SAFE_ONLY,

    /** 连 {@link FixSafety#REVIEW} 级一起执行，需要人明确表态。 */
    INCLUDE_REVIEW
}
