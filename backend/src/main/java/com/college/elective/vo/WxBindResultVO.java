package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 微信绑定结果。
 *
 * <p>由 {@code bound} 字段区分两种返回形态：</p>
 * <ul>
 *   <li>{@code bound = true}：已绑定，直接携带令牌与用户信息完成登录；</li>
 *   <li>{@code bound = false}：首次使用，需前端引导用户输入学号密码调用绑定接口。</li>
 * </ul>
 */
@Data
@Schema(description = "微信绑定结果")
public class WxBindResultVO {

    @Schema(description = "是否已绑定系统账号")
    private Boolean bound;

    @Schema(description = "待绑定的微信 OpenID；未绑定时需回传给绑定接口")
    private String openId;

    @Schema(description = "系统用户ID（已绑定时返回）")
    private Long userId;

    @Schema(description = "JWT 令牌（已绑定时返回）")
    private String token;

    @Schema(description = "令牌有效期（秒）")
    private Long expiresIn;

    @Schema(description = "登录账号（已绑定时返回）")
    private String username;

    @Schema(description = "真实姓名（已绑定时返回）")
    private String realName;

    @Schema(description = "角色（已绑定时返回）")
    private String role;

    @Schema(description = "提示文案（未绑定时返回）")
    private String message;
}
