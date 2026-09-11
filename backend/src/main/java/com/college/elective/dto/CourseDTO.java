package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 课程新增/修改请求参数。
 */
@Data
@Schema(description = "课程保存参数")
public class CourseDTO {

    @Schema(description = "课程ID，新增时为空")
    private Long id;

    @Schema(description = "课程编号")
    @NotBlank(message = "课程编号不能为空")
    private String courseCode;

    @Schema(description = "课程名称")
    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    @Schema(description = "开课学期ID")
    @NotNull(message = "请选择开课学期")
    private Long semesterId;

    @Schema(description = "开课院系ID")
    @NotNull(message = "请选择开课院系")
    private Long deptId;

    @Schema(description = "授课教师ID")
    private Long teacherId;

    @Schema(description = "学分")
    @NotNull(message = "请填写学分")
    @DecimalMin(value = "0.5", message = "学分不能小于 0.5")
    private BigDecimal credit;

    @Schema(description = "总学时")
    @NotNull(message = "请填写总学时")
    @Min(value = 1, message = "总学时必须大于 0")
    private Integer hours;

    @Schema(description = "课程类型：REQUIRED/ELECTIVE/PUBLIC")
    @NotBlank(message = "请选择课程类型")
    private String courseType;

    @Schema(description = "考核方式：EXAM/CHECK")
    private String examType;

    @Schema(description = "最大容量")
    @NotNull(message = "请填写课程容量")
    @Min(value = 1, message = "课程容量必须大于 0")
    private Integer maxCapacity;

    @Schema(description = "教材")
    private String textbook;

    @Schema(description = "课程简介")
    private String introduce;

    @Schema(description = "是否可退选：0-不可 1-可")
    private Integer selectable;

    @Schema(description = "状态：0-下架 1-正常 2-已结课")
    private Integer status;

    @Schema(description = "排课信息")
    @Valid
    private List<CourseScheduleDTO> schedules;
}
