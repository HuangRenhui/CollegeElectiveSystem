package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体（日志表不含逻辑删除字段）。
 */
@Data
@TableName("sys_log")
@Schema(description = "操作日志")
public class SysLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "操作人ID")
    private Long userId;

    @Schema(description = "操作人账号")
    private String username;

    @Schema(description = "业务模块")
    private String module;

    @Schema(description = "操作描述")
    private String operation;

    @Schema(description = "请求方法")
    private String method;

    @Schema(description = "请求地址")
    private String requestUri;

    @Schema(description = "请求参数")
    private String requestParam;

    @Schema(description = "操作IP")
    private String ip;

    @Schema(description = "耗时（毫秒）")
    private Long costTime;

    @Schema(description = "是否成功：0-失败 1-成功")
    private Integer success;

    @Schema(description = "异常信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
