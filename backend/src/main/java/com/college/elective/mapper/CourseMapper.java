package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.vo.StatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程 Mapper。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 分页查询课程（联表教师、院系、学期）。
     */
    IPage<Course> selectCoursePage(IPage<Course> page, @Param("query") CourseQueryDTO query);

    /**
     * 查询课程详情（含关联信息）。
     */
    Course selectCourseDetail(@Param("id") Long id);

    /**
     * 数据库侧扣减余量（兜底方案，Redis 为准）。
     */
    int increaseSelectedCount(@Param("courseId") Long courseId);

    /**
     * 数据库侧归还余量。
     */
    int decreaseSelectedCount(@Param("courseId") Long courseId);

    /**
     * 同步已选人数为真实选课数。
     */
    int syncSelectedCount(@Param("courseId") Long courseId);

    /**
     * 查询选课热度排行。
     */
    List<StatisticsVO.CourseRankVO> selectHotCourses(@Param("semesterId") Long semesterId,
                                                     @Param("limit") Integer limit);

    /**
     * 按院系统计选课分布。
     */
    List<StatisticsVO.ChartItemVO> selectDeptDistribution(@Param("semesterId") Long semesterId);
}
