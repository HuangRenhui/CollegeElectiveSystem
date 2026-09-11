package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.PendingFeature;
import com.college.elective.common.Result;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.dto.GradeInputDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseGrade;
import com.college.elective.entity.CourseSelection;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseGradeService;
import com.college.elective.service.CourseSelectionService;
import com.college.elective.service.CourseService;
import com.college.elective.vo.TimetableVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师课程与成绩管理接口。
 *
 * <p>学生名单与成绩录入/发布相关方法尚未实现，需在完成
 * {@link CourseGradeService} 与 {@link CourseSelectionService} 的实现类后补充。
 * 应用通过 {@link ObjectProvider} 延迟获取这两个服务，因此在其实现类缺失时
 * 仍可正常启动，调用未实现接口会返回明确的业务提示。
 * 我的授课课程与教学课表可直接使用。</p>
 *
 * @see com.college.elective.service.CourseGradeService
 * @see com.college.elective.service.CourseSelectionService
 */
@Tag(name = "04-教师工作台", description = "我的授课、学生名单、成绩录入与发布、教师课表")
@RestController
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherCourseController {

    private final CourseService courseService;
    private final ObjectProvider<CourseGradeService> gradeServiceProvider;
    private final ObjectProvider<CourseSelectionService> selectionServiceProvider;

    @Operation(summary = "我的授课课程")
    @GetMapping("/courses")
    public Result<PageResult<Course>> myCourses(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "20") Long pageSize,
            @RequestParam(required = false) Long semesterId) {
        Long teacherId = SecurityUtils.requireTeacherId();
        CourseQueryDTO query = new CourseQueryDTO();
        query.setPageNum(pageNum);
        query.setPageSize(pageSize);
        query.setTeacherId(teacherId);
        query.setSemesterId(semesterId);
        return Result.success(courseService.pageCourses(query));
    }

    @Operation(summary = "课程学生名单（分页）")
    @GetMapping("/courses/{courseId}/students")
    public Result<PageResult<CourseSelection>> courseStudents(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "50") Long pageSize,
            @RequestParam(required = false) String keyword) {
        CourseSelectionService selectionService = PendingFeature.readyOrNull(selectionServiceProvider);
        if (selectionService == null) {
            return PendingFeature.result("课程学生名单");
        }
        return Result.success(selectionService.pageCourseStudents(courseId, pageNum, pageSize, keyword));
    }

    @Operation(summary = "查询课程名单（选课记录）")
    @GetMapping("/courses/{courseId}/selections")
    public Result<List<CourseSelection>> listSelections(@PathVariable Long courseId) {
        return Result.success(courseService.listCourseStudents(courseId));
    }

    @Operation(summary = "成绩录入单", description = "返回该课程全部学生的当前成绩状态")
    @GetMapping("/courses/{courseId}/grade-sheet")
    public Result<List<CourseGrade>> gradeSheet(@PathVariable Long courseId) {
        CourseGradeService gradeService = PendingFeature.readyOrNull(gradeServiceProvider);
        if (gradeService == null) {
            return PendingFeature.result("成绩录入单");
        }
        return Result.success(gradeService.listCourseGradeSheet(courseId));
    }

    @Operation(summary = "批量录入/暂存成绩")
    @OperationLog(module = "成绩管理", operation = "录入成绩")
    @PostMapping("/grades/input")
    public Result<Void> inputGrades(@Valid @RequestBody GradeInputDTO dto) {
        CourseGradeService gradeService = PendingFeature.readyOrNull(gradeServiceProvider);
        if (gradeService == null) {
            return PendingFeature.result("批量录入成绩");
        }
        gradeService.inputGrades(dto);
        return Result.success("成绩已保存为草稿", null);
    }

    @Operation(summary = "发布课程成绩")
    @OperationLog(module = "成绩管理", operation = "发布成绩")
    @PostMapping("/courses/{courseId}/grades/publish")
    public Result<Void> publishGrades(@PathVariable Long courseId) {
        CourseGradeService gradeService = PendingFeature.readyOrNull(gradeServiceProvider);
        if (gradeService == null) {
            return PendingFeature.result("发布课程成绩");
        }
        gradeService.publishGrades(courseId);
        return Result.success("成绩发布成功", null);
    }

    @Operation(summary = "撤回成绩发布")
    @OperationLog(module = "成绩管理", operation = "撤回成绩")
    @PostMapping("/courses/{courseId}/grades/revoke")
    public Result<Void> revokeGrades(@PathVariable Long courseId) {
        CourseGradeService gradeService = PendingFeature.readyOrNull(gradeServiceProvider);
        if (gradeService == null) {
            return PendingFeature.result("撤回成绩发布");
        }
        gradeService.revokeGrades(courseId);
        return Result.success("成绩发布已撤回", null);
    }

    @Operation(summary = "教师课表")
    @GetMapping("/timetable")
    public Result<List<TimetableVO>> myTimetable(@RequestParam(required = false) Long semesterId) {
        Long teacherId = SecurityUtils.requireTeacherId();
        return Result.success(courseService.getTeacherTimetable(teacherId, semesterId));
    }
}
