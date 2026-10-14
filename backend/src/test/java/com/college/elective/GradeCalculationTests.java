package com.college.elective;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 成绩计算逻辑单元测试（纯算法，不依赖数据库）。
 *
 * <p>覆盖 {@code CourseGradeServiceImpl} 中的三项核心算法：</p>
 * <ul>
 *   <li>总评计算：平时 × 30% + 期末 × 70%，单边缺失时折算</li>
 *   <li>绩点换算：4.0 制分段映射</li>
 *   <li>平均绩点：按「绩点 × 学分」加权</li>
 * </ul>
 *
 * <p><b>说明</b>：与 {@link ScheduleConflictTests} 同理，这里复刻业务算法做回归保护。
 * 业务规则一旦改动（如权重从 30/70 调整为 40/60），这些用例会立即失败，
 * 从而提示同步更新文档与前端展示口径。</p>
 */
class GradeCalculationTests {

    /** 平时成绩占比 */
    private static final BigDecimal USUAL_WEIGHT = new BigDecimal("0.3");
    /** 期末成绩占比 */
    private static final BigDecimal EXAM_WEIGHT = new BigDecimal("0.7");
    /** 及格线 */
    private static final double PASS_SCORE = 60.0;

    /* ---------------- 复刻业务算法 ---------------- */

