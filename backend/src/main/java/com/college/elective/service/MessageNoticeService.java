package com.college.elective.service;

import com.college.elective.common.PageResult;
import com.college.elective.entity.MessageNotice;

import java.util.List;

/**
 * 站内信服务。
 *
 * <p>站内信是<b>点对点</b>消息，与公告（广播）互补：</p>
 * <ul>
 *   <li>公告：面向角色的群发，如「选课即将开始」</li>
 *   <li>站内信：面向个人的事件通知，如「你已成功选上《XX》」「你的成绩已发布」</li>
 * </ul>
 */
public interface MessageNoticeService {

    /**
     * 发送站内信。
     *
     * @param receiverId 接收人用户ID
     * @param msgType    消息类型：SYSTEM/SELECTION/GRADE/REVIEW
     * @param title      标题
     * @param content    内容
     * @param bizId      关联业务ID（可为空）
     * @param bizType    业务类型（可为空）
     */
    void send(Long receiverId, String msgType, String title, String content,
              Long bizId, String bizType);

    /**
     * 批量发送（同一内容给多个接收人）。
     *
     * <p>用于「成绩发布」这类一次事件通知全班学生的场景，
     * 逐条插入会产生 N 次 SQL。</p>
     *
     * @param receiverIds 接收人用户ID集合
     * @param msgType     消息类型
     * @param title       标题
     * @param content     内容
     * @param bizId       关联业务ID（可为空）
     * @param bizType     业务类型（可为空）
     */
    void sendBatch(List<Long> receiverIds, String msgType, String title, String content,
                   Long bizId, String bizType);

    /**
     * 分页查询当前登录用户的站内信。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param readFlag 已读状态（可为空）：0-未读 1-已读
     * @return 分页站内信
     */
    PageResult<MessageNotice> pageMyMessages(Long pageNum, Long pageSize, Integer readFlag);

    /**
     * 查询当前登录用户的未读消息数。
     *
     * @return 未读数，无未读时返回 0
     */
    long countMyUnread();

    /**
     * 将指定消息标记为已读（仅限本人消息）。
     *
     * @param messageId 消息ID
     */
    void markRead(Long messageId);

    /**
     * 将当前登录用户的全部未读消息标记为已读。
     *
     * @return 被更新的条数
     */
    int markAllRead();
}
