package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 成绩录入请求参数（批量）。
 */
@Data
@Schema(description = "成绩录入参数")
public class GradeInputDTO {

    /**
     * 单次批量录入上限。
     *
     * <p>批量接口会在同一个事务里逐条查询并更新，没有上限时一次提交上万条
     * 会长时间持有行锁并占用数据库连接，相当于给了一个「用小请求拖垮数据库」的入口。</p>
     */
    public static final int MAX_ITEMS = 1000;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "成绩明细列表，单次最多 1000 条")
    @NotEmpty(message = "成绩列表不能为空")
    @Size(max = MAX_ITEMS, message = "单次最多提交 " + MAX_ITEMS + " 条成绩，请分批提交")
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
