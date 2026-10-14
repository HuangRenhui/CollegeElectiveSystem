package com.college.elective.service;

/**
 * Excel 报表导出服务。
 *
 * <p>统一走字节流返回（{@code byte[]}），由 Controller 设置
 * {@code Content-Disposition} 等响应头后写出，避免临时文件落盘。</p>
 */
public interface ExcelExportService {

    /**
     * 导出当前登录学生的成绩单。
     *
     * @param semesterId 学期ID，为空表示全部学期
     * @return Excel 文件字节流
     */
    byte[] exportStudentGradeReport(Long semesterId);

    /**
     * 导出指定课程的成绩录入单（含草稿）。
     *
     * @param courseId 课程ID
     * @return Excel 文件字节流
     */
    byte[] exportCourseGradeSheet(Long courseId);

    /**
     * 导出指定课程的选课名单。
     *
     * @param courseId 课程ID
     * @return Excel 文件字节流
     */
    byte[] exportCourseSelections(Long courseId);
}
