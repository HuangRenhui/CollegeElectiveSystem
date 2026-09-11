package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course")
@Schema(description = "课程")
public class Course extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "课程编号")
    private String courseCode;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "开课学期ID")
    private Long semesterId;

    @Schema(description = "开课院系ID")
    private Long deptId;

    @Schema(description = "授课教师ID")
    private Long teacherId;

    @Schema(description = "学分")
    private BigDecimal credit;

    @Schema(description = "总学时")
    private Integer hours;

    @Schema(description = "课程类型：REQUIRED/ELECTIVE/PUBLIC")
    private String courseType;

    @Schema(description = "考核方式：EXAM/CHECK")
    private String examType;

    @Schema(description = "最大容量")
    private Integer maxCapacity;

    @Schema(description = "已选人数")
    private Integer selectedCount;

    @Schema(description = "教材")
    private String textbook;

    @Schema(description = "课程简介")
    private String introduce;

    @Schema(description = "是否可退选：0-不可 1-可")
    private Integer selectable;

    @Schema(description = "状态：0-下架 1-正常 2-已结课")
    private Integer status;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

    // ---------------- 非数据库字段 ----------------

    @Schema(description = "教师姓名")
    @TableField(exist = false)
    private String teacherName;

    @Schema(description = "院系名称")
    @TableField(exist = false)
    private String deptName;

    @Schema(description = "学期名称")
    @TableField(exist = false)
    private String semesterName;

    @Schema(description = "上课时间地点描述")
    @TableField(exist = false)
    private String scheduleText;

    @Schema(description = "排课明细")
    @TableField(exist = false)
    private List<CourseSchedule> schedules;

    @Schema(description = "剩余容量（Redis 实时值）")
    @TableField(exist = false)
    private Integer remainingCapacity;

    @Schema(description = "当前登录学生是否已选")
    @TableField(exist = false)
    private Boolean selected;
}
