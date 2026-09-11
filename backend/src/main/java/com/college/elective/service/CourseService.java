package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.dto.CourseDTO;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseSchedule;
import com.college.elective.entity.CourseSelection;
import com.college.elective.vo.StatisticsVO;
import com.college.elective.vo.TimetableVO;

import java.util.List;

/**
 * 课程服务。
 */
public interface CourseService extends IService<Course> {

    /**
     * 分页查询课程。
     */
    PageResult<Course> pageCourses(CourseQueryDTO query);

    /**
     * 查询课程详情（含排课明细）。
     */
    Course getCourseDetail(Long id);

    /**
     * 新增课程（含排课、冲突校验）。
     */
    Long createCourse(CourseDTO dto);

    /**
     * 修改课程。
     */
    void updateCourse(CourseDTO dto);

    /**
     * 删除课程（存在有效选课记录时禁止删除）。
     */
    void deleteCourse(Long id);

    /**
     * 更新课程状态（上架/下架/结课）。
     */
    void updateStatus(Long id, Integer status);

    /**
     * 查询课程排课明细。
     */
    List<CourseSchedule> listSchedules(Long courseId);

    /**
     * 查询课程的学生名单（用于成绩录入）。
     */
    List<CourseSelection> listCourseStudents(Long courseId);

    /**
     * 查询某学生某学期的课表。
     */
    List<TimetableVO> getStudentTimetable(Long studentId, Long semesterId);

    /**
     * 查询教师课表。
     */
    List<TimetableVO> getTeacherTimetable(Long teacherId, Long semesterId);

    /**
     * 汇总统计数据。
     */
    StatisticsVO getStatistics(Long semesterId);

    /**
     * 校验排课是否存在教师/教室时间冲突。
     */
    void validateScheduleConflict(CourseDTO dto);
}
