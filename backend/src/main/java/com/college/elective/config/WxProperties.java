package com.college.elective.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置。
 *
 * <p><b>安全要求</b>：{@code appSecret} 等同于该小程序的「钥匙」，
 * 泄露后任何人都可伪造用户身份调用微信接口。因此：</p>
 * <ul>
 *   <li>绝不能提交到 Git 仓库；</li>
 *   <li>通过环境变量 {@code WX_APP_SECRET} 或配置中心注入；</li>
 *   <li>未配置时 {@link #isEnabled()} 返回 false，
 *       相关功能自动禁用而不会导致启动失败。</li>
 * </ul>
 */
@Data
@Component
@ConfigurationProperties(prefix = "wx.miniapp")
public class WxProperties {

    /** 小程序 AppID */
    private String appId;

    /** 小程序 AppSecret，必须通过环境变量注入 */
    private String appSecret;

    /** 订阅消息-评价开放场景的模板ID */
    private String reviewOpenTemplateId;

    /** 订阅消息-成绩发布场景的模板ID */
    private String gradePublishTemplateId;

    /**
     * 微信相关功能是否可用。
     *
     * <p>未配置 AppID / AppSecret 时返回 false，
     * 前端据此隐藏「微信一键登录」入口，避免用户点了才看到报错。</p>
     *
     * @return true 表示微信能力已就绪
     */
    public boolean isEnabled() {
        return appId != null && !appId.isBlank()
                && appSecret != null && !appSecret.isBlank();
    }

    /**
     * 按场景取模板ID。
     *
     * @param scene 场景：REVIEW_OPEN / GRADE_PUBLISH
     * @return 模板ID；未配置该场景时返回 null
     */
    public String templateIdOf(String scene) {
        if ("REVIEW_OPEN".equals(scene)) {
            return reviewOpenTemplateId;
        }
        if ("GRADE_PUBLISH".equals(scene)) {
            return gradePublishTemplateId;
        }
        return null;
    }
}
