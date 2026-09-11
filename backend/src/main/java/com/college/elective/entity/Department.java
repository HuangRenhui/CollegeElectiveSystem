package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 院系实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("department")
@Schema(description = "院系")
public class Department extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "院系编码")
    private String deptCode;

    @Schema(description = "院系名称")
    private String deptName;

    @Schema(description = "负责人")
    private String deanName;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "简介")
    private String description;

    @Schema(description = "排序值")
    private Integer sort;
}
