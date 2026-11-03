package com.sqlcompat.core.model;

/**
 * 问题严重级别。CLI 的 {@code --fail-on} 选项用它来决定是否让构建失败。
 */
public enum Severity {

    /** 在目标方言上必然报错，直接阻塞迁移。 */
    BLOCKER(3),

    /** 能编译通过，但语义会变或结果不同。 */
    MAJOR(2),

    /** 能跑，但属于已废弃或不推荐的写法。 */
    MINOR(1),

    /** 纯提示信息，例如"该语句解析降级了"。 */
    INFO(0);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public boolean atLeast(Severity other) {
        return this.weight >= other.weight;
    }
}
