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
 * 教师扩展信息实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teacher")
@Schema(description = "教师信息")
public class Teacher extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "工号")
    private String teacherNo;

    @Schema(description = "所属院系ID")
    private Long deptId;

    @Schema(description = "职称")
    private String title;

    @Schema(description = "研究方向")
    private String researchArea;

    // ---------------- 非数据库字段 ----------------

    @Schema(description = "姓名")
    @TableField(exist = false)
    private String realName;

    @Schema(description = "性别")
    @TableField(exist = false)
    private Integer gender;

    @Schema(description = "手机号")
    @TableField(exist = false)
    private String phone;

    @Schema(description = "邮箱")
    @TableField(exist = false)
    private String email;

    @Schema(description = "账号状态")
    @TableField(exist = false)
    private Integer status;

    @Schema(description = "院系名称")
    @TableField(exist = false)
    private String deptName;
}
