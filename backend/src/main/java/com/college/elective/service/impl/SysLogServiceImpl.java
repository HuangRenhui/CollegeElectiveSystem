package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.PageResult;
import com.college.elective.entity.SysLog;
import com.college.elective.mapper.SysLogMapper;
import com.college.elective.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    /** 分批清理时每批删除的最大行数，避免单条 SQL 长时间持有大量行锁 */
    private static final int CLEAN_BATCH_SIZE = 5000;

    /** 分批清理的最大轮次，防止异常情况下无限循环 */
    private static final int CLEAN_MAX_ROUNDS = 1000;

    @Async("operationLogExecutor")
    @Override
    public void saveAsync(SysLog sysLog) {
        try {
            if (sysLog.getCreateTime() == null) {
                sysLog.setCreateTime(LocalDateTime.now());
            }
            save(sysLog);
        } catch (Exception e) {
            // 异步线程中异常不会向上传播，必须记录完整堆栈以便排查；
            // 带上模块名便于定位是哪类操作触发的
            log.warn("异步保存操作日志失败: module={}, operation={}", sysLog.getModule(), sysLog.getOperation(), e);
        }

    }

    @Override
    public PageResult<SysLog> pageLogs(Long pageNum, Long pageSize, String keyword,
                                       String module, Integer success) {
        IPage<SysLog> page = page(new Page<>(pageNum, pageSize), Wrappers.<SysLog>lambdaQuery()
                .and(StrUtil.isNotBlank(keyword), w -> w
                        .like(SysLog::getUsername, keyword)
                        .or().like(SysLog::getOperation, keyword)
                        .or().like(SysLog::getRequestUri, keyword))
                .eq(StrUtil.isNotBlank(module), SysLog::getModule, module)
                .eq(success != null, SysLog::getSuccess, success)
                .orderByDesc(SysLog::getId));
        return PageResult.of(page);
    }


    /**
     * 同步分批清理指定天数之前的历史日志。
     *
     * <p><b>为什么分批</b>：{@code DELETE ... WHERE create_time < ?} 在日志量较大时会一次性
     * 锁定大量行，持有时间可能长达数十秒，期间会阻塞 {@code saveAsync} 所在线程写入日志，
     * 也会拖长接口响应时间（甚至触发前端超时）。改为每批最多 {@value #CLEAN_BATCH_SIZE} 行，
     * 可把一次长事务拆成多个短事务，显著缩小单次持锁范围。</p>
     *
     * <p><b>为什么不在循环上包大事务</b>：若在方法上直接加 {@code @Transactional}，
     * 各批次的锁会一直累积到方法结束才统一释放，反而放大了锁范围，
     * 与分批的初衷相悖。因此此处刻意<b>不</b>加事务注解，
     * 依赖 MySQL 自动提交语义让每批删除独立生效。
     * 日志清理属于可重复执行的幂等操作，中途失败后再次触发即可继续，无需整体回滚。</p>
     *
     * <p><b>关于轮次上限</b>：设置 {@value #CLEAN_MAX_ROUNDS} 轮上限是为了防御性兜底——
     * 若删除条件无法清空数据（例如并发持续写入更早的日志），避免陷入死循环。
     * 达到上限会记录 warn 日志，便于运维发现异常。</p>
     *
     * @param days 保留天数，早于「当前时间 - days」的日志将被删除
     */
    @Override
    public void cleanExpiredLogs(int days) {
        LocalDateTime deadline = LocalDate.now().minusDays(days).atStartOfDay();
        long total = 0;
        int rounds = 0;

        while (rounds++ < CLEAN_MAX_ROUNDS) {
            int removed = deleteBatch(deadline);
            if (removed == 0) {
                break;
            }
            total += removed;
        }

        if (rounds >= CLEAN_MAX_ROUNDS) {
            log.warn("日志清理达到最大轮次 {} 仍未删完，deadline={}，已删除 {} 条",
                    CLEAN_MAX_ROUNDS, deadline, total);
        }
        log.info("已清理 {} 之前的操作日志，共 {} 条", deadline, total);
    }

    /**
     * 删除一批过期日志，单批最多 {@value #CLEAN_BATCH_SIZE} 行。
     *
     * <p>通过 {@code LIMIT} 限制单批行数。该方法在无事务的自动提交模式下执行，
     * SQL 执行完毕即提交并释放行锁，使后续批次与日志写入线程能尽快获得锁。</p>
     *
     * @param deadline 过期时间界限，早于该时间的日志将被删除
     * @return 本批次实际删除的行数，为 0 表示已无过期数据
     */
    private int deleteBatch(LocalDateTime deadline) {
        return baseMapper.delete(Wrappers.<SysLog>lambdaQuery()
                .lt(SysLog::getCreateTime, deadline)
                .last("LIMIT " + CLEAN_BATCH_SIZE));
    }
}
