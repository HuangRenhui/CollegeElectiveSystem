package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 排课请求参数。
 */
@Data
@Schema(description = "排课参数")
public class CourseScheduleDTO {

    @Schema(description = "排课ID")
    private Long id;

    @Schema(description = "教室ID")
    private Long classroomId;

    @Schema(description = "星期：1-7")
    @NotNull(message = "请选择上课星期")
    @Min(value = 1, message = "星期取值 1-7")
    @Max(value = 7, message = "星期取值 1-7")
    private Integer dayOfWeek;

    @Schema(description = "开始节次：1-12")
    @NotNull(message = "请选择开始节次")
    @Min(value = 1, message = "开始节次取值 1-12")
    @Max(value = 12, message = "开始节次取值 1-12")
    private Integer startSection;

    @Schema(description = "结束节次：1-12")
    @NotNull(message = "请选择结束节次")
    @Min(value = 1, message = "结束节次取值 1-12")
    @Max(value = 12, message = "结束节次取值 1-12")
    private Integer endSection;

    @Schema(description = "起始周")
    private Integer startWeek = 1;

    @Schema(description = "结束周")
    private Integer endWeek = 16;

    @Schema(description = "周次类型：ALL/ODD/EVEN")
    private String weekType = "ALL";
}
