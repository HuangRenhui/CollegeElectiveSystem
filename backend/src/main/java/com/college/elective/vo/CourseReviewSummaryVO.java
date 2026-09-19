package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 课程评价汇总。
 *
 * <p>用于学生端课程卡片、教师端评价卡片与管理员统计。</p>
 */
@Data
@Schema(description = "课程评价汇总")
public class CourseReviewSummaryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程编号")
    private String courseCode;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "学期名称")
    private String semesterName;

    @Schema(description = "综合平均分")
    private BigDecimal averageScore;

    @Schema(description = "教学内容平均分")
    private BigDecimal scoreContent;

    @Schema(description = "教学方法平均分")
    private BigDecimal scoreTeaching;

    @Schema(description = "教学态度平均分")
    private BigDecimal scoreAttitude;

    @Schema(description = "学习收获平均分")
    private BigDecimal scoreGain;

    @Schema(description = "评价人数")
    private Long reviewCount;

    @Schema(description = "应参评人数（该课程已修完的学生数）")
    private Long studentCount;
}
