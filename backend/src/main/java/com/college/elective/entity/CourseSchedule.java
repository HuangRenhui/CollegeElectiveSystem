package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 课程排课实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course_schedule")
@Schema(description = "课程排课")
public class CourseSchedule extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "学期ID")
    private Long semesterId;

    @Schema(description = "教室ID")
    private Long classroomId;

    @Schema(description = "星期：1-7")
    private Integer dayOfWeek;

    @Schema(description = "开始节次：1-12")
    private Integer startSection;

    @Schema(description = "结束节次：1-12")
    private Integer endSection;

    @Schema(description = "起始周")
    private Integer startWeek;

    @Schema(description = "结束周")
    private Integer endWeek;

    @Schema(description = "周次类型：ALL/ODD/EVEN")
    private String weekType;

    // ---------------- 非数据库字段 ----------------

    @Schema(description = "课程名称")
    @TableField(exist = false)
    private String courseName;

    @Schema(description = "课程编号")
    @TableField(exist = false)
    private String courseCode;

    @Schema(description = "授课教师ID，仅在冲突校验的关联查询中填充")
    @TableField(exist = false)
    private Long teacherId;

    @Schema(description = "教师姓名")
    @TableField(exist = false)
    private String teacherName;

    @Schema(description = "教室编号")
    @TableField(exist = false)
    private String roomNo;

    @Schema(description = "教学楼")
    @TableField(exist = false)
    private String building;
}
