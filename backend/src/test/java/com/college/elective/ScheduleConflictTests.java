package com.college.elective;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排课时间冲突算法单元测试。
 *
 * <p>覆盖同一天节次重叠、周次不重叠、单双周互斥等关键边界场景。</p>
 */
class ScheduleConflictTests {

    /**
     * 复刻业务中的冲突判定逻辑，用于纯算法验证。
     */
    private boolean isConflict(int dayA, int startA, int endA, int weekStartA, int weekEndA, String typeA,
                              int dayB, int startB, int endB, int weekStartB, int weekEndB, String typeB) {
        if (dayA != dayB) {
            return false;
        }
        boolean sectionOverlap = startA <= endB && endA >= startB;
        if (!sectionOverlap) {
            return false;
        }
        if (weekStartA > weekEndB || weekEndA < weekStartB) {
            return false;
        }
        if ("ALL".equals(typeA) || "ALL".equals(typeB) || typeA.equals(typeB)) {
            return true;
        }
        int from = Math.max(weekStartA, weekStartB);
        int to = Math.min(weekEndA, weekEndB);
        for (int week = from; week <= to; week++) {
            boolean odd = week % 2 == 1;
            boolean matchA = "ODD".equals(typeA) == odd;
            boolean matchB = "ODD".equals(typeB) == odd;
            if (matchA && matchB) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("同一天节次重叠应判定为冲突")
    void sameDaySectionOverlap() {
        assertTrue(isConflict(1, 1, 2, 1, 16, "ALL",
                1, 2, 3, 1, 16, "ALL"));
    }

    @Test
    @DisplayName("不同星期不冲突")
    void differentDayNoConflict() {
        assertFalse(isConflict(1, 1, 2, 1, 16, "ALL",
                2, 1, 2, 1, 16, "ALL"));
    }

    @Test
    @DisplayName("节次不重叠不冲突")
    void sectionNotOverlap() {
        assertFalse(isConflict(1, 1, 2, 1, 16, "ALL",
                1, 3, 4, 1, 16, "ALL"));
    }

    @Test
    @DisplayName("周次不重叠不冲突")
    void weekNotOverlap() {
        assertFalse(isConflict(1, 1, 2, 1, 8, "ALL",
                1, 1, 2, 9, 16, "ALL"));
    }

    @Test
    @DisplayName("单双周同一时段互不冲突")
    void oddEvenWeekNoConflict() {
        assertFalse(isConflict(1, 1, 2, 1, 16, "ODD",
                1, 1, 2, 1, 16, "EVEN"));
    }

    @Test
    @DisplayName("单周与每周仍存在冲突")
    void oddVersusAllConflict() {
        assertTrue(isConflict(1, 1, 2, 1, 16, "ODD",
                1, 1, 2, 1, 16, "ALL"));
    }

    @Test
    @DisplayName("边界接触节次应判定为冲突")
    void boundarySectionConflict() {
        assertTrue(isConflict(3, 1, 2, 1, 16, "ALL",
                3, 2, 4, 1, 16, "ALL"));
    }
}