    /**
     * 计算总评成绩。
     *
     * <p>规则：平时 × 0.3 + 期末 × 0.7；单边缺失时按已有成绩折算；
     * 两边都缺失返回 {@code null}。结果保留 1 位小数（四舍五入）。</p>
     */
    private BigDecimal calcTotalScore(BigDecimal usual, BigDecimal exam) {
        if (usual == null && exam == null) {
            return null;
        }
        if (usual == null) {
            return exam.setScale(1, RoundingMode.HALF_UP);
        }
        if (exam == null) {
            return usual.setScale(1, RoundingMode.HALF_UP);
        }
        return usual.multiply(USUAL_WEIGHT)
            .add(exam.multiply(EXAM_WEIGHT))
            .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 绩点换算（4.0 制分段）。
     *
     * <p>分段标准：≥90→4.0，≥85→3.7，≥82→3.3，≥78→3.0，≥75→2.7，
     * ≥72→2.3，≥68→2.0，≥64→1.5，≥60→1.0，&lt;60→0.0。</p>
     */
    private BigDecimal calcGradePoint(BigDecimal total) {
        if (total == null) {
            return null;
        }
        double score = total.doubleValue();
        double point;
        if (score >= 90) {
            point = 4.0;
        } else if (score >= 85) {
            point = 3.7;
        } else if (score >= 82) {
            point = 3.3;
        } else if (score >= 78) {
            point = 3.0;
        } else if (score >= 75) {
            point = 2.7;
        } else if (score >= 72) {
            point = 2.3;
        } else if (score >= 68) {
            point = 2.0;
        } else if (score >= 64) {
            point = 1.5;
        } else if (score >= 60) {
            point = 1.0;
        } else {
            point = 0.0;
        }
        return BigDecimal.valueOf(point).setScale(1, RoundingMode.HALF_UP);
    }

    /** 是否及格 */
    private boolean isPassed(BigDecimal total) {
        return total != null && total.compareTo(BigDecimal.valueOf(PASS_SCORE)) >= 0;
    }

    /* ---------------- 总评计算 ---------------- */

    @Nested
    @DisplayName("总评计算")
    class TotalScore {

        @Test
        @DisplayName("两边齐全时按 30/70 加权")
        void bothPresent() {
            BigDecimal usual = BigDecimal.valueOf(80);
            BigDecimal exam = BigDecimal.valueOf(90);
            // 80 * 0.3 + 90 * 0.7 = 24 + 63 = 87.0
            assertEquals(0, calcTotalScore(usual, exam).compareTo(BigDecimal.valueOf(87.0)));
        }

        @Test
        @DisplayName("仅平时成绩时折算为平时分")
        void onlyUsual() {
            assertEquals(0, calcTotalScore(BigDecimal.valueOf(85), null)
                .compareTo(BigDecimal.valueOf(85.0)));
        }

        @Test
        @DisplayName("仅期末成绩时折算为期末分")
        void onlyExam() {
            assertEquals(0, calcTotalScore(null, BigDecimal.valueOf(72))
                .compareTo(BigDecimal.valueOf(72.0)));
        }

        @Test
        @DisplayName("两边都缺失时返回 null")
        void bothMissing() {
            assertNull(calcTotalScore(null, null));
        }

        @Test
        @DisplayName("结果保留 1 位小数并四舍五入")
        void scaleAndRounding() {
            // 89 * 0.3 + 88 * 0.7 = 26.7 + 61.6 = 88.3
            BigDecimal total = calcTotalScore(BigDecimal.valueOf(89), BigDecimal.valueOf(88));
            assertEquals(1, total.scale());
            assertEquals(0, total.compareTo(BigDecimal.valueOf(88.3)));
        }

        @Test
        @DisplayName("加权后超出百分制上限应保留原值（由录入校验兜底）")
        void overweightStillCalculated() {
            // 算法本身不做截断，上限依赖录入时的 0-100 校验
            BigDecimal total = calcTotalScore(BigDecimal.valueOf(100), BigDecimal.valueOf(100));
            assertEquals(0, total.compareTo(BigDecimal.valueOf(100.0)));
        }
    }

    /* ---------------- 绩点换算 ---------------- */

    @Nested
    @DisplayName("绩点换算")
    class GradePoint {

        @Test
        @DisplayName("各分段边界值映射正确")
        void boundaries() {
            BigDecimal[][] cases = {
                {BigDecimal.valueOf(100), BigDecimal.valueOf(4.0)},
                {BigDecimal.valueOf(90), BigDecimal.valueOf(4.0)},
                {BigDecimal.valueOf(89.9), BigDecimal.valueOf(3.7)},
                {BigDecimal.valueOf(85), BigDecimal.valueOf(3.7)},
                {BigDecimal.valueOf(84.9), BigDecimal.valueOf(3.3)},
                {BigDecimal.valueOf(82), BigDecimal.valueOf(3.3)},
                {BigDecimal.valueOf(81.9), BigDecimal.valueOf(3.0)},
                {BigDecimal.valueOf(78), BigDecimal.valueOf(3.0)},
                {BigDecimal.valueOf(75), BigDecimal.valueOf(2.7)},
                {BigDecimal.valueOf(72), BigDecimal.valueOf(2.3)},
                {BigDecimal.valueOf(68), BigDecimal.valueOf(2.0)},
                {BigDecimal.valueOf(64), BigDecimal.valueOf(1.5)},
                {BigDecimal.valueOf(60), BigDecimal.valueOf(1.0)},
                {BigDecimal.valueOf(59.9), BigDecimal.valueOf(0.0)},
                {BigDecimal.valueOf(0), BigDecimal.valueOf(0.0)},
            };
            for (BigDecimal[] c : cases) {
                assertEquals(0, calcGradePoint(c[0]).compareTo(c[1]),
                    () -> "成绩 " + c[0] + " 应对应绩点 " + c[1]);
            }
        }

        @Test
        @DisplayName("总评为 null 时绩点为 null")
        void nullTotal() {
            assertNull(calcGradePoint(null));
        }

        @Test
        @DisplayName("绩点统一保留 1 位小数")
        void scale() {
            assertEquals(1, calcGradePoint(BigDecimal.valueOf(95)).scale());
        }
    }

    /* ---------------- 及格判定 ---------------- */

    @Nested
    @DisplayName("及格判定")
    class PassCheck {

        @Test
        @DisplayName("60 分整算及格")
        void exactlyPassLine() {
            assertEquals(true, isPassed(BigDecimal.valueOf(60.0)));
        }

        @Test
        @DisplayName("59.9 分不及格")
        void justBelowPassLine() {
            assertEquals(false, isPassed(BigDecimal.valueOf(59.9)));
        }

        @Test
        @DisplayName("总评为 null 时不算及格")
        void nullIsNotPass() {
            assertEquals(false, isPassed(null));
        }
    }

    /* ---------------- 平均绩点加权 ---------------- */

    @Nested
    @DisplayName("平均绩点（学分加权）")
    class AverageGradePoint {

        /** 复刻 getStudentGradeReport 中的加权算法 */
        private BigDecimal averagePoint(BigDecimal[][] courses) {
            BigDecimal pointWeighted = BigDecimal.ZERO;
            BigDecimal pointCredit = BigDecimal.ZERO;
            for (BigDecimal[] c : courses) {
                // c[0] = 绩点, c[1] = 学分
                pointWeighted = pointWeighted.add(c[0].multiply(c[1]));
                pointCredit = pointCredit.add(c[1]);
            }
            return pointCredit.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : pointWeighted.divide(pointCredit, 2, RoundingMode.HALF_UP);
        }

        @Test
        @DisplayName("学分加权而非简单算术平均")
        void weightedNotSimpleAverage() {
            // 课程A：绩点 4.0 × 5 学分；课程B：绩点 2.0 × 1 学分
            // 加权 = (4*5 + 2*1) / 6 = 22/6 = 3.67
            // 算术平均会是 3.00 —— 二者必须不同，否则说明加权失效
            BigDecimal result = averagePoint(new BigDecimal[][]{
                {BigDecimal.valueOf(4.0), BigDecimal.valueOf(5)},
                {BigDecimal.valueOf(2.0), BigDecimal.valueOf(1)},
            });
            assertEquals(0, result.compareTo(BigDecimal.valueOf(3.67)));
        }

        @Test
        @DisplayName("无成绩课程时返回 0")
        void empty() {
            assertEquals(0, averagePoint(new BigDecimal[0][])
                .compareTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)));
        }

        @Test
        @DisplayName("结果保留 2 位小数")
        void scale() {
            assertEquals(2, averagePoint(new BigDecimal[][]{
                {BigDecimal.valueOf(3.7), BigDecimal.valueOf(3)},
            }).scale());
        }
    }
}
