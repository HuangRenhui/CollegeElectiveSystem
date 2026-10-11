package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信账号绑定请求。
 */
@Data
@Schema(description = "微信账号绑定请求")
public class WxBindDTO {

    @Schema(description = "待绑定的微信 OpenID（由后端下发，前端不可自行构造）",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "OpenID 不能为空")
    private String openId;

    @Schema(description = "学号或工号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账号不能为空")
    private String username;

    @Schema(description = "登录密码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    private String password;
}
