package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.PageResult;
import com.college.elective.common.Result;
import com.college.elective.dto.StudentDTO;
import com.college.elective.entity.CourseSelection;
import com.college.elective.entity.Student;
import com.college.elective.service.CourseSelectionService;
import com.college.elective.service.StudentService;
import com.college.elective.vo.SelectionResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员学生管理接口。
 */
@Tag(name = "06-学生管理（管理员）", description = "学生信息维护、账号状态管理与教务代选")
@RestController
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class AdminStudentController {

    private final StudentService studentService;
    private final CourseSelectionService selectionService;

    @Operation(summary = "分页查询学生")
    @GetMapping
    public Result<PageResult<Student>> page(
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer gradeYear) {
        return Result.success(studentService.pageStudents(pageNum, pageSize, keyword, deptId, majorId, gradeYear));
    }

    @Operation(summary = "学生详情")
    @GetMapping("/{id}")
    public Result<Student> detail(@PathVariable Long id) {
        return Result.success(studentService.getStudentDetail(id));
    }

    @Operation(summary = "新增学生", description = "自动创建登录账号，初始密码 123456")
    @OperationLog(module = "学生管理", operation = "新增学生")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody StudentDTO dto) {
        studentService.createStudent(dto);
        return Result.success("学生创建成功，初始密码为 123456", null);
    }

    @Operation(summary = "修改学生")
    @OperationLog(module = "学生管理", operation = "修改学生")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody StudentDTO dto) {
        studentService.updateStudent(dto);
        return Result.success("学生信息修改成功", null);
    }

    @Operation(summary = "删除学生")
    @OperationLog(module = "学生管理", operation = "删除学生")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return Result.success("学生删除成功", null);
    }

    @Operation(summary = "启用/禁用学生账号")
    @OperationLog(module = "学生管理", operation = "变更学生状态")
    @PutMapping("/{id}/status/{status}")
    public Result<Void> updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        studentService.updateStatus(id, status);
        return Result.success("状态更新成功", null);
    }

    // ==================== 教务代选 ====================

    @Operation(summary = "查询指定学生的选课记录")
    @GetMapping("/{studentId}/selections")
    public Result<PageResult<CourseSelection>> studentSelections(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") Long pageNum,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status) {
        return Result.success(selectionService.pageStudentSelections(
                studentId, pageNum, pageSize, semesterId, status));
    }

    @Operation(summary = "教务代选", description = "绕过选课时间窗口，保留学分/冲突/容量校验")
    @OperationLog(module = "教务代选", operation = "代选课程")
    @PostMapping("/{studentId}/selections")
    public Result<SelectionResultVO> addSelection(@PathVariable Long studentId,
                                                  @RequestParam Long courseId,
                                                  @RequestParam String reason) {
        return Result.success("代选成功",
                selectionService.adminAddSelection(studentId, courseId, reason));
    }

    @Operation(summary = "教务代退选", description = "已录入成绩的课程不可退选")
    @OperationLog(module = "教务代选", operation = "代退选课程")
    @DeleteMapping("/{studentId}/selections/{courseId}")
    public Result<Void> removeSelection(@PathVariable Long studentId,
                                       @PathVariable Long courseId,
                                       @RequestParam String reason) {
        selectionService.adminRemoveSelection(studentId, courseId, reason);
        return Result.success("代退选成功", null);
    }
}
