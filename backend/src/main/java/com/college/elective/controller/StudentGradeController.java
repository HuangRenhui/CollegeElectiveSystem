package com.college.elective.controller;

import com.college.elective.common.PageResult;
import com.college.elective.common.PendingFeature;
import com.college.elective.common.Result;
import com.college.elective.entity.CourseGrade;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseGradeService;
import com.college.elective.vo.GradeReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学生成绩查询接口。
 *
 * <p>方法尚未实现，需在完成 {@link CourseGradeService} 的实现类后补充。
 * 应用通过 {@link ObjectProvider} 延迟获取该服务，因此在其实现类缺失时
 * 仍可正常启动，调用未实现接口会返回明确的业务提示。</p>
 *
 * @see com.college.elective.service.CourseGradeService
 */
@Tag(name = "03-学生成绩", description = "查询成绩单与绩点统计")
@RestController
@RequestMapping("/student/grades")
@RequiredArgsConstructor
public class StudentGradeController {

    private final ObjectProvider<CourseGradeService> gradeServiceProvider;

    @Operation(summary = "我的成绩单", description = "含总学分、已获学分、平均分与平均绩点")
    @GetMapping("/report")
    public Result<GradeReportVO> myReport(@RequestParam(required = false) Long semesterId) {
        CourseGradeService gradeService = PendingFeature.readyOrNull(gradeServiceProvider);
        if (gradeService == null) {
            return PendingFeature.result("我的成绩单");
        }
        return Result.success(gradeService.getStudentGradeReport(semesterId));
    }

    @Operation(summary = "已发布成绩列表")
    @GetMapping
    public Result<PageResult<CourseGrade>> myGrades(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long semesterId) {
        CourseGradeService gradeService = PendingFeature.readyOrNull(gradeServiceProvider);
        if (gradeService == null) {
            return PendingFeature.result("已发布成绩列表");
        }
        Long studentId = SecurityUtils.requireStudentId();
        return Result.success(gradeService.pageGrades(pageNum, pageSize, studentId, null, semesterId, 1));
    }
}
