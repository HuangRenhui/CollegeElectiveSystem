package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息订阅关系.
 *
 * <p>微信订阅消息的规则是「<b>用户每次授权只能收到一条消息</b>」，
 * 用完即失效。因此本表记录剩余可发送次数：</p>
 * <ul>
 *   <li>用户每次授权 → <code>remain_count + 1</code>；</li>
 *   <li>每发送一条 → <code>remain_count - 1</code>；</li>
 *   <li>归零或过期 → 需重新引导用户授权。</li>
 * </ul>
 */
@Data
@TableName("message_subscribe")
@Schema(description = "消息订阅关系")
public class MessageSubscribe {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "微信订阅消息模板ID")
    private String templateId;

    @Schema(description = "场景：REVIEW_OPEN-评价开放 GRADE_PUBLISH-成绩发布")
    private String scene;

    @Schema(description = "剩余可发送次数")
    private Integer remainCount;

    @Schema(description = "订阅关系过期时间（微信默认7天）")
    private LocalDateTime expireTime;

    @Schema(description = "订阅时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除")
    @TableLogic
    private Integer deleted;
}