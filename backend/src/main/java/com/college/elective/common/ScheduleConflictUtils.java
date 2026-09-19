package com.college.elective.common;

import com.college.elective.dto.CourseScheduleDTO;
import com.college.elective.entity.CourseSchedule;

import java.util.Objects;

/**
 * 排课时段冲突判定工具。
 *
 * <p>用于课程排课时的教师、教室占用校验，以及学生选课时的上课时间冲突预检。
 * 两处场景共用同一套判定规则，避免分别实现导致行为不一致。</p>
 *
 * <p><b>冲突判定需同时满足四个维度：</b></p>
 * <ol>
 *   <li><b>星期相同</b> —— 不同星期不存在冲突；</li>
 *   <li><b>节次相交</b> —— 两段节次区间有重叠，如 1-2 节与 2-3 节冲突；</li>
 *   <li><b>周次相交</b> —— 两段周次区间有公共周次；</li>
 *   <li><b>周类型兼容</b> —— 见下方说明。</li>
 * </ol>
 *
 * <p><b>周类型兼容规则：</b></p>
 * <ul>
 *   <li>任一方为 {@code ALL}（每周），或两者类型相同，则周次区间相交即为冲突；</li>
 *   <li>一方为 {@code ODD}（单周）、另一方为 {@code EVEN}（双周）时，
 *       需在公共周次区间内存在<b>同奇偶的周次</b>才算冲突。
 *       例如 ODD 排第 1-8 周、EVEN 排第 5-12 周时，第 6、8 周为双方都要上课的偶数周，
 *       仍判定为冲突；不可简单认为单双周永不冲突。</li>
 * </ul>
 *
 * @see <a href="file:../../../../../../test/java/com/college/elective/ScheduleConflictTests.java">冲突判定测试用例</a>
 */
public final class ScheduleConflictUtils {

    /** 起始周默认值：未指定时视为第 1 周 */
    public static final int DEFAULT_START_WEEK = 1;

    /** 结束周默认值：未指定时视为第 16 周 */
    public static final int DEFAULT_END_WEEK = 16;

    private ScheduleConflictUtils() {
        // 工具类禁止实例化
    }

    /**
     * 判断两条排课是否冲突。
     *
     * @param dayA         排课A的上课星期，可为 {@code null}
     * @param startSectionA 排课A起始节次
     * @param endSectionA   排课A结束节次
     * @param startWeekA    排课A起始周
     * @param endWeekA      排课A结束周
     * @param weekTypeA     排课A周类型（ALL/ODD/EVEN）
     * @param dayB         排课B的上课星期，可为 {@code null}
     * @param startSectionB 排课B起始节次
     * @param endSectionB   排课B结束节次
     * @param startWeekB    排课B起始周
     * @param endWeekB      排课B结束周
     * @param weekTypeB     排课B周类型
     * @return {@code true} 表示存在时间冲突
     */
    public static boolean isConflict(Integer dayA, Integer startSectionA, Integer endSectionA,
                                     int startWeekA, int endWeekA, String weekTypeA,
                                     Integer dayB, Integer startSectionB, Integer endSectionB,
                                     int startWeekB, int endWeekB, String weekTypeB) {
        // 维度一：星期必须相同
        if (!Objects.equals(dayA, dayB)) {
            return false;
        }
        // 维度二：节次区间需相交
        if (startSectionA == null || endSectionA == null
                || startSectionB == null || endSectionB == null
                || startSectionA > endSectionB || endSectionA < startSectionB) {
            return false;
        }
        // 维度三：周次区间需相交
        if (startWeekA > endWeekB || endWeekA < startWeekB) {
            return false;
        }
        // 维度四：周类型需兼容
        return isWeekTypeCompatible(startWeekA, endWeekA, weekTypeA,
                startWeekB, endWeekB, weekTypeB);
    }

    /**
     * 判断排课参数与既有排课记录是否冲突。
     *
     * @param dto    排课参数
     * @param entity 既有排课记录
     * @return {@code true} 表示存在时间冲突
     */
    public static boolean isConflict(CourseScheduleDTO dto, CourseSchedule entity) {
        return isConflict(dto.getDayOfWeek(), dto.getStartSection(), dto.getEndSection(),
                defaultStartWeek(dto), defaultEndWeek(dto), dto.getWeekType(),
                entity.getDayOfWeek(), entity.getStartSection(), entity.getEndSection(),
                defaultStartWeek(entity), defaultEndWeek(entity), entity.getWeekType());
    }

