package com.sqlcompat.verify.report;

import com.sqlcompat.verify.model.CaseOutcome;
import com.sqlcompat.verify.model.ComparisonReport;
import com.sqlcompat.verify.model.QueryOutcome;
import com.sqlcompat.verify.model.SqlCase;
import com.sqlcompat.verify.model.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 一致性报告要能被**外部人**读懂：拿到一份 .md 的人，不该从「一致率 80%」推断出「工具有问题」。
 *
 * <p>所以报告必须自带口径说明：不一致既可能是故意构造的方言差异，也可能是真发现。
 */
class ComparisonReportRendererTest {

    private final ComparisonReportRenderer renderer = new ComparisonReportRenderer();

    private static QueryOutcome ok(long millis) {
        return new QueryOutcome(List.of(), millis, null);
    }

    private static ComparisonReport report() {
        SqlCase same = new SqlCase("01_ok", "SELECT 1", true);
        SqlCase diff = new SqlCase("07_null_order", "SELECT owner_id FROM t ORDER BY owner_id", true);
        List<CaseOutcome> outcomes = List.of(
                new CaseOutcome(same, Status.CONSISTENT, null, ok(1), ok(0)),
                new CaseOutcome(diff, Status.DIFFERENT,
                        "第 0 行第 0 列不同：mysql='<null>'，postgres='1'", ok(1), ok(0)));
        return new ComparisonReport("MySQL", "PostgreSQL", outcomes);
    }

    @Test
    @DisplayName("每个用例都出现在报告里，不一致的带原因")
    void rendersBothCases() {
        String md = renderer.render(report());

        assertThat(md).contains("01_ok").contains("07_null_order");
        assertThat(md).contains("第 0 行第 0 列不同");
        assertThat(md).contains("一致率");
    }

    @Test
    @DisplayName("报告自带口径说明：不一致 != 缺陷")
    void explainsThatDifferenceIsNotABug() {
        String md = renderer.render(report());

        assertThat(md).contains("不一致不等于缺陷");
        assertThat(md).contains("known_");
    }

    @Test
    @DisplayName("耗时声明为指示性对比，不能当 benchmark")
    void labelsTimingAsIndicative() {
        assertThat(renderer.render(report())).contains("不是 benchmark");
    }
}
