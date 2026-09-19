package com.college.elective.common;

import lombok.Getter;

/**
 * 统一响应状态码。
 *
 * <p>约定：1xx 通用状态；2xxx 用户与权限；3xxx 课程与选课；4xxx 成绩；5xxx 系统。</p>
 */
@Getter
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    FAIL(500, "操作失败"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "请求资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // ---------------- 用户与权限 ----------------
    LOGIN_FAILED(2001, "账号或密码错误"),
    ACCOUNT_DISABLED(2002, "账号已被禁用，请联系管理员"),
    TOKEN_INVALID(2003, "无效的身份凭证"),
    OLD_PASSWORD_ERROR(2004, "原密码不正确"),
    USERNAME_EXISTS(2005, "登录账号已存在"),
    USER_NOT_FOUND(2006, "用户不存在"),
    ROLE_NOT_ALLOWED(2007, "当前身份无法执行该操作"),
    STUDENT_NOT_FOUND(2008, "学生信息不存在"),
    TEACHER_NOT_FOUND(2009, "教师信息不存在"),

    // ---------------- 课程与选课 ----------------
    COURSE_NOT_FOUND(3001, "课程不存在或已下架"),
    COURSE_FULL(3002, "课程余量不足，选课失败"),
    COURSE_ALREADY_SELECTED(3003, "您已选择该课程，请勿重复选课"),
    COURSE_NOT_SELECTED(3004, "您尚未选择该课程"),
    COURSE_TIME_CONFLICT(3005, "上课时间与已选课程冲突"),
    SELECTION_NOT_OPEN(3006, "当前不在选课开放时间内"),
    SELECTION_CLOSED(3007, "选课通道已关闭"),
    CREDIT_LIMIT_EXCEEDED(3008, "选课学分已超出上限"),
    DROP_NOT_ALLOWED(3009, "该课程不允许退选"),
    COURSE_CACHE_NOT_READY(3010, "课程缓存尚未就绪，请稍后重试"),
    SYSTEM_BUSY(3011, "系统繁忙，请稍后重试"),
    SELECTION_CONFLICT(3012, "选课冲突：%s"),

    // ---------------- 成绩 ----------------
    GRADE_NOT_FOUND(4001, "成绩记录不存在"),
    GRADE_ALREADY_PUBLISHED(4002, "成绩已发布，无法修改"),
    GRADE_SCORE_ILLEGAL(4003, "成绩取值必须在 0-100 之间"),
    NOT_COURSE_TEACHER(4004, "您不是该课程的授课教师"),

    // ---------------- 系统 ----------------
    DATA_NOT_FOUND(5001, "数据不存在"),
    DATA_ALREADY_EXISTS(5002, "数据已存在"),
    OPERATION_FORBIDDEN(5003, "该数据禁止被删除或修改"),
    SEMESTER_NOT_FOUND(5004, "学期不存在"),
    CURRENT_SEMESTER_NOT_SET(5005, "尚未设置当前学期"),
    FEATURE_NOT_IMPLEMENTED(5006, "该功能尚未实现"),

    // ---------------- 教学评价 ----------------
    REVIEW_NOT_FOUND(6001, "评价记录不存在"),
    REVIEW_ALREADY_EXISTS(6002, "该课程已提交过评价"),
    REVIEW_NOT_ALLOWED(6003, "该课程尚未修完，暂无评价资格"),
    REVIEW_CONTENT_ILLEGAL(6004, "评价评分需为 1-5 分，文字评价需 5-500 字"),
    REVIEW_HIDDEN(6005, "该评价已被管理员隐藏，无法修改");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
