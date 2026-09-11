package com.college.elective.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.Constants;
import com.college.elective.common.PageResult;
import com.college.elective.common.ResultCode;
import com.college.elective.entity.Notice;
import com.college.elective.mapper.NoticeMapper;
import com.college.elective.security.LoginUser;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.NoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public PageResult<Notice> pageNotices(Long pageNum, Long pageSize, String keyword,
                                          String noticeType, Integer status) {
        IPage<Notice> page = page(new Page<>(pageNum, pageSize), Wrappers.<Notice>lambdaQuery()
                .like(StrUtil.isNotBlank(keyword), Notice::getTitle, keyword)
                .eq(StrUtil.isNotBlank(noticeType), Notice::getNoticeType, noticeType)
                .eq(status != null, Notice::getStatus, status)
                .orderByDesc(Notice::getTopFlag)
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId));
        return PageResult.of(page);
    }

    @Override
    public List<Notice> listVisibleNotices(String role, Integer limit) {
        int size = limit == null || limit <= 0 ? 10 : limit;
        return list(Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getStatus, 1)
                .and(w -> w.eq(Notice::getTargetRole, "ALL")
                        .or().eq(StrUtil.isNotBlank(role), Notice::getTargetRole, role))
                .orderByDesc(Notice::getTopFlag)
                .orderByDesc(Notice::getPublishTime)
                .last("LIMIT " + size));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Notice getNoticeDetail(Long id) {
        Notice notice = getById(id);
        BusinessException.throwIf(notice == null, ResultCode.DATA_NOT_FOUND, "公告不存在");
        baseMapper.increaseViewCount(id);
        notice.setViewCount((notice.getViewCount() == null ? 0 : notice.getViewCount()) + 1);
        return notice;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveNotice(Notice notice) {
        BusinessException.throwIf(StrUtil.isBlank(notice.getTitle()), ResultCode.PARAM_ERROR, "公告标题不能为空");
        BusinessException.throwIf(StrUtil.isBlank(notice.getContent()), ResultCode.PARAM_ERROR, "公告内容不能为空");

        LoginUser loginUser = SecurityUtils.getLoginUser();
        notice.setPublisherId(loginUser.getUserId());
        notice.setPublisher(loginUser.getRealName());
        notice.setNoticeType(StrUtil.blankToDefault(notice.getNoticeType(), "SYSTEM"));
        notice.setTargetRole(StrUtil.blankToDefault(notice.getTargetRole(), "ALL"));
        notice.setTopFlag(notice.getTopFlag() == null ? 0 : notice.getTopFlag());

        if (notice.getId() == null) {
            notice.setViewCount(0);
            if (notice.getStatus() == null) {
                notice.setStatus(1);
            }
            if (Integer.valueOf(1).equals(notice.getStatus()) && notice.getPublishTime() == null) {
                notice.setPublishTime(LocalDateTime.now());
            }
            save(notice);
        } else {
            updateById(notice);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Notice notice = getById(id);
        BusinessException.throwIf(notice == null, ResultCode.DATA_NOT_FOUND, "公告不存在");

        Notice update = new Notice();
        update.setId(id);
        update.setStatus(status);
        if (Integer.valueOf(1).equals(status)) {
            update.setPublishTime(LocalDateTime.now());
        }
        updateById(update);
        log.info("公告状态变更: id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long id) {
        Notice notice = getById(id);
        BusinessException.throwIf(notice == null, ResultCode.DATA_NOT_FOUND, "公告不存在");
        removeById(id);
        log.info("删除公告: {}", notice.getTitle());
    }
}
