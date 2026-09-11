package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.dto.CourseQueryDTO;
import com.college.elective.entity.Course;
import com.college.elective.entity.CourseSelection;
import com.college.elective.vo.ConflictVO;
import com.college.elective.vo.SelectionResultVO;

import java.util.List;

/**
 * 选课服务。
 *
 * <p>本接口定义选课核心业务的能力契约，实现类尚未实现，需由开发者补充编码。
 * 请参考项目文档 {@code docs/待实现功能.md} 中的「选课模块」章节，
 * 关键设计要点如下：</p>
 *
 * <ul>
 *   <li><b>并发控制</b>：通过 Redis + Lua 脚本原子化完成「查重 → 余量校验 → 扣减 → 写入已选集合」，
 *       防止高并发场景下超选。脚本位于 {@code resources/lua/select_course.lua} 与
 *       {@code resources/lua/drop_course.lua}，已在 {@code RedisConfig} 中注册为 Bean。</li>
 *   <li><b>时间冲突</b>：基于「星期 + 节次区间 + 周次区间 + 单双周」四维判定，
 *       排课数据通过 {@code CourseScheduleMapper} 获取。</li>
 *   <li><b>数据一致性</b>：Redis 为余量主数据，数据库 {@code course.selected_count} 为兜底；
 *       落库失败需回滚 Redis 预占；定时任务 {@code SelectionSyncTask} 周期校正。</li>
 *   <li><b>幂等性</b>：Redis Set 去重 + {@code course_selection} 唯一索引双重保障。</li>
 * </ul>
 */
public interface CourseSelectionService extends IService<CourseSelection> {

    /**
     * 学生选课。
     *
     * <p>完整流程：校验选课开关与时间窗口 → 校验课程状态 → 校验学分上限 →
     * 时间冲突预检 → Redis 原子预占 → 落库 → 同步已选人数；任一步失败需回滚 Redis 预占。</p>
     *
     * @param courseId 课程ID
     * @return 选课结果，包含最新剩余容量
     */
    SelectionResultVO selectCourse(Long courseId);

    /**
     * 学生退课。
     *
     * <p>需校验：课程是否允许退选（{@code course.selectable}）、是否已选、是否已录入成绩。
     * 退课不删除记录，而是将 {@code status} 置为 0（已退选）并记录 {@code drop_time}，
     * 同时归还 Redis 余量。</p>
     *
     * @param courseId 课程ID
     * @return 退课结果，包含最新剩余容量
     */
    SelectionResultVO dropCourse(Long courseId);

    /**
     * 查询当前学生的选课列表。
     *
     * @param pageNum    页码
     * @param pageSize   每页条数
     * @param semesterId 学期ID（可选）
     * @param status     选课状态（可选）：0-已退选 1-已选课 2-已修完
     * @return 分页选课记录
     */
    PageResult<CourseSelection> pageMySelections(Long pageNum, Long pageSize,
                                                 Long semesterId, Integer status);

    /**
     * 查询指定课程的选课名单（教师/管理员）。
     *
     * @param courseId 课程ID
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param keyword  姓名或学号模糊搜索（可选）
     * @return 分页选课记录
     */
    PageResult<CourseSelection> pageCourseStudents(Long courseId, Long pageNum,
                                                   Long pageSize, String keyword);

    /**
     * 选课前的冲突预检，返回冲突明细，不产生任何副作用。
     *
     * @param courseId 课程ID
     * @return 冲突列表，无冲突时返回空集合
     */
    List<ConflictVO> checkConflict(Long courseId);

    /**
     * 预热课程缓存：将课程余量与已选学生集合写入 Redis。
     *
     * <p>建议在开放选课前由管理员手动触发，可显著降低首个请求的回源开销。</p>
     *
     * @param semesterId 学期ID（可选，为空则预热全部正常状态课程）
     */
    void preloadCourseCache(Long semesterId);

    /**
     * 将 Redis 中的选课结果同步为数据库真实值（定时兜底任务）。
     *
     * <p>用于校正 {@code course.selected_count} 与 Redis 余量的偏差。</p>
     */
    void syncSelectionCount();

    /**
     * 查询可选课程列表（学生视角，含实时余量与选课状态）。
     *
     * @param query     查询条件
     * @param studentId 学生ID，用于标记 {@code selected} 字段
     * @return 分页课程列表
     */
    PageResult<Course> pageAvailableCourses(CourseQueryDTO query, Long studentId);
}
