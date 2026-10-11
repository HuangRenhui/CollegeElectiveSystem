package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 微信身份绑定.
 *
 * <p>独立于 {@code sys_user}存储，便于同一系统账号绑定多个微信应用
 * （公众号 + 小程序），也避免侵入核心账号表。</p>
 *
 * <p><b>安全要点</b>：一个微信身份（{@code wxAppid + openId}）只能绑定
 * 一个系统账号，由唯一索引 {@code uk_appid_openid} 在数据库层兜底，
 * 防止并发下同一微信身份被绑到多个账号。</p>
 */
@Data
@TableName("user_wx_bind")
@Schema(description = "微信身份绑定")
public class UserWxBind {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "系统用户ID")
    private Long userId;

    @Schema(description = "微信应用AppID")
    private String wxAppid;

    @Schema(description = "微信OpenID")
    private String openId;

    @Schema(description = "微信UnionID（开放平台）")
    private String unionId;

    @Schema(description = "微信昵称")
    private String nickname;

    @Schema(description = "微信头像地址")
    private String avatarUrl;

    @Schema(description = "绑定时间")
    private LocalDateTime createTime;

    @Schema(description = "逻辑删除")
    @TableLogic
    private Integer deleted;
}