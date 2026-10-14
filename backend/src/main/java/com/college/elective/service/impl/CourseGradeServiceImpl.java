package com.college.elective.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.*;
import com.college.elective.dto.GradeInputDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseGrade;
import com.college.elective.entity.CourseSelection;
import com.college.elective.entity.Student;
import com.college.elective.mapper.CourseGradeMapper;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.CourseSelectionMapper;
import com.college.elective.mapper.StudentMapper;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseGradeService;
import com.college.elective.service.MessageNoticeService;
import com.college.elective.service.MessageSubscribeService;
import com.college.elective.vo.GpaTrendVO;
import com.college.elective.vo.GradeReportVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成绩服务实现。
 *
 * <h3>能力范围</h3>
 * <p>本类承担两类职责：</p>
 * <ul>
 *   <li><b>查询类</b>：{@link #pageGrades}（成绩分页）、
 *       {@link #listCourseGradeSheet}（录入单，含教师归属校验）、
 *       {@link #getStudentGradeReport}（成绩单与绩点统计）</li>
 *   <li><b>录入与发布</b>：{@link #inputGrades}（批量录入，草稿）、
 *       {@link #publishGrades}（草稿 → 已发布）、
 *       {@link #revokeGrades}（已发布 → 草稿）</li>
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
 * <h3>状态联动的连带影响</h3>
 * <p>成绩与选课记录通过 {@code course_grade.selection_id}（唯一键）一对一关联。
 * 录入阶段会把总评回写 {@code course_selection.score} 供学生端快速展示；
 * 发布与撤回阶段联动 {@code course_selection.status}（1-已选课 ⇄ 2-已修完），
 * 而该状态同时是<b>教学评价「参评资格」</b>的判定依据，也是「我的选课」页
 * 「去评价」按钮的显示条件——<b>改动联动逻辑时需一并考虑评价模块</b>。</p>
 *
 * <h3>实现要点</h3>
 * <ul>
 *   <li><b>总评与绩点由后端计算</b>：DTO 不接收总评字段，杜绝前端篡改为不合理高分</li>
 *   <li><b>清空字段须用 {@code LambdaUpdateWrapper.set(..., null)}</b>：
 *       {@code updateById} 默认忽略 null，无法把 {@code publishTime} / {@code dropTime} 置空</li>
 *   <li><b>批量操作</b>：成绩明细逐条处理，联动选课时用 {@code IN} 一次批量更新</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseGradeServiceImpl extends ServiceImpl<CourseGradeMapper, CourseGrade>
    implements CourseGradeService {

    /**
     * 课程 Mapper：查询课程详情、校验成绩操作权限
     */
    private final CourseMapper courseMapper;

    /**
     * 选课 Mapper：定位选课记录、联动选课状态
     */
    private final CourseSelectionMapper selectionMapper;

    /**
     * 订阅消息服务：成绩发布后向学生推送提醒（未配置模板时自动跳过）
     */
    private final MessageSubscribeService subscribeService;

    /**
     * 学生 Mapper：把学生表主键转换为 sys_user.id，供站内信定位接收人
     */
    private final StudentMapper studentMapper;

    /**
     * 站内信服务：成绩发布后批量通知学生（站内可见，不依赖微信配置）
     */
    private final MessageNoticeService messageNoticeService;
    /**
     * 及格分数线
     */
    private static final BigDecimal PASS_SCORE = new BigDecimal("60");

    /**
     * 教师批量录入成绩（保存为草稿）。
     *
     * <p><b>录入流程</b>：</p>
     * <ol>
     *   <li>校验 {@code courseId} 非空，查询课程并做教师权限校验
     *       （管理员不受限，教师仅能操作自己授课的课程，越权返回 4004）</li>
     *   <li>遍历 {@code dto.getItems()}：
     *     <ul>
     *   <li>校验成绩范围（0-100），越界抛 4003；允许为 {@code null}（该项暂未录入）</li>
     *       <li>定位选课记录：优先 {@code selectionId}，其次 {@code studentId} + 本课程</li>
     *       <li>查询已有成绩（{@code selectionId} 唯一）：已发布/已归档抛 4002；
     *  新增时初始化关联字段并置 {@code status = 0}（草稿）</li>
     *       <li>由<b>后端</b>计算总评与绩点（DTO 不接收总评，杜绝前端篡改）</li>
     *    <li>设置 {@code inputTime}、{@code inputBy} 后保存</li>
     *       <li>把总评回写 {@code course_selection.score}，供学生端快速展示</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <p><b>整体事务</b>：任一条明细校验失败即整体回滚，避免出现「部分录入」的中间态。</p>
     *
     * @param dto 成绩录入参数（批量）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inputGrades(GradeInputDTO dto) {
        Long courseId = dto.getCourseId();
        BusinessException.throwIf(courseId == null, ResultCode.PARAM_ERROR);

        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);

        checkGradeAccess(course);

        Long operatorId = SecurityUtils.getLoginUser().getUserId();
        LocalDateTime now = LocalDateTime.now();
        Long semesterId = course.getSemesterId();

        for (GradeInputDTO.GradeItemDTO item : dto.getItems()) {
            // 1. 取值校验：允许 null（表示该项暂未录入），非 null 时必须在 0-100
            checkScoreRange(item.getUsualScore(), "平时成绩");
            checkScoreRange(item.getExamScore(), "期末成绩");

            // 2. 定位选课记录：优先 selectionId，其次 studentId + 本课程
            CourseSelection selection = locateSelection(item, courseId);
            BusinessException.throwIf(selection == null, ResultCode.DATA_NOT_FOUND, "未找到对应的选课记录");
            // 3. 查询是否已有成绩（selectionId 唯一）
            CourseGrade existing = baseMapper.selectOne(Wrappers.<CourseGrade>lambdaQuery()
                .eq(CourseGrade::getSelectionId, selection.getId()));

            // 已发布/已归档不允许修改，需先撤回
            BusinessException.throwIf(existing != null
                    && !Constants.GRADE_STATUS_DRAFT.equals(existing.getStatus()),
                ResultCode.GRADE_ALREADY_PUBLISHED);

            CourseGrade grade = existing != null ? existing : new CourseGrade();
            if (existing == null) {
                grade.setCourseId(courseId);
                grade.setSemesterId(semesterId);
                grade.setSelectionId(selection.getId());
                grade.setStatus(Constants.GRADE_STATUS_DRAFT);
                grade.setStudentId(selection.getStudentId());
            }

            // 4. 转换成绩（Double → BigDecimal，保留 1 位小数）
            BigDecimal usual = toScore(item.getUsualScore());
            BigDecimal exam = toScore(item.getExamScore());
            grade.setUsualScore(usual);
            grade.setExamScore(exam);

            // 5. 总评与绩点由后端计算，防止前端篡改
            BigDecimal total = calcTotalScore(usual, exam);
            grade.setTotalScore(total);
            grade.setGradePoint(calcGradePoint(total));
            grade.setIsPass(total != null && total.compareTo(BigDecimal.valueOf(Constants.PASS_SCORE)) >= 0 ? 1 : 0);
            grade.setRemark(item.getRemark());
            grade.setInputBy(operatorId);
            grade.setInputTime(now);

            // 6. 保存
            saveOrUpdate(grade);

            // 7. 联动：总评回写 course_selection.score，供学生端快速展示
            selectionMapper.update(null, Wrappers.<CourseSelection>lambdaUpdate()
                .eq(CourseSelection::getId, selection.getId())
                .set(CourseSelection::getScore, total));
        }
        log.info("[成绩录入] 成功 courseId={}, 明细数={}, operatorId={}",
            courseId, dto.getItems().size(), operatorId);
    }

    /**
     * 发布课程成绩（草稿 → 已发布）。
     *
     * <p><b>发布流程</b>：</p>
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
    @Transactional(rollbackFor = Exception.class)
    public void publishGrades(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        checkGradeAccess(course);
        // 1. 取该课程草稿状态的成绩记录
        List<CourseGrade> drafts = baseMapper.selectList(Wrappers.<CourseGrade>lambdaQuery()
            .eq(CourseGrade::getCourseId, courseId)
            .eq(CourseGrade::getStatus, Constants.GRADE_STATUS_DRAFT));
        BusinessException.throwIf(drafts.isEmpty(), ResultCode.DATA_NOT_FOUND,
            "暂无可发布的成绩记录");

        // 2. 完整性校验（两重，缺一不可）
        // 2.1 草稿记录中仍有总评为 null 的，禁止发布
        long missing = drafts.stream().filter(g -> g.getTotalScore() == null).count();
        BusinessException.throwIf(missing > 0, ResultCode.PARAM_ERROR,
            String.format("还有 %d 名学生未录入成绩，无法发布", missing));

        // 2.2 已选课学生必须都有成绩记录
        //     只校验 2.1 是不够的：若某学生从未被录入过成绩，他根本不会出现在
        //     drafts 中，「草稿内无空 totalScore」这条就形同虚设，发布后会出现
        //     「已选课却查不到成绩」的情况。故需比对「已选课学生 − 已有成绩记录」差集。
        List<Long> selectedSelectionIds = selectionMapper.selectList(
                Wrappers.<CourseSelection>lambdaQuery()
                    .eq(CourseSelection::getCourseId, courseId)
                    .in(CourseSelection::getStatus,
                        Constants.SELECTION_SELECTED, Constants.SELECTION_FINISHED))
            .stream().map(CourseSelection::getId).toList();
        Set<Long> gradedSelectionIds = drafts.stream()
            .map(CourseGrade::getSelectionId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        long ungraded = selectedSelectionIds.stream()
            .filter(id -> !gradedSelectionIds.contains(id))
            .count();
        BusinessException.throwIf(ungraded > 0, ResultCode.PARAM_ERROR,
            String.format("还有 %d 名已选课学生没有成绩记录，无法发布", ungraded));

        LocalDateTime now = LocalDateTime.now();

        // 3. 批量置为已发布
        baseMapper.update(null, Wrappers.<CourseGrade>lambdaUpdate()
            .eq(CourseGrade::getCourseId, courseId)
            .eq(CourseGrade::getStatus, Constants.GRADE_STATUS_DRAFT)
            .set(CourseGrade::getStatus, Constants.GRADE_STATUS_PUBLISHED)
            .set(CourseGrade::getPublishTime, now));

        // 4. 数据联动：对应选课记录 status 1（已选课）→ 2（已修完）
        //    注意：status = 2 是教学评价「参评资格」的判定依据，
        //    也是「我的选课」页「去评价」按钮的显示条件
        List<Long> selectionIds = drafts.stream().map(CourseGrade::getSelectionId)
            .filter(Objects::nonNull).toList();
        if (!selectionIds.isEmpty()) {
            selectionMapper.update(null, Wrappers.<CourseSelection>lambdaUpdate()
                .in(CourseSelection::getId, selectionIds)
                .set(CourseSelection::getStatus, Constants.SELECTION_FINISHED));
        }

        // 5. 事务提交后推送「成绩发布」订阅消息
        List<Long> studentIds = drafts.stream()
            .map(CourseGrade::getStudentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        notifyGradePublished(course, studentIds);

        log.info("[成绩发布] 成功 courseId={}, 发布数={}", courseId, drafts.size());
    }

    /**
     * 成绩发布后推送订阅消息。
     *
     * <p><b>为什么注册在事务提交后</b>：推送涉及外部 HTTP 调用，
     * 若放在事务内，一旦事务回滚就会出现「消息已发、数据未落库」的不一致；
     * 同时外部调用的耗时也会长时间占用数据库连接。</p>
     *
     * <p>单个学生推送失败不影响其余学生，也不影响成绩发布本身。</p>
     *
     * @param course     已发布成绩的课程
     * @param studentIds 接收提醒的学生ID列表
     */
    private void notifyGradePublished(Course course, List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return;
        }
        // 无活动事务时（如单元测试直接调用）退化为立即执行
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            doPushGradePublished(course, studentIds);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                doPushGradePublished(course, studentIds);
            }
        });
    }

    /**
     * 实际执行推送。
     *
     * <p>模板字段键（thing1 / thing2 等）由微信公众平台申请模板时确定，
     * 若模板变更需同步调整此处。</p>
     */
    private void doPushGradePublished(Course course, List<Long> studentIds) {
        // 批量发站内信：一次事件通知全班，用 sendBatch 避免逐条插入的 N 次 SQL。
        // receiver_id 需要 sys_user.id，而此处 studentIds 是学生表主键，
        // 故先经 studentMapper 批量转换。
        List<Long> userIds = studentIds.stream()
            .map(studentMapper::selectById)
            .filter(s -> s != null && s.getUserId() != null)
            .map(Student::getUserId)
            .toList();
        if (!userIds.isEmpty()) {
            try {
                messageNoticeService.sendBatch(
                    userIds,
                    "GRADE",
                    "成绩已发布",
                    String.format("《%s》的成绩已发布，请前往「我的成绩」查看。", course.getCourseName()),
                    course.getId(),
                    "GRADE");
            } catch (Exception e) {
                log.warn("[成绩发布] 发送站内信失败 courseId={}", course.getId(), e);
            }
        }

        // 微信订阅消息仍按人逐条推送（受「一次授权只能发一条」限制，无法批量）
        for (Long studentId : studentIds) {
            try {
                subscribeService.pushToUser(studentId,
                    MessageSubscribeService.SCENE_GRADE_PUBLISH,
                    Map.of(
                        "thing1", Map.of("value", course.getCourseName()),
                        "thing2", Map.of("value", "成绩已发布")));
            } catch (Exception e) {
                // 推送失败不影响主流程，仅记录
                log.warn("[成绩发布] 推送订阅消息失败 studentId={}", studentId, e);
            }
        }
    }

    /**
     * 撤回成绩发布（已发布 → 草稿）。
     *
     * <p><b>撤回流程</b>：</p>
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
    @Transactional(rollbackFor = Exception.class)
    public void revokeGrades(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        checkGradeAccess(course);

        // 1. 取该课程已发布状态的成绩记录
        List<CourseGrade> published = baseMapper.selectList(Wrappers.<CourseGrade>lambdaQuery()
            .eq(CourseGrade::getCourseId, courseId)
            .eq(CourseGrade::getStatus, Constants.GRADE_STATUS_PUBLISHED));
        BusinessException.throwIf(published.isEmpty(), ResultCode.DATA_NOT_FOUND,
            "暂无已发布的成绩记录");

        // 2. 改回草稿，并清空 publishTime
        //    注意：updateById 会忽略 null 字段，必须用 LambdaUpdateWrapper.set(..., null)
        baseMapper.update(null, Wrappers.<CourseGrade>lambdaUpdate()
            .eq(CourseGrade::getCourseId, courseId)
            .eq(CourseGrade::getStatus, Constants.GRADE_STATUS_PUBLISHED)
            .set(CourseGrade::getStatus, Constants.GRADE_STATUS_DRAFT)
            .set(CourseGrade::getPublishTime, null));

        // 3. 联动：选课记录 status 2（已修完）→ 1（已选课）
        //    否则学生会保留「已修完」状态、继续持有教学评价参评资格，
        //    与「成绩尚未发布」的事实相矛盾
        List<Long> selectionIds = published.stream()
            .map(CourseGrade::getSelectionId)
            .filter(Objects::nonNull)
            .toList();
        if (!selectionIds.isEmpty()) {
            selectionMapper.update(null, Wrappers.<CourseSelection>lambdaUpdate()
                .in(CourseSelection::getId, selectionIds)
                .set(CourseSelection::getStatus, Constants.SELECTION_SELECTED));
        }

        log.info("[成绩撤回] 成功 courseId={}, 撤回数={}", courseId, published.size());

    }

    /**
     * 定位选课记录：优先用 selectionId，其次用 studentId + 本课程（限 status = 1）。
     *
     * <p>两者都取不到时返回 null，由调用方统一报「未找到对应的选课记录」。</p>
     */
    private CourseSelection locateSelection(GradeInputDTO.GradeItemDTO item, Long courseId) {
        if (item.getSelectionId() != null) {
            return selectionMapper.selectById(item.getSelectionId());
        }
        if (item.getStudentId() != null) {
            return selectionMapper.selectOne(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getStudentId, item.getStudentId())
                .eq(CourseSelection::getCourseId, courseId)
                .eq(CourseSelection::getStatus, Constants.SELECTION_SELECTED));
        }
        return null;
    }

    /**
     * 校验成绩取值范围：允许 {@code null}（表示该项暂未录入），非 null 时须在 0-100。
     */
    private void checkScoreRange(Double score, String fieldName) {
        BusinessException.throwIf(score != null && (score < 0 || score > 100),
            ResultCode.GRADE_SCORE_ILLEGAL,
            String.format("%s %s", fieldName, ResultCode.GRADE_SCORE_ILLEGAL.getMessage()));
    }

    /**
     * Double → BigDecimal，保留 1 位小数；入参为 null 时返回 null
     */
    private BigDecimal toScore(Double score) {
        return score == null ? null
            : BigDecimal.valueOf(score).setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 计算总评成绩。
     *
     * <p>规则：平时 × 0.3 + 期末 × 0.7；单边缺失时按已有成绩折算
     * （仅平时 → 平时成绩；仅期末 → 期末成绩）；两边都缺失 → {@code null}。</p>
     */
    private BigDecimal calcTotalScore(BigDecimal usual, BigDecimal exam) {
        if (usual == null && exam == null) {
            return null;
        }
        if (usual == null) {
            return exam.setScale(1, RoundingMode.HALF_UP);
        }
        if (exam == null) {
            return usual.setScale(1, RoundingMode.HALF_UP);
        }
        return usual.multiply(new BigDecimal("0.3"))
            .add(exam.multiply(new BigDecimal("0.7")))
            .setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 绩点换算（4.0 制分段）。
     *
     * @param total 总评成绩，可为 null
     * @return 绩点，总评为 null 时返回 null
     */
    private BigDecimal calcGradePoint(BigDecimal total) {
        if (total == null) {
            return null;
        }
        double score = total.doubleValue();
        double point;
        if (score >= 90) {
            point = 4.0;
        } else if (score >= 85) {
            point = 3.7;
        } else if (score >= 82) {
            point = 3.3;
        } else if (score >= 78) {
            point = 3.0;
        } else if (score >= 75) {
            point = 2.7;
        } else if (score >= 72) {
            point = 2.3;
        } else if (score >= 68) {
            point = 2.0;
        } else if (score >= 64) {
            point = 1.5;
        } else if (score >= 60) {
            point = 1.0;
        } else {
            point = 0.0;
        }
        return BigDecimal.valueOf(point).setScale(1, RoundingMode.HALF_UP);
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

    /**
     * {@inheritDoc}
     *
     * <p>实现要点：</p>
     * <ul>
     *   <li>复用 {@code CourseGradeMapper#selectGpaTrend}，SQL 层完成分组聚合，
     *       不在内存里二次遍历（避免把全部成绩加载到 JVM）</li>
     *   <li>「最高绩点学期」在 Java 侧计算：SQL 已按学期升序返回，
     *       一次遍历即可，无需再排一次序</li>
     *   <li>绩点为 null 的学期（如该学期只有 0 学分课程）在比较时跳过</li>
     * </ul>
     */
    @Override
    public GpaTrendVO getGpaTrend() {
        Long studentId = SecurityUtils.requireStudentId();

        GpaTrendVO vo = new GpaTrendVO();
        List<GpaTrendVO.TrendPoint> points = baseMapper.selectGpaTrend(studentId);
        vo.setPoints(points);
        vo.setSemesterCount(points.size());

        // 找最高绩点的学期（并列时取时间上较早的一个，符合「首次达到最好水平」的直觉）
        BigDecimal best = null;
        for (GpaTrendVO.TrendPoint p : points) {
            if (p.getGradePoint() != null
                    && (best == null || p.getGradePoint().compareTo(best) > 0)) {
                best = p.getGradePoint();
                vo.setBestSemester(p.getSemesterName());
                vo.setBestPoint(best);
            }
        }

        log.debug("[GPA趋势] studentId={}, 学期数={}, 最高绩点={}", studentId, points.size(), best);
        return vo;
    }

    /**
     * 成绩录入单一次加载的最大条数
     */
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
        // 用 NOT_COURSE_TEACHER(4004) 而非 ROLE_NOT_ALLOWED(2007)：
        // 4004 的语义正是「您不是该课程的授课教师」，与前端约定一致；
        // 2007 表示「角色本身无权做这类操作」，用于此处会让前端难以区分
        // 「角色不对」与「不是这门课的教师」两种情况。
        BusinessException.throwIf(!loginUser.isTeacher()
                || !Objects.equals(course.getTeacherId(), loginUser.getTeacherId()),
            ResultCode.NOT_COURSE_TEACHER);
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


}
