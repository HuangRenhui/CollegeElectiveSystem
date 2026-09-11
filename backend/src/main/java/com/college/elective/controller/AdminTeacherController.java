package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.dto.TeacherDTO;
import com.college.elective.entity.Teacher;
import com.college.elective.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员教师管理接口。
 */
@Tag(name = "07-教师管理（管理员）", description = "教师信息维护与账号状态管理")
@RestController
@RequestMapping("/admin/teachers")
@RequiredArgsConstructor
public class AdminTeacherController {

    private final TeacherService teacherService;

    @Operation(summary = "分页查询教师")
    @GetMapping
    public Result<PageResult<Teacher>> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String title) {
        return Result.success(teacherService.pageTeachers(pageNum, pageSize, keyword, deptId, title));
    }

    @Operation(summary = "教师下拉选项")
    @GetMapping("/options")
    public Result<List<Teacher>> options(@RequestParam(required = false) Long deptId) {
        return Result.success(teacherService.listOptions(deptId));
    }

    @Operation(summary = "教师详情")
    @GetMapping("/{id}")
    public Result<Teacher> detail(@PathVariable Long id) {
        return Result.success(teacherService.getTeacherDetail(id));
    }

    @Operation(summary = "新增教师", description = "自动创建登录账号，初始密码 123456")
    @OperationLog(module = "教师管理", operation = "新增教师")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody TeacherDTO dto) {
        teacherService.createTeacher(dto);
        return Result.success("教师创建成功，初始密码为 123456", null);
    }

    @Operation(summary = "修改教师")
    @OperationLog(module = "教师管理", operation = "修改教师")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody TeacherDTO dto) {
        teacherService.updateTeacher(dto);
        return Result.success("教师信息修改成功", null);
    }

    @Operation(summary = "删除教师")
    @OperationLog(module = "教师管理", operation = "删除教师")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        teacherService.deleteTeacher(id);
        return Result.success("教师删除成功", null);
    }

    @Operation(summary = "启用/禁用教师账号")
    @OperationLog(module = "教师管理", operation = "变更教师状态")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        teacherService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }
}
