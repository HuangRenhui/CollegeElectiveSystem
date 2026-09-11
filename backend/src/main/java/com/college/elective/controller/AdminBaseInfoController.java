package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.Result;
import com.college.elective.entity.Classroom;
import com.college.elective.entity.Department;
import com.college.elective.entity.Major;
import com.college.elective.service.BaseInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理员基础信息接口：院系、专业、教室。
 */
@Tag(name = "08-基础信息（管理员）", description = "院系、专业、教室的维护")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminBaseInfoController {

    private final BaseInfoService baseInfoService;

    // ---------------- 院系 ----------------

    @Operation(summary = "院系列表")
    @GetMapping("/departments")
    public Result<List<Department>> listDepartments(@RequestParam(required = false) String keyword) {
        return Result.success(baseInfoService.listDepartments(keyword));
    }

    @Operation(summary = "保存院系")
    @OperationLog(module = "基础信息", operation = "保存院系")
    @PostMapping("/departments")
    public Result<Void> saveDepartment(@RequestBody Department department) {
        baseInfoService.saveDepartment(department);
        return Result.success("保存成功", null);
    }

    @Operation(summary = "删除院系")
    @OperationLog(module = "基础信息", operation = "删除院系")
    @DeleteMapping("/departments/{id}")
    public Result<Void> deleteDepartment(@PathVariable Long id) {
        baseInfoService.deleteDepartment(id);
        return Result.success("删除成功", null);
    }

    // ---------------- 专业 ----------------

    @Operation(summary = "专业列表")
    @GetMapping("/majors")
    public Result<List<Major>> listMajors(@RequestParam(required = false) Long deptId,
                                          @RequestParam(required = false) String keyword) {
        return Result.success(baseInfoService.listMajors(deptId, keyword));
    }

    @Operation(summary = "保存专业")
    @OperationLog(module = "基础信息", operation = "保存专业")
    @PostMapping("/majors")
    public Result<Void> saveMajor(@RequestBody Major major) {
        baseInfoService.saveMajor(major);
        return Result.success("保存成功", null);
    }

    @Operation(summary = "删除专业")
    @OperationLog(module = "基础信息", operation = "删除专业")
    @DeleteMapping("/majors/{id}")
    public Result<Void> deleteMajor(@PathVariable Long id) {
        baseInfoService.deleteMajor(id);
        return Result.success("删除成功", null);
    }

    // ---------------- 教室 ----------------

    @Operation(summary = "教室列表")
    @GetMapping("/classrooms")
    public Result<List<Classroom>> listClassrooms(@RequestParam(required = false) String keyword) {
        return Result.success(baseInfoService.listClassrooms(keyword));
    }

    @Operation(summary = "保存教室")
    @OperationLog(module = "基础信息", operation = "保存教室")
    @PostMapping("/classrooms")
    public Result<Void> saveClassroom(@RequestBody Classroom classroom) {
        baseInfoService.saveClassroom(classroom);
        return Result.success("保存成功", null);
    }

    @Operation(summary = "删除教室")
    @OperationLog(module = "基础信息", operation = "删除教室")
    @DeleteMapping("/classrooms/{id}")
    public Result<Void> deleteClassroom(@PathVariable Long id) {
        baseInfoService.deleteClassroom(id);
        return Result.success("删除成功", null);
    }
}
