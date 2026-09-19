package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.PageResult;
import com.college.elective.common.ScheduleConflictUtils;
import com.college.elective.common.RedisKeys;
import com.college.elective.common.ResultCode;
import com.college.elective.dto.CourseDTO;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.dto.CourseScheduleDTO;
import com.college.elective.entity.Classroom;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseSchedule;
import com.college.elective.entity.CourseSelection;
import com.college.elective.entity.Student;
import com.college.elective.entity.SysUser;
import com.college.elective.entity.Teacher;
import com.college.elective.mapper.ClassroomMapper;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.CourseScheduleMapper;
import com.college.elective.mapper.CourseSelectionMapper;
import com.college.elective.mapper.DepartmentMapper;
import com.college.elective.mapper.StudentMapper;
import com.college.elective.mapper.SysUserMapper;
import com.college.elective.mapper.TeacherMapper;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseService;
import com.college.elective.service.SemesterService;
import com.college.elective.vo.StatisticsVO;
import com.college.elective.vo.TimetableVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
    /** 学期服务：获取当前学期，课程查询与统计均以其为基准 */
    private final SemesterService semesterService;
    private final TeacherMapper teacherMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper userMapper;
    private final ClassroomMapper classroomMapper;
    private final DepartmentMapper departmentMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final Map<Integer, String> DAY_TEXTS = Map.of(
        1, "周一", 2, "周二", 3, "周三", 4, "周四",
        5, "周五", 6, "周六", 7, "周日");

    @Override
    public PageResult<Course> pageCourses(CourseQueryDTO query) {
        IPage<Course> page = baseMapper.selectCoursePage(new Page<>(query.getPageNum(), query.getPageSize()), query);
        fillRemainingCapacity(page.getRecords());
        return PageResult.of(page);
    }

    /**
     * 用 Redis 实时余量覆盖数据库记录，保证展示与选课结果一致。
     *
     * <p><b>为什么需要这个方法</b></p>
     * <p>课程的剩余容量（{@code remainingCapacity}）是选课业务中的高频读取字段，
     * 但在数据库里它并非独立字段，而是由 {@code 最大容量 - 已选人数} 推算得出。
     * 若每次查询都依赖数据库的 {@code selected_count}，会有两个问题：</p>
     * <ul>
     *   <li><b>数据延迟</b>：选课高峰期 {@code selected_count} 采用异步或批量回写，
     *       数据库中的值可能滞后于真实情况，导致学生看到「还有名额」但提交时被拒；</li>
     *   <li><b>写入压力</b>：若改为每次选课都实时更新数据库，会对课程表产生大量行锁竞争。</li>
     * </ul>
     * <p>因此系统采用「<b>Redis 承载实时余量、数据库作为持久化底账</b>」的方案。
     * 本方法的作用就是在返回课程数据前，将 Redis 中的权威余量回填到实体上，
     * 使前端展示的余量与选课接口的判定结果保持一致。</p>
     *
     * <p><b>取值策略（Redis 优先，数据库兜底）</b></p>
     * <ol>
     *   <li>Redis 中存在 {@code elective:course:capacity:{courseId}} → 以其为准。
     *       该值由选课/退课的 Lua 脚本原子维护，是当前最精确的余量；</li>
     *   <li>Redis 中不存在该 Key → 说明缓存尚未预热（如服务刚启动、
     *       课程为新建、或缓存被淘汰），此时退化为
     *       {@code maxCapacity - selectedCount} 的数据库推算值，并用
     *       {@code Math.max(..., 0)} 兜底，避免出现负余量。</li>
     * </ol>
     *
     * <p><b>性能考量</b></p>
     * <p>本方法在分页查询与详情查询中都会被调用，属于请求链路的关键路径，
     * 因此刻意做了两点优化：</p>
     * <ul>
     *   <li>使用 {@code multiGet} 一次批量拉取全部课程的余量，
     *       将 N 次 Redis 往返合并为 1 次，避免循环内单次读取造成的网络开销；</li>
     *   <li>不逐条校验 Key 是否存在（避免 {@code hasKey} + {@code get} 的两次往返），
     *       直接依据 {@code multiGet} 返回的 {@code null} 判定缺失。</li>
     * </ul>
     * <p>注意：{@code multiGet} 返回的列表顺序与传入的 Key 顺序严格对应，
     * 但列表本身可能为 {@code null}（Redis 无返回时），故下方做了空值保护。</p>
     *
     * <p><b>调用场景</b></p>
     * <ul>
     *   <li>{@link #pageCourses} —— 课程列表分页，逐条回填；</li>
     *   <li>{@link #getCourseDetail} —— 课程详情，单条回填。</li>
     * </ul>
     *
     * @param courses 待回填余量的课程列表，允许为 {@code null} 或空集合
     */
    private void fillRemainingCapacity(List<Course> courses) {
        // 空集合直接返回，避免后续无意义的 Redis 请求
        if (courses == null || courses.isEmpty()) {
            return;
        }

        // 批量构造 Redis Key，保持与 courses 的索引一一对应
        List<String> keys = courses.stream()
                .map(c -> RedisKeys.COURSE_CAPACITY + c.getId())
                .collect(Collectors.toList());

        // 一次请求取回全部余量，返回列表的索引与 keys 严格对应
        List<String> values = stringRedisTemplate.opsForValue().multiGet(keys);

        for (int i = 0; i < courses.size(); i++) {
            Course course = courses.get(i);
            // multiGet 可能整体返回 null，需先判空再按下标取值
            String value = values == null ? null : values.get(i);

            if (value != null) {
                // 命中缓存：以 Redis 中的实时余量为准
                course.setRemainingCapacity(Integer.parseInt(value));
            } else {
                // 未命中缓存：退化为数据库推算值，并保证余量不为负数
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
       //周一 第1-2节 A栋101；周三 第3-4节 B栋205
        return schedules.stream()
                .map(s -> String.format("%s 第%d-%d节 %s",
                        DAY_TEXTS.getOrDefault(s.getDayOfWeek(), "待定"),// 周一
                        s.getStartSection(),// 1
                        s.getEndSection(),// 2
                        StrUtil.blankToDefault(s.getBuilding(), "") + StrUtil.blankToDefault(s.getRoomNo(), "")))// A栋101
                .collect(Collectors.joining("；"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(CourseDTO dto) {
        // 学期为必填项：课程归属学期是排课、选课与统计的关联依据。
        // 此处显式校验，避免依赖 DTO 上的注解校验被绕过时创建出无学期的课程
        BusinessException.throwIf(dto.getSemesterId() == null, ResultCode.PARAM_ERROR, "所属学期不能为空");

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

        // 未传学期时沿用库中记录，保证唯一性校验与后续写入使用同一学期
        Long semesterId = dto.getSemesterId() != null ? dto.getSemesterId() : exist.getSemesterId();

        validateCourseCodeUnique(dto.getCourseCode(), semesterId, dto.getId());
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

        // 重建排课：排课依赖 semester_id 关联学期，若写入 null，
        // 将导致按学期查询课表时丢失该课程的排课，故统一使用上方解析出的学期
        if (dto.getSchedules() != null) {
            scheduleMapper.delete(Wrappers.<CourseSchedule>lambdaQuery()
                    .eq(CourseSchedule::getCourseId, dto.getId()));
            saveSchedules(dto.getId(), semesterId, dto.getSchedules());
        }

        // 容量变化时更新 Redis 余量
        int remaining = Math.max(dto.getMaxCapacity() - (int) selected, 0);
        stringRedisTemplate.opsForValue().set(
                RedisKeys.COURSE_CAPACITY + dto.getId(), String.valueOf(remaining));

        log.info("修改课程成功: id={}, {}", dto.getId(), dto.getCourseName());
    }

    /**
     * 将 DTO 中的字段复制到课程实体。
     *
     * <p>{@code semesterId} 仅在 DTO 显式传入时覆盖：学期是排课、选课与统计的关联依据，
     * 若被置为 {@code null}，会导致按学期查询时该课程及其排课整体消失。
     * 此处的处理方式与 {@code selectable}、{@code status} 保持一致，
     * 未传的字段一律保留原值。</p>
     *
     * @param dto    课程请求参数
     * @param course 待写入的课程实体，新增时为空对象，修改时为待更新对象
     */
    private void copyProperties(CourseDTO dto, Course course) {
        course.setCourseCode(dto.getCourseCode());
        course.setCourseName(dto.getCourseName());
        if (dto.getSemesterId() != null) {
            course.setSemesterId(dto.getSemesterId());
        }
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
        List<CourseScheduleDTO> list = dto.getSchedules();
        if (list == null || list.isEmpty()) {
            return;
        }

        // 1. 先校验字段合法性，保证后续区间比较基于合法数据，
        //    否则非法区间（如开始节次大于结束节次）会先触发重叠报错，掩盖真实问题
        for (int i = 0; i < list.size(); i++) {
            validateScheduleField(list.get(i), i);
        }

        // 2. 课程内部多条排课之间不允许重叠
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                if (ScheduleConflictUtils.isConflict(list.get(i), list.get(j))) {
                    throw new BusinessException(ResultCode.SELECTION_CONFLICT,
                            "排课时段存在重叠，请检查第 " + (i + 1) + " 条与第 " + (j + 1) + " 条排课");
                }
            }
        }

        // 3. 教师与教室冲突：一次性取出该学期相关排课，在内存中比对，
        //    避免在循环内逐条查询造成 N+1
        List<CourseSchedule> existing = loadExistingSchedules(dto);
        for (int i = 0; i < list.size(); i++) {
            CourseScheduleDTO schedule = list.get(i);
            checkTeacherConflict(dto, schedule, existing);
            checkClassroomConflict(dto, schedule, existing);
        }

        // 4. 教室容量校验：容量不足属于业务约束，不能仅记录警告后放过，
        //    否则排课可保存但学生无法容纳
        checkClassroomCapacity(dto, list);
    }

    /**
     * 校验单条排课的字段合法性。
     *
     * @param schedule 排课参数
     * @param index    在列表中的下标，用于定位报错位置
     */
    private void validateScheduleField(CourseScheduleDTO schedule, int index) {
        String position = "第 " + (index + 1) + " 条排课：";
        BusinessException.throwIf(schedule.getDayOfWeek() == null, ResultCode.PARAM_ERROR,
                position + "请选择上课星期");
        BusinessException.throwIf(schedule.getStartSection() == null || schedule.getEndSection() == null,
                ResultCode.PARAM_ERROR, position + "请填写起止节次");
        BusinessException.throwIf(schedule.getStartSection() > schedule.getEndSection(),
                ResultCode.PARAM_ERROR, position + "开始节次不能大于结束节次");

        int startWeek = schedule.getStartWeek() == null
                ? ScheduleConflictUtils.DEFAULT_START_WEEK : schedule.getStartWeek();
        int endWeek = schedule.getEndWeek() == null
                ? ScheduleConflictUtils.DEFAULT_END_WEEK : schedule.getEndWeek();
        BusinessException.throwIf(startWeek > endWeek, ResultCode.PARAM_ERROR,
                position + "起始周不能大于结束周");
    }

    /**
     * 加载与本次排课相关的既有排课记录，供冲突比对使用。
     *
     * <p>按教师与教室两个维度分别查询一次，替代原先在循环内逐条统计的做法。
     * 更新课程时需排除自身记录（{@code dto.getId()}）。</p>
     *
     * @param dto 课程参数
     * @return 该学期内与本次排课涉及同一教师或同一教室的排课记录
     */
    private List<CourseSchedule> loadExistingSchedules(CourseDTO dto) {
        if (dto.getSemesterId() == null) {
            return Collections.emptyList();
        }
        Set<Long> classroomIds = dto.getSchedules().stream()
                .map(CourseScheduleDTO::getClassroomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return scheduleMapper.selectRelatedSchedules(dto.getSemesterId(), dto.getTeacherId(),
                classroomIds.isEmpty() ? null : classroomIds, dto.getId());
    }

    /**
     * 校验教师在该时段是否已有其他排课。
     */
    private void checkTeacherConflict(CourseDTO dto, CourseScheduleDTO schedule,
                                      List<CourseSchedule> existing) {
        if (dto.getTeacherId() == null) {
            return;
        }
        boolean conflict = existing.stream().anyMatch(item ->
                Objects.equals(item.getTeacherId(), dto.getTeacherId())
                        && ScheduleConflictUtils.isConflict(schedule, item));
        BusinessException.throwIf(conflict, ResultCode.SELECTION_CONFLICT,
                "教师在该时段已有其他排课，请调整上课时间");
    }

    /**
     * 校验教室在该时段是否已被占用。
     */
    private void checkClassroomConflict(CourseDTO dto, CourseScheduleDTO schedule,
                                        List<CourseSchedule> existing) {
        if (schedule.getClassroomId() == null) {
            return;
        }
        boolean conflict = existing.stream().anyMatch(item ->
                Objects.equals(item.getClassroomId(), schedule.getClassroomId())
                        && ScheduleConflictUtils.isConflict(schedule, item));
        BusinessException.throwIf(conflict, ResultCode.SELECTION_CONFLICT,
                "该教室在此时段已被占用，请更换教室或调整时间");
    }

    /**
     * 校验所选教室容量是否满足课程容量要求。
     *
     * <p>教室容量小于课程容量时拒绝保存：若仅记录警告，课程虽可排课，
     * 但选课阶段将出现学生无法容纳的问题。</p>
     *
     * @param dto  课程参数
     * @param list 排课列表
     */
    private void checkClassroomCapacity(CourseDTO dto, List<CourseScheduleDTO> list) {
        if (dto.getMaxCapacity() == null) {
            return;
        }
        // 同一教室可能被多个时段复用，先收集去重，一次性查询
        Set<Long> classroomIds = list.stream()
                .map(CourseScheduleDTO::getClassroomId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (classroomIds.isEmpty()) {
            return;
        }

        Map<Long, Classroom> classroomMap = classroomMapper.selectBatchIds(classroomIds).stream()
                .collect(Collectors.toMap(Classroom::getId, Function.identity(), (a, b) -> a));
        for (Long classroomId : classroomIds) {
            Classroom classroom = classroomMap.get(classroomId);
            if (classroom == null) {
                continue;
            }
            BusinessException.throwIf(classroom.getCapacity() != null
                            && classroom.getCapacity() < dto.getMaxCapacity(),
                    ResultCode.PARAM_ERROR,
                    "教室「" + StrUtil.blankToDefault(classroom.getRoomNo(), String.valueOf(classroomId))
                            + "」容量为 " + classroom.getCapacity()
                            + "，小于课程容量 " + dto.getMaxCapacity() + "，请更换教室或调整课程容量");
        }
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

    /**
     * 查询指定课程的选课记录（仅返回已选状态的记录）。
     *
     * <p>该接口为轻量级列表，仅返回选课记录本身，不含学生姓名等关联信息，
     * 适用于需要课程学生ID集合的内部调用；如需展示名单请使用分页接口。</p>
     *
     * <p>访问权限：管理员不受限，教师仅能查询本人授课的课程。</p>
     *
     * @param courseId 课程ID
     * @return 该课程的选课记录列表
     */
    @Override
    public List<CourseSelection> listCourseStudents(Long courseId) {
        Course course = getById(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        checkCourseAccess(course);
        return selectionMapper.selectList(Wrappers.<CourseSelection>lambdaQuery()
                .eq(CourseSelection::getCourseId, courseId)
                .eq(CourseSelection::getStatus, Constants.SELECTION_SELECTED));
    }

    /**
     * 校验当前用户是否有权访问指定课程的数据。
     *
     * <p>管理员不受限；教师仅能访问自己授课的课程，越权返回 403。</p>
     *
     * @param course 课程实体
     */
    private void checkCourseAccess(Course course) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        BusinessException.throwIf(!loginUser.isTeacher()
                        || !Objects.equals(course.getTeacherId(), loginUser.getTeacherId()),
                ResultCode.ROLE_NOT_ALLOWED, "无权查看该课程的学生名单");
    }

    // ==================================================================
    //  课表
    // ==================================================================

    @Override
    public List<TimetableVO> getStudentTimetable(Long studentId, Long semesterId) {
        Long effectiveSemester = semesterId != null ? semesterId : semesterService.getCurrentSemesterId();
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
        List<CourseSchedule> schedules = scheduleMapper.selectByCourseIds(courseIds, effectiveSemester);

        // 课程与教师姓名一次性查出，避免在循环内逐条查询造成 N+1
        Map<Long, Course> courseMap = loadCourses(courseIds);
        Map<Long, String> teacherNameMap = loadTeacherNames(courseMap.values());

        List<TimetableVO> result = new ArrayList<>(schedules.size());
        for (CourseSchedule schedule : schedules) {
            Course course = courseMap.get(schedule.getCourseId());
            result.add(toTimetableVO(schedule, course, selectionMap.get(schedule.getCourseId()),
                    resolveTeacherName(course, teacherNameMap)));
        }
        return result;
    }

    @Override
    public List<TimetableVO> getTeacherTimetable(Long teacherId, Long semesterId) {
        Long effectiveSemester = semesterId != null ? semesterId : semesterService.getCurrentSemesterId();
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
        List<CourseSchedule> schedules = scheduleMapper.selectByCourseIds(
                new ArrayList<>(courseMap.keySet()), effectiveSemester);
        Map<Long, String> teacherNameMap = loadTeacherNames(courseMap.values());

        List<TimetableVO> result = new ArrayList<>(schedules.size());
        for (CourseSchedule schedule : schedules) {
            Course course = courseMap.get(schedule.getCourseId());
            result.add(toTimetableVO(schedule, course, null, resolveTeacherName(course, teacherNameMap)));
        }
        return result;
    }

    /**
     * 批量查询课程，返回 课程ID → 课程 的映射。
     *
     * @param courseIds 课程ID列表
     * @return 课程映射；入参为空时返回空 Map
     */
    private Map<Long, Course> loadCourses(Collection<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return baseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * 批量查询教师姓名，返回 教师ID → 姓名 的映射。
     *
     * <p>课程的 {@code teacher_name} 为冗余字段，正常写入时应已填充。
     * 此处仅针对冗余字段为空的课程做一次兜底查询：先一次性取出涉及的教师，
     * 再一次性取出对应用户，避免在课表循环中逐条查询（N+1）。</p>
     *
     * @param courses 课程集合
     * @return 教师ID → 教师姓名 的映射；无需兜底时返回空 Map
     */
    private Map<Long, String> loadTeacherNames(Collection<Course> courses) {
        Set<Long> teacherIds = courses.stream()
                .filter(c -> StrUtil.isBlank(c.getTeacherName()) && c.getTeacherId() != null)
                .map(Course::getTeacherId)
                .collect(Collectors.toSet());
        if (teacherIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Teacher> teachers = teacherMapper.selectBatchIds(teacherIds);
        if (teachers.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Long> userIds = teachers.stream()
                .map(Teacher::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 一次性查出所有教师对应的用户，再按 教师ID → 姓名 组装
        Map<Long, String> userNameMap = userMapper.selectBatchIds(userIds).stream()
                .filter(u -> u.getRealName() != null)
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
        return teachers.stream()
                .filter(t -> userNameMap.containsKey(t.getUserId()))
                .collect(Collectors.toMap(Teacher::getId, t -> userNameMap.get(t.getUserId()), (a, b) -> a));
    }

    /**
     * 获取课程展示用的教师姓名，优先使用课程上的冗余字段，缺失时回退到兜底映射。
     *
     * @param course         课程，可为 {@code null}
     * @param teacherNameMap 兜底教师姓名映射
     * @return 教师姓名，均无法获取时返回 {@code null}
     */
    private String resolveTeacherName(Course course, Map<Long, String> teacherNameMap) {
        if (course == null) {
            return null;
        }
        if (StrUtil.isNotBlank(course.getTeacherName())) {
            return course.getTeacherName();
        }
        return course.getTeacherId() == null ? null : teacherNameMap.get(course.getTeacherId());
    }

    private TimetableVO toTimetableVO(CourseSchedule schedule, Course course, CourseSelection selection,
                                      String teacherName) {
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
            vo.setTeacherName(teacherName);
        }
        return vo;
    }

    // ==================================================================
    //  统计
    // ==================================================================

    @Override
    public StatisticsVO getStatistics(Long semesterId) {
        StatisticsVO vo = new StatisticsVO();
        Long effectiveSemester = semesterId != null ? semesterId : semesterService.getCurrentSemesterId();

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
