package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.dto.CourseReviewQueryDTO;
import com.college.elective.service.CourseReviewService;
import com.college.elective.vo.CourseReviewVO;
import com.college.elective.vo.ReviewStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员课程评价管理接口。
 *
 * <p>管理员可查看全部评价（含学号，用于审计）、公开或隐藏违规评价、
 * 删除评价以及查看全校评价统计。隐藏与删除均需填写原因。</p>
 *
 * @see com.college.elective.service.CourseReviewService
 */
@Tag(name = "15-评价管理（管理员）", description = "评价查询、公开/隐藏、删除与统计")
@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final CourseReviewService reviewService;

    @Operation(summary = "分页查询评价", description = "支持课程关键词、学期、状态、评分档位筛选")
    @GetMapping
    public Result<PageResult<CourseReviewVO>> page(CourseReviewQueryDTO query) {
        return Result.success(reviewService.pageReviews(query));
    }

    @Operation(summary = "变更评价状态", description = "2-已公开（教师可见） 3-已隐藏（违规下架）")
    @OperationLog(module = "教学评价", operation = "变更评价状态")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @PathVariable Integer status,
                                     @RequestParam String reason) {
        reviewService.updateReviewStatus(id, status, reason);
        return Result.success(status == 2 ? "评价已公开" : "评价已隐藏", null);
    }

    @Operation(summary = "删除评价", description = "物理删除，删除后学生可重新填写该课程评价")
    @OperationLog(module = "教学评价", operation = "删除评价")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, @RequestParam String reason) {
        reviewService.deleteReview(id, reason);
        return Result.success("评价已删除", null);
    }

    @Operation(summary = "评价统计", description = "评价总数、公开/隐藏数、平均分、参评率、覆盖课程数")
    @GetMapping("/statistics")
    public Result<ReviewStatisticsVO> statistics(@RequestParam(required = false) Long semesterId) {
        return Result.success(reviewService.getReviewStatistics(semesterId));
    }
}
