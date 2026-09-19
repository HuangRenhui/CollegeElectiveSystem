package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 课程评价查询条件（分页）。
 */
@Data
@Schema(description = "课程评价查询条件")
public class CourseReviewQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "页码")
    private Long pageNum = 1L;

    @Schema(description = "每页条数")
    private Long pageSize = 10L;

    @Schema(description = "关键词：课程名称 / 课程编号 / 教师姓名")
    private String keyword;

    @Schema(description = "学期ID")
    private Long semesterId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "教师ID")
    private Long teacherId;

    @Schema(description = "状态：1-已提交 2-已公开 3-已隐藏")
    private Integer status;

    /**
     * 综合评分档位：high(≥4.5) / good(3.5-4.4) / normal(2.5-3.4) / low(&lt;2.5)。
     *
     * <p>由 SQL 依据 {@code average_score} 区间过滤，避免前端自行拼分段。</p>
     */
    @Schema(description = "评分档位：high/good/normal/low")
    private String scoreLevel;
}
