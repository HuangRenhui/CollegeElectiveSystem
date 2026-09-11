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
 * <p>Redis 承担高并发选课的主要压力，数据库计数可能因异常中断产生偏差，
 * 因此需要通过定时任务周期性校正「课程已选人数」与「Redis 余量」。</p>
 *
 * <p>由于 {@link CourseSelectionService#syncSelectionCount()} 尚未实现，
 * 任务在服务 Bean 缺失或未启用时自动跳过，避免产生异常日志。
 * 完成实现后，将 {@link #TASK_ENABLED} 置为 {@code true} 即可启用。</p>
 *
 * @see com.college.elective.service.CourseSelectionService#syncSelectionCount()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SelectionSyncTask {

    private final ObjectProvider<CourseSelectionService> selectionServiceProvider;

    /** 是否启用同步任务。待 syncSelectionCount 实现完成后置为 true 即可生效。 */
    private static final boolean TASK_ENABLED = false;

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
