package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.PageResult;
import com.college.elective.common.RedisKeys;
import com.college.elective.common.ResultCode;
import com.college.elective.dto.CourseDTO;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.dto.CourseScheduleDTO;
import com.college.elective.entity.Classroom;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseSchedule;
import com.college.elective.entity.CourseSelection;
import com.college.elective.entity.Semester;
import com.college.elective.entity.Student;
import com.college.elective.entity.SysUser;
import com.college.elective.entity.Teacher;
import com.college.elective.mapper.ClassroomMapper;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.CourseScheduleMapper;
import com.college.elective.mapper.CourseSelectionMapper;
import com.college.elective.mapper.DepartmentMapper;
import com.college.elective.mapper.SemesterMapper;
import com.college.elective.mapper.StudentMapper;
import com.college.elective.mapper.SysUserMapper;
import com.college.elective.mapper.TeacherMapper;
import com.college.elective.service.CourseService;
import com.college.elective.vo.StatisticsVO;
import com.college.elective.vo.TimetableVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课程服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

    private final CourseScheduleMapper scheduleMapper;
    private final CourseSelectionMapper selectionMapper;
    private final SemesterMapper semesterMapper;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper userMapper;
    private final ClassroomMapper classroomMapper;
    private final DepartmentMapper departmentMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public PageResult<Course> pageCourses(CourseQueryDTO query) {
        IPage<Course> page = baseMapper.selectCoursePage(new Page<>(query.getPageNum(), query.getPageSize()), query);
        fillRemainingCapacity(page.getRecords());
        return PageResult.of(page);
    }

    /**
     * 用 Redis 实时余量覆盖数据库记录，保证展示与选课结果一致。
     */
    private void fillRemainingCapacity(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return;
        }
        List<String> keys = courses.stream()
                .map(c -> RedisKeys.COURSE_CAPACITY + c.getId())
                .collect(Collectors.toList());
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);

        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            String value = values == null ? null : values.get(i);
            if (value != null) {
                course.setRemainingCapacity(Integer.parseInt(value));
            } else {
                int selected = course.getSelectedCount() == null ? 0 : course.getSelectedCount();
                course.setRemainingCapacity(Math.max(course.getMaxCapacity() - selected, 0));
            }
        }
    }

    @Override
    public Course getCourseDetail(Long id) {
        Course course = baseMapper.selectCourseDetail(id);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        List<CourseSchedule> schedules = scheduleMapper.selectByCourseId(id);
        course.setSchedules(schedules);
        course.setScheduleText(buildScheduleText(schedules));
        fillRemainingCapacity(List.of(course));
        return course;
    }

    private String buildScheduleText(List<CourseSchedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return "暂无排课";
        }
        String[] dayTexts = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        return schedules.stream()
                .map(s -> String.format("%s 第%d-%d节 %s",
                        dayTexts[s.getDayOfWeek()],
                        s.getStartSection(),
                        s.getEndSection(),
                        StrUtil.blankToDefault(s.getBuilding(), "") + StrUtil.blankToDefault(s.getRoomNo(), "")))
                .collect(Collectors.joining("；"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(CourseDTO dto) {
        validateCourseCodeUnique(dto.getCourseCode(), dto.getSemesterId(), null);
        validateScheduleConflict(dto);

        Course course = new Course();
        copyProperties(dto, course);
        course.setSelectedCount(0);
        course.setVersion(0);
        course.setSelectable(dto.getSelectable() == null ? 1 : dto.getSelectable());
        course.setStatus(dto.getStatus() == null ? Constants.COURSE_STATUS_NORMAL : dto.getStatus());
        course.setExamType(StrUtil.blankToDefault(dto.getExamType(), "EXAM"));
        save(course);

        saveSchedules(course.getId(), course.getSemesterId(), dto.getSchedules());

        // 新课程立即写入 Redis 余量
        stringRedisTemplate.opsForValue().set(
                RedisKeys.COURSE_CAPACITY + course.getId(), String.valueOf(course.getMaxCapacity()));

        log.info("新增课程成功: {} ({})", course.getCourseName(), course.getCourseCode());
        return course.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCourse(CourseDTO dto) {
        BusinessException.throwIf(dto.getId() == null, ResultCode.PARAM_ERROR, "课程ID不能为空");
        Course exist = getById(dto.getId());
        BusinessException.throwIf(exist == null, ResultCode.COURSE_NOT_FOUND);

        validateCourseCodeUnique(dto.getCourseCode(), dto.getSemesterId(), dto.getId());
        validateScheduleConflict(dto);

        // 容量不允许小于已选人数
        Long selectedCount = selectionMapper.selectCount(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getCourseId, dto.getId())
                .eq(CourseSelection::getStatus, Constants.SELECTION_SELECTED));
        long selected = selectedCount == null ? 0L : selectedCount;
        if (dto.getMaxCapacity() < selected) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "课程容量不能小于已选人数（当前已选 " + selected + " 人）");
        }

        Course course = new Course();
        copyProperties(dto, course);
        course.setId(dto.getId());
        course.setVersion(exist.getVersion());
        course.setSelectedCount((int) selected);
        updateById(course);

        // 重建排课
        if (dto.getSchedules() != null) {
            scheduleMapper.delete(Wrappers.<CourseSchedule>lambdaQuery()
                    .eq(CourseSchedule::getCourseId, dto.getId()));
            saveSchedules(dto.getId(), dto.getSemesterId(), dto.getSchedules());
        }

        // 容量变化时更新 Redis 余量
        int remaining = Math.max(dto.getMaxCapacity() - (int) selected, 0);
        stringRedisTemplate.opsForValue().set(
                RedisKeys.COURSE_CAPACITY + dto.getId(), String.valueOf(remaining));

        log.info("修改课程成功: id={}, {}", dto.getId(), dto.getCourseName());
    }

    private void copyProperties(CourseDTO dto, Course course) {
        course.setCourseCode(dto.getCourseCode());
        course.setCourseName(dto.getCourseName());
        course.setSemesterId(dto.getSemesterId());
        course.setDeptId(dto.getDeptId());
        course.setTeacherId(dto.getTeacherId());
        course.setCredit(dto.getCredit());
        course.setHours(dto.getHours());
        course.setCourseType(dto.getCourseType());
        course.setExamType(dto.getExamType());
        course.setMaxCapacity(dto.getMaxCapacity());
        course.setTextbook(dto.getTextbook());
        course.setIntroduce(dto.getIntroduce());
        if (dto.getSelectable() != null) {
            course.setSelectable(dto.getSelectable());
        }
        if (dto.getStatus() != null) {
            course.setStatus(dto.getStatus());
        }
    }

    private void saveSchedules(Long courseId, Long semesterId, List<CourseScheduleDTO> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            return;
        }
        for (CourseScheduleDTO dto : schedules) {
            CourseSchedule schedule = new CourseSchedule();
            schedule.setCourseId(courseId);
            schedule.setSemesterId(semesterId);
            schedule.setClassroomId(dto.getClassroomId());
            schedule.setDayOfWeek(dto.getDayOfWeek());
            schedule.setStartSection(dto.getStartSection());
            schedule.setEndSection(dto.getEndSection());
            schedule.setStartWeek(dto.getStartWeek() == null ? 1 : dto.getStartWeek());
            schedule.setEndWeek(dto.getEndWeek() == null ? 16 : dto.getEndWeek());
            schedule.setWeekType(StrUtil.blankToDefault(dto.getWeekType(), Constants.WEEK_TYPE_ALL));
            scheduleMapper.insert(schedule);
        }
    }

    private void validateCourseCodeUnique(String courseCode, Long semesterId, Long excludeId) {
        Long count = baseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(Course::getCourseCode, courseCode)
                .eq(Course::getSemesterId, semesterId)
                .ne(excludeId != null, Course::getId, excludeId));
        BusinessException.throwIf(count != null && count > 0, ResultCode.DATA_ALREADY_EXISTS,
                "同一学期下课程编号 " + courseCode + " 已存在");
    }

    @Override
    public void validateScheduleConflict(CourseDTO dto) {
        if (dto.getSchedules() == null || dto.getSchedules().isEmpty()) {
            return;
        }
        // 课程内部多条排课相互校验
        List<CourseScheduleDTO> list = dto.getSchedules();
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                if (schedulesOverlap(list.get(i), list.get(j))) {
                    throw new BusinessException(ResultCode.SELECTION_CONFLICT,
                            "排课时段存在重叠，请检查第 " + (i + 1) + " 条与第 " + (j + 1) + " 条排课");
                }
            }
        }

        for (CourseScheduleDTO schedule : list) {
            if (schedule.getStartSection() > schedule.getEndSection()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "开始节次不能大于结束节次");
            }
            if (schedule.getStartWeek() != null && schedule.getEndWeek() != null
                    && schedule.getStartWeek() > schedule.getEndWeek()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "起始周不能大于结束周");
            }

            // 教师冲突
            if (dto.getTeacherId() != null) {
                int teacherConflict = scheduleMapper.countTeacherConflict(
                        dto.getTeacherId(), dto.getSemesterId(), dto.getId(),
                        schedule.getDayOfWeek(), schedule.getStartSection(), schedule.getEndSection(),
                        schedule.getStartWeek(), schedule.getEndWeek());
                if (teacherConflict > 0) {
                    throw new BusinessException(ResultCode.SELECTION_CONFLICT,
                            "教师在该时段已有其他排课，请调整上课时间");
                }
            }

            // 教室冲突
            if (schedule.getClassroomId() != null) {
                int roomConflict = scheduleMapper.countClassroomConflict(
                        schedule.getClassroomId(), dto.getSemesterId(), dto.getId(),
                        schedule.getDayOfWeek(), schedule.getStartSection(), schedule.getEndSection(),
                        schedule.getStartWeek(), schedule.getEndWeek());
                if (roomConflict > 0) {
                    throw new BusinessException(ResultCode.SELECTION_CONFLICT,
                            "该教室在此时段已被占用，请更换教室或调整时间");
                }
                Classroom classroom = classroomMapper.selectById(schedule.getClassroomId());
                if (classroom != null && classroom.getCapacity() < dto.getMaxCapacity()) {
                    log.warn("教室容量({})小于课程容量({})，courseId={}", classroom.getCapacity(),
                            dto.getMaxCapacity(), dto.getId());
                }
            }
        }
    }

    private boolean schedulesOverlap(CourseScheduleDTO a, CourseScheduleDTO b) {
        if (!a.getDayOfWeek().equals(b.getDayOfWeek())) {
            return false;
        }
        boolean sectionOverlap = a.getStartSection() <= b.getEndSection()
                && a.getEndSection() >= b.getStartSection();
        if (!sectionOverlap) {
            return false;
        }
        int aStart = a.getStartWeek() == null ? 1 : a.getStartWeek();
        int aEnd = a.getEndWeek() == null ? 16 : a.getEndWeek();
        int bStart = b.getStartWeek() == null ? 1 : b.getStartWeek();
        int bEnd = b.getEndWeek() == null ? 16 : b.getEndWeek();
        return aStart <= bEnd && aEnd >= bStart;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCourse(Long id) {
        Course course = getById(id);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);

        Long selectedCount = selectionMapper.selectCount(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getCourseId, id)
                .eq(CourseSelection::getStatus, Constants.SELECTION_SELECTED));
        if (selectedCount != null && selectedCount > 0) {
            throw new BusinessException(ResultCode.OPERATION_FORBIDDEN,
                    "该课程已有 " + selectedCount + " 名学生选课，请先结课或下架，不能直接删除");
        }

        removeById(id);
        scheduleMapper.delete(Wrappers.<CourseSchedule>lambdaQuery()
                .eq(CourseSchedule::getCourseId, id));

        // 清理 Redis 缓存
        stringRedisTemplate.delete(List.of(
                RedisKeys.COURSE_CAPACITY + id,
                RedisKeys.COURSE_SELECTED + id,
                RedisKeys.COURSE_INFO + id));

        log.info("删除课程成功: id={}, {}", id, course.getCourseName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        BusinessException.throwIf(status == null, ResultCode.PARAM_ERROR, "状态不能为空");
        if (status < 0 || status > 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "非法的课程状态");
        }
        Course course = getById(id);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);

        Course update = new Course();
        update.setId(id);
        update.setStatus(status);
        updateById(update);
        log.info("课程状态变更: id={}, status={}", id, status);
    }

    @Override
    public List<CourseSchedule> listSchedules(Long courseId) {
        return scheduleMapper.selectByCourseId(courseId);
    }

    @Override
    public List<CourseSelection> listCourseStudents(Long courseId) {
        return selectionMapper.selectList(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getCourseId, courseId)
                .eq(CourseSelection::getStatus, Constants.SELECTION_SELECTED));
    }

    // ==================================================================
    //  课表
    // ==================================================================

    @Override
    public List<TimetableVO> getStudentTimetable(Long studentId, Long semesterId) {
        Long effectiveSemester = semesterId != null ? semesterId : currentSemesterId();
        if (effectiveSemester == null) {
            return Collections.emptyList();
        }

        List<CourseSelection> selections = selectionMapper.selectList(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getStudentId, studentId)
                .eq(CourseSelection::getSemesterId, effectiveSemester)
                .eq(CourseSelection::getStatus, Constants.SELECTION_SELECTED));
        if (selections.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, CourseSelection> selectionMap = selections.stream()
                .collect(Collectors.toMap(CourseSelection::getCourseId, Function.identity(), (a, b) -> a));
        List<Long> courseIds = new ArrayList<>(selectionMap.keySet());

        List<CourseSchedule> schedules = scheduleMapper.selectByCourseIds(courseIds);
        List<TimetableVO> result = new ArrayList<>(schedules.size());
        for (CourseSchedule schedule : schedules) {
            CourseSelection selection = selectionMap.get(schedule.getCourseId());
            Course course = baseMapper.selectById(schedule.getCourseId());
            result.add(toTimetableVO(schedule, course, selection));
        }
        return result;
    }

    @Override
    public List<TimetableVO> getTeacherTimetable(Long teacherId, Long semesterId) {
        Long effectiveSemester = semesterId != null ? semesterId : currentSemesterId();
        if (effectiveSemester == null) {
            return Collections.emptyList();
        }
        List<Course> courses = baseMapper.selectList(Wrappers.<Course>lambdaQuery()
                .eq(Course::getTeacherId, teacherId)
                .eq(Course::getSemesterId, effectiveSemester));
        if (courses.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Course> courseMap = courses.stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));
        List<CourseSchedule> schedules = scheduleMapper.selectByCourseIds(new ArrayList<>(courseMap.keySet()));

        List<TimetableVO> result = new ArrayList<>(schedules.size());
        for (CourseSchedule schedule : schedules) {
            result.add(toTimetableVO(schedule, courseMap.get(schedule.getCourseId()), null));
        }
        return result;
    }

    private TimetableVO toTimetableVO(CourseSchedule schedule, Course course, CourseSelection selection) {
        TimetableVO vo = new TimetableVO();
        vo.setCourseId(schedule.getCourseId());
        vo.setDayOfWeek(schedule.getDayOfWeek());
        vo.setStartSection(schedule.getStartSection());
        vo.setEndSection(schedule.getEndSection());
        vo.setStartWeek(schedule.getStartWeek());
        vo.setEndWeek(schedule.getEndWeek());
        vo.setWeekType(schedule.getWeekType());
        vo.setLocation(StrUtil.blankToDefault(schedule.getBuilding(), "")
                + StrUtil.blankToDefault(schedule.getRoomNo(), ""));
        vo.setDropped(selection != null && Constants.SELECTION_DROPPED.equals(selection.getStatus()));

        if (course != null) {
            vo.setCourseName(course.getCourseName());
            vo.setCourseCode(course.getCourseCode());
            vo.setCredit(course.getCredit());
            vo.setTeacherName(course.getTeacherName());
            if (vo.getTeacherName() == null && course.getTeacherId() != null) {
                Teacher teacher = teacherMapper.selectById(course.getTeacherId());
                if (teacher != null) {
                    SysUser teacherUser = userMapper.selectById(teacher.getUserId());
                    if (teacherUser != null) {
                        vo.setTeacherName(teacherUser.getRealName());
                    }
                }
            }
        }
        return vo;
    }

    private Long currentSemesterId() {
        Semester semester = semesterMapper.selectOne(Wrappers.<Semester>lambdaQuery()
                .eq(Semester::getIsCurrent, 1).last("LIMIT 1"));
        return semester == null ? null : semester.getId();
    }

    // ==================================================================
    //  统计
    // ==================================================================

    @Override
    public StatisticsVO getStatistics(Long semesterId) {
        StatisticsVO vo = new StatisticsVO();
        Long effectiveSemester = semesterId != null ? semesterId : currentSemesterId();

        vo.setStudentCount(studentMapper.selectCount(null));
        vo.setTeacherCount(teacherMapper.selectCount(null));
        vo.setCourseCount(baseMapper.selectCount(null));
        vo.setDeptCount(departmentMapper.selectCount(null));

        Long selectionCount = effectiveSemester != null
                ? selectionMapper.countBySemester(effectiveSemester)
                : selectionMapper.selectCount(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getStatus, Constants.SELECTION_SELECTED));
        vo.setSelectionCount(selectionCount);

        Long currentCourseCount = baseMapper.selectCount(Wrappers.<Course>lambdaQuery()
                .eq(effectiveSemester != null, Course::getSemesterId, effectiveSemester)
                .eq(Course::getStatus, Constants.COURSE_STATUS_NORMAL));
        vo.setCurrentSemesterCourseCount(currentCourseCount);

        vo.setHotCourses(baseMapper.selectHotCourses(effectiveSemester, 10));
        vo.setDeptDistribution(baseMapper.selectDeptDistribution(effectiveSemester));

        // 成绩分布依赖 CourseGradeMapper.selectScoreDistribution，待成绩服务实现后
        // 取消注释并注入 CourseGradeMapper 即可启用。
        // vo.setScoreDistribution(gradeMapper.selectScoreDistribution(null, effectiveSemester));
        vo.setScoreDistribution(Collections.emptyList());
        return vo;
    }
}
