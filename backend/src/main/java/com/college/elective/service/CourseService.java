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
     * 查询指定课程的选课记录列表（仅含已选状态的记录）。
     *
     * <p>轻量级接口：只返回选课记录本身，不含学生姓名等关联信息，
     * 适用于需要课程学生ID集合的场景；如需展示学生名单请使用
     * {@link com.college.elective.service.CourseSelectionService#pageCourseStudents}。</p>
     *
     * <p>当前前端未调用该接口，保留供后续内部使用。访问权限为
     * 管理员不受限、教师仅限本人授课课程。</p>
     *
     * @param courseId 课程ID
     * @return 该课程的选课记录列表
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
