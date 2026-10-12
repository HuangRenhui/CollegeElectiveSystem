package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 学生评价趋势（按学期聚合）。
 *
 * <p>统计的是「该学生作为评价人所给出的评分」随学期的变化，
 * 用于小程序端绘制各维度雷达图与学期趋势折线图。</p>
 *
 * <p>字段名与 {@code course_review} 表列一一对应，
 * 便于 MyBatis 直接映射：{@code score_content} → {@code scoreContent} 等。</p>
 */
@Data
@Schema(description = "学生评价趋势（按学期聚合）")
public class ReviewTrendVO {

    @Schema(description = "学期ID")
    private Long semesterId;

    @Schema(description = "学期名称")
    private String semesterName;

    @Schema(description = "评价课程数")
    private Integer courseCount;

    @Schema(description = "综合评分均值（四维平均）")
    private BigDecimal averageScore;

    @Schema(description = "教学内容均分")
    private BigDecimal scoreContent;

    @Schema(description = "教学方法均分")
    private BigDecimal scoreTeaching;

    @Schema(description = "教学态度均分")
    private BigDecimal scoreAttitude;

    @Schema(description = "学习收获均分")
    private BigDecimal scoreGain;
}
