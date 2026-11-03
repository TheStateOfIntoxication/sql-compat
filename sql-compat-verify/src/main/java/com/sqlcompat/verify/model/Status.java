package com.sqlcompat.verify.model;

/**
 * 一条用例在两个数据库上的对比结论。
 */
public enum Status {

    /** 归一化后的行相同（有序用例还要求行序相同）。 */
    CONSISTENT,

    /** 两边都执行成功，但结果不同。这是最有价值的一类。 */
    DIFFERENT,

    /** 至少一侧执行失败。 */
    ERROR
}
