package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.dto.CourseReviewDTO;
import com.college.elective.dto.CourseReviewQueryDTO;
import com.college.elective.entity.CourseReview;
import com.college.elective.vo.CourseReviewDetailVO;
import com.college.elective.vo.CourseReviewSummaryVO;
import com.college.elective.vo.CourseReviewVO;
import com.college.elective.vo.PendingReviewVO;
import com.college.elective.vo.ReviewStatisticsVO;

import java.util.List;

/**
 * 教学评价服务（学生评教）。
 *
 * <p>核心业务规则：</p>
 * <ul>
 *   <li><b>参评资格</b>：仅可评价本人「已修完」（{@code course_selection.status = 2}）的课程；</li>
 *   <li><b>一课一评</b>：以 {@code selection_id} 唯一键约束，重复提交返回 6002；</li>
 *   <li><b>匿名保护</b>：{@code anonymous = 1} 时教师端与公开列表隐藏学生身份，
 *       必须在 VO 层过滤，不得直接返回实体；</li>
 *   <li><b>状态流转</b>：已提交(1) → 已公开(2) → 已隐藏(3)；已隐藏评价学生不可修改；</li>
 *   <li><b>管理员操作</b>：隐藏与删除均需填写原因（≥ 2 字），随操作日志落库。</li>
 * </ul>
 *
 * <p>业务详情见 {@code docs/待实现功能.md} 第 4 节。</p>
 */
public interface CourseReviewService extends IService<CourseReview> {

    // ------------------------------ 学生端 ------------------------------

    /**
     * 查询当前学生的待评价课程（已修完且尚未评价）。
     *
     * @param semesterId 学期ID（可选，为空则查询全部学期）
     */
    List<PendingReviewVO> listPendingReviews(Long semesterId);

    /**
     * 提交或修改课程评价。
     *
     * <p>{@code dto.id} 为空表示新增，非空表示修改；修改时仅可操作本人评价。</p>
     *
     * @param dto 评价参数
     * @return 评价ID
     */
    Long submitReview(CourseReviewDTO dto);

    /**
     * 查询当前学生的评价列表。
     */
    List<CourseReviewVO> listMyReviews(Long semesterId);

    /**
     * 查询当前学生对指定课程的评价。
     *
     * @param courseId 课程ID
     * @return 评价；未评价时返回 {@code null}
     */
    CourseReviewVO getMyReview(Long courseId);

    /**
     * 撤回（删除）当前学生的评价。
     *
     * <p>撤回后该课程重新回到待评价列表。已隐藏的评价不可撤回。</p>
     *
     * @param id 评价ID
     */
    void deleteMyReview(Long id);

    // ------------------------------ 课程维度查询 ------------------------------

    /**
     * 查询课程评价汇总（平均分、各维度得分、参评人数）。
     */
    CourseReviewSummaryVO getCourseReviewSummary(Long courseId);

    /**
     * 分页查询课程公开评价列表（匿名脱敏）。
     *
     * @param courseId  课程ID
     * @param pageNum   页码
     * @param pageSize  每页条数
     */
    PageResult<CourseReviewVO> listCourseReviews(Long courseId, Long pageNum, Long pageSize);

    // ------------------------------ 教师端（只读） ------------------------------

    /**
     * 查询当前教师授课课程的评价汇总列表。
     */
    List<CourseReviewSummaryVO> listMyReviewSummary(Long semesterId);

    /**
     * 查询指定课程的评价汇总与公开评价明细。
     *
     * <p>教师仅能查看自己授课课程的评价，越权返回 403。</p>
     */
    CourseReviewDetailVO getCourseReviews(Long courseId);

    // ------------------------------ 管理员端 ------------------------------

    /**
     * 分页查询评价（管理员）。
     */
    PageResult<CourseReviewVO> pageReviews(CourseReviewQueryDTO query);

    /**
     * 变更评价状态（公开 / 隐藏）。
     *
     * @param id     评价ID
     * @param status 目标状态：2-已公开 3-已隐藏
     * @param reason 操作原因（≥ 2 字）
     */
    void updateReviewStatus(Long id, Integer status, String reason);

    /**
     * 删除评价（管理员，物理删除，便于学生重新提交）。
     *
     * @param id     评价ID
     * @param reason 删除原因（≥ 2 字）
     */
    void deleteReview(Long id, String reason);

    /**
     * 查询评价统计。
     */
    ReviewStatisticsVO getReviewStatistics(Long semesterId);
}
