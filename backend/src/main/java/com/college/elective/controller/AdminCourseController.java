package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.PendingFeature;
import com.college.elective.common.Result;
import com.college.elective.dto.CourseDTO;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.service.CourseSelectionService;
import com.college.elective.service.CourseService;
import com.college.elective.vo.StatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员课程管理接口。
 *
 * <p>缓存预热与选课人数同步方法尚未实现，需在完成
 * {@link CourseSelectionService} 的实现类后补充。
 * 应用通过 {@link ObjectProvider} 延迟获取该服务，因此在其实现类缺失时
 * 仍可正常启动，调用未实现接口会返回明确的业务提示。
 * 课程 CRUD 与统计可直接使用。</p>
 *
 * @see com.college.elective.service.CourseSelectionService
 */
@Tag(name = "05-课程管理（管理员）", description = "课程 CRUD、状态变更、缓存预热、统计")
@RestController
@RequestMapping("/admin/courses")
@RequiredArgsConstructor
public class AdminCourseController {

    private final CourseService courseService;
    private final ObjectProvider<CourseSelectionService> selectionServiceProvider;

    @Operation(summary = "分页查询课程")
    @GetMapping
    public Result<PageResult<Course>> page(CourseQueryDTO query) {
        return Result.success(courseService.pageCourses(query));
    }

    @Operation(summary = "课程详情")
    @GetMapping("/{id}")
    public Result<Course> detail(@PathVariable Long id) {
        return Result.success(courseService.getCourseDetail(id));
    }

    @Operation(summary = "新增课程")
    @OperationLog(module = "课程管理", operation = "新增课程")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CourseDTO dto) {
        return Result.success("课程创建成功", courseService.createCourse(dto));
    }

    @Operation(summary = "修改课程")
    @OperationLog(module = "课程管理", operation = "修改课程")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody CourseDTO dto) {
        courseService.updateCourse(dto);
        return Result.success("课程修改成功", null);
    }

    @Operation(summary = "删除课程")
    @OperationLog(module = "课程管理", operation = "删除课程")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return Result.success("课程删除成功", null);
    }

    @Operation(summary = "变更课程状态", description = "0-下架 1-正常 2-已结课")
    @OperationLog(module = "课程管理", operation = "变更课程状态")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        courseService.updateStatus(id, status);
        return Result.success("课程状态更新成功", null);
    }

    @Operation(summary = "预热选课缓存", description = "将课程余量与已选学生写入 Redis，提升选课性能")
    @OperationLog(module = "选课管理", operation = "预热课程缓存")
    @PostMapping("/cache/preload")
    public Result<Void> preloadCache(@RequestParam(required = false) Long semesterId) {
        CourseSelectionService selectionService = PendingFeature.readyOrNull(selectionServiceProvider);
        if (selectionService == null) {
            return PendingFeature.result("缓存预热");
        }
        selectionService.preloadCourseCache(semesterId);
        return Result.success("缓存预热完成", null);
    }

    @Operation(summary = "手动同步选课人数")
    @OperationLog(module = "选课管理", operation = "同步选课人数")
    @PostMapping("/selection/sync")
    public Result<Void> syncSelection() {
        CourseSelectionService selectionService = PendingFeature.readyOrNull(selectionServiceProvider);
        if (selectionService == null) {
            return PendingFeature.result("同步选课人数");
        }
        selectionService.syncSelectionCount();
        return Result.success("选课人数同步完成", null);
    }

    @Operation(summary = "首页统计数据")
    @GetMapping("/statistics")
    public Result<StatisticsVO> statistics(@RequestParam(required = false) Long semesterId) {
        return Result.success(courseService.getStatistics(semesterId));
    }
}
