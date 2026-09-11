package com.college.elective.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 教师新增/修改请求参数。
 */
@Data
@Schema(description = "教师保存参数")
public class TeacherDTO {

    @Schema(description = "教师ID，新增时为空")
    private Long id;

    @Schema(description = "用户ID，新增时为空")
    private Long userId;

    @Schema(description = "工号（同时作为登录账号）")
    @NotBlank(message = "工号不能为空")
    private String teacherNo;

    @Schema(description = "姓名")
    @NotBlank(message = "姓名不能为空")
    private String realName;

    @Schema(description = "性别：0-未知 1-男 2-女")
    private Integer gender = 0;

    @Schema(description = "手机号")
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "所属院系ID")
    @NotNull(message = "请选择所属院系")
    private Long deptId;

    @Schema(description = "职称")
    private String title;

    @Schema(description = "研究方向")
    private String researchArea;
}
