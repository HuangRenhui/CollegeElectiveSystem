package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.IpUtils;
import com.college.elective.common.Result;
import com.college.elective.dto.ChangePasswordDTO;
import com.college.elective.dto.LoginDTO;
import com.college.elective.entity.SysUser;
import com.college.elective.service.SysUserService;
import com.college.elective.vo.LoginVO;
import com.college.elective.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证与个人中心接口。
 */
@Tag(name = "01-认证与个人中心", description = "登录、退出、获取用户信息、修改密码")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;

    @Operation(summary = "用户登录", description = "支持学生、教师、管理员统一登录，返回 JWT 令牌")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        return Result.success("登录成功", sysUserService.login(loginDTO, IpUtils.resolveIp(request)));
    }

    @Operation(summary = "退出登录")
    @OperationLog(module = "认证", operation = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        sysUserService.logout();
        return Result.success("退出成功", null);
    }

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.success(sysUserService.getCurrentUserInfo());
    }

    @Operation(summary = "修改密码")
    @OperationLog(module = "认证", operation = "修改密码", saveParam = false)
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        sysUserService.changePassword(dto);
        return Result.success("密码修改成功，请重新登录", null);
    }

    @Operation(summary = "修改个人资料")
    @OperationLog(module = "认证", operation = "修改个人资料")
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody SysUser user) {
        sysUserService.updateProfile(user);
        return Result.success("资料更新成功", null);
    }
}
