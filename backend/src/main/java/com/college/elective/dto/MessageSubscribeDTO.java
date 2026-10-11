package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订阅消息授权上报。
 *
 * <p>小程序端调用 {@code wx.requestSubscribeMessage} 获得用户同意后，
 * 将场景上报给后端，后端据此把该场景的剩余可发送次数 +1。</p>
 */
@Data
@Schema(description = "订阅消息授权上报")
public class MessageSubscribeDTO {

    @Schema(description = "场景：REVIEW_OPEN-评价开放 / GRADE_PUBLISH-成绩发布",
            requiredMode = Schema.RequiredMode.REQUIRED,
            allowableValues = {"REVIEW_OPEN", "GRADE_PUBLISH"})
    @NotBlank(message = "场景不能为空")
    private String scene;
}
