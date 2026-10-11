package com.college.elective.controller;

import com.college.elective.common.Result;
import com.college.elective.entity.Notice;
import com.college.elective.entity.Semester;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.NoticeService;
import com.college.elective.service.SemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公共接口：公告、当前学期、服务器时间基准等无需角色区分的查询。
 */
@Tag(name = "12-公共接口", description = "公告查看、学期信息、服务器时间基准")
@RestController
@RequestMapping("/common")
@RequiredArgsConstructor
public class CommonController {

    private final NoticeService noticeService;
    private final SemesterService semesterService;

    @Operation(summary = "公告列表（按角色过滤）")
    @GetMapping("/notices")
    public Result<List<Notice>> notices(@RequestParam(required = false) Integer limit) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return Result.success(noticeService.listVisibleNotices(loginUser.getRole(), limit));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/notices/{id}")
    public Result<Notice> noticeDetail(@PathVariable Long id) {
        return Result.success(noticeService.getNoticeDetail(id));
    }

    @Operation(summary = "学期下拉列表")
    @GetMapping("/semesters")
    public Result<List<Semester>> semesters() {
        return Result.success(semesterService.listAll());
    }

    @Operation(summary = "当前学期")
    @GetMapping("/semesters/current")
    public Result<Semester> currentSemester() {
        return Result.success(semesterService.getCurrentSemester());
    }

    /**
     * 服务器当前时间，作为前端时间基准。
     *
     * <p><b>为什么需要</b>：选课倒计时依赖时间比较，若前端直接使用浏览器本地时间，
     * 用户修改系统时间即可提前或延后看到选课窗口，进而绕过前端的按钮禁用。
     * 前端应以本接口返回的服务器时间为基准，按「偏移量」修正本地时间。</p>
     *
     * <p><b>为什么返回 epoch 毫秒而非格式化字符串</b>：epoch 是绝对时刻，
     * 不受服务器与浏览器时区差异影响，前端拿到后直接与 {@code Date.now()} 相减
     * 即可得到偏移量，无需解析时区。</p>
     *
     * @return 服务器当前时刻的 epoch 毫秒
     */
    @Operation(summary = "服务器当前时间",
            description = "返回 epoch 毫秒，供前端校正本地时钟偏差")
    @GetMapping("/server-time")
    public Result<Long> serverTime() {
        return Result.success(System.currentTimeMillis());
    }
}
