package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程成绩实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_grade")
@Schema(description = "课程成绩")
public class CourseGrade extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "选课记录ID")
    private Long selectionId;

    @Schema(description = "学生ID")
    private Long studentId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "学期ID")
    private Long semesterId;

    @Schema(description = "平时成绩")
    private BigDecimal usualScore;

    @Schema(description = "期末成绩")
    private BigDecimal examScore;

    @Schema(description = "总评成绩")
    private BigDecimal totalScore;

    @Schema(description = "绩点")
    private BigDecimal gradePoint;

    @Schema(description = "是否及格：0-否 1-是")
    private Integer isPass;

    @Schema(description = "状态：0-草稿 1-已发布 2-已归档")
    private Integer status;

    @Schema(description = "录入人ID")
    private Long inputBy;

    @Schema(description = "录入时间")
    private LocalDateTime inputTime;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;

    @Schema(description = "备注")
    private String remark;

    // ---------------- 非数据库字段 ----------------

    @Schema(description = "学生姓名")
    @TableField(exist = false)
    private String studentName;

    @Schema(description = "学号")
    @TableField(exist = false)
    private String stuNo;

    @Schema(description = "行政班级")
    @TableField(exist = false)
    private String className;

    @Schema(description = "课程编号")
    @TableField(exist = false)
    private String courseCode;

    @Schema(description = "课程名称")
    @TableField(exist = false)
    private String courseName;

    @Schema(description = "学分")
    @TableField(exist = false)
    private BigDecimal credit;

    @Schema(description = "学期名称")
    @TableField(exist = false)
    private String semesterName;
}
