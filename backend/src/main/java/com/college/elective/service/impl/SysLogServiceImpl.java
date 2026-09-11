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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Async("operationLogExecutor")
    @Override
    public void saveAsync(SysLog sysLog) {
        try {
            if (sysLog.getCreateTime() == null) {
                sysLog.setCreateTime(LocalDateTime.now());
            }
            save(sysLog);
        } catch (Exception e) {
            log.warn("异步保存操作日志失败: {}", e.getMessage());
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

    @Async("operationLogExecutor")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public int cleanExpiredLogs(int days) {
        LocalDateTime deadline = LocalDate.now().minusDays(days).atStartOfDay();
        boolean removed = remove(Wrappers.<SysLog>lambdaQuery().lt(SysLog::getCreateTime, deadline));
        if (removed) {
            log.info("已清理 {} 之前的操作日志", deadline);
        }
        return removed ? 1 : 0;
    }
}
