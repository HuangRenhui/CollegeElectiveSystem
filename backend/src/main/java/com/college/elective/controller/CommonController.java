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
 * 公共接口：公告、当前学期等无需角色区分的查询。
 */
@Tag(name = "12-公共接口", description = "公告查看、学期信息")
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
}
