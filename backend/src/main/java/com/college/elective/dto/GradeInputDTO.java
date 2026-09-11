package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 成绩录入请求参数（批量）。
 */
@Data
@Schema(description = "成绩录入参数")
public class GradeInputDTO {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "成绩明细列表")
    @NotEmpty(message = "成绩列表不能为空")
    @Valid
    private List<GradeItemDTO> items;

    @Data
    @Schema(description = "单条成绩")
    public static class GradeItemDTO {

        @Schema(description = "选课记录ID")
        private Long selectionId;

        @Schema(description = "学生ID")
        private Long studentId;

        @Schema(description = "平时成绩，0-100")
        private Double usualScore;

        @Schema(description = "期末成绩，0-100")
        private Double examScore;

        @Schema(description = "备注")
        private String remark;
    }
}