    /**
     * 判断两条排课参数是否冲突。
     */
    public static boolean isConflict(CourseScheduleDTO a, CourseScheduleDTO b) {
        return isConflict(a.getDayOfWeek(), a.getStartSection(), a.getEndSection(),
                defaultStartWeek(a), defaultEndWeek(a), a.getWeekType(),
                b.getDayOfWeek(), b.getStartSection(), b.getEndSection(),
                defaultStartWeek(b), defaultEndWeek(b), b.getWeekType());
    }

    /**
     * 判断两条排课记录是否冲突。
     */
    public static boolean isConflict(CourseSchedule a, CourseSchedule b) {
        return isConflict(a.getDayOfWeek(), a.getStartSection(), a.getEndSection(),
                defaultStartWeek(a), defaultEndWeek(a), a.getWeekType(),
                b.getDayOfWeek(), b.getStartSection(), b.getEndSection(),
                defaultStartWeek(b), defaultEndWeek(b), b.getWeekType());
    }

    /**
     * 判断周类型是否兼容（即在公共周次内是否存在双方都要上课的周）。
     *
     * @param startWeekA 排课A起始周
     * @param endWeekA   排课A结束周
     * @param weekTypeA  排课A周类型
     * @param startWeekB 排课B起始周
     * @param endWeekB   排课B结束周
     * @param weekTypeB  排课B周类型
     * @return {@code true} 表示存在双方同上的周次
     */
    private static boolean isWeekTypeCompatible(int startWeekA, int endWeekA, String weekTypeA,
                                                int startWeekB, int endWeekB, String weekTypeB) {
        // 任一方每周上课，或类型一致时，周次相交即冲突
        if (Constants.WEEK_TYPE_ALL.equals(weekTypeA)
                || Constants.WEEK_TYPE_ALL.equals(weekTypeB)
                || Objects.equals(weekTypeA, weekTypeB)) {
            return true;
        }
        // 单双周类型不同：逐个公共周次检查是否存在两者同时上课的周
        int from = Math.max(startWeekA, startWeekB);
        int to = Math.min(endWeekA, endWeekB);
        for (int week = from; week <= to; week++) {
            boolean odd = week % 2 == 1;
            boolean matchA = Constants.WEEK_TYPE_ODD.equals(weekTypeA) == odd;
            boolean matchB = Constants.WEEK_TYPE_ODD.equals(weekTypeB) == odd;
            if (matchA && matchB) {
                return true;
            }
        }
        return false;
    }

    /**
     * 生成节次描述文本，如「第1-2节」。
     */
    public static String sectionText(Integer startSection, Integer endSection) {
        if (startSection == null || endSection == null) {
            return "节次待定";
        }
        return "第" + startSection + "-" + endSection + "节";
    }

    /**
     * 生成周次描述文本，如「第1-16周(单周)」。
     */
    public static String weekText(int startWeek, int endWeek, String weekType) {
        String text = "第" + startWeek + "-" + endWeek + "周";
        if (Constants.WEEK_TYPE_ODD.equals(weekType)) {
            return text + "(单周)";
        }
        if (Constants.WEEK_TYPE_EVEN.equals(weekType)) {
            return text + "(双周)";
        }
        return text;
    }

    /** 星期描述文本 */
    private static final String[] DAY_TEXTS = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /**
     * 生成星期描述文本，如「周一」。
     *
     * @param dayOfWeek 星期序号（1-7）
     * @return 星期文本；超出范围时返回「星期待定」
     */
    public static String dayText(Integer dayOfWeek) {
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek >= DAY_TEXTS.length) {
            return "星期待定";
        }
        return DAY_TEXTS[dayOfWeek];
    }

    private static int defaultStartWeek(CourseScheduleDTO schedule) {
        return schedule.getStartWeek() == null ? DEFAULT_START_WEEK : schedule.getStartWeek();
    }

    private static int defaultEndWeek(CourseScheduleDTO schedule) {
        return schedule.getEndWeek() == null ? DEFAULT_END_WEEK : schedule.getEndWeek();
    }

    private static int defaultStartWeek(CourseSchedule schedule) {
        return schedule.getStartWeek() == null ? DEFAULT_START_WEEK : schedule.getStartWeek();
    }

    private static int defaultEndWeek(CourseSchedule schedule) {
        return schedule.getEndWeek() == null ? DEFAULT_END_WEEK : schedule.getEndWeek();
    }
}
