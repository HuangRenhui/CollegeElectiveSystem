package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.*;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseSchedule;
import com.college.elective.entity.CourseSelection;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.CourseScheduleMapper;
import com.college.elective.mapper.CourseSelectionMapper;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.CourseSelectionService;
import com.college.elective.service.SemesterService;
import com.college.elective.vo.ConflictVO;
import com.college.elective.vo.SelectionResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 选课服务实现。
 *
 * <p><b>当前为骨架实现</b>：所有方法体仅包含 TODO 占位，尚未实现具体业务逻辑。
 * 请参考项目文档 {@code docs/待实现功能.md} 的「选课模块」章节逐步补全。</p>
 *
 * <h3>核心设计要点</h3>
 * <ol>
 *   <li><b>Redis 原子预占</b>：通过 Lua 脚本在 Redis 中一次性完成
 *       「查重 → 余量判断 → 扣减 → 写入已选集合」，保证并发安全、防止超选。
 *       脚本已注册为 Bean：{@code selectCourseScript}、{@code dropCourseScript}。</li>
 *   <li><b>时间冲突校验</b>：基于「星期 + 节次区间 + 周次区间 + 单双周」四维判定。</li>
 *   <li><b>落库与回滚</b>：预占成功后写入选课记录，失败需归还 Redis 预占。</li>
 *   <li><b>一致性兜底</b>：定时任务校正数据库计数与 Redis 余量。</li>
 * </ol>
 *
 * <h3>建议注入的依赖</h3>
 * <pre>{@code
 * private final CourseMapper courseMapper;
 * private final StudentMapper studentMapper;
 * private final SemesterMapper semesterMapper;
 * private final CourseScheduleMapper scheduleMapper;
 * private final StringRedisTemplate stringRedisTemplate;
 * private final DefaultRedisScript<Long> selectCourseScript;
 * private final DefaultRedisScript<Long> dropCourseScript;
 * private final ElectiveProperties properties;
 * }</pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSelectionServiceImpl extends ServiceImpl<CourseSelectionMapper, CourseSelection> implements CourseSelectionService, PendingImplementation {

    /**
     * 课程 Mapper：查询课程详情、校验选课权限
     */
    private final CourseMapper courseMapper;

    /**
     * 学期服务：获取当前学期，选课窗口与冲突检测均以其为基准
     */
    private final SemesterService semesterService;

    // TODO 待注入依赖（实现选课/退课/预热/同步时启用）
    //  private final StudentMapper studentMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final StringRedisTemplate stringRedisTemplate;
    //  private final DefaultRedisScript<Long> selectCourseScript;
    //  private final DefaultRedisScript<Long> dropCourseScript;
    //  private final ElectiveProperties properties;

    @Override
    public SelectionResultVO selectCourse(Long courseId) {
        // TODO 实现学生选课逻辑
        //  1. 校验选课开关与选课时间窗口（ElectiveProperties + Redis 开关 + 学期时间）
        //  2. 校验课程是否存在且状态正常（course.status == 1）
        //  3. 校验学分上限：已选学分 + 本课程学分 <= elective.selection.max-credit
        //  4. 时间冲突预检（调用 checkConflict）
        //  5. 确保 Redis 缓存就绪（capacityKey 缺失时从数据库回源重建）
        //  6. 执行 select_course.lua 原子预占：
        //     参数 KEYS=[capacityKey, selectedKey]，ARGV=[studentId, RedisKeys.DEFAULT_CACHE_SECONDS]
        //     返回值 0-成功 / 1-已选过(3003) / 2-余量不足(3002) / 3-缓存未就绪(3010)
        //  7. 落库写入选课记录（status=1）；失败时执行 drop_course.lua 回滚预占
        //  8. courseMapper.increaseSelectedCount(courseId) 同步数据库计数
        throw new UnsupportedOperationException("TODO：学生选课 尚未实现，请参考 docs/待实现功能.md");
    }

    @Override
    public SelectionResultVO dropCourse(Long courseId) {
        // TODO 实现学生退课逻辑
        //  1. 校验课程允许退选（course.selectable == 1），否则抛 3009
        //  2. 查询该学生有效的选课记录（status = 1），不存在则抛 3004
        //  3. 若已录入成绩（selection.score != null）则禁止退课（3009）
        //  4. 更新记录：status = 0、dropTime = now()（不物理删除，便于审计与复用）
        //  5. 执行 drop_course.lua 归还 Redis 余量
        //     参数 KEYS=[capacityKey, selectedKey]，ARGV=[studentId, RedisKeys.DEFAULT_CACHE_SECONDS]
        //  6. courseMapper.decreaseSelectedCount(courseId) 同步数据库计数
        throw new UnsupportedOperationException("TODO：学生退课 尚未实现，请参考 docs/待实现功能.md");
    }

    @Override
    public PageResult<CourseSelection> pageMySelections(Long pageNum, Long pageSize, Long semesterId, Integer status) {
        Long studentId = SecurityUtils.requireStudentId();
        IPage<CourseSelection> page = baseMapper.selectStudentSelections(new Page<>(pageNum, pageSize), studentId, semesterId, status);
        return PageResult.of(page);
    }

    @Override
    public PageResult<CourseSelection> pageCourseStudents(Long courseId, Long pageNum, Long pageSize, String keyword) {
        checkCourseAccess(courseId);
        IPage<CourseSelection> page = baseMapper.selectCourseStudents(new Page<>(pageNum, pageSize), courseId, keyword);
        return PageResult.of(page);
    }

    /**
     * 校验当前用户是否有权访问指定课程的名单。
     *
     * <p>管理员不受限；教师仅能访问自己授课的课程。</p>
     *
     * @param courseId 课程ID
     */
    private void checkCourseAccess(Long courseId) {
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);

        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser.isAdmin()) {
            return;
        }
        BusinessException.throwIf(!loginUser.isTeacher() || !Objects.equals(course.getTeacherId(), loginUser.getTeacherId()), ResultCode.ROLE_NOT_ALLOWED, "无权查看该课程的学生名单");
    }

    @Override
    public List<ConflictVO> checkConflict(Long courseId) {
        Long studentId = SecurityUtils.requireStudentId();

        // 课表按学期划分，冲突比对必须在同一学期内进行，否则会与历史学期课程误判
        Long semesterId = semesterService.getCurrentSemesterId();
        BusinessException.throwIf(semesterId == null, ResultCode.CURRENT_SEMESTER_NOT_SET);

        // 目标课程的排课
        List<CourseSchedule> schedules = scheduleMapper.selectByCourseId(courseId);
        if (schedules.isEmpty()) {
            return Collections.emptyList();
        }

        // 该学生本学期已选课程的排课（SQL 已限定 status = 1，不含已退选）
        List<CourseSchedule> selectedSchedules = scheduleMapper.selectByStudentSelection(studentId, semesterId);
        if (selectedSchedules.isEmpty()) {
            return Collections.emptyList();
        }

        // 两两比对，命中即记录一条冲突信息，供前端逐条展示
        List<ConflictVO> conflicts = new ArrayList<>();
        for (CourseSchedule schedule : schedules) {
            for (CourseSchedule selected : selectedSchedules) {
                // 目标课程可能已在已选列表中（重复选课场景），跳过自身避免误报
                if (Objects.equals(schedule.getCourseId(), selected.getCourseId())) {
                    continue;
                }
                if (ScheduleConflictUtils.isConflict(schedule, selected)) {
                    conflicts.add(toConflictVO(schedule, selected));
                }
            }
        }
        return conflicts;
    }

    /**
     * 构造冲突提示信息。
     *
     * @param target   本次准备选修的课程排课
     * @param selected 学生已选课程的排课
     * @return 冲突信息，描述以「已选课程」为主体，便于学生定位是撞了哪门课
     */
    private ConflictVO toConflictVO(CourseSchedule target, CourseSchedule selected) {
        String day = ScheduleConflictUtils.dayText(target.getDayOfWeek());
        String section = ScheduleConflictUtils.sectionText(target.getStartSection(), target.getEndSection());
        String week = ScheduleConflictUtils.weekText(defaultStartWeek(target), defaultEndWeek(target), target.getWeekType());

        String description = String.format("与已选课程《%s》的 %s %s %s 时间冲突", StrUtil.blankToDefault(selected.getCourseName(), "未知课程"), day, section, week);

        return new ConflictVO(selected.getCourseName(), selected.getCourseCode(), description, target.getDayOfWeek(), section, week);
    }

    /**
     * 起始周默认值：未指定时视为第 1 周
     */
    private int defaultStartWeek(CourseSchedule schedule) {
        return schedule.getStartWeek() == null ? ScheduleConflictUtils.DEFAULT_START_WEEK : schedule.getStartWeek();
    }

    /**
     * 结束周默认值：未指定时视为第 16 周
     */
    private int defaultEndWeek(CourseSchedule schedule) {
        return schedule.getEndWeek() == null ? ScheduleConflictUtils.DEFAULT_END_WEEK : schedule.getEndWeek();
    }

    /**
     * 单门课程的缓存预热数据：一次 pipeline 提交所需的最小信息集。
     *
     * <p><b>为什么需要这个载体</b>：缓存预热要执行「查询数据库 → 写入 Redis」两步。
     * 若在同一个循环里逐门课程调用 {@code executePipelined}，网络往返次数等于课程数（N 次）。
     * 因此先在第一轮循环中把结果暂存为 {@code CourseCachePayload}，
     * 再统一遍历该列表，把全部 Redis 命令合并到<b>一次</b> pipeline 提交，
     * 从而把 N 次往返压缩为 1 次。</p>
     *
     * <p>使用 {@code record} 而非普通类，是因为它只承担不可变的数据传输职责，
     * 不包含任何行为：编译器会自动生成规范构造器、访问器方法
     * （{@code courseId()} / {@code studentIds()} / {@code remaining()}）以及
     * {@code equals} / {@code hashCode} / {@code toString}，无需手写样板代码。</p>
     *
     * <p><b>字段在 Redis 写入中的作用</b>：</p>
     * <ul>
     *   <li>{@code courseId} —— 拼接两个 Redis Key：
     *       {@code elective:course:capacity:{courseId}} 与 {@code elective:course:selected:{courseId}}</li>
     *   <li>{@code studentIds} —— 写入已选学生集合（Set），作为选课查重（{@code SISMEMBER}）的依据</li>
     *   <li>{@code remaining} —— 写入剩余容量（String），供选课脚本 {@code DECR} 扣减</li>
     * </ul>
     *
     * @param courseId   课程ID
     * @param studentIds 真实已选学生ID（已去重，字符串形式）；无学生时为空集合，不可为 {@code null}
     * @param remaining  剩余容量，取值不小于 0（{@code maxCapacity - 已选人数}，负数按 0 处理）
     */
    private record CourseCachePayload(Long courseId, List<String> studentIds, int remaining) {
    }

    @Override
    public void preloadCourseCache(Long semesterId) {
        // 1. 查询目标课程：status = 1（正常）；若指定了学期则加学期条件
        List<Course> courses = courseMapper.selectList(Wrappers.<Course>lambdaQuery().eq(Course::getStatus, 1).eq(semesterId != null, Course::getSemesterId, semesterId));
        if (courses.isEmpty()) {
            log.warn("[缓存预热] 未找到可预热课程，semesterId={}", semesterId);
            return;
        }

        // 2. 一次查出所有课程的有效选课学生，避免逐门课程查询（N 次 IO 压缩为 1 次）
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        List<CourseSelection> validSelections = baseMapper.selectValidSelectionsByCourseIds(courseIds);

        // 在内存中按课程分组，得到每门课程的已选学生ID集合
        Map<Long, Set<String>> studentIdsByCourse = new HashMap<>();
        for (CourseSelection selection : validSelections) {
            studentIdsByCourse.computeIfAbsent(selection.getCourseId(), key -> new HashSet<>()).add(String.valueOf(selection.getStudentId()));
        }

        // 3. 计算每门课程待写入 Redis 的剩余容量
        List<CourseCachePayload> payloads = new ArrayList<>(courses.size());
        int studentCount = 0;
        for (Course course : courses) {
            // 该课程的已选学生（数据库查不到时为空的不可变集合）
            List<String> studentIds = List.copyOf(studentIdsByCourse.getOrDefault(course.getId(), Set.of()));
            int maxCapacity = course.getMaxCapacity() == null ? 0 : course.getMaxCapacity();
            int remaining = Math.max(maxCapacity - studentIds.size(), 0);

            payloads.add(new CourseCachePayload(course.getId(), studentIds, remaining));
            studentCount += studentIds.size();
        }

        // 4. 所有课程的 Redis 命令合并为一次 pipeline 提交，把 N 轮往返压缩为 1 轮
        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings({"rawtypes", "unchecked"})
            public Object execute(RedisOperations operations) {
                for (CourseCachePayload payload : payloads) {
                    Long courseId = payload.courseId();

                    // 4.1 重建「已选学生集合」：先删后写，保证与数据库一致（幂等）
                    // 空集合时 key 不存在，expire 不生效；此后首次选课由 select_course.lua 补齐 TTL
                    String selectedKey = RedisKeys.COURSE_SELECTED + courseId;
                    operations.delete(selectedKey);
                    if (!payload.studentIds().isEmpty()) {
                        operations.opsForSet().add(selectedKey, payload.studentIds().toArray(new String[0]));
                        operations.expire(selectedKey, RedisKeys.DEFAULT_CACHE_MINUTES, TimeUnit.MINUTES);
                    }

                    // 4.2 写入「剩余容量」：覆盖写，即使为 0 也要写；容量 key 与集合 key 同生命周期
                    String capacityKey = RedisKeys.COURSE_CAPACITY + courseId;
                    operations.opsForValue().set(capacityKey, String.valueOf(payload.remaining()), RedisKeys.DEFAULT_CACHE_MINUTES, TimeUnit.MINUTES);
                }
                return null;
            }
        });

        log.info("[缓存预热] 完成，课程数={}，涉及选课记录数={}，semesterId={}", payloads.size(), studentCount, semesterId);
    }

    @Override
    public void syncSelectionCount() {
        // TODO 实现选课人数一致性同步（定时兜底任务）
        //  1. 遍历正常状态的课程
        //  2. 统计数据库真实选课数（course_selection 中 status = 1 的记录数）
        //  3. 与 course.selected_count 比对，不一致则：
        //     - courseMapper.syncSelectedCount(courseId) 重置数据库计数
        //     - 用真实值重算并覆盖 Redis 余量
        //  完成后可将 SelectionSyncTask.TASK_ENABLED 置为 true 启用定时任务
        throw new UnsupportedOperationException("TODO：同步选课人数 尚未实现，请参考 docs/待实现功能.md");
    }

    /**
     * 可选课程查询（学生视角）
     *
     * @param query     查询条件
     * @param studentId 学生ID，用于标记 {@code selected} 字段
     * @return
     */
    @Override
    public PageResult<Course> pageAvailableCourses(CourseQueryDTO query, Long studentId) {
        // 1. 分页查询课程（SQL 已联表教师、院系、学期，并支持各类筛选条件）
        IPage<Course> page = courseMapper.selectCoursePage(new Page<>(query.getPageNum(), query.getPageSize()), query);
        List<Course> courses = page.getRecords();
        if (courses.isEmpty()) {
            return PageResult.of(page);
        }
        //  2. 批量读取 Redis 余量（multiGet），为空则回退到 course.selected_count 计算
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, Integer> remainingByCourse = loadRemainingCapacity(courseIds);
        //  3. 查询该学生已选课程ID集合，为每门课程设置 selected 标记
        Set<Long> selectedCourseIds = new HashSet<>();
        Long semesterId = query.getSemesterId() != null ? query.getSemesterId() : semesterService.getCurrentSemesterId();
        if (semesterId != null) {
            selectedCourseIds.addAll(baseMapper.selectSelectedCourseIds(studentId, semesterId));
        }
        // 4. 逐门课程填充「实时余量」与「是否已选」两个展示字段
        for (Course course : courses) {
            course.setRemainingCapacity(remainingByCourse.get(course.getId()));
            course.setSelected(selectedCourseIds.contains(course.getId()));
        }
        return PageResult.of(page);
    }

    /**
     * 批量获取课程实时余量。
     *
     * <p>优先读取 Redis；Redis 中缺失的课程按数据库兜底值补充，
     * 避免因缓存未预热导致列表余量全部为空。</p>
     *
     * @param courseIds 课程ID列表（非空）
     * @return 课程ID -> 剩余容量
     */
    private Map<Long, Integer> loadRemainingCapacity(List<Long> courseIds) {
        List<String> capacityKeys = courseIds.stream().map(id -> RedisKeys.COURSE_CAPACITY + id).toList();
        // 一次取回全部容量值，顺序与 courseIds 一一对应；不存在的 key 返回 null
        List<String> values = stringRedisTemplate.opsForValue().multiGet(capacityKeys);
        Map<Long, Integer> result = new HashMap<>(courseIds.size());
        List<Long> cacheMissIds = new ArrayList<>();
        for (int i = 0; i < courseIds.size(); i++) {
            String value = values == null ? null : values.get(i);
            if (StrUtil.isBlank(value)) {
                cacheMissIds.add(courseIds.get(i));
            } else {
                result.put(courseIds.get(i), Integer.parseInt(value));
            }
        }
        // 缓存缺失的回退到数据库计算：余量 = 最大容量 - 已选人数（不小于 0）
        if (!cacheMissIds.isEmpty()) {
            List<Course> courses = courseMapper.selectBatchIds(cacheMissIds);
            for (Course course : courses) {
                int maxCapacity = course.getMaxCapacity() == null ? 0 : course.getMaxCapacity();
                int selectedCount = course.getSelectedCount() == null ? 0 : course.getSelectedCount();
                result.put(course.getId(), Math.max(maxCapacity - selectedCount, 0));
            }
        }
        return result;
    }
}
