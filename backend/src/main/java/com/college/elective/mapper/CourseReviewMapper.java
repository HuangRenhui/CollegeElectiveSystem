package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.college.elective.dto.CourseReviewQueryDTO;
import com.college.elective.entity.CourseReview;
import com.college.elective.vo.CourseReviewSummaryVO;
import com.college.elective.vo.PendingReviewVO;
import com.college.elective.vo.ReviewStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程评价 Mapper。
 */
@Mapper
public interface CourseReviewMapper extends BaseMapper<CourseReview> {

    /**
     * 查询学生的待评价课程。
     *
     * <p>取该学生「已修完（status = 2）」的选课记录，用 {@code LEFT JOIN course_review}
     * 排除已评价的，避免逐门课程查询评价状态。</p>
     */
    List<PendingReviewVO> selectPendingReviews(@Param("studentId") Long studentId,
                                               @Param("semesterId") Long semesterId);

    /**
     * 查询学生的评价列表（含课程、教师信息）。
     */
    List<CourseReview> selectStudentReviews(@Param("studentId") Long studentId,
                                            @Param("semesterId") Long semesterId);

    /**
     * 查询教师授课课程的评价汇总（仅统计已公开评价）。
     */
    List<CourseReviewSummaryVO> selectTeacherReviewSummary(@Param("teacherId") Long teacherId,
                                                           @Param("semesterId") Long semesterId);

    /**
     * 分页查询评价（管理员端）。
     */
    IPage<CourseReview> selectReviewPage(IPage<CourseReview> page,
                                         @Param("query") CourseReviewQueryDTO query);

    /**
     * 查询指定课程的评价汇总（仅统计非隐藏评价）。
     *
     * @param courseId 课程ID
     * @return 汇总；无评价课程仍返回基础信息与 0 分
     */
    CourseReviewSummaryVO selectCourseReviewSummary(@Param("courseId") Long courseId);

    /**
     * 查询指定课程的公开评价明细（status = 2）。
     */
    List<CourseReview> selectCoursePublishedReviews(@Param("courseId") Long courseId);

    /**
     * 统计评价概览。
     */
    ReviewStatisticsVO selectStatistics(@Param("semesterId") Long semesterId);

    /**
     * 统计某课程「已修完」的学生数（参评率分母）。
     */
    Long countFinishedStudents(@Param("courseId") Long courseId);

    /**
     * 检查学生的选课记录是否已评价。
     */
    Long countBySelectionId(@Param("selectionId") Long selectionId);
}
