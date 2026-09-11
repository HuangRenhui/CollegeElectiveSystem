package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 选课/退课操作结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "选课结果")
public class SelectionResultVO {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程名称")
    private String courseName;

    @Schema(description = "剩余容量")
    private Integer remainingCapacity;

    @Schema(description = "提示信息")
    private String message;
}
