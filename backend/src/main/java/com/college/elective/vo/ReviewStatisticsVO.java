package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 全校课程评价统计（管理员端概览卡片）。
 */
@Data
@Schema(description = "评价统计")
public class ReviewStatisticsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评价总数（不含已隐藏）")
    private Long reviewCount;

    @Schema(description = "已公开评价数")
    private Long publishedCount;

    @Schema(description = "已隐藏评价数")
    private Long hiddenCount;

    @Schema(description = "平均分")
    private BigDecimal averageScore;

    @Schema(description = "参评率百分比（取整）")
    private Integer participateRate;

    @Schema(description = "有评价的课程数")
    private Long courseCount;
}
