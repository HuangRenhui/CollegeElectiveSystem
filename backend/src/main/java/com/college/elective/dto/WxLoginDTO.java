package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信一键登录请求。
 */
@Data
@Schema(description = "微信一键登录请求")
public class WxLoginDTO {

    @Schema(description = "wx.login() 得到的临时登录凭证",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "登录凭证不能为空")
    private String code;
}
