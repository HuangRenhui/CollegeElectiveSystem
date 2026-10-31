package com.college.elective.task;

import com.college.elective.common.RedisKeys;
import com.college.elective.service.CourseSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 选课数据一致性兜底任务。
 *
 * <p>Redis 承担高并发选课的主要压力，数据库计数与缓存可能因异常中断产生偏差，
 * 因此通过定时任务周期性校正「课程已选人数 + Redis 容量 + Redis 已选集合」。</p>
 *
 * <p><b>执行时机的取舍</b>：任务是自愈手段，不能反过来成为故障源——
 * 它在写回容量时会短暂持有课程回源锁，而选课脚本一旦发现锁存在就返回「缓存未就绪」。
 * 因此本任务提供两个开关：</p>
 * <ul>
 *   <li>{@code elective.selection.sync-enabled}：总开关，出问题可热配置停用（无需改代码重启构建）；</li>
 *   <li>{@code elective.selection.sync-skip-when-open}：选课通道开启时是否跳过本轮同步。
 *       默认关闭，建议在教学期选课高峰期间打开，把「可能干扰选课」的窗口降到 0。</li>
 * </ul>
 *
 * @see com.college.elective.service.CourseSelectionService#syncSelectionCount()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionSyncTask {

    private final ObjectProvider<CourseSelectionService> selectionServiceProvider;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 是否启用同步任务。
     *
     * <p><b>为什么用配置而不是常量</b>：原实现是 {@code private static final boolean TASK_ENABLED = true}，
     * 属于编译期常量——注释写着「便于快速停用」，实际必须改代码重新构建才能生效，
     * 在故障现场等于没有开关。</p>
     */
    @Value("${elective.selection.sync-enabled:true}")
    private boolean taskEnabled;

    /**
     * 选课通道开放时是否跳过本轮同步。
     *
     * <p>默认 {@code false}：只有在已知高峰（如开学选课第一天）才建议打开，
     * 避免同步任务与选课流量争抢同一把课程回源锁。</p>
     */
    @Value("${elective.selection.sync-skip-when-open:false}")
    private boolean skipWhenOpen;

    /**
     * 每 5 分钟同步一次选课人数。
     */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void syncSelectionCount() {
        if (!taskEnabled) {
            log.debug("[人数同步] 任务已通过 elective.selection.sync-enabled=false 停用");
            return;
        }
        if (skipWhenOpen && isSelectionOpen()) {
            log.info("[人数同步] 选课通道开启中，按配置跳过本轮同步");
            return;
        }
        CourseSelectionService selectionService = selectionServiceProvider.getIfAvailable();
        if (selectionService == null) {
            return;
        }
        try {
            selectionService.syncSelectionCount();
        } catch (Exception e) {
            log.error("选课人数同步任务执行失败", e);
        }
    }

    /**
     * 判断选课通道当前是否开放。
     *
     * <p>口径与 {@code CourseSelectionServiceImpl#checkSelectionOpen} 的「Redis 动态开关」一致：
     * 只有显式的 {@code off} 才算关闭，键不存在按开启处理。
     * 这里只做粗判（不查学期状态），目的是「拿不准就跳过」，保守优先。</p>
     */
    private boolean isSelectionOpen() {
        try {
            String state = stringRedisTemplate.opsForValue().get(RedisKeys.SELECTION_SWITCH);
            return !"off".equalsIgnoreCase(state);
        } catch (Exception e) {
            // Redis 异常时无法判定是否高峰，按「可能开放」处理，宁可不跑也不要干扰选课
            log.warn("[人数同步] 读取选课开关失败，本轮按选课开放处理并跳过", e);
            return true;
        }
    }
}
