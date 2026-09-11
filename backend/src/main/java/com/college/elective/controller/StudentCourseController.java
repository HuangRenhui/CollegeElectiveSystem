package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.PendingFeature;
import com.college.elective.common.Result;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseSelection;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseSelectionService;
import com.college.elective.service.CourseService;
import com.college.elective.vo.ConflictVO;
import com.college.elective.vo.SelectionResultVO;
import com.college.elective.vo.TimetableVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生选课接口。
 *
 * <p>选课、退课、冲突检测相关方法尚未实现，需在完成
 * {@link CourseSelectionService} 的实现类后补充。
 * 应用通过 {@link ObjectProvider} 延迟获取该服务，因此在其实现类缺失时
 * 仍可正常启动，调用未实现接口会返回明确的业务提示。
 * 课程浏览与课表查询（{@link CourseService}）可直接使用。</p>
 *
 * @see com.college.elective.service.CourseSelectionService
 */
@Tag(name = "02-学生选课", description = "浏览课程、选课退课、查看课表与冲突检测")
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentCourseController {

    private final ObjectProvider<CourseSelectionService> selectionServiceProvider;
    private final CourseService courseService;

    @Operation(summary = "分页查询可选课程", description = "含 Redis 实时余量与学生选课状态")
    @GetMapping("/courses")
    public Result<PageResult<Course>> listCourses(CourseQueryDTO query) {
        CourseSelectionService selectionService = PendingFeature.readyOrNull(selectionServiceProvider);
        if (selectionService == null) {
            return PendingFeature.result("分页查询可选课程");
        }
        Long studentId = SecurityUtils.requireStudentId();
        return Result.success(selectionService.pageAvailableCourses(query, studentId));
    }

    @Operation(summary = "查询课程详情")
    @GetMapping("/courses/{id}")
    public Result<Course> courseDetail(@PathVariable Long id) {
        return Result.success(courseService.getCourseDetail(id));
    }

    @Operation(summary = "选课", description = "Redis Lua 原子预占：查重、余量校验、时间冲突检测")
    @OperationLog(module = "选课", operation = "学生选课")
    @PostMapping("/courses/{courseId}/select")
    public Result<SelectionResultVO> selectCourse(@PathVariable Long courseId) {
        CourseSelectionService selectionService = PendingFeature.readyOrNull(selectionServiceProvider);
        if (selectionService == null) {
            return PendingFeature.result("学生选课");
        }
        return Result.success("选课成功", selectionService.selectCourse(courseId));
    }

    @Operation(summary = "退课")
    @OperationLog(module = "选课", operation = "学生退课")
    @PostMapping("/courses/{courseId}/drop")
    public Result<SelectionResultVO> dropCourse(@PathVariable Long courseId) {
        CourseSelectionService selectionService = PendingFeature.readyOrNull(selectionServiceProvider);
        if (selectionService == null) {
            return PendingFeature.result("学生退课");
        }
        return Result.success("退课成功", selectionService.dropCourse(courseId));
    }

    @Operation(summary = "选课时间冲突预检")
    @GetMapping("/courses/{courseId}/conflict")
    public Result<List<ConflictVO>> checkConflict(@PathVariable Long courseId) {
        CourseSelectionService selectionService = PendingFeature.readyOrNull(selectionServiceProvider);
        if (selectionService == null) {
            return PendingFeature.result("选课时间冲突预检");
        }
        return Result.success(selectionService.checkConflict(courseId));
    }

    @Operation(summary = "我的选课记录")
    @GetMapping("/selections")
    public Result<PageResult<CourseSelection>> mySelections(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {
        CourseSelectionService selectionService = PendingFeature.readyOrNull(selectionServiceProvider);
        if (selectionService == null) {
            return PendingFeature.result("我的选课记录");
        }
        return Result.success(selectionService.pageMySelections(pageNum, pageSize, semesterId, status));
    }

    @Operation(summary = "我的课表")
    @GetMapping("/timetable")
    public Result<List<TimetableVO>> myTimetable(@RequestParam(required = false) Long semesterId) {
        Long studentId = SecurityUtils.requireStudentId();
        return Result.success(courseService.getStudentTimetable(studentId, semesterId));
    }
}
