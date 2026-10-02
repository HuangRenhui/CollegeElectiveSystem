package com.college.elective.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.PageResult;
import com.college.elective.common.Pages;
import com.college.elective.common.PendingFeature;
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
 * <h3>实现状态</h3>
 * <ul>
 *   <li><b>已实现</b>：{@link #pageGrades}、{@link #listCourseGradeSheet}、
 *       {@link #getStudentGradeReport}——成绩查询、录入单与成绩单统计。</li>
 *   <li><b>待实现</b>：{@link #inputGrades}、{@link #publishGrades}、{@link #revokeGrades}
 *       ——成绩录入与发布。三者经由 {@link PendingFeature#unsupported(String)}
 *       抛出统一业务异常（业务码 5006），不再抛运行时异常，
 *       因此本类已从 {@code PendingImplementation} 摘除，
 *       Controller 可以直接调用上面已实现的方法。</li>
 * </ul>
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
 * <h3>与其他组件的协作</h3>
 * <p>成绩与选课记录通过 {@code course_grade.selection_id}（唯一键）一对一关联。
 * 录入阶段会把总评回写 {@code course_selection.score} 供学生端快速展示；
 * 发布与撤回阶段联动 {@code course_selection.status}（1-已选课 ⇄ 2-已修完），
 * 而该状态同时是教学评价「参评资格」的判定依据——改动联动逻辑时需一并考虑评价模块。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseGradeServiceImpl extends ServiceImpl<CourseGradeMapper, CourseGrade>
        implements CourseGradeService {

    /** 课程 Mapper：查询课程详情、校验成绩操作权限 */
    private final CourseMapper courseMapper;

    // TODO 待注入依赖（实现成绩录入/发布/撤回/审核时启用）
    //  private final CourseSelectionMapper selectionMapper;
    //  private final SemesterMapper semesterMapper;

    /**
     * 教师批量录入成绩（保存为草稿）—— 待实现。
     *
     * <p><b>为什么改抛业务异常而不是 UnsupportedOperationException</b>：
     * 本类已从 {@link PendingImplementation} 摘除，Controller 会直接调用真实实现。
     * 若未实现的方法仍抛运行时异常，会被全局异常处理器包装成 HTTP 500，
     * 破坏「业务异常统一返回 HTTP 200 + 业务码」的既有约定。
     * 改用 {@link PendingFeature#unsupported(String)} 后，前端收到的仍是
     * 明确的业务码 5006，与实现前的表现完全一致，也不会污染错误日志。</p>
     *
     * <p><b>实现要点</b>（完整设计见 {@code docs/待实现功能.md} 的规则 1、2、5）：</p>
     * <ol>
     *   <li>校验 {@code courseId} 非空，查询课程并做教师权限校验
     *       （管理员不受限，教师仅能操作自己授课的课程，越权返回 4004）；</li>
     *   <li>遍历 {@code dto.getItems()}：
     *     <ul>
     *       <li>校验成绩范围（0-100），越界抛 4003；允许为 {@code null}（表示该项暂未录入）；</li>
     *       <li>定位选课记录：优先按 {@code selectionId}，其次按 {@code studentId} + 本课程；</li>
     *       <li>查询已有成绩（按 {@code selectionId} 唯一）：已发布/已归档抛 4002，
     *           新增时初始化 {@code studentId}/{@code courseId}/{@code semesterId} 并置
     *           {@code status = 0}（草稿）；</li>
     *       <li>由<b>后端</b>计算总评与绩点（DTO 不接收总评，防止前端篡改为不合理高分）；</li>
     *       <li>设置 {@code inputTime}、{@code inputBy} 后保存（{@code saveOrUpdate}）；</li>
     *       <li>把总评回写 {@code course_selection.score}，便于学生端列表快速展示。</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param dto 成绩录入参数（批量）
     */
    @Override
    public void inputGrades(GradeInputDTO dto) {
        throw PendingFeature.unsupported("批量录入成绩");
    }

    /**
     * 发布课程成绩（草稿 → 已发布）—— 待实现。
     *
     * <p>与 {@link #inputGrades} 同理，此处抛业务异常（5006）而非运行时异常，
     * 保证摘除待实现标记后接口仍返回规范的业务提示。</p>
     *
     * <p><b>实现要点</b>（完整设计见 {@code docs/待实现功能.md} 规则 3、6）：</p>
     * <ol>
     *   <li>查询课程并做教师权限校验；</li>
     *   <li>取该课程草稿状态的成绩记录，为空则提示「暂无可发布的成绩记录」；</li>
     *   <li><b>完整性校验</b>：若仍有记录的总评为 {@code null}，抛
     *       「还有 N 名学生未录入成绩，无法发布」——避免学生查到「已发布但没分数」；</li>
     *   <li>批量置为已发布（{@code status = 1}）并写入 {@code publishTime}；</li>
     *   <li><b>数据联动</b>：将对应选课记录 {@code status} 由 1（已选课）改为 2（已修完）。
     *       注意 {@code status = 2} 是教学评价的参评资格依据，也是「我的选课」页
     *       「去评价」按钮的显示条件；</li>
     *   <li>副作用提示：选课记录变为 2 后，课表查询与时间冲突检测（均只查
     *       {@code status = 1}）将不再包含这些课程——这是符合预期的，课程已结束。</li>
     * </ol>
     *
     * @param courseId 课程ID
     */
    @Override
    public void publishGrades(Long courseId) {
        throw PendingFeature.unsupported("发布课程成绩");
    }

    /**
     * 撤回成绩发布（已发布 → 草稿）—— 待实现。
     *
     * <p>与 {@link #inputGrades} 同理，此处抛业务异常（5006）而非运行时异常。</p>
     *
     * <p><b>实现要点</b>（完整设计见 {@code docs/待实现功能.md} 规则 3、7）：</p>
     * <ol>
     *   <li>查询课程并做教师权限校验；</li>
     *   <li>取该课程已发布状态的成绩记录，为空则提示暂无已发布成绩；</li>
     *   <li>批量改回草稿（{@code status = 0}）并清空 {@code publishTime}——
     *       清空操作必须用 {@code LambdaUpdateWrapper.set(..., null)}，
     *       因为 MyBatis-Plus 的 {@code updateById} 会忽略值为 {@code null} 的字段；</li>
     *   <li>同时将对应选课记录 {@code status} 由 2（已修完）改回 1（已选课），
     *       否则学生会保留「已修完」状态、继续持有教学评价的参评资格，
     *       与成绩尚未发布的事实相矛盾。</li>
     * </ol>
     *
     * @param courseId 课程ID
     */
    @Override
    public void revokeGrades(Long courseId) {
        throw PendingFeature.unsupported("撤回成绩发布");
    }

    @Override
    public PageResult<CourseGrade> pageGrades(Long pageNum, Long pageSize, Long studentId,
                                              Long courseId, Long semesterId, Integer status) {
        IPage<CourseGrade> page = baseMapper.selectGradePage(
                new Page<>(Pages.pageNum(pageNum), Pages.pageSize(pageSize)), studentId, courseId, semesterId, status);
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
