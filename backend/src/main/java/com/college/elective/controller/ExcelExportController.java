package com.college.elective.controller;

import com.college.elective.aspect.OperationLog;
import com.college.elective.common.Result;
import com.college.elective.service.ExcelExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Excel 报表导出接口。
 *
 * <p>返回原始字节流（{@code application/vnd.ms-excel}），
 * 不是 JSON 包装，前端需用 {@code responseType: 'blob'} 接收。</p>
 */
@Tag(name = "06-报表导出", description = "成绩单、课程成绩单、选课名单 Excel 导出")
@RestController
@RequestMapping
@RequiredArgsConstructor
public class ExcelExportController {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final ExcelExportService excelExportService;

    @Operation(summary = "导出我的成绩单", description = "学生端：导出当前登录学生的成绩单，semesterId 为空表示全部学期")
    @OperationLog(module = "报表导出", operation = "导出成绩单")
    @GetMapping("/student/grades/export")
    public ResponseEntity<byte[]> exportMyGrades(
            @RequestParam(required = false) Long semesterId) {
        byte[] data = excelExportService.exportStudentGradeReport(semesterId);
        return toResponse(data, "成绩单-" + LocalDateTime.now().format(FILE_TS) + ".xlsx");
    }

    @Operation(summary = "导出课程成绩单", description = "教师端：导出指定课程的成绩录入单（含草稿）")
    @OperationLog(module = "报表导出", operation = "导出课程成绩单")
    @GetMapping("/teacher/courses/{courseId}/grade-sheet/export")
    public ResponseEntity<byte[]> exportCourseGrades(@PathVariable Long courseId) {
        byte[] data = excelExportService.exportCourseGradeSheet(courseId);
        return toResponse(data, "课程成绩单-" + courseId + "-" + LocalDateTime.now().format(FILE_TS) + ".xlsx");
    }

    @Operation(summary = "导出选课名单", description = "教师/管理员：导出指定课程的选课学生名单")
    @OperationLog(module = "报表导出", operation = "导出选课名单")
    @GetMapping("/teacher/courses/{courseId}/selections/export")
    public ResponseEntity<byte[]> exportCourseSelections(@PathVariable Long courseId) {
        byte[] data = excelExportService.exportCourseSelections(courseId);
        return toResponse(data, "选课名单-" + courseId + "-" + LocalDateTime.now().format(FILE_TS) + ".xlsx");
    }

    /**
     * 组装下载响应。
     *
     * <p>文件名必须 URL 编码：中文名在部分浏览器下会乱码，
     * 且 Spring 默认不允许响应头含非 ASCII 字符。</p>
     */
    private ResponseEntity<byte[]> toResponse(byte[] data, String fileName) {
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename*=UTF-8''" + encoded)
            .body(data);
    }
}
