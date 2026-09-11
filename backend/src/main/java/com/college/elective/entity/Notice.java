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
 * 系统公告实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notice")
@Schema(description = "系统公告")
public class Notice extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "类型：SYSTEM/SELECTION/EXAM")
    private String noticeType;

    @Schema(description = "目标角色：ALL/STUDENT/TEACHER")
    private String targetRole;

    @Schema(description = "发布人ID")
    private Long publisherId;

    @Schema(description = "发布人姓名")
    private String publisher;

    @Schema(description = "是否置顶：0-否 1-是")
    private Integer topFlag;

    @Schema(description = "浏览量")
    private Integer viewCount;

    @Schema(description = "状态：0-草稿 1-已发布 2-已下架")
    private Integer status;

    @Schema(description = "发布时间")
    private LocalDateTime publishTime;
}
