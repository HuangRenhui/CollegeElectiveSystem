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
     *
     * <p>同一课程ID若在不同学期存在排课记录，需传入 {@code semesterId} 精确限定，
     * 否则会查出其他学期的排课。该参数可为 {@code null}，此时不加学期过滤。</p>
     *
     * @param courseIds  课程ID列表
     * @param semesterId 学期ID，可为 {@code null}
     */
    List<CourseSchedule> selectByCourseIds(@Param("courseIds") List<Long> courseIds,
                                           @Param("semesterId") Long semesterId);

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
