package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.college.elective.entity.CourseSelection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 选课记录 Mapper。
 */
@Mapper
public interface CourseSelectionMapper extends BaseMapper<CourseSelection> {

    /**
     * 分页查询学生选课记录（含课程、教师、排课信息）。
     */
    IPage<CourseSelection> selectStudentSelections(IPage<CourseSelection> page,
                                                   @Param("studentId") Long studentId,
                                                   @Param("semesterId") Long semesterId,
                                                   @Param("status") Integer status);

    /**
     * 查询教师的课程选课名单。
     */
    IPage<CourseSelection> selectCourseStudents(IPage<CourseSelection> page,
                                                @Param("courseId") Long courseId,
                                                @Param("keyword") String keyword);

    /**
     * 查询指定学期学生已选课程的排课所需课程ID列表。
     */
    List<Long> selectSelectedCourseIds(@Param("studentId") Long studentId,
                                       @Param("semesterId") Long semesterId);

    /**
     * 统计学生已选学分合计。
     */
    BigDecimal sumSelectedCredit(@Param("studentId") Long studentId,
                                 @Param("semesterId") Long semesterId);

    /**
     * 查询某学期选课记录总数。
     */
    Long countBySemester(@Param("semesterId") Long semesterId);

    /**
     * 批量查询所选课程的有效选课学生（一次查出，供缓存预热使用）。
     *
     * <p>入参为空集合时由调用方保证不触发查询，避免生成 {@code IN ()} 非法 SQL。</p>
     *
     * @param courseIds 课程ID集合
     * @return 扁平的「课程ID + 学生ID」记录，由调用方在内存中按课程分组
     */
    List<CourseSelection> selectValidSelectionsByCourseIds(
        @Param("courseIds") Collection<Long> courseIds);
}
