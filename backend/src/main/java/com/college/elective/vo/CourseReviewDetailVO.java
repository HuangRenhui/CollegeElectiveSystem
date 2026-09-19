package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 课程评价明细（教师端「查看评价」弹窗）。
 *
 * <p>前端对 {@code { summary, reviews }} 与「纯明细数组」两种结构均做了容错，
 * 此处返回带汇总的组合结构，便于一次请求渲染完整弹窗。</p>
 */
@Data
@Schema(description = "课程评价明细")
public class CourseReviewDetailVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评价汇总")
    private CourseReviewSummaryVO summary;

    @Schema(description = "公开评价明细（已匿名脱敏）")
    private List<CourseReviewVO> reviews;
}
