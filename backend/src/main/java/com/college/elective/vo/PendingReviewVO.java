package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 待评价课程。
 *
 * <p>来源于「已修完的选课记录 - 已评价记录」，供学生端卡片列表展示。</p>
 */
@Data
@Schema(description = "待评价课程")
public class PendingReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "选课记录ID")
    private Long selectionId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程编号")
    private String courseCode;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "学分")
    private BigDecimal credit;

    @Schema(description = "学期名称")
    private String semesterName;

    @Schema(description = "总评成绩")
    private BigDecimal totalScore;
}
