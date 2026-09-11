package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("semester")
@Schema(description = "学期")
public class Semester extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "学期编码，如 2026-2027-1")
    private String semesterCode;

    @Schema(description = "学期名称")
    private String semesterName;

    @Schema(description = "学期开始日期")
    private LocalDate startDate;

    @Schema(description = "学期结束日期")
    private LocalDate endDate;

    @Schema(description = "选课开放时间")
    private LocalDateTime selectStartTime;

    @Schema(description = "选课截止时间")
    private LocalDateTime selectEndTime;

    @Schema(description = "总教学周数")
    private Integer totalWeeks;

    @Schema(description = "是否当前学期：0-否 1-是")
    private Integer isCurrent;

    @Schema(description = "状态：0-未开始 1-选课中 2-进行中 3-已结束")
    private Integer status;

    /**
     * 当前时间是否处于选课窗口内。
     */
    public boolean inSelectionPeriod() {
        LocalDateTime now = LocalDateTime.now();
        return selectStartTime != null && selectEndTime != null
                && !now.isBefore(selectStartTime) && !now.isAfter(selectEndTime);
    }
}
