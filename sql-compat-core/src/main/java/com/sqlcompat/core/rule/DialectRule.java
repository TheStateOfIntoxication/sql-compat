package com.sqlcompat.core.rule;

import com.sqlcompat.core.model.Issue;
import com.sqlcompat.core.model.Severity;

import java.util.List;

/**
 * 可插拔的兼容性规则。
 *
 * <p>实现必须无状态且线程安全：引擎会复用同一个实例处理多条语句。
 *
 * <p>实现本接口后，把类登记到
 * {@code META-INF/services/com.sqlcompat.core.rule.DialectRule}，即可被
 * {@code RuleRegistry#defaults()} 自动发现。
 */
public interface DialectRule {

    /** 稳定编号，发布后不再变更、不复用，例如 {@code R001}。 */
    String id();

    /** 规则名称，显示在报告里。 */
    String name();

    /** 默认级别；同一条规则也可以针对不同发现给出不同级别。 */
    Severity defaultSeverity();

    /** 一句话说明规则在找什么，会同步到规则文档。 */
    String description();

    /** 目标方言下的预期改写，显示在报告里。 */
    default String suggestion() {
        return "";
    }

    /**
     * 检查一条语句。
     *
     * @return 发现的问题，不得返回 {@code null}；没有问题时返回 {@link List#of()}
     */
    List<Issue> check(RuleContext context);
}
