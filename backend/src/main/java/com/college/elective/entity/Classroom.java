package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教室实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("classroom")
@Schema(description = "教室")
public class Classroom extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "教室编号")
    private String roomNo;

    @Schema(description = "教学楼")
    private String building;

    @Schema(description = "容纳人数")
    private Integer capacity;

    @Schema(description = "类型：NORMAL/MULTIMEDIA/LAB/GYM")
    private String roomType;

    @Schema(description = "状态：0-停用 1-可用")
    private Integer status;
}
