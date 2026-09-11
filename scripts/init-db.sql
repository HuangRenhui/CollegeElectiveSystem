-- =====================================================================
-- 一键初始化脚本（便捷入口）
--
-- 使用方式：
--   mysql -uroot -p < scripts/init-db.sql
--
-- 说明：本脚本按顺序执行建表与初始化数据。
--      若使用 Docker Compose 部署，无需手动执行，容器首次启动会自动完成。
-- =====================================================================

SOURCE ../backend/src/main/resources/db/schema.sql;
SOURCE ../backend/src/main/resources/db/data.sql;

SELECT '数据库初始化完成' AS message,
       (SELECT COUNT(*) FROM college_elective.course) AS course_count,
       (SELECT COUNT(*) FROM college_elective.sys_user) AS user_count;
