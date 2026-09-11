package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.entity.Semester;
import com.college.elective.service.SemesterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员学期管理接口。
 */
@Tag(name = "09-学期管理（管理员）", description = "学期设置、选课时间窗口、当前学期切换")
@RestController
@RequestMapping("/admin/semesters")
@RequiredArgsConstructor
public class AdminSemesterController {

    private final SemesterService semesterService;

    @Operation(summary = "分页查询学期")
    @GetMapping
    public Result<PageResult<Semester>> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(semesterService.pageSemesters(pageNum, pageSize, keyword));
    }

    @Operation(summary = "学期下拉列表")
    @GetMapping("/options")
    public Result<List<Semester>> options() {
        return Result.success(semesterService.listAll());
    }

    @Operation(summary = "当前学期")
    @GetMapping("/current")
    public Result<Semester> current() {
        return Result.success(semesterService.getCurrentSemester());
    }

    @Operation(summary = "保存学期")
    @OperationLog(module = "学期管理", operation = "保存学期")
    @PostMapping
    public Result<Void> save(@RequestBody Semester semester) {
        semesterService.saveSemester(semester);
        return Result.success("保存成功", null);
    }

    @Operation(summary = "设为当前学期")
    @OperationLog(module = "学期管理", operation = "设置当前学期")
    @PutMapping("/{id}/current")
    public Result<Void> setCurrent(@PathVariable Long id) {
        semesterService.setCurrent(id);
        return Result.success("已设为当前学期", null);
    }

    @Operation(summary = "删除学期")
    @OperationLog(module = "学期管理", operation = "删除学期")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return Result.success("删除成功", null);
    }
}
