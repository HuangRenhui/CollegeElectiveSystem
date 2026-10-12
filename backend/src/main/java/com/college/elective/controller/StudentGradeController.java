package com.college.elective.controller;

import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.entity.CourseGrade;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseGradeService;
import com.college.elective.vo.GradeReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生成绩查询接口。
 *
 * <p>提供学生个人成绩单、绩点统计与成绩明细查询，
 * 由 {@link CourseGradeService} 统一处理。</p>
 */
@Tag(name = "03-学生成绩", description = "查询成绩单与绩点统计")
@RestController
@RequestMapping("/student/grades")
@RequiredArgsConstructor
public class StudentGradeController {

    private final CourseGradeService gradeService;

    @Operation(summary = "我的成绩单", description = "含总学分、已获学分、平均分与平均绩点")
    @GetMapping("/report")
    public Result<GradeReportVO> myReport(@RequestParam(required = false) Long semesterId) {
        return Result.success(gradeService.getStudentGradeReport(semesterId));
    }

    @Operation(summary = "已发布成绩列表")
    @GetMapping
    public Result<PageResult<CourseGrade>> myGrades(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long semesterId) {
        Long studentId = SecurityUtils.requireStudentId();
        return Result.success(gradeService.pageGrades(pageNum, pageSize, studentId, null, semesterId, 1));
    }
}