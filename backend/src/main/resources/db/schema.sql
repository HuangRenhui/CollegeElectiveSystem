-- =====================================================================
-- 高校选修课管理系统 - 数据库结构脚本
--
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4 / utf8mb4_general_ci
--
-- 【使用场景】
--   需要手动创建表结构时使用（创建 college_elective 库并建表）。
--
--   ⚠️ 开发环境通常无需执行本脚本：
--      应用启动时会自动创建缺失的数据表（见 config/DatabaseInitializer.java），
--      且不会删除已有数据。使用其他库名时，只需在 application-dev.yml 中
--      修改连接串的库名即可，无需改动本脚本。
--
-- 【执行方式】
--   mysql -uroot -p < schema.sql
--
-- 【风险提示】
--   ⚠️ 本脚本包含 DROP TABLE IF EXISTS 语句，会清空本系统 13 张表的既有数据，
--      但不会影响同库中其他业务表。生产环境请勿随意执行。
--
-- 【完整初始化顺序】
--   schema.sql → data.sql → demo-data.sql
--   或直接执行一键脚本：mysql -uroot -p < scripts/init-db.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `college_elective`
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE `college_elective`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `sys_log`;
DROP TABLE IF EXISTS `notice`;
DROP TABLE IF EXISTS `course_grade`;
DROP TABLE IF EXISTS `course_selection`;
DROP TABLE IF EXISTS `course_schedule`;
DROP TABLE IF EXISTS `course`;
DROP TABLE IF EXISTS `classroom`;
DROP TABLE IF EXISTS `semester`;
DROP TABLE IF EXISTS `student`;
DROP TABLE IF EXISTS `teacher`;
DROP TABLE IF EXISTS `major`;
DROP TABLE IF EXISTS `department`;
DROP TABLE IF EXISTS `sys_user`;

-- ---------------------------------------------------------------------
-- 用户表（统一身份认证，通过 role 区分角色）
-- ---------------------------------------------------------------------
CREATE TABLE `sys_user`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '登录账号（学号/工号/管理员账号）',
    `password`    VARCHAR(200) NOT NULL COMMENT '密码（Spring Security DelegatingPasswordEncoder 加密）',
    `real_name`   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
    `role`        VARCHAR(20)  NOT NULL COMMENT '角色：STUDENT-学生 TEACHER-教师 ADMIN-管理员',
    `gender`      TINYINT      NOT NULL DEFAULT 0 COMMENT '性别：0-未知 1-男 2-女',
    `phone`       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `email`       VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    `avatar`      VARCHAR(255)          DEFAULT NULL COMMENT '头像地址',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    `last_login_time` DATETIME          DEFAULT NULL COMMENT '最后登录时间',
    `last_login_ip`   VARCHAR(64)       DEFAULT NULL COMMENT '最后登录IP',
    `remark`      VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

-- ---------------------------------------------------------------------
-- 院系表
-- ---------------------------------------------------------------------
CREATE TABLE `department`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `dept_code`   VARCHAR(30) NOT NULL COMMENT '院系编码',
    `dept_name`   VARCHAR(80) NOT NULL COMMENT '院系名称',
    `dean_name`   VARCHAR(50)          DEFAULT NULL COMMENT '负责人',
    `contact_phone` VARCHAR(20)        DEFAULT NULL COMMENT '联系电话',
    `description` VARCHAR(255)         DEFAULT NULL COMMENT '简介',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='院系表';

