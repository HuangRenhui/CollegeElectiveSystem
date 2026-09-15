package com.college.elective.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.PageResult;
import com.college.elective.common.PendingImplementation;
import com.college.elective.common.ResultCode;
import com.college.elective.dto.GradeInputDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseGrade;
import com.college.elective.mapper.CourseGradeMapper;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseGradeService;
import com.college.elective.vo.GradeReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * 成绩服务实现。
 *
 * <p><b>当前为骨架实现</b>：所有方法体仅包含 TODO 占位，尚未实现具体业务逻辑。
 * 请参考项目文档 {@code docs/待实现功能.md} 的「成绩模块」章节逐步补全。</p>
 *
 * <h3>核心业务规则</h3>
 * <ul>
 *   <li><b>成绩构成</b>：总评 = 平时成绩 × 30% + 期末成绩 × 70%；单边缺失时按已有成绩折算</li>
 *   <li><b>绩点换算</b>：4.0 制分段映射（≥90 为 4.0，&lt;60 为 0，区间内分档）</li>
 *   <li><b>状态流转</b>：草稿(0) → 已发布(1) → 已归档(2)；已发布/已归档不可直接修改</li>
 *   <li><b>发布约束</b>：存在未录入总评成绩的学生时禁止发布</li>
 *   <li><b>数据联动</b>：发布后将 course_selection.status 置为 2，并回写成绩到 selection.score</li>
 *   <li><b>权限校验</b>：教师仅能操作自己授课课程的成绩（管理员除外）</li>
 * </ul>
 *
 * <h3>建议注入的依赖</h3>
 * <pre>{@code
 * private final CourseMapper courseMapper;
 * private final CourseSelectionMapper selectionMapper;
 * private final SemesterMapper semesterMapper;
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseGradeServiceImpl extends ServiceImpl<CourseGradeMapper, CourseGrade>
        implements CourseGradeService, PendingImplementation {

    /** 课程 Mapper：查询课程详情、校验成绩操作权限 */
    private final CourseMapper courseMapper;

    // TODO 待注入依赖（实现成绩录入/发布/撤回/审核时启用）
    //  private final CourseSelectionMapper selectionMapper;
    //  private final SemesterMapper semesterMapper;

    @Override
    public void inputGrades(GradeInputDTO dto) {
        // TODO 实现教师批量录入成绩（保存为草稿）
        //  1. 校验 courseId 非空，查询课程并做教师权限校验（教师仅能操作自己的课程）
        //  2. 遍历 dto.getItems()：
        //     - 校验成绩范围，超出 0-100 抛 4003
        //     - 定位选课记录：优先 selectionId，其次 studentId（需 courseId + status = 1）
        //     - 查询已有成绩记录：
        //       已发布/已归档 -> 抛 4002；新增则初始化并置 status = 0（草稿）
        //     - 计算总评与绩点，设置 inputTime / inputBy
        //     - 保存（saveOrUpdate）
        //     - 同步：将总评回写 course_selection.score，便于学生端展示
        throw new UnsupportedOperationException("TODO：批量录入成绩 尚未实现，请参考 docs/待实现功能.md");
    }

    @Override
    public void publishGrades(Long courseId) {
        // TODO 实现发布课程成绩
        //  1. 查询课程并做教师权限校验
        //  2. 查询该课程草稿状态的成绩记录，为空则抛「暂无可发布的成绩记录」
        //  3. 校验是否存在总评成绩为 null 的记录，存在则抛
        //     「还有 N 名学生未录入成绩，无法发布」
        //  4. 批量置为已发布（status = 1）并设置 publishTime
        //  5. 数据联动：将对应选课记录 status 由 1（已选课）改为 2（已修完）
        throw new UnsupportedOperationException("TODO：发布课程成绩 尚未实现，请参考 docs/待实现功能.md");
    }

    @Override
    public void revokeGrades(Long courseId) {
        // TODO 实现撤回成绩发布
        //  1. 查询课程并做教师权限校验
        //  2. 查询已发布状态的成绩记录，为空则提示无数据
        //  3. 批量改回草稿（status = 0）并清空 publishTime
        //  4. 建议同时将选课记录 status 由 2 改回 1，保持数据一致
        throw new UnsupportedOperationException("TODO：撤回成绩发布 尚未实现，请参考 docs/待实现功能.md");
    }

    @Override
    public PageResult<CourseGrade> pageGrades(Long pageNum, Long pageSize, Long studentId,
                                              Long courseId, Long semesterId, Integer status) {
        IPage<CourseGrade> page = baseMapper.selectGradePage(
                new Page<>(pageNum, pageSize), studentId, courseId, semesterId, status);
        return PageResult.of(page);
    }

    @Override
    public GradeReportVO getStudentGradeReport(Long semesterId) {
        Long studentId = SecurityUtils.requireStudentId();

        // 仅统计已发布的成绩
        List<CourseGrade> grades = baseMapper.selectStudentGrades(studentId, semesterId, true);

        GradeReportVO vo = new GradeReportVO();
        vo.setSemesterName(grades.isEmpty() ? null : grades.get(0).getSemesterName());
        vo.setGrades(grades.stream().map(this::toReportItem).toList());

        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal earnedCredit = BigDecimal.ZERO;
        BigDecimal scoreSum = BigDecimal.ZERO;
        BigDecimal pointWeighted = BigDecimal.ZERO;
        BigDecimal pointCredit = BigDecimal.ZERO;
        int scoreCount = 0;

        for (CourseGrade grade : grades) {
            BigDecimal credit = grade.getCredit() == null ? BigDecimal.ZERO : grade.getCredit();
            totalCredit = totalCredit.add(credit);

            boolean pass = isPassed(grade);
            if (pass) {
                earnedCredit = earnedCredit.add(credit);
            }

            if (grade.getTotalScore() != null) {
                scoreSum = scoreSum.add(grade.getTotalScore());
                scoreCount++;
            }

            // 平均绩点按「绩点 × 学分」的加权方式计算，仅统计已修完且有成绩的课程
            if (grade.getGradePoint() != null) {
                pointWeighted = pointWeighted.add(grade.getGradePoint().multiply(credit));
                pointCredit = pointCredit.add(credit);
            }
        }

        vo.setTotalCredit(totalCredit.setScale(1, RoundingMode.HALF_UP));
        vo.setEarnedCredit(earnedCredit.setScale(1, RoundingMode.HALF_UP));
        vo.setAverageScore(scoreCount == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : scoreSum.divide(BigDecimal.valueOf(scoreCount), 2, RoundingMode.HALF_UP));
        vo.setAverageGradePoint(pointCredit.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : pointWeighted.divide(pointCredit, 2, RoundingMode.HALF_UP));

        return vo;
    }

    @Override
    public List<CourseGrade> listCourseGradeSheet(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        checkGradeAccess(course);

        // 取该课程下全部成绩记录（不分页），供成绩录入单展示
        IPage<CourseGrade> page = baseMapper.selectGradePage(
                new Page<>(1, MAX_GRADE_SHEET_SIZE), null, courseId, null, null);
        return page.getRecords();
    }

    /** 成绩录入单一次加载的最大条数 */
    private static final long MAX_GRADE_SHEET_SIZE = 1000L;

    /**
     * 校验当前用户是否有权操作指定课程的成绩。
     *
     * <p>管理员不受限；教师仅能操作自己授课的课程。</p>
     *
     * @param course 课程实体
     */
    private void checkGradeAccess(Course course) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        BusinessException.throwIf(!loginUser.isTeacher()
                        || !Objects.equals(course.getTeacherId(), loginUser.getTeacherId()),
                ResultCode.ROLE_NOT_ALLOWED, "无权操作该课程的成绩");
    }

    /**
     * 将成绩实体转换为成绩单条目。
     */
    private GradeReportVO.CourseGrade toReportItem(CourseGrade grade) {
        GradeReportVO.CourseGrade item = new GradeReportVO.CourseGrade();
        item.setCourseCode(grade.getCourseCode());
        item.setCourseName(grade.getCourseName());
        item.setCredit(grade.getCredit());
        item.setUsualScore(grade.getUsualScore());
        item.setExamScore(grade.getExamScore());
        item.setTotalScore(grade.getTotalScore());
        item.setGradePoint(grade.getGradePoint());
        item.setPass(isPassed(grade));
        return item;
    }

    /**
     * 判断成绩是否及格。
     *
     * <p>优先依据 {@code isPass} 字段，缺失时按总评成绩是否达到 60 分推断。</p>
     */
    private boolean isPassed(CourseGrade grade) {
        if (grade.getIsPass() != null) {
            return grade.getIsPass() == 1;
        }
        return grade.getTotalScore() != null
                && grade.getTotalScore().compareTo(PASS_SCORE) >= 0;
    }

    /** 及格分数线 */
    private static final BigDecimal PASS_SCORE = new BigDecimal("60");
}
