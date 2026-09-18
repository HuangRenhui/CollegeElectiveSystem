package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.RedisKeys;
import com.college.elective.common.Result;
import com.college.elective.entity.SysLog;
import com.college.elective.service.SysLogService;
import com.college.elective.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员系统管理接口：操作日志、密码重置、缓存开关。
 */
@Tag(name = "11-系统管理（管理员）", description = "操作日志、密码重置、选课开关")
@RestController
@RequestMapping("/admin/system")
@RequiredArgsConstructor
public class AdminUserController {

    private final SysLogService sysLogService;
    private final SysUserService sysUserService;
    private final StringRedisTemplate stringRedisTemplate;

    @Operation(summary = "分页查询操作日志")
    @GetMapping("/logs")
    public Result<PageResult<SysLog>> pageLogs(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "20") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Integer success) {
        return Result.success(sysLogService.pageLogs(pageNum, pageSize, keyword, module, success));
    }

    @Operation(summary = "清理历史操作日志")
    @OperationLog(module = "系统管理", operation = "清理操作日志")
    @DeleteMapping("/logs")
    public Result<Void> cleanLogs(@RequestParam(defaultValue = "90") Integer days) {
        sysLogService.cleanExpiredLogs(days);
        return Result.success(days + " 天前的历史日志已清理完成", null);
    }

    @Operation(summary = "重置用户密码")
    @OperationLog(module = "系统管理", operation = "重置用户密码", saveParam = false)
    @PutMapping("/users/{userId}/password")
    public Result<Void> resetPassword(@PathVariable Long userId,
                                      @RequestParam(defaultValue = "123456") String newPassword) {
        sysUserService.resetPassword(userId, newPassword);
        return Result.success("密码重置成功", null);
    }

    @Operation(summary = "切换选课开关", description = "open-开启选课 off-关闭选课")
    @OperationLog(module = "系统管理", operation = "切换选课开关")
    @PutMapping("/selection-switch/{state}")
    public Result<Void> switchSelection(@PathVariable String state) {
        String value = "off".equalsIgnoreCase(state) ? "off" : "on";
        stringRedisTemplate.opsForValue().set(RedisKeys.SELECTION_SWITCH, value);
        return Result.success("选课开关已" + ("off".equals(value) ? "关闭" : "开启"), null);
    }

    @Operation(summary = "查询选课开关状态")
    @GetMapping("/selection-switch")
    public Result<String> selectionSwitch() {
        String value = stringRedisTemplate.opsForValue().get(RedisKeys.SELECTION_SWITCH);
        return Result.success(value == null ? "on" : value);
    }
}
