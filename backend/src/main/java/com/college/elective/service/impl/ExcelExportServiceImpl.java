package com.college.elective.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.ResultCode;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseGrade;
import com.college.elective.entity.CourseSelection;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseGradeService;
import com.college.elective.service.CourseService;
import com.college.elective.service.ExcelExportService;
import com.college.elective.vo.GradeReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Excel 报表导出服务实现。
 *
 * <h3>导出范围</h3>
 * <ul>
 *   <li>学生成绩单：当前登录学生的已发布成绩，含绩点与学分统计</li>
 *   <li>课程成绩单：指定课程的成绩录入单（含草稿）</li>
 *   <li>选课名单：指定课程的选课学生清单</li>
 * </ul>
 *
 * <h3>实现要点</h3>
 * <ul>
 *   <li>用 <b>Hutool {@code ExcelUtil}</b> 实现。项目已依赖 {@code hutool-all}，
 *       无需引入 EasyExcel / POI，减少依赖面与体积</li>
 *   <li>写入 {@link ByteArrayOutputStream} 后转 {@code byte[]}，
 *       由 Controller 以流返回，<b>不落临时文件</b></li>
 *   <li>复用现有 Service 查询（{@code getStudentGradeReport} /
 *       {@code listCourseGradeSheet} / {@code listCourseStudents}），
 *       保证导出数据与页面展示口径一致，不另写查询</li>
 *   <li>权限校验沿用各 Service 内部逻辑（教师仅能导出本人课程）</li>
 * </ul>
 *
 * <h3>Hutool API 易错点（改这段代码前必读）</h3>
 * <ul>
 *   <li><b>{@code writeCellValue(int x, int y, Object value)} 的 x 是「列」、
 *       y 是「行」</b>——与常见的 {@code (row, col)} 习惯相反，
 *       写错会导致表格整体转置且不报错</li>
 *   <li>{@code writeHeadRow(Iterable)} 写表头（自带标题样式），
 *       {@code writeRow(Iterable)} 写数据行，二者都从「当前行」开始，
 *       写完后当前行自动 +1</li>
 *   <li>{@code merge(int lastColumn, Object content)} 合并的是
 *       <b>当前行</b>的 0 到 lastColumn 列</li>
 *   <li><b>内存中生成必须用 {@code ExcelUtil.getBigWriter()} + {@code flush(out, true)}</b>：
 *       {@code getWriter(OutputStream)} 这个重载并不存在（只支持 String / File），
 *       且 {@code flush(out, true)} 的第二个参数 {@code closeWriter=true}
 *       会同时关闭 writer，无需再单独 {@code close()}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelExportServiceImpl implements ExcelExportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CourseGradeService gradeService;
    private final CourseService courseService;
    private final CourseMapper courseMapper;

    @Override
    public byte[] exportStudentGradeReport(Long semesterId) {
        Long studentId = SecurityUtils.requireStudentId();
        log.info("[Excel导出] 学生成绩单 studentId={}, semesterId={}", studentId, semesterId);

        // 复用成绩单查询逻辑，保证导出与页面展示口径一致
        GradeReportVO report = gradeService.getStudentGradeReport(semesterId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            // ExcelUtil.getWriter(OutputStream) 的重载不存在（只支持 String / File），
            // 内存中生成必须用 getBigWriter() + flush(OutputStream) + close()。
            ExcelWriter writer = ExcelUtil.getBigWriter();
            try {
                writer.merge(5, "学生成绩单"); // 合并当前行（第 0 行）的 0~5 列

                // 统计区：x 为列、y 为行
                writer.writeCellValue(0, 2, "学期");
                writer.writeCellValue(1, 2,
                    Optional.ofNullable(report.getSemesterName()).orElse("全部学期"));
                writer.writeCellValue(0, 3, "总学分");
                writer.writeCellValue(1, 3, report.getTotalCredit());
                writer.writeCellValue(0, 4, "已得学分");
                writer.writeCellValue(1, 4, report.getEarnedCredit());
                writer.writeCellValue(0, 5, "平均分");
                writer.writeCellValue(1, 5, report.getAverageScore());
                writer.writeCellValue(0, 6, "平均绩点");
                writer.writeCellValue(1, 6, report.getAverageGradePoint());

                // 表头 + 数据（writeHeadRow / writeRow 从当前行开始，写后自动 +1）
                writer.setCurrentRow(8);
                writer.writeHeadRow(
                    List.of("课程编号", "课程名称", "学分", "总评成绩", "绩点", "是否及格"));

                for (GradeReportVO.CourseGrade item : report.getGrades()) {
                    // 注意：该内部类字段是 Boolean pass，不是 Integer isPass
                    writer.writeRow(row(
                        item.getCourseCode(),
                        item.getCourseName(),
                        item.getCredit(),
                        item.getTotalScore(),
                        item.getGradePoint(),
                        Boolean.TRUE.equals(item.getPass()) ? "及格" : "不及格"
                    ));
                }

                writer.setColumnWidth(0, 16);
                writer.setColumnWidth(1, 32);
                for (int i = 2; i <= 5; i++) {
                    writer.setColumnWidth(i, 12);
                }

                // flush(out, true) 的第二个参数 closeWriter=true 会同时关闭 writer
                writer.flush(out, true);
            } catch (Exception e) {
                writer.close();
                throw e;
            }
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[Excel导出] 学生成绩单失败 studentId={}", studentId, e);
            throw new BusinessException(ResultCode.FAIL, "成绩单导出失败");
        }
    }

    @Override
    public byte[] exportCourseGradeSheet(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);

        log.info("[Excel导出] 课程成绩单 courseId={}", courseId);
        List<CourseGrade> grades = gradeService.listCourseGradeSheet(courseId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ExcelWriter writer = ExcelUtil.getBigWriter();
            try {
                writer.merge(7, "课程成绩单");

                writer.writeCellValue(0, 2, "课程编号");
                writer.writeCellValue(1, 2, course.getCourseCode());
                writer.writeCellValue(0, 3, "课程名称");
                writer.writeCellValue(1, 3, course.getCourseName());
                writer.writeCellValue(0, 4, "导出时间");
                writer.writeCellValue(1, 4, LocalDateTime.now().format(DATE_TIME));

                writer.setCurrentRow(6);
                writer.writeHeadRow(List.of(
                    "学号", "姓名", "班级", "平时成绩", "期末成绩", "总评成绩", "绩点", "状态"));

                for (CourseGrade g : grades) {
                    writer.writeRow(row(
                        g.getStuNo(),
                        g.getStudentName(),
                        g.getClassName(),
                        g.getUsualScore(),
                        g.getExamScore(),
                        g.getTotalScore(),
                        g.getGradePoint(),
                        gradeStatusText(g.getStatus())
                    ));
                }

                writer.setColumnWidth(0, 14);
                writer.setColumnWidth(1, 14);
                writer.setColumnWidth(2, 24);
                for (int i = 3; i <= 7; i++) {
                    writer.setColumnWidth(i, 12);
                }

                writer.flush(out, true);
            } catch (Exception e) {
                writer.close();
                throw e;
            }
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[Excel导出] 课程成绩单失败 courseId={}", courseId, e);
            throw new BusinessException(ResultCode.FAIL, "课程成绩单导出失败");
        }
    }

    @Override
    public byte[] exportCourseSelections(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);

        log.info("[Excel导出] 选课名单 courseId={}", courseId);
        List<CourseSelection> selections = courseService.listCourseStudents(courseId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ExcelWriter writer = ExcelUtil.getBigWriter();
            try {
                writer.merge(4, "选课名单");

                writer.writeCellValue(0, 2, "课程编号");
                writer.writeCellValue(1, 2, course.getCourseCode());
                writer.writeCellValue(0, 3, "课程名称");
                writer.writeCellValue(1, 3, course.getCourseName());
                writer.writeCellValue(0, 4, "选课人数");
                writer.writeCellValue(1, 4, selections.size());

                writer.setCurrentRow(6);
                writer.writeHeadRow(List.of("学号", "姓名", "班级", "选课时间", "状态"));

                for (CourseSelection s : selections) {
                    writer.writeRow(row(
                        s.getStuNo(),
                        s.getStudentName(),
                        s.getClassName(),
                        Optional.ofNullable(s.getSelectTime())
                            // LocalDateTimeUtil::format 需要额外格式参数，方法引用不匹配，
                            // 这里用 lambda 显式指定格式
                            .map(t -> LocalDateTimeUtil.format(t, DATE_TIME))
                            .orElse(""),
                        selectionStatusText(s.getStatus())
                    ));
                }

                writer.setColumnWidth(0, 14);
                writer.setColumnWidth(1, 14);
                writer.setColumnWidth(2, 24);
                writer.setColumnWidth(3, 22);
                writer.setColumnWidth(4, 12);

                writer.flush(out, true);
            } catch (Exception e) {
                writer.close();
                throw e;
            }
            return out.toByteArray();
        } catch (Exception e) {
            log.error("[Excel导出] 选课名单失败 courseId={}", courseId, e);
            throw new BusinessException(ResultCode.FAIL, "选课名单导出失败");
        }
    }

    /**
     * 成绩状态文本。
     *
     * <p>用 {@code if} 而非 {@code switch}：{@code Constants.GRADE_STATUS_*}
     * 是 {@code Integer}（包装类型），不是编译期常量，
     * 不能用作 {@code switch} 的 case 标签。</p>
     */
    /**
     * 构造数据行。
     *
     * <p><b>为什么不能用 {@code List.of(...)}</b>：{@code List.of} 是
     * <b>不允许 null 元素</b>的不可变集合，遇到 null 会立即抛
     * {@code NullPointerException}。而学生姓名、班级、平时成绩等字段在
     * 数据不完整（学生已删除、成绩未录入）时确实可能为 null，
     * 这会让整份导出直接 500 —— 且只在恰好有脏数据时才复现，很难排查。</p>
     *
     * <p>{@code Arrays.asList} 允许 null，Hutool 会将其写成空单元格。</p>
     */
    private static List<Object> row(Object... cells) {
        return Arrays.asList(cells);
    }

    private String gradeStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        if (status == Constants.GRADE_STATUS_DRAFT) {
            return "草稿";
        }
        if (status == Constants.GRADE_STATUS_PUBLISHED) {
            return "已发布";
        }
        if (status == Constants.GRADE_STATUS_ARCHIVED) {
            return "已归档";
        }
        return "未知";
    }

    /** 选课状态文本 */
    private String selectionStatusText(Integer status) {
        if (status == null) {
            return "";
        }
        if (status == 0) {
            return "已退选";
        }
        if (status == 1) {
            return "已选课";
        }
        if (status == 2) {
            return "已修完";
        }
        return "未知";
    }
}
