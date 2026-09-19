package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.dto.CourseReviewDTO;
import com.college.elective.service.CourseReviewService;
import com.college.elective.vo.CourseReviewSummaryVO;
import com.college.elective.vo.CourseReviewVO;
import com.college.elective.vo.PendingReviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生教学评价接口。
 *
 * <p>仅可评价本人「已修完」的课程；一课一评，公开前可修改与撤回。</p>
 *
 * @see com.college.elective.service.CourseReviewService
 */
@Tag(name = "13-学生教学评价", description = "待评价课程、提交评价、评价汇总与公开评价")
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentReviewController {

    private final CourseReviewService reviewService;

    @Operation(summary = "待评价课程列表", description = "已修完且尚未评价的课程")
    @GetMapping("/reviews/pending")
    public Result<List<PendingReviewVO>> pendingReviews(@RequestParam(required = false) Long semesterId) {
        return Result.success(reviewService.listPendingReviews(semesterId));
    }

    @Operation(summary = "提交课程评价", description = "带 id 表示修改；四维评分 1-5，文字 5-500 字")
    @OperationLog(module = "教学评价", operation = "提交评价")
    @PostMapping("/reviews")
    public Result<Long> submitReview(@Valid @RequestBody CourseReviewDTO dto) {
        Long id = reviewService.submitReview(dto);
        return Result.success(dto.getId() == null ? "评价提交成功" : "评价已更新", id);
    }

    @Operation(summary = "我的评价列表")
    @GetMapping("/reviews")
    public Result<List<CourseReviewVO>> myReviews(@RequestParam(required = false) Long semesterId) {
        return Result.success(reviewService.listMyReviews(semesterId));
    }

    @Operation(summary = "我对某课程的评价")
    @GetMapping("/reviews/{courseId}")
    public Result<CourseReviewVO> myReview(@PathVariable Long courseId) {
        return Result.success(reviewService.getMyReview(courseId));
    }

    @Operation(summary = "撤回我的评价", description = "撤回后该课程重新回到待评价列表")
    @OperationLog(module = "教学评价", operation = "撤回评价")
    @DeleteMapping("/reviews/{id}")
    public Result<Void> deleteMyReview(@PathVariable Long id) {
        reviewService.deleteMyReview(id);
        return Result.success("评价已撤回", null);
    }

    @Operation(summary = "课程评价汇总", description = "平均分、各维度得分、参评人数")
    @GetMapping("/courses/{courseId}/review-summary")
    public Result<CourseReviewSummaryVO> reviewSummary(@PathVariable Long courseId) {
        return Result.success(reviewService.getCourseReviewSummary(courseId));
    }

    @Operation(summary = "课程公开评价列表", description = "仅已公开评价，匿名脱敏")
    @GetMapping("/courses/{courseId}/reviews")
    public Result<PageResult<CourseReviewVO>> courseReviews(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return Result.success(reviewService.listCourseReviews(courseId, pageNum, pageSize));
    }
}
