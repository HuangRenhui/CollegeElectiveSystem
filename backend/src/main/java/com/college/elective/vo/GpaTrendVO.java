package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 学生 GPA 趋势视图对象。
 *
 * <p>按学期<b>升序</b>返回（时间顺序），供前端直接绘制折线图。</p>
 */
@Data
@Schema(description = "学生 GPA 趋势")
public class GpaTrendVO {

    @Schema(description = "各学期趋势点（按学期开始日期升序）")
    private List<TrendPoint> points;

    @Schema(description = "学期数量")
    private Integer semesterCount;

    @Schema(description = "最高绩点的学期名称（无数据时为空）")
    private String bestSemester;

    @Schema(description = "最高绩点")
    private BigDecimal bestPoint;

    /**
     * 单个学期的绩点趋势点。
     */
    @Data
    @Schema(description = "绩点趋势点")
    public static class TrendPoint {

        @Schema(description = "学期ID")
        private Long semesterId;

        @Schema(description = "学期名称")
        private String semesterName;

        @Schema(description = "平均绩点（学分加权，保留 2 位小数）")
        private BigDecimal gradePoint;

        @Schema(description = "平均成绩（保留 2 位小数）")
        private BigDecimal averageScore;

        @Schema(description = "该学期修完课程数")
        private Integer courseCount;

        @Schema(description = "该学期获得学分")
        private BigDecimal earnedCredit;
    }
}
