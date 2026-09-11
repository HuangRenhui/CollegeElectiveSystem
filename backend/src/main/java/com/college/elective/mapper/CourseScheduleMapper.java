package com.college.elective.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.college.elective.entity.CourseSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 排课 Mapper。
 */
@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {

    /**
     * 查询课程排课明细（含教室信息）。
     */
    List<CourseSchedule> selectByCourseId(@Param("courseId") Long courseId);

    /**
     * 批量查询多门课程的排课明细（学生课表）。
     */
    List<CourseSchedule> selectByCourseIds(@Param("courseIds") List<Long> courseIds);

    /**
     * 查询某学生在指定学期已选课程的排课明细（用于时间冲突检测）。
     */
    List<CourseSchedule> selectByStudentSelection(@Param("studentId") Long studentId,
                                                  @Param("semesterId") Long semesterId);

    /**
     * 统计教师在同一时间段是否已有排课（教师时间冲突检测）。
     */
    int countTeacherConflict(@Param("teacherId") Long teacherId,
                             @Param("semesterId") Long semesterId,
                             @Param("excludeCourseId") Long excludeCourseId,
                             @Param("dayOfWeek") Integer dayOfWeek,
                             @Param("startSection") Integer startSection,
                             @Param("endSection") Integer endSection,
                             @Param("startWeek") Integer startWeek,
                             @Param("endWeek") Integer endWeek);

    /**
     * 统计教室在同一时间段是否已被占用。
     */
    int countClassroomConflict(@Param("classroomId") Long classroomId,
                               @Param("semesterId") Long semesterId,
                               @Param("excludeCourseId") Long excludeCourseId,
                               @Param("dayOfWeek") Integer dayOfWeek,
                               @Param("startSection") Integer startSection,
                               @Param("endSection") Integer endSection,
                               @Param("startWeek") Integer startWeek,
                               @Param("endWeek") Integer endWeek);
}
