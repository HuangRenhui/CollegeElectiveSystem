package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程评价展示对象。
 *
 * <p><b>匿名保护</b>：当 {@code anonymous == 1} 时，
 * {@code studentName} 统一置为「匿名同学」、{@code stuNo} 置空。
 * 该转换必须在本 VO 层完成，切勿将实体直接返回给前端，
 * 否则匿名机制将失效。</p>
 *
 * <p>管理员端需要审计时，可通过 {@code ReviewVOConverter} 的
 * {@code keepIdentity} 参数保留学号。</p>
 */
@Data
@Schema(description = "课程评价")
public class CourseReviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评价ID")
    private Long id;

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

    @Schema(description = "教学内容得分")
    private BigDecimal scoreContent;

    @Schema(description = "教学方法得分")
    private BigDecimal scoreTeaching;

    @Schema(description = "教学态度得分")
    private BigDecimal scoreAttitude;

    @Schema(description = "学习收获得分")
    private BigDecimal scoreGain;

    @Schema(description = "综合评分")
    private BigDecimal averageScore;

    @Schema(description = "文字评价")
    private String content;

    @Schema(description = "是否匿名：0-实名 1-匿名")
    private Integer anonymous;

    @Schema(description = "评价学生姓名（匿名时为「匿名同学」）")
    private String studentName;

    @Schema(description = "学号（匿名或非管理员场景为空）")
    private String stuNo;

    @Schema(description = "状态：1-已提交 2-已公开 3-已隐藏")
    private Integer status;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;
}
