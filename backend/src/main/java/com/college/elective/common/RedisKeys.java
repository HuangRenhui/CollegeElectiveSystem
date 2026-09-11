package com.college.elective.common;

/**
 * Redis Key 统一定义，避免散落在业务代码中造成冲突。
 *
 * <p>命名规范：{@code elective:{模块}:{业务}:{标识}}</p>
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    public static final String PREFIX = "elective:";

    // ---------------------------- 认证 ----------------------------
    /** 登录令牌：elective:auth:token:{userId} -> token */
    public static final String AUTH_TOKEN = PREFIX + "auth:token:";
    /** 登录用户信息缓存：elective:auth:user:{userId} */
    public static final String AUTH_USER = PREFIX + "auth:user:";
    /** 登录失败次数：elective:auth:fail:{username} */
    public static final String LOGIN_FAIL = PREFIX + "auth:fail:";

    // ---------------------------- 课程缓存 ----------------------------
    /** 课程详情：elective:course:info:{courseId} */
    public static final String COURSE_INFO = PREFIX + "course:info:";
    /** 课程剩余容量：elective:course:capacity:{courseId} */
    public static final String COURSE_CAPACITY = PREFIX + "course:capacity:";
    /** 课程已选学生集合：elective:course:selected:{courseId} */
    public static final String COURSE_SELECTED = PREFIX + "course:selected:";
    /** 选课结果校验标记 */
    public static final String COURSE_SELECT_LOCK = PREFIX + "course:lock:";
    /** 选课总开关 */
    public static final String SELECTION_SWITCH = PREFIX + "selection:switch";
    /** 当前学期缓存 */
    public static final String CURRENT_SEMESTER = PREFIX + "semester:current";
    /** 课程列表缓存（按学期） */
    public static final String COURSE_LIST = PREFIX + "course:list:";
    /** 公告列表缓存 */
    public static final String NOTICE_LIST = PREFIX + "notice:list:";

    // ---------------------------- 标签 ----------------------------
    public static final String TAG_COURSE = PREFIX + "cache:course";
    public static final String TAG_NOTICE = PREFIX + "cache:notice";
    public static final String TAG_SEMESTER = PREFIX + "cache:semester";

    /** 默认缓存过期时间（分钟） */
    public static final long DEFAULT_CACHE_MINUTES = 30L;
}
