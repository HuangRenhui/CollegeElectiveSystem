package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 课程评价提交参数。
 *
 * <p>四个维度取值 1-5 分（支持半星，即 .5 步长），综合评分由后端计算，
 * 无需前端传入，避免被篡改。</p>
 */
@Data
@Schema(description = "课程评价提交参数")
public class CourseReviewDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "评价ID（为空表示新增，非空表示修改）")
    private Long id;

    @Schema(description = "课程ID")
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    @Schema(description = "教学内容评分：1-5")
    @NotNull(message = "请为教学内容打分")
    @DecimalMin(value = "1", message = "评分不能低于 1 分")
    @DecimalMax(value = "5", message = "评分不能高于 5 分")
    private BigDecimal scoreContent;

    @Schema(description = "教学方法评分：1-5")
    @NotNull(message = "请为教学方法打分")
    @DecimalMin(value = "1", message = "评分不能低于 1 分")
    @DecimalMax(value = "5", message = "评分不能高于 5 分")
    private BigDecimal scoreTeaching;

    @Schema(description = "教学态度评分：1-5")
    @NotNull(message = "请为教学态度打分")
    @DecimalMin(value = "1", message = "评分不能低于 1 分")
    @DecimalMax(value = "5", message = "评分不能高于 5 分")
    private BigDecimal scoreAttitude;

    @Schema(description = "学习收获评分：1-5")
    @NotNull(message = "请为学习收获打分")
    @DecimalMin(value = "1", message = "评分不能低于 1 分")
    @DecimalMax(value = "5", message = "评分不能高于 5 分")
    private BigDecimal scoreGain;

    @Schema(description = "文字评价：5-500 字")
    @Size(min = 5, max = 500, message = "文字评价需 5-500 字")
    private String content;

    @Schema(description = "是否匿名：0-实名 1-匿名，默认匿名")
    private Integer anonymous;
}
