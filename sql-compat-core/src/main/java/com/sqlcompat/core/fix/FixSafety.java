package com.sqlcompat.core.fix;

/**
 * 自动改写的安全等级。
 *
 * <p>判断标准只有一条：**改写后行为是否与原来完全等价**。
 * 不等价的改写不是不能做，但必须由人显式开启，见 {@link FixMode}。
 */
public enum FixSafety {

    /** 等价改写，默认执行。例如 IFNULL → COALESCE、反引号 → 双引号。 */
    SAFE,

    /** 改写可能改变行为或依赖默认值，需显式开启。例如 DATE_FORMAT 的格式串映射。 */
    REVIEW
}
