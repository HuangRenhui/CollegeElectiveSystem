package com.college.elective.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.college.elective.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 学生扩展信息实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student")
@Schema(description = "学生信息")
public class Student extends BaseEntity {

    @Schema(description = "主键ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "学号")
    private String stuNo;

    @Schema(description = "所属院系ID")
    private Long deptId;

    @Schema(description = "所属专业ID")
    private Long majorId;

    @Schema(description = "行政班级")
    private String className;

    @Schema(description = "年级")
    private Integer gradeYear;

    @Schema(description = "入学年份")
    private Integer enrollYear;

    @Schema(description = "已获总学分")
    private BigDecimal totalCredit;

    // ---------------- 非数据库字段，用于列表展示 ----------------

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

    @Schema(description = "专业名称")
    @TableField(exist = false)
    private String majorName;
}
