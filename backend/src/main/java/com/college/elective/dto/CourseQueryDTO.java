package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 课程分页查询条件。
 */
@Data
@Schema(description = "课程查询条件")
public class CourseQueryDTO {

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数")
    private Long pageSize = 10L;

    @Schema(description = "课程名称（模糊匹配）")
    private String courseName;

    @Schema(description = "课程编号（模糊匹配）")
    private String courseCode;

    @Schema(description = "学期ID")
    private Long semesterId;

    @Schema(description = "院系ID")
    private Long deptId;

    @Schema(description = "教师ID")
    private Long teacherId;

    @Schema(description = "课程类型：REQUIRED/ELECTIVE/PUBLIC")
    private String courseType;

    @Schema(description = "状态：0-下架 1-正常 2-已结课")
    private Integer status;

    @Schema(description = "是否只看有余量的课程")
    private Boolean onlyAvailable;

    @Schema(description = "是否只看未选的课程（学生视角）")
    private Boolean onlyUnselected;
}
