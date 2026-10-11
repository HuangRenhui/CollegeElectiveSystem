package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.Result;
import com.college.elective.dto.MessageSubscribeDTO;
import com.college.elective.dto.WxBindDTO;
import com.college.elective.dto.WxLoginDTO;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.MessageSubscribeService;
import com.college.elective.service.WxAuthService;
import com.college.elective.vo.WxBindResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 微信小程序专用接口。
 *
 * <p>包含微信一键登录与订阅消息授权两组能力，仅供小程序端调用。</p>
 */
@Tag(name = "16-微信小程序", description = "微信一键登录、账号绑定、订阅消息授权")
@RestController
@RequestMapping("/wx")
@RequiredArgsConstructor
public class WxAppController {

    private final WxAuthService wxAuthService;
    private final MessageSubscribeService subscribeService;

    @Operation(summary = "微信登录能力是否可用",
            description = "未配置 AppID/AppSecret 时返回 false，前端据此隐藏入口")
    @GetMapping("/enabled")
    public Result<Boolean> enabled() {
        return Result.success(wxAuthService.isEnabled());
    }

    @Operation(summary = "微信一键登录",
            description = "已绑定直接返回令牌；未绑定返回 openId 供后续绑定")
    @PostMapping("/login")
    public Result<WxBindResultVO> wxLogin(@Valid @RequestBody WxLoginDTO dto) {
        return Result.success(wxAuthService.wxLogin(dto));
    }

    @Operation(summary = "绑定系统账号", description = "首次使用需输入学号与密码完成绑定")
    @OperationLog(module = "微信登录", operation = "绑定账号")
    @PostMapping("/bind")
    public Result<WxBindResultVO> bind(@Valid @RequestBody WxBindDTO dto) {
        return Result.success("绑定成功", wxAuthService.bindAccount(dto));
    }

    @Operation(summary = "可订阅的场景模板", description = "未配置模板的场景不会返回")
    @GetMapping("/subscribe/templates")
    public Result<List<Map<String, String>>> templates() {
        return Result.success(subscribeService.listAvailableTemplates());
    }

    @Operation(summary = "上报订阅授权", description = "用户同意后调用，剩余可发送次数 +1")
    @PostMapping("/subscribe")
    public Result<Void> subscribe(@Valid @RequestBody MessageSubscribeDTO dto) {
        subscribeService.subscribe(SecurityUtils.getUserId(), dto.getScene());
        return Result.success("订阅成功", null);
    }

    @Operation(summary = "查询剩余订阅次数", description = "归零时前端应再次引导用户授权")
    @GetMapping("/subscribe/{scene}/remain")
    public Result<Integer> remain(@PathVariable String scene) {
        return Result.success(subscribeService.getRemainCount(
                SecurityUtils.getUserId(), scene));
    }
}
