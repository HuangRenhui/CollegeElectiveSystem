package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.college.elective.entity.CourseSelection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
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
}
