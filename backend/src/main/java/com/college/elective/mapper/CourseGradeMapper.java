package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseGrade;
import com.college.elective.vo.GpaTrendVO;
import com.college.elective.vo.StatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 成绩 Mapper。
 */
@Mapper
public interface CourseGradeMapper extends BaseMapper<CourseGrade> {

    /**
     * 分页查询成绩（按学生或课程维度）。
     */
    IPage<CourseGrade> selectGradePage(IPage<CourseGrade> page,
                                       @Param("studentId") Long studentId,
                                       @Param("courseId") Long courseId,
                                       @Param("semesterId") Long semesterId,
                                       @Param("status") Integer status);

    /**
     * 查询学生某学期成绩单明细。
     */
    List<CourseGrade> selectStudentGrades(@Param("studentId") Long studentId,
                                          @Param("semesterId") Long semesterId,
                                          @Param("publishedOnly") boolean publishedOnly);

    /**
     * 按分数段统计成绩分布。
     */
    List<StatisticsVO.ChartItemVO> selectScoreDistribution(@Param("courseId") Long courseId,
                                                           @Param("semesterId") Long semesterId);

    /**
     * 查询学生的 GPA 趋势（按学期聚合，升序）。
     *
     * <p>仅统计已发布（status = 1）且有总评/绩点的记录，口径与
     * {@code CourseGradeService#getStudentGradeReport} 一致。</p>
     *
     * @param studentId 学生ID
     * @return 各学期趋势点，无数据时返回空列表
     */
    List<GpaTrendVO.TrendPoint> selectGpaTrend(@Param("studentId") Long studentId);

    /**
     * 批量查询多门课程的历史平均成绩。
     *
     * <p>仅统计已发布成绩（status = 1）；无已发布成绩的课程不会出现在结果中，
     * 由调用方补 {@code null}（前端据此隐藏标签，而非显示 0 分）。</p>
     *
     * @param courseIds 课程ID集合，为空时应短路返回，避免 SQL 出现 {@code IN ()}
     * @return 每门课程的平均成绩，只包含有数据的课程
     */
    List<Course> selectCourseAvgScores(@Param("courseIds") Collection<Long> courseIds);
}
