package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.entity.Notice;
import com.college.elective.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员公告管理接口。
 */
@Tag(name = "10-公告管理（管理员）", description = "公告发布、下架与删除")
@RestController
@RequestMapping("/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "分页查询公告")
    @GetMapping
    public Result<PageResult<Notice>> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String noticeType,
            @RequestParam(required = false) Integer status) {
        return Result.success(noticeService.pageNotices(pageNum, pageSize, keyword, noticeType, status));
    }

    @Operation(summary = "公告详情")
    @GetMapping("/{id}")
    public Result<Notice> detail(@PathVariable Long id) {
        return Result.success(noticeService.getNoticeDetail(id));
    }

    @Operation(summary = "发布/保存公告")
    @OperationLog(module = "公告管理", operation = "保存公告")
    @PostMapping
    public Result<Void> save(@RequestBody Notice notice) {
        noticeService.saveNotice(notice);
        return Result.success("公告保存成功", null);
    }

    @Operation(summary = "变更公告状态", description = "0-草稿 1-已发布 2-已下架")
    @OperationLog(module = "公告管理", operation = "变更公告状态")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        noticeService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }

    @Operation(summary = "删除公告")
    @OperationLog(module = "公告管理", operation = "删除公告")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return Result.success("删除成功", null);
    }
}
