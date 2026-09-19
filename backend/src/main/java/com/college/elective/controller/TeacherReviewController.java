package com.college.elective.controller;

import com.college.elective.common.Result;
import com.college.elective.service.CourseReviewService;
import com.college.elective.vo.CourseReviewDetailVO;
import com.college.elective.vo.CourseReviewSummaryVO;
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
 * 教师教学评价查看接口（只读）。
 *
 * <p>教师仅能查看本人授课课程的评价，且仅可见管理员已公开的评价；
 * 返回数据已做匿名脱敏，不含学生学号。</p>
 *
 * @see com.college.elective.service.CourseReviewService
 */
@Tag(name = "14-教师教学评价", description = "查看本人课程的评价汇总与匿名明细")
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherReviewController {

    private final CourseReviewService reviewService;

    @Operation(summary = "本人课程评价汇总列表")
    @GetMapping("/reviews/summary")
    public Result<List<CourseReviewSummaryVO>> mySummary(@RequestParam(required = false) Long semesterId) {
        return Result.success(reviewService.listMyReviewSummary(semesterId));
    }

    @Operation(summary = "课程评价明细", description = "含汇总与已公开的匿名评价")
    @GetMapping("/courses/{courseId}/reviews")
    public Result<CourseReviewDetailVO> courseReviews(@PathVariable Long courseId) {
        return Result.success(reviewService.getCourseReviews(courseId));
    }
}
