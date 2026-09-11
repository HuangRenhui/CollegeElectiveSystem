package com.college.elective.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 当前登录用户信息。
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfoVO {

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "登录账号")
    private String username;

    @Schema(description = "姓名")
    private String realName;

    @Schema(description = "角色：STUDENT/TEACHER/ADMIN")
    private String role;

    @Schema(description = "性别")
    private Integer gender;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "头像")
    private String avatar;

    // ---------------- 学生扩展 ----------------

    @Schema(description = "学号")
    private String stuNo;

    @Schema(description = "专业名称")
    private String majorName;

    @Schema(description = "行政班级")
    private String className;

    @Schema(description = "已获总学分")
    private BigDecimal totalCredit;

    // ---------------- 教师扩展 ----------------

    @Schema(description = "工号")
    private String teacherNo;

    @Schema(description = "职称")
    private String title;

    // ---------------- 公共 ----------------

    @Schema(description = "院系名称")
    private String deptName;
}
