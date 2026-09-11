package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求参数。
 */
@Data
@Schema(description = "登录请求参数")
public class LoginDTO {

    @Schema(description = "登录账号（学号/工号/管理员账号）", example = "2026010101")
    @NotBlank(message = "登录账号不能为空")
    @Size(max = 50, message = "登录账号长度不能超过 50")
    private String username;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    private String password;

    @Schema(description = "验证码（可选）")
    private String captcha;

    @Schema(description = "验证码标识（可选）")
    private String captchaKey;
}
