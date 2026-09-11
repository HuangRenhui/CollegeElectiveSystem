package com.college.elective.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.college.elective.common.PageResult;
import com.college.elective.entity.Notice;

import java.util.List;

/**
 * 公告服务。
 */
public interface NoticeService extends IService<Notice> {

    /**
     * 分页查询公告（管理端）。
     */
    PageResult<Notice> pageNotices(Long pageNum, Long pageSize, String keyword,
                                   String noticeType, Integer status);

    /**
     * 查询当前用户可见的公告列表（含置顶优先排序）。
     */
    List<Notice> listVisibleNotices(String role, Integer limit);

    /**
     * 查看公告详情并累加浏览量。
     */
    Notice getNoticeDetail(Long id);

    /**
     * 发布/保存公告。
     */
    void saveNotice(Notice notice);

    /**
     * 变更公告状态：0-草稿 1-已发布 2-已下架。
     */
    void updateStatus(Long id, Integer status);

    /**
     * 删除公告。
     */
    void deleteNotice(Long id);
}
