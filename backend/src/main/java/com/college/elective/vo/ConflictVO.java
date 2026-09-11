package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 时间冲突提示信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "时间冲突信息")
public class ConflictVO {

    @Schema(description = "冲突的已选课程名称")
    private String conflictCourseName;

    @Schema(description = "冲突的已选课程编号")
    private String conflictCourseCode;

    @Schema(description = "冲突描述")
    private String description;

    @Schema(description = "冲突星期")
    private Integer dayOfWeek;

    @Schema(description = "冲突节次描述")
    private String sectionText;

    @Schema(description = "冲突周次描述")
    private String weekText;
}
