package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 课表条目，用于前端课表视图渲染。
 */
@Data
@Schema(description = "课表条目")
public class TimetableVO {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "课程编号")
    private String courseCode;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "上课地点")
    private String location;

    @Schema(description = "星期：1-7")
    private Integer dayOfWeek;

    @Schema(description = "开始节次")
    private Integer startSection;

    @Schema(description = "结束节次")
    private Integer endSection;

    @Schema(description = "起始周")
    private Integer startWeek;

    @Schema(description = "结束周")
    private Integer endWeek;

    @Schema(description = "周次类型：ALL/ODD/EVEN")
    private String weekType;

    @Schema(description = "学分")
    private java.math.BigDecimal credit;

    @Schema(description = "是否已退课")
    private Boolean dropped;
}
