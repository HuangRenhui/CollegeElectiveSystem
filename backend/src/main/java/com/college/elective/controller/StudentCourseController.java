package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseSelection;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseSelectionService;
import com.college.elective.service.CourseService;
import com.college.elective.vo.ConflictVO;
import com.college.elective.vo.QueueStatusVO;
import com.college.elective.vo.SelectionResultVO;
import com.college.elective.vo.TimetableVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学生选课接口。
 *
 * <p>覆盖学生侧选课全流程：可选课程浏览、冲突预检、选课、退课与选课记录查询，
 * 均委托给 {@link CourseSelectionService} 处理。</p>
 */
@Tag(name = "02-学生选课", description = "浏览课程、选课退课、查看课表与冲突检测")
@RestController
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentCourseController {

    private final CourseSelectionService selectionService;
    private final CourseService courseService;

    @Operation(summary = "分页查询可选课程", description = "含 Redis 实时余量与学生选课状态")
    @GetMapping("/courses")
    public Result<PageResult<Course>> listCourses(CourseQueryDTO query) {
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
        return Result.success("选课成功", selectionService.selectCourse(courseId));
    }

    @Operation(summary = "退课")
    @OperationLog(module = "选课", operation = "学生退课")
    @PostMapping("/courses/{courseId}/drop")
    public Result<SelectionResultVO> dropCourse(@PathVariable Long courseId) {
        return Result.success("退课成功", selectionService.dropCourse(courseId));
    }

    @Operation(summary = "选课时间冲突预检")
    @GetMapping("/courses/{courseId}/conflict")
    public Result<List<ConflictVO>> checkConflict(@PathVariable Long courseId) {
        return Result.success(selectionService.checkConflict(courseId));
    }

    @Operation(summary = "选课排队取号",
            description = "高峰期削峰：先取号，叫到号后再提交选课；未启用排队时返回可直接选课的状态")
    @PostMapping("/courses/{courseId}/queue")
    public Result<QueueStatusVO> requestQueue(@PathVariable Long courseId) {
        return Result.success("取号成功", selectionService.requestQueue(courseId));
    }

    @Operation(summary = "查询排队进度", description = "前端每 2-3 秒轮询一次")
    @GetMapping("/courses/{courseId}/queue")
    public Result<QueueStatusVO> queryQueue(@PathVariable Long courseId) {
        return Result.success(selectionService.queryQueueStatus(courseId));
    }

    @Operation(summary = "放弃排队")
    @DeleteMapping("/courses/{courseId}/queue")
    public Result<Void> cancelQueue(@PathVariable Long courseId) {
        selectionService.cancelQueue(courseId);
        return Result.success("已退出排队", null);
    }

    @Operation(summary = "我的选课记录")
    @GetMapping("/selections")
    public Result<PageResult<CourseSelection>> mySelections(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {
        return Result.success(selectionService.pageMySelections(pageNum, pageSize, semesterId, status));
    }

    @Operation(summary = "我的课表")
    @GetMapping("/timetable")
    public Result<List<TimetableVO>> myTimetable(@RequestParam(required = false) Long semesterId) {
        Long studentId = SecurityUtils.requireStudentId();
        return Result.success(courseService.getStudentTimetable(studentId, semesterId));
    }
}