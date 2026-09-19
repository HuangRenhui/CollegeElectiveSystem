package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.PageResult;
import com.college.elective.common.ResultCode;
import com.college.elective.dto.CourseReviewDTO;
import com.college.elective.dto.CourseReviewQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseReview;
import com.college.elective.entity.CourseSelection;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.CourseReviewMapper;
import com.college.elective.mapper.CourseSelectionMapper;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseReviewService;
import com.college.elective.vo.CourseReviewDetailVO;
import com.college.elective.vo.CourseReviewSummaryVO;
import com.college.elective.vo.CourseReviewVO;
import com.college.elective.vo.PendingReviewVO;
import com.college.elective.vo.ReviewStatisticsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 教学评价服务实现。
 *
 * <h3>实现要点</h3>
 * <ol>
 *   <li><b>参评资格</b>：以 {@code course_selection} 中 {@code status = 2}（已修完）
 *       的记录作为评价准入凭据，评价与选课记录一对一绑定。</li>
 *   <li><b>匿名脱敏</b>：所有对外返回均经 {@link #toVO} 转换，
 *       匿名评价统一显示「匿名同学」并清空学号，避免实体直出泄露身份。</li>
 *   <li><b>越权防护</b>：学生仅能操作本人评价；教师仅能查看本人授课课程的评价。</li>
 *   <li><b>统计口径</b>：课程平均分仅统计非隐藏评价；参评率分母为该课程已修完人数。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseReviewServiceImpl extends ServiceImpl<CourseReviewMapper, CourseReview>
        implements CourseReviewService {

    /** 课程 Mapper：查询课程详情与授课教师 */
    private final CourseMapper courseMapper;

    /** 选课 Mapper：校验参评资格、取选课记录 */
    private final CourseSelectionMapper selectionMapper;

    /** 匿名评价的展示名称 */
    private static final String ANONYMOUS_NAME = "匿名同学";

    // ============================ 学生端 ============================

    @Override
    public List<PendingReviewVO> listPendingReviews(Long semesterId) {
        Long studentId = SecurityUtils.requireStudentId();
        return baseMapper.selectPendingReviews(studentId, semesterId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitReview(CourseReviewDTO dto) {
        Long studentId = SecurityUtils.requireStudentId();
        Course course = courseMapper.selectCourseDetail(dto.getCourseId());
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);

        // 参评资格：必须存在已修完的选课记录
        CourseSelection selection = findFinishedSelection(studentId, dto.getCourseId());
        BusinessException.throwIf(selection == null, ResultCode.REVIEW_NOT_ALLOWED);

        BigDecimal average = calculateAverage(dto);

        if (dto.getId() == null) {
            return createReview(dto, selection, course, average);
        }
        return updateReview(dto, selection, average);
    }

    /**
     * 新增评价。
     *
     * <p>先用 {@code selection_id} 唯一性做业务校验，最终由数据库唯一索引兜底，
     * 避免并发下重复插入。</p>
     */
    private Long createReview(CourseReviewDTO dto, CourseSelection selection,
                              Course course, BigDecimal average) {
        Long exists = baseMapper.countBySelectionId(selection.getId());
        BusinessException.throwIf(exists != null && exists > 0, ResultCode.REVIEW_ALREADY_EXISTS);

        CourseReview review = new CourseReview();
        review.setSelectionId(selection.getId());
        review.setStudentId(selection.getStudentId());
        review.setCourseId(course.getId());
        review.setTeacherId(course.getTeacherId());
        review.setSemesterId(selection.getSemesterId());
        applyScores(review, dto, average);
        review.setStatus(Constants.REVIEW_STATUS_SUBMITTED);
        review.setSubmitTime(LocalDateTime.now());

        save(review);
        log.info("学生提交课程评价: studentId={}, courseId={}, reviewId={}",
                selection.getStudentId(), course.getId(), review.getId());
        return review.getId();
    }

    /**
     * 修改已提交的评价。
     *
     * <p>三条硬约束：评价必须属于当前学生、必须对应本人已修完的选课记录、
     * 未被管理员隐藏。</p>
     */
    private Long updateReview(CourseReviewDTO dto, CourseSelection selection, BigDecimal average) {
        CourseReview exist = getById(dto.getId());
        BusinessException.throwIf(exist == null, ResultCode.REVIEW_NOT_FOUND);
        BusinessException.throwIf(!Objects.equals(exist.getStudentId(), selection.getStudentId()),
                ResultCode.FORBIDDEN, "无权修改他人的评价");
        BusinessException.throwIf(!Objects.equals(exist.getSelectionId(), selection.getId()),
                ResultCode.REVIEW_NOT_ALLOWED);
        BusinessException.throwIf(Constants.REVIEW_STATUS_HIDDEN.equals(exist.getStatus()),
                ResultCode.REVIEW_HIDDEN);

        CourseReview update = new CourseReview();
        update.setId(exist.getId());
        applyScores(update, dto, average);
        // 修改后重新提交，状态回到「已提交」，需管理员重新公开
        update.setStatus(Constants.REVIEW_STATUS_SUBMITTED);
        update.setSubmitTime(LocalDateTime.now());
        updateById(update);

        log.info("学生修改课程评价: reviewId={}, courseId={}", exist.getId(), exist.getCourseId());
        return exist.getId();
    }

    @Override
    public List<CourseReviewVO> listMyReviews(Long semesterId) {
        Long studentId = SecurityUtils.requireStudentId();
        List<CourseReview> reviews = baseMapper.selectStudentReviews(studentId, semesterId);
        // 本人评价无需匿名处理，保持原样展示，便于确认自己填了什么
        return reviews.stream().map(review -> toVO(review, true)).toList();
    }

    @Override
    public CourseReviewVO getMyReview(Long courseId) {
        Long studentId = SecurityUtils.requireStudentId();
        CourseReview review = getOne(Wrappers.<CourseReview>lambdaQuery()
                .eq(CourseReview::getStudentId, studentId)
                .eq(CourseReview::getCourseId, courseId)
                .last("LIMIT 1"));
        return review == null ? null : toVO(review, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMyReview(Long id) {
        Long studentId = SecurityUtils.requireStudentId();
        CourseReview review = getById(id);
        BusinessException.throwIf(review == null, ResultCode.REVIEW_NOT_FOUND);
        BusinessException.throwIf(!Objects.equals(review.getStudentId(), studentId),
                ResultCode.FORBIDDEN, "无权撤回他人的评价");
        BusinessException.throwIf(Constants.REVIEW_STATUS_HIDDEN.equals(review.getStatus()),
                ResultCode.REVIEW_HIDDEN);

        // 物理删除：CourseReview 未继承 BaseEntity（无 @TableLogic 的 deleted 字段），
        // 因此 removeById 执行真实 DELETE，selection_id 唯一索引随之释放，
        // 学生撤回后可重新提交该课程的评价
        removeById(id);
        log.info("学生撤回课程评价: studentId={}, reviewId={}, courseId={}",
                studentId, id, review.getCourseId());
    }

    // ============================ 课程维度查询 ============================

    @Override
    public CourseReviewSummaryVO getCourseReviewSummary(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        return baseMapper.selectCourseReviewSummary(courseId);
    }

    @Override
    public PageResult<CourseReviewVO> listCourseReviews(Long courseId, Long pageNum, Long pageSize) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);

        IPage<CourseReview> page = page(new Page<>(pageNum, pageSize),
                Wrappers.<CourseReview>lambdaQuery()
                        .eq(CourseReview::getCourseId, courseId)
                        .eq(CourseReview::getStatus, Constants.REVIEW_STATUS_PUBLISHED)
                        .orderByDesc(CourseReview::getAverageScore)
                        .orderByDesc(CourseReview::getSubmitTime));
        // 公开列表对所有人匿名，即使学生本人查看他人评价也不例外
        return PageResult.of(page, review -> toVO(review, false));
    }

    // ============================ 教师端（只读） ============================

    @Override
    public List<CourseReviewSummaryVO> listMyReviewSummary(Long semesterId) {
        Long teacherId = SecurityUtils.requireTeacherId();
        return baseMapper.selectTeacherReviewSummary(teacherId, semesterId);
    }

    @Override
    public CourseReviewDetailVO getCourseReviews(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        checkCourseOwner(course, "无权查看该课程的评价");

        CourseReviewDetailVO vo = new CourseReviewDetailVO();
        vo.setSummary(baseMapper.selectCourseReviewSummary(courseId));
        // 教师端仅可见已公开评价，且一律匿名脱敏
        List<CourseReview> reviews = baseMapper.selectCoursePublishedReviews(courseId);
        vo.setReviews(reviews.stream().map(review -> toVO(review, false)).toList());
        return vo;
    }

    // ============================ 管理员端 ============================

    @Override
    public PageResult<CourseReviewVO> pageReviews(CourseReviewQueryDTO query) {
        IPage<CourseReview> page = baseMapper.selectReviewPage(
                new Page<>(query.getPageNum(), query.getPageSize()), query);
        // 管理员保留完整身份信息用于审计，但仍区分匿名标记供前端展示
        return PageResult.of(page, review -> toVO(review, false, true));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateReviewStatus(Long id, Integer status, String reason) {
        CourseReview review = getById(id);
        BusinessException.throwIf(review == null, ResultCode.REVIEW_NOT_FOUND);
        BusinessException.throwIf(!Constants.REVIEW_STATUS_PUBLISHED.equals(status)
                        && !Constants.REVIEW_STATUS_HIDDEN.equals(status),
                ResultCode.PARAM_ERROR, "仅支持变更为『已公开』或『已隐藏』");
        validateReason(reason);

        CourseReview update = new CourseReview();
        update.setId(id);
        update.setStatus(status);
        updateById(update);

        log.info("管理员变更评价状态: reviewId={}, status={}, reason={}", id, status, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReview(Long id, String reason) {
        CourseReview review = getById(id);
        BusinessException.throwIf(review == null, ResultCode.REVIEW_NOT_FOUND);
        validateReason(reason);

        // 同样为物理删除，保证学生可重新提交该课程评价
        removeById(id);
        log.info("管理员删除评价: reviewId={}, courseId={}, reason={}",
                id, review.getCourseId(), reason);
    }

    @Override
    public ReviewStatisticsVO getReviewStatistics(Long semesterId) {
        ReviewStatisticsVO vo = baseMapper.selectStatistics(semesterId);
        if (vo == null) {
            vo = new ReviewStatisticsVO();
            vo.setReviewCount(0L);
            vo.setPublishedCount(0L);
            vo.setHiddenCount(0L);
            vo.setAverageScore(BigDecimal.ZERO);
            vo.setCourseCount(0L);
        }
        vo.setParticipateRate(calculateParticipateRate(semesterId));
        return vo;
    }

    /**
     * 计算参评率：已评价记录数 / 已修完人次。
     *
     * <p>分母取「已修完人次」而非「已选人次」，否则未修完的学生会稀释参评率，
     * 使指标失去参考意义。</p>
     */
    private Integer calculateParticipateRate(Long semesterId) {
        Long finished = selectionMapper.selectCount(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getStatus, Constants.SELECTION_FINISHED)
                .eq(semesterId != null, CourseSelection::getSemesterId, semesterId));
        if (finished == null || finished == 0) {
            return 0;
        }
        Long reviewed = baseMapper.selectCount(Wrappers.<CourseReview>lambdaQuery()
                .eq(semesterId != null, CourseReview::getSemesterId, semesterId));
        long count = reviewed == null ? 0L : reviewed;
        return (int) Math.round(count * 100.0 / finished);
    }

    // ============================ 内部方法 ============================

    /**
     * 查询学生某门课程「已修完」的选课记录。
     *
     * @return 选课记录；不存在或未修完时返回 {@code null}
     */
    private CourseSelection findFinishedSelection(Long studentId, Long courseId) {
        return selectionMapper.selectOne(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getCourseId, courseId)
                .eq(CourseSelection::getStatus, Constants.SELECTION_FINISHED)
                .last("LIMIT 1"));
    }

    /**
     * 校验当前用户是否有权查看指定课程的评价。
     *
     * <p>管理员不受限；教师仅能查看自己授课的课程。</p>
     */
    private void checkCourseOwner(Course course, String message) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        BusinessException.throwIf(!loginUser.isTeacher()
                        || !Objects.equals(course.getTeacherId(), loginUser.getTeacherId()),
                ResultCode.ROLE_NOT_ALLOWED, message);
    }

    /**
     * 校验分数取值与文字长度。
     *
     * <p>{@code @DecimalMin/@DecimalMax} 已在 DTO 层拦截常规越界，
     * 此处兜底防御绕过校验的调用（如内部服务调用）。</p>
     */
    private void applyScores(CourseReview review, CourseReviewDTO dto, BigDecimal average) {
        review.setScoreContent(validateScore(dto.getScoreContent(), "教学内容"));
        review.setScoreTeaching(validateScore(dto.getScoreTeaching(), "教学方法"));
        review.setScoreAttitude(validateScore(dto.getScoreAttitude(), "教学态度"));
        review.setScoreGain(validateScore(dto.getScoreGain(), "学习收获"));
        review.setAverageScore(average);

        String content = StrUtil.trim(dto.getContent());
        if (StrUtil.isNotBlank(content)) {
            BusinessException.throwIf(content.length() < Constants.REVIEW_MIN_CONTENT_LENGTH
                            || content.length() > Constants.REVIEW_MAX_CONTENT_LENGTH,
                    ResultCode.REVIEW_CONTENT_ILLEGAL);
        }
        review.setContent(content);
        review.setAnonymous(dto.getAnonymous() == null
                ? Constants.REVIEW_ANONYMOUS_YES : dto.getAnonymous());
    }

    /**
     * 校验单个评价维度得分是否落在 1-5 区间。
     */
    private BigDecimal validateScore(BigDecimal score, String dimension) {
        BusinessException.throwIf(score == null, ResultCode.REVIEW_CONTENT_ILLEGAL,
                "请为" + dimension + "打分");
        BusinessException.throwIf(
                score.compareTo(BigDecimal.valueOf(Constants.REVIEW_MIN_SCORE)) < 0
                        || score.compareTo(BigDecimal.valueOf(Constants.REVIEW_MAX_SCORE)) > 0,
                ResultCode.REVIEW_CONTENT_ILLEGAL,
                dimension + "评分需在 " + Constants.REVIEW_MIN_SCORE
                        + "-" + Constants.REVIEW_MAX_SCORE + " 分之间");
        return score.setScale(1, RoundingMode.HALF_UP);
    }

    /**
     * 计算综合评分：四个维度的算术平均，保留 1 位小数。
     *
     * <p>综合分由后端计算而非接收前端入参，避免被篡改为不合理的高分。</p>
     */
    private BigDecimal calculateAverage(CourseReviewDTO dto) {
        BigDecimal sum = dto.getScoreContent()
                .add(dto.getScoreTeaching())
                .add(dto.getScoreAttitude())
                .add(dto.getScoreGain());
        return sum.divide(BigDecimal.valueOf(Constants.REVIEW_DIMENSION_COUNT),
                1, RoundingMode.HALF_UP);
    }

    /**
     * 校验管理员操作原因。
     */
    private void validateReason(String reason) {
        BusinessException.throwIf(StrUtil.isBlank(reason)
                        || StrUtil.trim(reason).length() < Constants.REVIEW_REASON_MIN_LENGTH,
                ResultCode.PARAM_ERROR,
                "请填写操作原因（不少于 " + Constants.REVIEW_REASON_MIN_LENGTH + " 个字）");
    }

    /**
     * 实体转展示对象。
     *
     * @param review  评价实体
     * @param keepOwn 是否保留本人身份（学生查看自己的评价时为 true）
     */
    private CourseReviewVO toVO(CourseReview review, boolean keepOwn) {
        return toVO(review, keepOwn, false);
    }

    /**
     * 实体转展示对象。
     *
     * @param review       评价实体
     * @param keepOwn      是否保留本人身份（学生查看自己的评价时为 true）
     * @param keepIdentity 是否保留完整身份（仅管理员审计时为 true，学号下发）
     */
    private CourseReviewVO toVO(CourseReview review, boolean keepOwn, boolean keepIdentity) {
        if (review == null) {
            return null;
        }
        CourseReviewVO vo = new CourseReviewVO();
        vo.setId(review.getId());
        vo.setCourseId(review.getCourseId());
        vo.setCourseCode(review.getCourseCode());
        vo.setCourseName(review.getCourseName());
        vo.setTeacherName(review.getTeacherName());
        vo.setSemesterName(review.getSemesterName());
        vo.setScoreContent(review.getScoreContent());
        vo.setScoreTeaching(review.getScoreTeaching());
        vo.setScoreAttitude(review.getScoreAttitude());
        vo.setScoreGain(review.getScoreGain());
        vo.setAverageScore(review.getAverageScore());
        vo.setContent(review.getContent());
        vo.setAnonymous(review.getAnonymous());
        vo.setStatus(review.getStatus());
        vo.setSubmitTime(review.getSubmitTime());

        boolean anonymous = Constants.REVIEW_ANONYMOUS_YES.equals(review.getAnonymous());
        if (keepOwn || keepIdentity) {
            vo.setStudentName(review.getStudentName());
            // 匿名评价对教师与同学隐藏学号，仅管理员审计时保留
            vo.setStuNo(keepIdentity ? review.getStuNo() : null);
        } else {
            vo.setStudentName(anonymous
                    ? ANONYMOUS_NAME : StrUtil.blankToDefault(review.getStudentName(), ANONYMOUS_NAME));
            // 非本人、非管理员场景一律不下发学号
            vo.setStuNo(null);
        }
        return vo;
    }
}
