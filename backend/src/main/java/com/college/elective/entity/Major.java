package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 专业实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("major")
@Schema(description = "专业")
public class Major extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "专业编码")
    private String majorCode;

    @Schema(description = "专业名称")
    private String majorName;

    @Schema(description = "所属院系ID")
    private Long deptId;

    @Schema(description = "培养层次")
    private String degreeType;

    @Schema(description = "学制（年）")
    private Integer duration;

    @Schema(description = "专业简介")
    private String description;

    @Schema(description = "所属院系名称（非数据库字段）")
    @TableField(exist = false)
    private String deptName;
}
