package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程评价（学生评教）实体。
 *
 * <p>以 {@code selection_id} 作为唯一键，保证「一选课一评价」；
 * 重修课程因选课记录不同，可各自独立评价。</p>
 *
 * <p><b>刻意不继承 {@link com.college.elective.common.BaseEntity}</b>：
 * 基类带有 {@code @TableLogic} 的 {@code deleted} 字段，会使
 * MyBatis-Plus 把所有查询改写成「追加 deleted = 0」、把删除改写为逻辑删除。
 * 但 {@code course_review} 表**没有 deleted 列**（改用物理删除，
 * 否则 {@code selection_id} 唯一键会持续占用，学生撤回后无法重新提交），
 * 一旦继承基类将导致所有评价查询报错。</p>
 */
@Data
@TableName("course_review")
@Schema(description = "课程评价")
public class CourseReview implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "创建时间")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "选课记录ID（唯一，一选课一评价）")
    private Long selectionId;

    @Schema(description = "学生ID")
    private Long studentId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "授课教师ID")
    private Long teacherId;

    @Schema(description = "学期ID")
    private Long semesterId;

    @Schema(description = "教学内容评分：1-5")
    private BigDecimal scoreContent;

    @Schema(description = "教学方法评分：1-5")
    private BigDecimal scoreTeaching;

    @Schema(description = "教学态度评分：1-5")
    private BigDecimal scoreAttitude;

    @Schema(description = "学习收获评分：1-5")
    private BigDecimal scoreGain;

    @Schema(description = "综合评分（四维平均，保留 1 位小数）")
    private BigDecimal averageScore;

    @Schema(description = "文字评价")
    private String content;

    @Schema(description = "是否匿名：0-实名 1-匿名")
    private Integer anonymous;

    @Schema(description = "状态：1-已提交 2-已公开 3-已隐藏")
    private Integer status;

    @Schema(description = "提交时间")
    private LocalDateTime submitTime;

    // ---------------- 非数据库字段 ----------------

    @Schema(description = "学生姓名")
    @TableField(exist = false)
    private String studentName;

    @Schema(description = "学号")
    @TableField(exist = false)
    private String stuNo;

    @Schema(description = "课程编号")
    @TableField(exist = false)
    private String courseCode;

    @Schema(description = "课程名称")
    @TableField(exist = false)
    private String courseName;

    @Schema(description = "授课教师姓名")
    @TableField(exist = false)
    private String teacherName;

    @Schema(description = "学期名称")
    @TableField(exist = false)
    private String semesterName;
}
