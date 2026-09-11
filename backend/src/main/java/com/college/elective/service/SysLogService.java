package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.entity.SysLog;

/**
 * 操作日志服务。
 */
public interface SysLogService extends IService<SysLog> {

    /**
     * 异步保存操作日志。
     */
    void saveAsync(SysLog sysLog);

    /**
     * 分页查询操作日志。
     */
    PageResult<SysLog> pageLogs(Long pageNum, Long pageSize, String keyword, String module, Integer success);

    /**
     * 清空指定天数之前的历史日志。
     */
    int cleanExpiredLogs(int days);
}
