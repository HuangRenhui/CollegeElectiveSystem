package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.*;
import com.college.elective.config.ElectiveProperties;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseSchedule;
import com.college.elective.entity.CourseSelection;
import com.college.elective.entity.Semester;
import com.college.elective.entity.Student;
import com.college.elective.mapper.CourseMapper;
import com.college.elective.mapper.CourseScheduleMapper;
import com.college.elective.mapper.CourseSelectionMapper;
import com.college.elective.mapper.StudentMapper;
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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 选课服务实现。
 *
 * <h3>实现状态</h3>
 * <ul>
 *   <li><b>已实现</b>：{@link #selectCourse(Long)}（含缓存回源 {@code prepareCourseCache}）、
 *       {@link #dropCourse(Long)}、{@link #syncSelectionCount()}、{@link #checkConflict(Long)}、
 *       {@link #pageAvailableCourses}、{@link #pageMySelections}、
 *       {@link #pageCourseStudents}、{@link #preloadCourseCache}——
 *       {@code CourseSelectionService} 接口声明的方法已<b>全部实现</b>。</li>
 *   <li><b>尚未实现</b>：教务代选能力（查询学生选课记录、代选、代退选）。
 *       这 3 个方法目前<b>连接口都尚未声明</b>，需要「接口 + 实现 + Controller」
 *       三层一起补，详见 {@code docs/待实现功能.md} 第 2.6 节。</li>
 * </ul>
 *
 * <h3>学期基准（重要）</h3>
 * <p>学分统计与时间冲突检测<b>一律以「课程所属学期」为基准</b>，而不是「当前学期」。
 * 课程可能不属于管理员当前设置的学期，若两处各取各的基准，
 * 会出现「学分统计在 A 学期、冲突比对在 B 学期」的数据错位。</p>
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseSelectionServiceImpl extends ServiceImpl<CourseSelectionMapper, CourseSelection>
    implements CourseSelectionService {

    /**
     * 课程 Mapper：查询课程详情、校验选课权限、同步数据库侧的已选人数计数
     */
    private final CourseMapper courseMapper;

    /**
     * 学期服务：选课开关与时间窗口以「当前学期」为准；冲突检测的学期由调用方显式传入
     */
    private final SemesterService semesterService;

    /**
     * 排课 Mapper：冲突检测需要目标课程与该生已选课程的排课明细
     */
    private final CourseScheduleMapper scheduleMapper;

    /**
     * Redis 模板：以 String 序列化读写容量与已选学生集合，编码方式必须与 Lua 脚本一致
     */
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 选课原子脚本：查重 + 余量扣减 + 写入已选集合（resources/lua/select_course.lua）
     */
    private final DefaultRedisScript<Long> selectCourseScript;

    /**
     * 退课原子脚本：移出已选集合 + 归还余量（resources/lua/drop_course.lua），也用于预占回滚
     */
    private final DefaultRedisScript<Long> dropCourseScript;

    /** 持锁重建容量与已选集合，避免 Java 侧分段写入被并发预占插队 */
    private final DefaultRedisScript<Long> rebuildCourseCacheScript;

    /** 按锁令牌释放回源短锁，避免误删后来者持有的锁 */
    private final DefaultRedisScript<Long> releaseLockScript;

    /** 选课落库前锁定学生行，串行化学分与冲突复检 */
    private final StudentMapper studentMapper;

    private final PlatformTransactionManager transactionManager;

    /**
     * 业务配置：读取选课开关与学分上限（对应 application.yml 的 elective.selection.*）
     */
    private final ElectiveProperties properties;

    // ---------------------------- Lua 脚本返回码 ----------------------------
    // 取值与 resources/lua/select_course.lua 的 return 一一对应，任一侧改动必须同步修改

    /**
     * 预占成功，可以继续落库
     */
    private static final int LUA_OK = 0;

    /**
     * 该学生已存在于已选集合中（重复选课）
     */
    private static final int LUA_ALREADY_SELECTED = 1;

    /**
     * 余量不足（容量已扣减到 0）
     */
    private static final int LUA_FULL = 2;

    /**
     * 容量 key 不存在：缓存尚未就绪，需要先回源重建
     */
    private static final int LUA_CACHE_NOT_READY = 3;

    /**
     * 退课脚本：正在回源，调用方应稍后重试
     */
    private static final int LUA_DROP_LOCKED = 2;

    /**
     * 非预期返回码（含极端情况下脚本返回 null），统一走「系统繁忙」分支
     */
    private static final int LUA_UNEXPECTED = -1;

    // ---------------------------- 缓存回源自旋参数 ----------------------------

    /**
     * 未抢到回源锁时的最大自旋次数
     */
    private static final int CACHE_LOCK_SPIN_MAX_RETRY = 5;

    /**
     * 每次自旋前的等待毫秒数（5 × 50ms = 最长等待 250ms）
     */
    private static final long CACHE_LOCK_SPIN_INTERVAL_MILLIS = 50L;

    /**
     * 回源短锁的持有时长（秒），需大于一次「查库 + 写 Redis」的最长耗时
     */
    private static final long CACHE_LOCK_TTL_SECONDS = 3L;

    /**
     * 学生选课。
     *
     * <p><b>整体流程</b>（任一步失败即中断，已完成的 Redis 预占会被归还）：</p>
     * <ol>
     *   <li>从登录态取当前学生 ID，不接受请求参数，避免越权为他人选课；</li>
     *   <li>校验选课开关与选课时间窗口；</li>
     *   <li>校验课程存在且状态正常；</li>
     *   <li>校验学分上限；</li>
     *   <li>检测上课时间冲突；</li>
     *   <li>确保 Redis 缓存就绪（必要时从数据库回源）；</li>
     *   <li>执行 Lua 脚本原子预占；</li>
     *   <li>落库写入选课记录；</li>
     *   <li>同步数据库侧的已选人数计数。</li>
     * </ol>
     *
     * <p>Redis 预占不纳入事务，避免自旋等待占用数据库连接。落库通过
     * {@link TransactionTemplate} 开启事务：锁定学生行后复检学分与冲突，
     * 再写入选课记录并同步计数。失败时归还 Redis 预占。</p>
     *
     * @param courseId 课程ID
     * @return 选课结果，包含最新的 Redis 剩余容量
     */
    @Override
    public SelectionResultVO selectCourse(Long courseId) {
        Long studentId = SecurityUtils.requireStudentId();

        checkSelectionOpen();

        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        BusinessException.throwIf(!Constants.COURSE_STATUS_NORMAL.equals(course.getStatus()),
            ResultCode.COURSE_NOT_FOUND, "该课程当前不可选（已下架或已结课）");

        Long semesterId = course.getSemesterId();
        BusinessException.throwIf(semesterId == null, ResultCode.COURSE_NOT_FOUND, "课程未关联学期，无法选课");

        checkCreditLimit(studentId, semesterId, course);
        List<ConflictVO> conflicts = findConflicts(studentId, courseId, semesterId);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(ResultCode.COURSE_TIME_CONFLICT, conflicts.get(0).getDescription());
        }

        prepareCourseCache(courseId, course);

        int result = normalizeLuaResult(executeSelectCourseScript(courseId, studentId));
        if (result == LUA_CACHE_NOT_READY) {
            prepareCourseCache(courseId, course);
            result = normalizeLuaResult(executeSelectCourseScript(courseId, studentId));
        }
        switch (result) {
            case LUA_OK -> {
            }
            case LUA_ALREADY_SELECTED -> throw new BusinessException(ResultCode.COURSE_ALREADY_SELECTED);
            case LUA_FULL -> throw new BusinessException(ResultCode.COURSE_FULL);
            case LUA_CACHE_NOT_READY -> throw new BusinessException(ResultCode.COURSE_CACHE_NOT_READY);
            default -> throw new BusinessException(ResultCode.SYSTEM_BUSY);
        }

        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                lockStudentRow(studentId);
                checkCreditLimit(studentId, semesterId, course);
                List<ConflictVO> lockedConflicts = findConflicts(studentId, courseId, semesterId);
                if (!lockedConflicts.isEmpty()) {
                    throw new BusinessException(ResultCode.COURSE_TIME_CONFLICT,
                        lockedConflicts.get(0).getDescription());
                }
                persistSelection(studentId, courseId, semesterId);
                if (courseMapper.increaseSelectedCount(courseId) == 0) {
                    throw new BusinessException(ResultCode.COURSE_FULL);
                }
            });
        } catch (RuntimeException e) {
            rollbackReservation(courseId, studentId);
            throw e;
        }

        Integer remaining = currentCapacity(courseId);
        log.info("[选课] 成功 studentId={}, courseId={}, 剩余容量={}", studentId, courseId, remaining);
        return new SelectionResultVO(true, courseId, course.getCourseName(), remaining, "选课成功");
    }

    /**
     * 锁定学生行，使同一学生的选课落库串行化，避免学分/冲突的 TOCTOU。
     */
    private void lockStudentRow(Long studentId) {
        Student student = studentMapper.selectOne(Wrappers.<Student>lambdaQuery()
            .eq(Student::getId, studentId)
            .last("FOR UPDATE"));
        BusinessException.throwIf(student == null, ResultCode.STUDENT_NOT_FOUND);
    }

    /**
     * 把 Lua 脚本的原始返回值归一化为统一的返回码。
     *
     * <p>{@code StringRedisTemplate#execute} 的返回类型是包装类型 {@link Long}，
     * 极端情况下可能为 {@code null}；此处统一折算为 {@link #LUA_UNEXPECTED}，
     * 由 switch 的 default 分支按「系统繁忙」处理，避免直接拆箱触发 NPE、
     * 被全局异常处理器包装成 500 系统错误。</p>
     *
     * @param result 脚本原始返回值，可能为 {@code null}
     * @return 0-3 为脚本约定的返回码，-1 表示非预期返回
     */
    private int normalizeLuaResult(Long result) {
        return result == null ? LUA_UNEXPECTED : result.intValue();
    }

    /**
     * 读取课程当前的 Redis 剩余容量，用于组装返回给前端的提示信息。
     *
     * @param courseId 课程ID
     * @return 剩余容量；key 不存在（缓存已过期）时返回 {@code null}，前端展示为「-」
     */
    private Integer currentCapacity(Long courseId) {
        String value = stringRedisTemplate.opsForValue().get(RedisKeys.COURSE_CAPACITY + courseId);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        Integer parsed = parseCapacity(value);
        if (parsed == null) {
            log.warn("[选课] 容量缓存不是合法整数，courseId={}, value={}", courseId, value);
        }
        return parsed;
    }

    /**
     * 归还 Redis 预占（选课失败时的回滚）。
     *
     * <p><b>为什么复用退课脚本</b>：退课与「回滚预占」要做的事完全一致——
     * 把学生从已选集合中移除并归还一个余量，因此直接执行 {@code drop_course.lua} 即可，
     * 无需为回滚单独维护一个脚本，避免两份语义各自的漂移。</p>
     *
     * <p><b>为什么吞掉异常</b>：本方法处在异常处理路径上，若回滚本身再抛异常，
     * 会覆盖掉导致回滚的原始业务异常，让前端看到错误的失败原因。
     * 回滚失败只记录 error 日志，最终一致性由 {@code SelectionSyncTask} 定时校正兜底。</p>
     *
     * @param courseId  课程ID
     * @param studentId 学生ID
     */
    private void rollbackReservation(Long courseId, Long studentId) {
        try {
            returnReservation(courseId, studentId);
        } catch (Exception ex) {
            log.error("[选课回滚] 归还 Redis 预占失败，courseId={}, studentId={}，将由 SelectionSyncTask 校正",
                courseId, studentId, ex);
        }
    }

    /**
     * 落库写入选课记录：无记录则新增，已有历史记录则复用。
     *
     * <p><b>为什么要处理「复用」</b>：{@code course_selection} 上有唯一键
     * {@code uk_student_course_semester(student_id, course_id, semester_id)}，
     * 同一学生对同一门课在同一学期只可能存在一行；而退课是「置 status = 0」的软删除，
     * 记录会保留下来。因此重新选课时不能直接 insert（会撞唯一键），必须复用原行。</p>
     *
     * <p><b>复用时的字段处理</b>：置 {@code status = 1}、刷新 {@code selectTime}、
     * 清空 {@code dropTime}、并把 {@code selectType} 重置为 1（正常选课）。</p>
     *
     * <p><b>为什么用 LambdaUpdateWrapper 而不是 updateById</b>：MyBatis-Plus 的
     * {@code updateById} 默认忽略值为 {@code null} 的字段（{@code FieldStrategy.NOT_NULL}），
     * 因此 {@code dropTime} 传 null 根本不会被写进 SQL，退课痕迹清不掉。
     * 必须用 {@code set(SFunction, null)} 显式声明，生成的 SQL 才会是 {@code drop_time = null}。</p>
     *
     * <p><b>关于重修（existing.status = 2）</b>：此处<b>刻意不清空</b> {@code score}。
     * 原因是 {@code score} 只是便于列表展示的冗余字段，成绩的权威数据在 {@code course_grade} 表，
     * 重修成绩应由成绩录入流程覆盖写入；在这里清空反而会丢失上一次的成绩展示值。
     * 若后续要求重修必须清空历史成绩，应改为同时清理 {@code course_grade}，而不是只清冗余字段。</p>
     *
     * @param studentId  学生ID
     * @param courseId   课程ID
     * @param semesterId 学期ID（课程所属学期）
     */
    private void persistSelection(Long studentId, Long courseId, Long semesterId) {
        CourseSelection existing = baseMapper.selectOne(Wrappers.<CourseSelection>lambdaQuery()
            .eq(CourseSelection::getStudentId, studentId)
            .eq(CourseSelection::getCourseId, courseId)
            .eq(CourseSelection::getSemesterId, semesterId));

        // 数据库侧的二重防重：Redis 集合已查过一次，此处再以数据库为准兜底，
        // 避免「缓存丢失 → 回源后 SISMEMBER 未命中」时重复插入有效记录
        BusinessException.throwIf(existing != null && Constants.SELECTION_SELECTED.equals(existing.getStatus()),
            ResultCode.COURSE_ALREADY_SELECTED);

        LocalDateTime now = LocalDateTime.now();

        // 场景一：从未选过该课程，新增记录
        if (existing == null) {
            CourseSelection selection = new CourseSelection();
            selection.setStudentId(studentId);
            selection.setCourseId(courseId);
            selection.setSemesterId(semesterId);
            selection.setSelectTime(now);
            selection.setStatus(Constants.SELECTION_SELECTED);
            // 选课方式：1-正常选课，2-管理员代选
            selection.setSelectType(1);
            baseMapper.insert(selection);
            return;
        }

        // 场景二：存在历史记录（status = 0 已退选，或 status = 2 已修完重修），复用该行
        baseMapper.update(null, Wrappers.<CourseSelection>lambdaUpdate()
            .eq(CourseSelection::getId, existing.getId())
            .set(CourseSelection::getStatus, Constants.SELECTION_SELECTED)
            .set(CourseSelection::getSelectTime, now)
            .set(CourseSelection::getDropTime, null)
            .set(CourseSelection::getSelectType, 1));
    }

    /**
     * 执行选课原子脚本。
     *
     * <p>参数约定必须与 {@code resources/lua/select_course.lua} 保持一致：</p>
     * <ul>
     *   <li>KEYS[1] = {@code elective:course:capacity:{courseId}}（String，剩余容量）</li>
     *   <li>KEYS[2] = {@code elective:course:selected:{courseId}}（Set，已选学生ID）</li>
     *   <li>ARGV[1] = studentId</li>
     *   <li>ARGV[2] = TTL 秒数，用于让容量 key 与集合 key 保持同一生命周期</li>
     * </ul>
     *
     * <p><b>为什么用 StringRedisTemplate</b>：脚本内部用 {@code GET/DECR/SADD} 直接操作字符串数值，
     * 必须用 String 序列化器读写；若改用带 JSON 序列化的 {@code RedisTemplate}，
     * 写进去的值会多一层引号，导致 {@code tonumber(capacity)} 得到 nil 而误判为缓存未就绪。</p>
     *
     * @param courseId  课程ID
     * @param studentId 学生ID
     * @return 脚本返回码：0-成功 / 1-已选过 / 2-余量不足 / 3-缓存未就绪
     */
    private Long executeSelectCourseScript(Long courseId, Long studentId) {
        return stringRedisTemplate.execute(
            selectCourseScript,
            List.of(
                RedisKeys.COURSE_CAPACITY + courseId,
                RedisKeys.COURSE_SELECTED + courseId,
                RedisKeys.COURSE_SELECT_LOCK + courseId),
            String.valueOf(studentId),
            String.valueOf(RedisKeys.DEFAULT_CACHE_SECONDS));
    }

    /**
     * 确保课程缓存就绪：容量 key 不存在时，依据数据库重建「剩余容量 + 已选学生集合」。
     *
     * <p><b>为什么必须加短锁</b>：回源是「读数据库 → 写 Redis」两步且非原子。
     * 若多个线程同时回源，后写的线程会用它读取到的旧数据覆盖容量，
     * 把并发期间其它线程已经扣减掉的余量「还」回去，造成超卖。
     * 因此同一时刻只允许一个线程真正执行回源。</p>
     *
     * <p><b>为什么未抢到锁时要自旋等待，而不是直接返回</b>：
     * 未抢到锁说明此刻有线程正在回源、容量 key 尚未写入。
     * 若立即返回，紧随其后的 Lua 脚本必然返回 3（缓存未就绪），
     * 在「首次开放选课」这类缓存大面积失效的瞬间会产生大量 3010 失败。
     * 这里改为短暂自旋探测（最多 {@link #CACHE_LOCK_SPIN_MAX_RETRY} 次、
     * 每次间隔 {@link #CACHE_LOCK_SPIN_INTERVAL_MILLIS} 毫秒）：
     * 持锁线程一旦写成功即可立即继续，把瞬时失败率降到最低。</p>
     *
     * <p><b>最坏情况</b>：自旋耗尽仍未就绪，则由 {@link #selectCourse} 在 Lua 返回 3 时
     * 回源重试一次，最终仍失败才返回 3010。这是可接受的降级——自旋总耗时（最坏 250ms）
     * 远小于「回源查库 + 写 Redis」的耗时上限。</p>
     *
     * @param courseId 课程ID
     * @param course   课程详情，提供 maxCapacity，避免回源时再查一次库
     */
    private void prepareCourseCache(Long courseId, Course course) {
        String capacityKey = RedisKeys.COURSE_CAPACITY + courseId;

        // 快路径：缓存已就绪。绝大多数请求走这里，不产生任何额外 Redis 开销
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(capacityKey))) {
            return;
        }

        // 慢路径：抢回源锁。SET NX EX 保证只有一个线程能进入重建逻辑
        String lockKey = RedisKeys.COURSE_SELECT_LOCK + courseId;
        String lockToken = UUID.randomUUID().toString();
        Boolean locked = stringRedisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockToken, Duration.ofSeconds(CACHE_LOCK_TTL_SECONDS));

        if (!Boolean.TRUE.equals(locked)) {
            awaitCacheReady(capacityKey);
            return;
        }

        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(capacityKey))) {
                return;
            }
            rebuildCourseCacheLocked(courseId, course, lockToken);
            log.info("[选课回源] 缓存重建完成 courseId={}", courseId);
        } finally {
            releaseRebuildLock(lockKey, lockToken);
        }
    }

    /**
     * 以「数据库有效选课 ∪ Redis 已有成员」重建缓存，保留尚未落库的预占，避免回源超选。
     * 调用前必须已持有回源锁；select/drop 脚本在锁存在时不会改这两个 key。
     */
    private void rebuildCourseCacheLocked(Long courseId, Course course, String lockToken) {
        List<CourseSelection> validSelections =
            baseMapper.selectValidSelectionsByCourseIds(List.of(courseId));
        LinkedHashSet<String> members = new LinkedHashSet<>();
        for (CourseSelection selection : validSelections) {
            members.add(String.valueOf(selection.getStudentId()));
        }
        Set<String> cached = stringRedisTemplate.opsForSet().members(RedisKeys.COURSE_SELECTED + courseId);
        if (cached != null) {
            members.addAll(cached);
        }

        int maxCapacity = course.getMaxCapacity() == null ? 0 : course.getMaxCapacity();
        int remaining = Math.max(maxCapacity - members.size(), 0);

        List<String> args = new ArrayList<>();
        args.add(lockToken);
        args.add(String.valueOf(remaining));
        args.add(String.valueOf(RedisKeys.DEFAULT_CACHE_SECONDS));
        args.addAll(members);

        stringRedisTemplate.execute(
            rebuildCourseCacheScript,
            List.of(
                RedisKeys.COURSE_CAPACITY + courseId,
                RedisKeys.COURSE_SELECTED + courseId,
                RedisKeys.COURSE_SELECT_LOCK + courseId),
            args.toArray());
    }

    private void releaseRebuildLock(String lockKey, String lockToken) {
        stringRedisTemplate.execute(releaseLockScript, List.of(lockKey), lockToken);
    }

    /**
     * 自旋等待容量 key 被其它线程写入就绪。
     *
     * <p>每次重试前先休眠固定间隔，再探测一次 key，命中即返回。
     * 全部重试耗尽后静默返回，由调用方按「缓存仍未就绪」继续后续流程。</p>
     *
     * <p>本方法不再处于数据库事务内，自旋不会占用连接池。</p>
     *
     * @param capacityKey 容量 key
     */
    private void awaitCacheReady(String capacityKey) {
        for (int i = 0; i < CACHE_LOCK_SPIN_MAX_RETRY; i++) {
            try {
                Thread.sleep(CACHE_LOCK_SPIN_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                // 恢复中断标记后立即退出等待，不吞掉中断状态，交由上层按未就绪处理
                Thread.currentThread().interrupt();
                return;
            }
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(capacityKey))) {
                return;
            }
        }
        log.debug("[选课回源] 自旋等待缓存就绪超时，capacityKey={}", capacityKey);
    }

    /**
     * 学分上限校验：本学期已选学分 + 本课程学分 不得超过配置上限。
     *
     * <p>已选学分由 {@code CourseSelectionMapper#sumSelectedCredit} 统计，
     * 该 SQL 内层使用 {@code IFNULL(SUM(...), 0)} 且按 {@code status = 1} 过滤，
     * 因此无选课记录时返回 0（而非 null），也不会计入已退选与已修完的记录。</p>
     *
     * <p><b>学期口径</b>：{@code semesterId} 由调用方传入「课程所属学期」，
     * 与本次选课要落入的学期保持一致，避免与其它学期的学分混算。</p>
     *
     * @param studentId  学生ID
     * @param semesterId 学期ID（课程所属学期）
     * @param course     待选课程
     */
    private void checkCreditLimit(Long studentId, Long semesterId, Course course) {
        BigDecimal selected = baseMapper.sumSelectedCredit(studentId, semesterId);
        BigDecimal credit = course.getCredit() == null ? BigDecimal.ZERO : course.getCredit();
        BigDecimal total = selected.add(credit);
        BigDecimal limit = BigDecimal.valueOf(properties.getSelection().getMaxCredit());

        if (total.compareTo(limit) > 0) {
            // stripTrailingZeros + toPlainString：避免提示出现「30.00」或被格式化成科学计数法
            throw new BusinessException(ResultCode.CREDIT_LIMIT_EXCEEDED,
                String.format("选课后总学分将达到 %s，超过本学期上限 %s 学分",
                    total.stripTrailingZeros().toPlainString(),
                    limit.stripTrailingZeros().toPlainString()));
        }
    }

    /**
     * 选课开关与选课时间窗口校验，四项全部通过才放行。
     *
     * <p>校验顺序按「代价从低到高」排列，尽量在触及数据库之前就拒绝掉无效请求：</p>
     * <ol>
     *   <li><b>全局配置开关</b>：{@code elective.selection.enabled}，通常用于紧急停用选课；</li>
     *   <li><b>Redis 动态开关</b>：{@code elective:selection:switch}，管理员可在运行时切换，
     *       约定值 {@code off} 表示关闭，其余（含 key 不存在）一律视为开启；</li>
     *   <li><b>学期状态</b>：{@code semester.status = 1}（选课中）。该字段由管理员手工维护；</li>
     *   <li><b>选课时间窗口</b>：{@code semester.selectStartTime ~ selectEndTime}，
     *       由 {@link Semester#inSelectionPeriod()} 按服务器时间自动判定。</li>
     * </ol>
     *
     * <p><b>为什么状态与时间窗口取交集</b>：两者可能不一致——例如管理员忘记把状态改为
     * 「选课中」，但设置的时间已到。当前设计采取保守策略（都满足才放行），
     * 宁可拒绝也不能让未确认的选课窗口意外开放。若日后希望「时间到就自动开放」，
     * 需要改为并集，或用定时任务同步 {@code status}，那是一处明确的产品决策。</p>
     */
    private void checkSelectionOpen() {
        // 1. 全局配置开关
        BusinessException.throwIf(!properties.getSelection().isEnabled(), ResultCode.SELECTION_CLOSED);

        // 2. Redis 动态开关：只有显式的 off 才算关闭，key 不存在按开启处理，避免运维漏配导致选课不可用
        String state = stringRedisTemplate.opsForValue().get(RedisKeys.SELECTION_SWITCH);
        BusinessException.throwIf("off".equalsIgnoreCase(state), ResultCode.SELECTION_CLOSED);

        // 3. 学期状态
        Semester semester = semesterService.getCurrentSemester();
        BusinessException.throwIf(semester == null, ResultCode.CURRENT_SEMESTER_NOT_SET);
        BusinessException.throwIf(!Constants.SEMESTER_STATUS_SELECTING.equals(semester.getStatus()),
            ResultCode.SELECTION_NOT_OPEN, "当前学期未开放选课");

        // 4. 选课时间窗口
        BusinessException.throwIf(!semester.inSelectionPeriod(),
            ResultCode.SELECTION_NOT_OPEN, "当前不在选课时间段内");
    }

    /**
     * 学生退课。
     *
     * <p><b>流程</b>：课程可退选校验 → 定位选课记录 → 已录成绩拦截
     * → 归还 Redis 余量 → 记录置为已退选（软删除）→ 同步数据库计数。</p>
     *
     * <p><b>为什么采用软删除而非物理删除</b>：{@code course_selection} 上有唯一键
     * {@code uk_student_course_semester(student_id, course_id, semester_id)}，
     * 物理删除会让审计痕迹丢失；保留记录（{@code status = 0} + {@code dropTime}）
     * 既便于审计，也让重新选课能复用同一行，避免插入时撞唯一键。</p>
     *
     * <p><b>为什么 Redis 归还失败不阻断退课</b>：数据库才是权威数据源。
     * 若因缓存异常让学生「退不了课」，体验损失远大于一次余量偏差；
     * 偏差交由 {@link #syncSelectionCount()} 的定时任务校正。</p>
     *
     * <p><b>为什么不受选课时间窗口限制</b>：退课只受 {@code course.selectable} 约束，
     * 与选课开关、学期状态、选课时间窗口均无关——学生不应因窗口关闭而被锁定在课程中。</p>
     *
     * <p><b>事务边界</b>：方法内有两处写库（选课记录置为已退选、课程计数 -1），
     * 必须处于同一事务，否则第二步失败会留下「已退课但计数没减」的不一致。</p>
     *
     * @param courseId 课程ID
     * @return 退课结果，包含最新剩余容量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SelectionResultVO dropCourse(Long courseId) {
        Long studentId = SecurityUtils.requireStudentId();
        // 1. 课程存在且允许退选（course.selectable = 1）
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        BusinessException.throwIf(!Constants.STATUS_ENABLED.equals(course.getSelectable()),
            ResultCode.DROP_NOT_ALLOWED, "该课程不允许退选");
        Long semesterId = course.getSemesterId();
        BusinessException.throwIf(semesterId == null, ResultCode.COURSE_NOT_FOUND, "课程未关联学期，无法退课");

        // 2. 定位该生在本课程上的选课记录
        CourseSelection selection = baseMapper.selectOne(Wrappers.<CourseSelection>lambdaQuery()
            .eq(CourseSelection::getStudentId, studentId)
            .eq(CourseSelection::getCourseId, courseId)
            .eq(CourseSelection::getSemesterId, semesterId));
        BusinessException.throwIf(selection == null, ResultCode.COURSE_NOT_SELECTED);

        // 记录存在但状态不是「已选课」时，区分两种语义给出准确提示：
        //   status = 0（已退选）→ 语义上等同「未选该课程」，返回 3004
        //   status = 2（已修完）→ 属于不允许退课，返回 3009
        if (Constants.SELECTION_DROPPED.equals(selection.getStatus())) {
            throw new BusinessException(ResultCode.COURSE_NOT_SELECTED);
        }
        BusinessException.throwIf(!Constants.SELECTION_SELECTED.equals(selection.getStatus()),
            ResultCode.DROP_NOT_ALLOWED, "该课程已修完，无法退课");

        // 3. 已录入成绩的课程禁止退课（score 为冗余字段，由成绩录入流程回写）
        BusinessException.throwIf(selection.getScore() != null,
            ResultCode.DROP_NOT_ALLOWED, "该课程已录入成绩，无法退课");

        // 4. 归还 Redis 余量。脚本返回 0-归还成功 / 1-该生不在已选集合中
        //    注意：Redis 失败不阻断退课——数据库才是权威数据，
        //    余量偏差由 SelectionSyncTask 定时校正；若在此抛异常，会让学生「退不了课」
        returnReservation(courseId, studentId);
        // 5. 记录置为已退选（软删除，不物理删除）
        LocalDateTime now = LocalDateTime.now();
        baseMapper.update(null, Wrappers.<CourseSelection>lambdaUpdate()
            .eq(CourseSelection::getId, selection.getId())
            .set(CourseSelection::getStatus, Constants.SELECTION_DROPPED)
            .set(CourseSelection::getDropTime, now));
        // 6. 同步数据库计数（SQL 自带 selected_count > 0 条件，不会减成负数）
        courseMapper.decreaseSelectedCount(courseId);
        Integer remaining = currentCapacity(courseId);
        log.info("[退课] 成功 studentId={}, courseId={}, 剩余容量={}", studentId, courseId, remaining);
        return new SelectionResultVO(true, courseId, course.getCourseName(), remaining, "退课成功");
    }

    /**
     * 归还 Redis 预占（退课 / 选课回滚共用）。
     *
     * <p>脚本返回 1（学生不在已选集合中）属正常情况——例如缓存回源时该生恰好已退课，
     * 此时无需归还余量，仅记录 warn 便于排查。</p>
     */
    private void returnReservation(Long courseId, Long studentId) {
        try {
            Long result = stringRedisTemplate.execute(
                dropCourseScript,
                List.of(
                    RedisKeys.COURSE_CAPACITY + courseId,
                    RedisKeys.COURSE_SELECTED + courseId,
                    RedisKeys.COURSE_SELECT_LOCK + courseId),
                String.valueOf(studentId),
                String.valueOf(RedisKeys.DEFAULT_CACHE_SECONDS));
            if (result != null && result == LUA_DROP_LOCKED) {
                awaitCacheReady(RedisKeys.COURSE_CAPACITY + courseId);
                result = stringRedisTemplate.execute(
                    dropCourseScript,
                    List.of(
                        RedisKeys.COURSE_CAPACITY + courseId,
                        RedisKeys.COURSE_SELECTED + courseId,
                        RedisKeys.COURSE_SELECT_LOCK + courseId),
                    String.valueOf(studentId),
                    String.valueOf(RedisKeys.DEFAULT_CACHE_SECONDS));
            }
            if (result != null && result == 1L) {
                log.warn("[退课] 学生不在 Redis 已选集合中，未归还余量，courseId={}, studentId={}",
                    courseId, studentId);
            }
        } catch (Exception ex) {
            // 交由定时任务校正，不阻断退课
            log.error("[退课] 归还 Redis 余量失败，courseId={}, studentId={}，将由 SelectionSyncTask 校正",
                courseId, studentId, ex);
        }
    }

    @Override
    public PageResult<CourseSelection> pageMySelections(Long pageNum, Long pageSize, Long semesterId, Integer status) {
        Long studentId = SecurityUtils.requireStudentId();
        IPage<CourseSelection> page = baseMapper.selectStudentSelections(
            new Page<>(Pages.pageNum(pageNum), Pages.pageSize(pageSize)), studentId, semesterId, status);
        return PageResult.of(page);
    }

    @Override
    public PageResult<CourseSelection> pageCourseStudents(Long courseId, Long pageNum, Long pageSize, String keyword) {
        checkCourseAccess(courseId);
        IPage<CourseSelection> page = baseMapper.selectCourseStudents(
            new Page<>(Pages.pageNum(pageNum), Pages.pageSize(pageSize)), courseId, keyword);
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

    /**
     * 选课前的冲突预检（对外接口），返回冲突明细，不产生任何副作用。
     *
     * <p><b>学期基准</b>：与 {@link #selectCourse(Long)} 一致，取课程所属学期。</p>
     *
     * <p>当前学期未设置时返回 {@code 5005} 而不是静默返回空列表：
     * 冲突检测 SQL 中 {@code semester_id} 是硬编码条件，传入 null 会查不到任何数据，
     * 从而让「检测通过」变成假象，这是必须避免的静默失效。</p>
     *
     * @param courseId 课程ID
     * @return 冲突列表，无冲突时返回空集合
     */
    @Override
    public List<ConflictVO> checkConflict(Long courseId) {
        Long studentId = SecurityUtils.requireStudentId();

        // 课表按学期划分，冲突比对必须在同一学期内进行，否则会与历史学期课程误判
        Course course = courseMapper.selectCourseDetail(courseId);
        BusinessException.throwIf(course == null, ResultCode.COURSE_NOT_FOUND);
        Long semesterId = course.getSemesterId();
        BusinessException.throwIf(semesterId == null, ResultCode.CURRENT_SEMESTER_NOT_SET, "课程未关联学期，无法检测冲突");

        return findConflicts(studentId, courseId, semesterId);
    }

    /**
     * 按指定学期检测「目标课程」与该生「已选课程」的上课时间冲突。
     *
     * <p><b>学期为什么由调用方传入</b>：预检接口需要当前学期，而选课流程需要课程所属学期。
     * 把学期抽成参数，可让两处共用同一套判定逻辑，同时各自使用正确的基准，
     * 避免「学分按 A 学期算、冲突按 B 学期比」的错位。</p>
     *
     * <p>判定维度为「星期 + 节次区间 + 周次区间 + 周类型」，实现在
     * {@link ScheduleConflictUtils#isConflict(CourseSchedule, CourseSchedule)} 中，
     * 与排课校验（{@code CourseServiceImpl#validateScheduleConflict}）共用，避免两处行为不一致。</p>
     *
     * @param studentId  学生ID
     * @param courseId   目标课程ID
     * @param semesterId 比对所在学期ID
     * @return 冲突列表；目标课程无排课、或该生该学期无已选排课时返回空集合
     */
    private List<ConflictVO> findConflicts(Long studentId, Long courseId, Long semesterId) {
        // 目标课程的排课。没有排课（如纯线上课）时不存在冲突
        List<CourseSchedule> schedules = scheduleMapper.selectByCourseId(courseId);
        if (schedules.isEmpty()) {
            return Collections.emptyList();
        }

        // 该学生本学期的排课（SQL 已限定 status = 1，不含已退选）
        List<CourseSchedule> selectedSchedules = scheduleMapper.selectByStudentSelection(studentId, semesterId);
        if (selectedSchedules.isEmpty()) {
            return Collections.emptyList();
        }

        // 两两比对，命中即记录一条冲突信息，供前端逐条展示
        List<ConflictVO> conflicts = new ArrayList<>();
        for (CourseSchedule schedule : schedules) {
            for (CourseSchedule selected : selectedSchedules) {
                // 目标课程可能已在已选列表中（重复选课场景），跳过自身避免误报冲突
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

    /**
     * 同步选课人数：以数据库真实选课数为准，校正「数据库计数 + Redis 容量 + Redis 已选集合」。
     *
     * <p><b>为什么以数据库为准</b>：选课路径是「Redis 预占 → 落库」，Redis 是性能层、
     * 数据库是权威层。任何一次回滚失败或人工改库都可能造成偏差，
     * 用数据库反向校正 Redis 才是收敛方向。</p>
     *
     * <p><b>为什么容量与集合必须一起校正</b>：容量（String）决定能否继续选课，
     * 已选集合（Set）是 Lua 脚本中 {@code SISMEMBER} 查重的唯一依据，二者语义耦合：</p>
     * <ul>
     *   <li>集合残留已退课学生 → 该生重新选课被误判为「已选过」，返回 3003；</li>
     *   <li>集合缺失成员 → 同一学生可被重复选入（虽由数据库唯一键兜底，
     *       但要多走一次写库才会失败）。</li>
     * </ul>
     * <p>因此两者必须同源、同 TTL 一起写回（集合 key 缺少 TTL 正是历史上踩过的坑）。</p>
     *
     * <p><b>触发条件（三方比对）</b>：数据库计数、Redis 容量、集合基数任一不符即修复。
     * 若只比对数据库计数，则「数据库没问题、Redis 余量被写错」的场景会被直接跳过，
     * 而验收清单明确要求「手工制造数据偏差后，同步接口能校正」。</p>
     *
     * <p><b>性能取舍</b>：数据库侧用一次批量查询取出全部课程的选课记录并按课程分组，
     * 替代逐门 {@code selectCount}（N 次查询压缩为 1 次），分组结果同时就是重建集合的成员列表；
     * Redis 侧只对判定为偏差的课程执行写回，避免在选课进行中无谓刷新健康课程的缓存。</p>
     *
     * <p><b>建议执行时机</b>：低峰或关闭选课的时段。若在选课进行中运行，
     * 写回容量可能覆盖并发请求刚完成的扣减，造成 1 个余量的瞬时偏差。</p>
     */
    @Override
    public void syncSelectionCount() {
        // 1. 取全部在架课程（不按学期过滤：历史学期课程的计数同样需要校正）
        List<Course> courses = courseMapper.selectList(Wrappers.<Course>lambdaQuery()
            .eq(Course::getStatus, Constants.COURSE_STATUS_NORMAL));
        if (courses.isEmpty()) {
            log.warn("[人数同步] 无可同步课程");
            return;
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();

        // 2. 一次批量查出全部课程的有效选课记录（SQL 已限定 status = 1、deleted = 0），
        //    按 courseId 分组后：分组大小即真实人数，成员列表即重建 Redis 集合所需数据
        Map<Long, Set<String>> studentIdsByCourse = new HashMap<>();
        for (CourseSelection selection : baseMapper.selectValidSelectionsByCourseIds(courseIds)) {
            studentIdsByCourse
                .computeIfAbsent(selection.getCourseId(), key -> new HashSet<>())
                .add(String.valueOf(selection.getStudentId()));
        }

        int fixedCount = 0;
        for (Course course : courses) {
            Long courseId = course.getId();
            Set<String> studentIds = studentIdsByCourse.getOrDefault(courseId, Set.of());
            int real = studentIds.size();
            int recorded = course.getSelectedCount() == null ? 0 : course.getSelectedCount();
            int maxCapacity = course.getMaxCapacity() == null ? 0 : course.getMaxCapacity();
            int expected = Math.max(maxCapacity - real, 0);

            // 3. 三方比对，任一不符即需修复
            boolean dbMismatch = real != recorded;
            String cachedCapacity = stringRedisTemplate.opsForValue()
                .get(RedisKeys.COURSE_CAPACITY + courseId);
            boolean capacityMismatch = cachedCapacity == null
                || !String.valueOf(expected).equals(cachedCapacity);
            Long cachedSetSize = stringRedisTemplate.opsForSet()
                .size(RedisKeys.COURSE_SELECTED + courseId);
            int setSize = cachedSetSize == null ? 0 : cachedSetSize.intValue();
            boolean setMismatch = setSize != real;

            if (!dbMismatch && !capacityMismatch && !setMismatch) {
                continue;
            }

            // 4. 数据库计数偏差：按真实值重置（XML 内部按 status = 1 重新统计）
            if (dbMismatch) {
                courseMapper.syncSelectedCount(courseId);
            }

            String lockKey = RedisKeys.COURSE_SELECT_LOCK + courseId;
            String lockToken = UUID.randomUUID().toString();
            Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, lockToken, Duration.ofSeconds(CACHE_LOCK_TTL_SECONDS));
            if (Boolean.TRUE.equals(locked)) {
                try {
                    rebuildCourseCacheLocked(courseId, course, lockToken);
                } finally {
                    releaseRebuildLock(lockKey, lockToken);
                }
            } else {
                log.warn("[人数同步] 课程正在回源，跳过 Redis 写回 courseId={}", courseId);
            }

            fixedCount++;
            log.info("[人数同步] 校正课程 courseId={}, 库内计数 {} -> {}, 集合 {} -> {}, 余量重算为 {}",
                courseId, recorded, real, setSize, real, expected);
        }
        log.info("[人数同步] 完成，共检查 {} 门课程，校正 {} 门", courses.size(), fixedCount);
    }

    /**
     * 可选课程查询（学生视角），返回带「实时余量」与「是否已选」标记的课程分页。
     *
     * <p>余量优先读 Redis（选课期间的真实可用量），Redis 缺失的课程回退到
     * {@code maxCapacity - selectedCount} 计算，避免缓存未预热时列表余量全部为空。</p>
     *
     * @param query     查询条件，其 {@code semesterId} 为空时回退到当前学期
     * @param studentId 学生ID，用于标记每门课程的 {@code selected} 字段
     * @return 课程分页结果
     */
    @Override
    public PageResult<Course> pageAvailableCourses(CourseQueryDTO query, Long studentId) {
        // 1. 分页查询课程（SQL 已联表教师、院系、学期，并支持各类筛选条件）
        query.setPageNum(Pages.pageNum(query.getPageNum()));
        query.setPageSize(Pages.pageSize(query.getPageSize()));
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
            Integer parsed = parseCapacity(value);
            if (parsed == null) {
                cacheMissIds.add(courseIds.get(i));
            } else {
                result.put(courseIds.get(i), parsed);
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

    private Integer parseCapacity(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
