package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 站内信实体。
 *
 * <p>与公告（{@link Notice}）的区别：公告是<b>广播</b>（按角色群发），
 * 站内信是<b>点对点</b>（精确到用户），用于「选课成功提醒」「成绩发布通知」
 * 这类只与单个用户相关的事件。</p>
 *
 * <p>没有单独的状态字段：站内信一旦写入即生效，
 * 「已读/未读」由 {@code readFlag} 表达，不做草稿与下架。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message_notice")
@Schema(description = "站内信")
public class MessageNotice extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "接收人用户ID")
    private Long receiverId;

    @Schema(description = "消息类型：SYSTEM/SELECTION/GRADE/REVIEW")
    private String msgType;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "关联业务ID（如课程ID、学期ID）")
    private Long bizId;

    @Schema(description = "业务类型：COURSE/SEMESTER/GRADE")
    private String bizType;

    @Schema(description = "是否已读：0-未读 1-已读")
    private Integer readFlag;

    @Schema(description = "阅读时间")
    private LocalDateTime readTime;
}
