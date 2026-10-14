package com.college.elective.controller;

import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.entity.MessageNotice;
import com.college.elective.service.MessageNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 站内信接口（三类角色共用）。
 */
@Tag(name = "13-站内信", description = "个人消息通知、未读数、已读管理")
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageNoticeController {

    private final MessageNoticeService messageNoticeService;

    @Operation(summary = "我的站内信列表")
    @GetMapping
    public Result<PageResult<MessageNotice>> myMessages(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Integer readFlag) {
        return Result.success(messageNoticeService.pageMyMessages(pageNum, pageSize, readFlag));
    }

    @Operation(summary = "我的未读消息数", description = "供导航栏红点展示")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(messageNoticeService.countMyUnread());
    }

    @Operation(summary = "标记单条已读")
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageNoticeService.markRead(id);
        return Result.success();
    }

    @Operation(summary = "全部标记为已读")
    @PostMapping("/read-all")
    public Result<Integer> markAllRead() {
        return Result.success(messageNoticeService.markAllRead());
    }
}
