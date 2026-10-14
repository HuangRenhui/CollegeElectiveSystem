package com.college.elective.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.college.elective.common.BusinessException;
import com.college.elective.common.PageResult;
import com.college.elective.common.ResultCode;
import com.college.elective.entity.MessageNotice;
import com.college.elective.mapper.MessageNoticeMapper;
import com.college.elective.security.SecurityUtils;
import com.college.elective.service.MessageNoticeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内信服务实现。
 *
 * <p><b>取当前用户统一用 {@link SecurityUtils#getUserId()}</b>：
 * 它内部已调用 {@code getLoginUser()} 并在未登录时抛 {@code UNAUTHORIZED}，
 * 无需自行判空。{@code LoginUser.userId} 指向 {@code sys_user.id}，
 * 正好是站内信 {@code receiver_id} 的口径（注意不是 {@code student.id}）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageNoticeServiceImpl extends ServiceImpl<MessageNoticeMapper, MessageNotice>
        implements MessageNoticeService {

    /** 已读 */
    private static final int READ = 1;

    @Override
    public void send(Long receiverId, String msgType, String title, String content,
                     Long bizId, String bizType) {
        BusinessException.throwIf(receiverId == null, ResultCode.PARAM_ERROR, "接收人不能为空");
        save(build(receiverId, msgType, title, content, bizId, bizType));
        log.debug("[站内信] 发送 receiverId={}, msgType={}, title={}", receiverId, msgType, title);
    }

    @Override
    public void sendBatch(List<Long> receiverIds, String msgType, String title, String content,
                          Long bizId, String bizType) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        List<MessageNotice> list = receiverIds.stream()
            .map(id -> build(id, msgType, title, content, bizId, bizType))
            .toList();
        // MyBatis-Plus 的 saveBatch 默认一批 1000 条，成绩发布场景通常远小于此
        saveBatch(list);
        log.debug("[站内信] 批量发送 {} 条, msgType={}, title={}", list.size(), msgType, title);
    }

    @Override
    public PageResult<MessageNotice> pageMyMessages(Long pageNum, Long pageSize, Integer readFlag) {
        Long userId = SecurityUtils.getUserId();

        Page<MessageNotice> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<MessageNotice> wrapper = new LambdaQueryWrapper<MessageNotice>()
            .eq(MessageNotice::getReceiverId, userId)
            .eq(readFlag != null, MessageNotice::getReadFlag, readFlag)
            .orderByDesc(MessageNotice::getCreateTime);
        page(page, wrapper);
        return PageResult.of(page);
    }

    @Override
    public long countMyUnread() {
        return count(new LambdaQueryWrapper<MessageNotice>()
            .eq(MessageNotice::getReceiverId, SecurityUtils.getUserId())
            .eq(MessageNotice::getReadFlag, 0));
    }

    @Override
    public void markRead(Long messageId) {
        Long userId = SecurityUtils.getUserId();
        BusinessException.throwIf(messageId == null, ResultCode.PARAM_ERROR);

        // 只允许标记本人的消息：越权改别人的已读状态虽不敏感，但属逻辑越权，一律拦截
        MessageNotice message = getById(messageId);
        BusinessException.throwIf(message == null, ResultCode.DATA_NOT_FOUND, "消息不存在");
        BusinessException.throwIf(!userId.equals(message.getReceiverId()),
            ResultCode.FORBIDDEN, "无权操作他人的消息");

        // 幂等：重复标记不报错，也不更新时间
        if (READ == message.getReadFlag()) {
            return;
        }
        update(Wrappers.<MessageNotice>lambdaUpdate()
            .eq(MessageNotice::getId, messageId)
            .set(MessageNotice::getReadFlag, READ)
            .set(MessageNotice::getReadTime, LocalDateTime.now()));
    }

    @Override
    public int markAllRead() {
        Long userId = SecurityUtils.getUserId();
        return baseMapper.update(null, Wrappers.<MessageNotice>lambdaUpdate()
            .eq(MessageNotice::getReceiverId, userId)
            .eq(MessageNotice::getReadFlag, 0)
            .set(MessageNotice::getReadFlag, READ)
            .set(MessageNotice::getReadTime, LocalDateTime.now()));
    }

    private MessageNotice build(Long receiverId, String msgType, String title, String content,
                                Long bizId, String bizType) {
        MessageNotice m = new MessageNotice();
        m.setReceiverId(receiverId);
        m.setMsgType(msgType);
        m.setTitle(title);
        m.setContent(content);
        m.setBizId(bizId);
        m.setBizType(bizType);
        m.setReadFlag(0);
        return m;
    }
}