-- ---------------------------------------------------------------------
-- 专业表
-- ---------------------------------------------------------------------
CREATE TABLE `major`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `major_code`  VARCHAR(30) NOT NULL COMMENT '专业编码',
    `major_name`  VARCHAR(80) NOT NULL COMMENT '专业名称',
    `dept_id`     BIGINT      NOT NULL COMMENT '所属院系ID',
    `degree_type` VARCHAR(20)          DEFAULT '本科' COMMENT '培养层次：本科/专科/研究生',
    `duration`    INT                  DEFAULT 4 COMMENT '学制（年）',
    `description` VARCHAR(255)         DEFAULT NULL COMMENT '专业简介',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_major_code` (`major_code`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='专业表';

-- ---------------------------------------------------------------------
-- 学生表（扩展信息，1:1 关联 sys_user）
-- ---------------------------------------------------------------------
CREATE TABLE `student`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `stu_no`      VARCHAR(30) NOT NULL COMMENT '学号',
    `dept_id`     BIGINT      NOT NULL COMMENT '所属院系ID',
    `major_id`    BIGINT      NOT NULL COMMENT '所属专业ID',
    `class_name`  VARCHAR(50)          DEFAULT NULL COMMENT '行政班级',
    `grade_year`  INT         NOT NULL DEFAULT 1 COMMENT '年级（1-4）',
    `enroll_year` INT                  DEFAULT NULL COMMENT '入学年份',
    `total_credit` DECIMAL(6, 1) NOT NULL DEFAULT 0.0 COMMENT '已获总学分',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stu_no` (`stu_no`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_major_id` (`major_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='学生信息表';

-- ---------------------------------------------------------------------
-- 教师表
-- ---------------------------------------------------------------------
CREATE TABLE `teacher`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `teacher_no`  VARCHAR(30) NOT NULL COMMENT '工号',
    `dept_id`     BIGINT      NOT NULL COMMENT '所属院系ID',
    `title`       VARCHAR(30)          DEFAULT NULL COMMENT '职称：助教/讲师/副教授/教授',
    `research_area` VARCHAR(255)       DEFAULT NULL COMMENT '研究方向',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_teacher_no` (`teacher_no`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='教师信息表';

-- ---------------------------------------------------------------------
-- 学期表
-- ---------------------------------------------------------------------
CREATE TABLE `semester`
(
    `id`                BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `semester_code`     VARCHAR(30) NOT NULL COMMENT '学期编码，如 2026-2027-1',
    `semester_name`     VARCHAR(50) NOT NULL COMMENT '学期名称，如 2026-2027学年第一学期',
    `start_date`        DATE        NOT NULL COMMENT '学期开始日期',
    `end_date`          DATE        NOT NULL COMMENT '学期结束日期',
    `select_start_time` DATETIME             DEFAULT NULL COMMENT '选课开放时间',
    `select_end_time`   DATETIME             DEFAULT NULL COMMENT '选课截止时间',
    `total_weeks`       INT         NOT NULL DEFAULT 20 COMMENT '总教学周数',
    `is_current`        TINYINT     NOT NULL DEFAULT 0 COMMENT '是否当前学期：0-否 1-是',
    `status`            TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0-未开始 1-选课中 2-进行中 3-已结束',
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_semester_code` (`semester_code`),
    KEY `idx_is_current` (`is_current`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='学期表';

-- ---------------------------------------------------------------------
-- 教室表
-- ---------------------------------------------------------------------
CREATE TABLE `classroom`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `room_no`     VARCHAR(30) NOT NULL COMMENT '教室编号',
    `building`    VARCHAR(50) NOT NULL COMMENT '教学楼',
    `capacity`    INT         NOT NULL DEFAULT 60 COMMENT '容纳人数',
    `room_type`   VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT '类型：NORMAL-普通 MULTIMEDIA-多媒体 LAB-实验室 GYM-体育场',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0-停用 1-可用',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_room_no` (`room_no`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='教室表';

-- ---------------------------------------------------------------------
-- 课程表（含开课信息）
-- ---------------------------------------------------------------------
CREATE TABLE `course`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_code`    VARCHAR(30)  NOT NULL COMMENT '课程编号',
    `course_name`    VARCHAR(100) NOT NULL COMMENT '课程名称',
    `semester_id`    BIGINT       NOT NULL COMMENT '开课学期ID',
    `dept_id`        BIGINT       NOT NULL COMMENT '开课院系ID',
    `teacher_id`     BIGINT                DEFAULT NULL COMMENT '授课教师ID',
    `credit`         DECIMAL(4, 1) NOT NULL DEFAULT 2.0 COMMENT '学分',
    `hours`          INT          NOT NULL DEFAULT 32 COMMENT '总学时',
    `course_type`    VARCHAR(20)  NOT NULL DEFAULT 'ELECTIVE' COMMENT '课程类型：REQUIRED-必修 ELECTIVE-选修 PUBLIC-公选',
    `exam_type`      VARCHAR(20)  NOT NULL DEFAULT 'EXAM' COMMENT '考核方式：EXAM-考试 CHECK-考查',
    `max_capacity`   INT          NOT NULL DEFAULT 60 COMMENT '最大容量',
    `selected_count` INT          NOT NULL DEFAULT 0 COMMENT '已选人数（Redis 为准，DB 异步/同步兜底）',
    `textbook`       VARCHAR(200)          DEFAULT NULL COMMENT '教材',
    `introduce`      VARCHAR(1000)         DEFAULT NULL COMMENT '课程简介',
    `selectable`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否可退选：0-不可 1-可',
    `status`         TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-下架 1-正常 2-已结课',
    `version`        INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_course_code_semester` (`course_code`, `semester_id`),
    KEY `idx_semester_id` (`semester_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_dept_id` (`dept_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='课程表';

-- ---------------------------------------------------------------------
-- 上课时间地点表（一门课可有多条排课记录）
-- ---------------------------------------------------------------------
CREATE TABLE `course_schedule`
(
    `id`            BIGINT  NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `course_id`     BIGINT  NOT NULL COMMENT '课程ID',
    `semester_id`   BIGINT  NOT NULL COMMENT '学期ID',
    `classroom_id`  BIGINT           DEFAULT NULL COMMENT '教室ID',
    `day_of_week`   TINYINT NOT NULL COMMENT '星期：1-7',
    `start_section` TINYINT NOT NULL COMMENT '开始节次：1-12',
    `end_section`   TINYINT NOT NULL COMMENT '结束节次：1-12',
    `start_week`    TINYINT NOT NULL DEFAULT 1 COMMENT '起始周',
    `end_week`      TINYINT NOT NULL DEFAULT 16 COMMENT '结束周',
    `week_type`     VARCHAR(10) NOT NULL DEFAULT 'ALL' COMMENT '周次类型：ALL-每周 ODD-单周 EVEN-双周',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_semester_day` (`semester_id`, `day_of_week`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='课程排课表';

-- ---------------------------------------------------------------------
-- 选课记录表
-- ---------------------------------------------------------------------
CREATE TABLE `course_selection`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `student_id`  BIGINT   NOT NULL COMMENT '学生ID',
    `course_id`   BIGINT   NOT NULL COMMENT '课程ID',
    `semester_id` BIGINT   NOT NULL COMMENT '学期ID',
    `select_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    `drop_time`   DATETIME          DEFAULT NULL COMMENT '退课时间',
    `status`      TINYINT  NOT NULL DEFAULT 1 COMMENT '状态：0-已退选 1-已选课 2-已修完',
    `score`       DECIMAL(5, 1)      DEFAULT NULL COMMENT '总评成绩（冗余，便于查询）',
    `select_type` TINYINT  NOT NULL DEFAULT 1 COMMENT '选课方式：1-正常选课 2-管理员代选',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_student_course_semester` (`student_id`, `course_id`, `semester_id`),
    KEY `idx_course_id` (`course_id`),
    KEY `idx_student_semester` (`student_id`, `semester_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='选课记录表';

-- ---------------------------------------------------------------------
-- 成绩表
-- ---------------------------------------------------------------------
CREATE TABLE `course_grade`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `selection_id` BIGINT       NOT NULL COMMENT '选课记录ID',
    `student_id`   BIGINT       NOT NULL COMMENT '学生ID',
    `course_id`    BIGINT       NOT NULL COMMENT '课程ID',
    `semester_id`  BIGINT       NOT NULL COMMENT '学期ID',
    `usual_score`  DECIMAL(5, 1)         DEFAULT NULL COMMENT '平时成绩',
    `exam_score`   DECIMAL(5, 1)         DEFAULT NULL COMMENT '期末成绩',
    `total_score`  DECIMAL(5, 1)         DEFAULT NULL COMMENT '总评成绩',
    `grade_point`  DECIMAL(3, 2)         DEFAULT NULL COMMENT '绩点',
    `is_pass`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否及格：0-否 1-是',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0-草稿 1-已发布 2-已归档',
    `input_by`     BIGINT                DEFAULT NULL COMMENT '录入人ID',
    `input_time`   DATETIME              DEFAULT NULL COMMENT '录入时间',
    `publish_time` DATETIME              DEFAULT NULL COMMENT '发布时间',
    `remark`       VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_selection_id` (`selection_id`),
    KEY `idx_student_semester` (`student_id`, `semester_id`),
    KEY `idx_course_id` (`course_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='课程成绩表';

-- ---------------------------------------------------------------------
-- 公告表
-- ---------------------------------------------------------------------
CREATE TABLE `notice`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `title`        VARCHAR(150) NOT NULL COMMENT '标题',
    `content`      TEXT         NOT NULL COMMENT '内容',
    `notice_type`  VARCHAR(20)  NOT NULL DEFAULT 'SYSTEM' COMMENT '类型：SYSTEM-系统 SELECTION-选课 EXAM-考试',
    `target_role`  VARCHAR(20)  NOT NULL DEFAULT 'ALL' COMMENT '目标角色：ALL/STUDENT/TEACHER',
    `publisher_id` BIGINT                DEFAULT NULL COMMENT '发布人ID',
    `publisher`    VARCHAR(50)           DEFAULT NULL COMMENT '发布人姓名',
    `top_flag`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    `view_count`   INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `status`       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0-草稿 1-已发布 2-已下架',
    `publish_time` DATETIME              DEFAULT NULL COMMENT '发布时间',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_status_publish` (`status`, `publish_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统公告表';

-- ---------------------------------------------------------------------
-- 操作日志表
-- ---------------------------------------------------------------------
CREATE TABLE `sys_log`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT                DEFAULT NULL COMMENT '操作人ID',
    `username`      VARCHAR(50)           DEFAULT NULL COMMENT '操作人账号',
    `module`        VARCHAR(50)           DEFAULT NULL COMMENT '业务模块',
    `operation`     VARCHAR(100)          DEFAULT NULL COMMENT '操作描述',
    `method`        VARCHAR(200)          DEFAULT NULL COMMENT '请求方法',
    `request_uri`   VARCHAR(255)          DEFAULT NULL COMMENT '请求地址',
    `request_param` TEXT                  DEFAULT NULL COMMENT '请求参数',
    `ip`            VARCHAR(64)           DEFAULT NULL COMMENT '操作IP',
    `cost_time`     BIGINT                DEFAULT NULL COMMENT '耗时（毫秒）',
    `success`       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否成功：0-失败 1-成功',
    `error_msg`     VARCHAR(1000)         DEFAULT NULL COMMENT '异常信息',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='系统操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
