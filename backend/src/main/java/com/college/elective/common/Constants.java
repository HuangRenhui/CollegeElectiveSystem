package com.college.elective.common;

/**
 * 全局业务常量。
 */
public final class Constants {

    private Constants() {
    }

    // ------------------------------ 角色 ------------------------------
    public static final String ROLE_STUDENT = "STUDENT";
    public static final String ROLE_TEACHER = "TEACHER";
    public static final String ROLE_ADMIN = "ADMIN";

    /** Spring Security 权限前缀 */
    public static final String ROLE_PREFIX = "ROLE_";

    // ------------------------------ 通用状态 ------------------------------
    public static final Integer STATUS_DISABLED = 0;
    public static final Integer STATUS_ENABLED = 1;

    // ------------------------------ 课程 ------------------------------
    /** 课程类型：必修 */
    public static final String COURSE_TYPE_REQUIRED = "REQUIRED";
    /** 课程类型：选修 */
    public static final String COURSE_TYPE_ELECTIVE = "ELECTIVE";
    /** 课程类型：公选 */
    public static final String COURSE_TYPE_PUBLIC = "PUBLIC";

    /** 课程状态：下架 */
    public static final Integer COURSE_STATUS_OFF = 0;
    /** 课程状态：正常 */
    public static final Integer COURSE_STATUS_NORMAL = 1;
    /** 课程状态：已结课 */
    public static final Integer COURSE_STATUS_FINISHED = 2;

    // ------------------------------ 选课记录 ------------------------------
    /** 已退选 */
    public static final Integer SELECTION_DROPPED = 0;
    /** 已选课 */
    public static final Integer SELECTION_SELECTED = 1;
    /** 已修完 */
    public static final Integer SELECTION_FINISHED = 2;

    // ------------------------------ 成绩 ------------------------------
    /** 成绩草稿 */
    public static final Integer GRADE_STATUS_DRAFT = 0;
    /** 成绩已发布 */
    public static final Integer GRADE_STATUS_PUBLISHED = 1;
    /** 成绩已归档 */
    public static final Integer GRADE_STATUS_ARCHIVED = 2;

    /** 及格线 */
    public static final double PASS_SCORE = 60.0;

    // ------------------------------ 学期 ------------------------------
    public static final Integer SEMESTER_STATUS_NOT_START = 0;
    public static final Integer SEMESTER_STATUS_SELECTING = 1;
    public static final Integer SEMESTER_STATUS_RUNNING = 2;
    public static final Integer SEMESTER_STATUS_FINISHED = 3;

    // ------------------------------ 周次类型 ------------------------------
    public static final String WEEK_TYPE_ALL = "ALL";
    public static final String WEEK_TYPE_ODD = "ODD";
    public static final String WEEK_TYPE_EVEN = "EVEN";

    // ------------------------------ 时间冲突 ------------------------------
    /** 上午第一节开始节次 */
    public static final int MAX_SECTION = 12;
    public static final int MAX_DAY_OF_WEEK = 7;
}
