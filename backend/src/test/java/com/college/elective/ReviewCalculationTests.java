package com.college.elective;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 教学评价汇总统计单元测试（纯算法，不依赖数据库）。
 *
 * <p>复刻 {@code CourseReviewMapper.xml#selectCourseReviewSummary} 的统计口径，
 * 用于在无数据库环境下回归验证：</p>
 *
 * <pre>
 *   averageScore = IFNULL(ROUND(AVG(average_score) WHERE status &lt;&gt; 3, 1), 0)
 *   reviewCount  = COUNT(status &lt;&gt; 3)
 *   studentCount = COUNT(course_selection WHERE status = 2)
 * </pre>
 *
 * <p><b>为什么值得单独测</b>：这段口径有 3 处容易出错且都不报错——</p>
 * <ol>
 *   <li>隐藏评价（{@code status = 3}）必须排除，否则违规下架的评价仍影响均分；</li>
 *   <li>无评价时必须返回 {@code 0} 而非 {@code null}（{@code IFNULL} 的作用），
 *       否则前端图表拿到 null 会渲染异常；</li>
 *   <li>参评率分母是「已修完人数」而不是「已选课人数」，
 *       也不是「评价总数」——口径错了会得到超过 100% 的参评率。</li>
 * </ol>
 */
class ReviewCalculationTests {

    /** 评价已隐藏（违规下架），统计时排除 */
    private static final int HIDDEN = 3;
    /** 评价已公开 */
    private static final int PUBLISHED = 2;
    /** 评价已提交（学生可改，教师不可见） */
    private static final int SUBMITTED = 1;

    /** 一条评价的四个维度分值 */
    private record Review(int status, BigDecimal average,
                          BigDecimal content, BigDecimal teaching,
                          BigDecimal attitude, BigDecimal gain) {
    }

    /* ---------------- 复刻业务算法 ---------------- */

    /** 统计某维度的平均分：仅计非隐藏评价，无数据时返回 0，保留 1 位小数 */
    private BigDecimal avgOf(List<Review> reviews, java.util.function.Function<Review, BigDecimal> pick) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (Review r : reviews) {
            if (r.status() == HIDDEN) {
                continue; // 隐藏评价不计入
            }
            BigDecimal v = pick.apply(r);
            if (v == null) {
                continue; // SQL 的 AVG 忽略 NULL
            }
            sum = sum.add(v);
            count++;
        }
        // 对应 SQL 的 IFNULL(..., 0)：无有效数据时返回 0 而非 null
        if (count == 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return sum.divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
    }

    /**
     * 统计汇总（对应 selectCourseReviewSummary 的全部输出）。
     *
     * <p>用 {@code HashMap} 而非 {@code Map.of}：取值类型既有 {@code BigDecimal}
     * 又有 {@code Long}，{@code Map.of} 会把 V 推断为二者的公共父类型，
     * 结果无法赋给 {@code Map<String, Object>}（泛型不变性）。</p>
     */
    private Map<String, Object> summary(List<Review> reviews, int finishedStudentCount) {
        long reviewCount = reviews.stream().filter(r -> r.status() != HIDDEN).count();
        Map<String, Object> m = new HashMap<>();
        m.put("averageScore", avgOf(reviews, Review::average));
        m.put("scoreContent", avgOf(reviews, Review::content));
        m.put("scoreTeaching", avgOf(reviews, Review::teaching));
        m.put("scoreAttitude", avgOf(reviews, Review::attitude));
        m.put("scoreGain", avgOf(reviews, Review::gain));
        m.put("reviewCount", reviewCount);
        m.put("studentCount", (long) finishedStudentCount);
        return m;
    }

    /** 参评率（百分比，保留 1 位小数）。分母为 0 时按 0 处理，避免除零。 */
    private BigDecimal participateRate(long reviewCount, long studentCount) {
        if (studentCount <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(reviewCount)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(studentCount), 1, RoundingMode.HALF_UP);
    }

    private Review r(int status, String avg, String c, String t, String a, String g) {
        return new Review(status, new BigDecimal(avg), new BigDecimal(c),
            new BigDecimal(t), new BigDecimal(a), new BigDecimal(g));
    }

    /* ---------------- 平均分 ---------------- */

    @Nested
    @DisplayName("平均分统计")
    class AverageScore {

        @Test
        @DisplayName("隐藏评价不计入平均分")
        void hiddenExcluded() {
            // 两条正常(4.0/5.0) + 一条隐藏(1.0)：均分应为 4.5，而不是 3.3
            List<Review> list = List.of(
                r(PUBLISHED, "4.0", "4", "4", "4", "4"),
                r(SUBMITTED, "5.0", "5", "5", "5", "5"),
                r(HIDDEN, "1.0", "1", "1", "1", "1")
            );
            Map<String, Object> s = summary(list, 10);
            assertEquals(0, ((BigDecimal) s.get("averageScore")).compareTo(new BigDecimal("4.5")));
        }

        @Test
        @DisplayName("无评价时返回 0 而非 null")
        void emptyReturnsZero() {
            Map<String, Object> s = summary(List.of(), 10);
            assertEquals(new BigDecimal("0.0"), s.get("averageScore"));
            assertEquals(0L, s.get("reviewCount"));
        }

        @Test
        @DisplayName("全部被隐藏时返回 0，且评价数为 0")
        void allHidden() {
            List<Review> list = List.of(
                r(HIDDEN, "5.0", "5", "5", "5", "5"),
                r(HIDDEN, "1.0", "1", "1", "1", "1")
            );
            Map<String, Object> s = summary(list, 5);
            assertEquals(new BigDecimal("0.0"), s.get("averageScore"));
            assertEquals(0L, s.get("reviewCount"));
        }

        @Test
        @DisplayName("结果保留 1 位小数并四舍五入")
        void rounding() {
            // (4.0 + 4.1 + 4.4) / 3 = 4.1666... → 4.2
            List<Review> list = List.of(
                r(PUBLISHED, "4.0", "4", "4", "4", "4"),
                r(PUBLISHED, "4.1", "4", "4", "4", "4"),
                r(PUBLISHED, "4.4", "4", "4", "4", "4")
            );
            Map<String, Object> s = summary(list, 3);
            assertEquals(new BigDecimal("4.2"), s.get("averageScore"));
            assertEquals(1, ((BigDecimal) s.get("averageScore")).scale());
        }
    }

    /* ---------------- 四维度 ---------------- */

    @Nested
    @DisplayName("四维度独立统计")
    class Dimensions {

        @Test
        @DisplayName("各维度分别取平均，互不干扰")
        void independent() {
            List<Review> list = List.of(
                r(PUBLISHED, "4.0", "5.0", "4.0", "3.0", "2.0"),
                r(PUBLISHED, "4.0", "3.0", "4.0", "5.0", "4.0")
            );
            Map<String, Object> s = summary(list, 2);
            assertEquals(new BigDecimal("4.0"), s.get("scoreContent"));
            assertEquals(new BigDecimal("4.0"), s.get("scoreTeaching"));
            assertEquals(new BigDecimal("4.0"), s.get("scoreAttitude"));
            assertEquals(new BigDecimal("3.0"), s.get("scoreGain"));
        }

        @Test
        @DisplayName("总分与四维度可以不一致（总分由学生端另外计算或直接提交）")
        void totalMayDifferFromDimensions() {
            List<Review> list = List.of(r(PUBLISHED, "5.0", "1", "1", "1", "1"));
            Map<String, Object> s = summary(list, 1);
            assertEquals(new BigDecimal("5.0"), s.get("averageScore"));
            assertEquals(new BigDecimal("1.0"), s.get("scoreContent"));
        }
    }

    /* ---------------- 参评率 ---------------- */

    @Nested
    @DisplayName("参评率")
    class ParticipateRate {

        @Test
        @DisplayName("分母为已修完人数，而非评价总数")
        void denominatorIsFinishedStudents() {
            // 30 人已修完，交来 12 条评价 → 40.0%
            assertEquals(new BigDecimal("40.0"), participateRate(12, 30));
        }

        @Test
        @DisplayName("全员参评时为 100%")
        void full() {
            assertEquals(new BigDecimal("100.0"), participateRate(20, 20));
        }

        @Test
        @DisplayName("分母为 0 时返回 0，不抛除零异常")
        void zeroDenominator() {
            assertEquals(new BigDecimal("0.0"), participateRate(0, 0));
        }

        @Test
        @DisplayName("保留 1 位小数并四舍五入")
        void rounding() {
            // 1/3 = 33.333... → 33.3
            assertEquals(new BigDecimal("33.3"), participateRate(1, 3));
        }

        @Test
        @DisplayName("隐藏评价不计入参评率分子")
        void hiddenNotCountedInNumerator() {
            List<Review> list = List.of(
                r(PUBLISHED, "4.0", "4", "4", "4", "4"),
                r(HIDDEN, "1.0", "1", "1", "1", "1")
            );
            Map<String, Object> s = summary(list, 10);
            // 分子为 1（隐藏的那条被排除）
            assertEquals(1L, s.get("reviewCount"));
            assertEquals(new BigDecimal("10.0"), participateRate((Long) s.get("reviewCount"), 10L));
        }
    }
}
