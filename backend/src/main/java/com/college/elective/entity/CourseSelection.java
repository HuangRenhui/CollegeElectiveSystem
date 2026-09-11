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
 * 选课记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_selection")
@Schema(description = "选课记录")
public class CourseSelection extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "学生ID")
    private Long studentId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "学期ID")
    private Long semesterId;

    @Schema(description = "选课时间")
    private LocalDateTime selectTime;

    @Schema(description = "退课时间")
    private LocalDateTime dropTime;

    @Schema(description = "状态：0-已退选 1-已选课 2-已修完")
    private Integer status;

    @Schema(description = "总评成绩")
    private BigDecimal score;

    @Schema(description = "选课方式：1-正常选课 2-管理员代选")
    private Integer selectType;

    // ---------------- 非数据库字段 ----------------

    @Schema(description = "课程编号")
    @TableField(exist = false)
    private String courseCode;

    @Schema(description = "课程名称")
    @TableField(exist = false)
    private String courseName;

    @Schema(description = "学分")
    @TableField(exist = false)
    private BigDecimal credit;

    @Schema(description = "课程类型")
    @TableField(exist = false)
    private String courseType;

    @Schema(description = "教师姓名")
    @TableField(exist = false)
    private String teacherName;

    @Schema(description = "上课时间地点")
    @TableField(exist = false)
    private String scheduleText;

    @Schema(description = "学生姓名")
    @TableField(exist = false)
    private String studentName;

    @Schema(description = "学号")
    @TableField(exist = false)
    private String stuNo;

    @Schema(description = "行政班级")
    @TableField(exist = false)
    private String className;
}
