package com.college.elective.task;

import com.college.elective.service.CourseSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 选课数据一致性兜底任务。
 *
 * <p>Redis 承担高并发选课的主要压力，数据库计数与缓存可能因异常中断产生偏差，
 * 因此通过定时任务周期性校正「课程已选人数 + Redis 容量 + Redis 已选集合」。</p>
 *
 * <p>{@link CourseSelectionService#syncSelectionCount()} 已实现，本任务默认启用。</p>
 *
 * <p><b>执行时机的取舍</b>：任务固定每 5 分钟执行一次，若此时正在选课，
 * 写回容量理论上可能覆盖并发请求刚完成的扣减，造成 1 个余量的瞬时偏差。
 * 由于 {@code syncSelectionCount} 只对「三方比对判定为偏差」的课程执行写回，
 * 健康课程完全不会被触碰，因此风险被限制在很小的范围；
 * 若日后选课高峰期仍观察到偏差，可改为按选课开关状态动态跳过执行。</p>
 *
 * @see com.college.elective.service.CourseSelectionService#syncSelectionCount()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionSyncTask {

    private final ObjectProvider<CourseSelectionService> selectionServiceProvider;

    /**
     * 是否启用同步任务。
     *
     * <p>{@code syncSelectionCount} 已实现，故置为 {@code true} 默认启用；
     * 保留该开关便于出现异常时快速停用兜底任务，而不必改动代码。</p>
     */
    private static final boolean TASK_ENABLED = true;

    /**
     * 每 5 分钟同步一次选课人数。
     */
    @Scheduled(fixedDelayString = "PT5M", initialDelayString = "PT1M")
    public void syncSelectionCount() {
        if (!TASK_ENABLED) {
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
}
