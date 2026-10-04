-- =====================================================================
-- 高校选修课管理系统 - 增量演示数据（教师课程 / 成绩 / 教学评价）
--
-- 【为什么需要这个脚本】
--   学生、教师、管理员三个「教学评价」页面默认只查「当前学期」，而
--   demo-data.sql 里的评价全部落在历史学期，导致默认视图为空；
--   另外教师 6/7/8 在当前学期没有任何课程。
--   本脚本为当前学期补上「已修完选课 + 已发布成绩 + 已公开评价」，
--   并为教师 6/7/8 各加一门当前学期课程，使各页面默认视图都有数据。
--
-- 【依赖】需先导入 data.sql 与 demo-data.sql（学期 / 学生 / 教师 / 院系等基础数据）
--
-- 【执行方式】
--   mysql -uroot -p college_elective < scripts/seed-extra-demo.sql
--   （已由 init-db.sql 在末尾通过 SOURCE 引入，重跑初始化不会丢这批数据）
--
-- 【幂等性】所有 INSERT 均带 NOT EXISTS 守卫，可重复执行，不会产生重复数据。
--
-- 【不影响】不修改 course.selected_count，选课余量与 Redis 缓存不受影响。
-- =====================================================================

SET NAMES utf8mb4;
SET @sem := (SELECT id FROM semester WHERE is_current = 1 LIMIT 1);
-- 当前学期：2026-2027-1（id=3）

-- ---------------------------------------------------------------------
-- 1. 教师 6/7/8 的当前学期课程（此前这三位教师在本学期没有课）
-- ---------------------------------------------------------------------
-- GE7001 大学计算机基础（教师ID=6）
INSERT INTO course (course_code, course_name, semester_id, dept_id, teacher_id, credit, hours, course_type, exam_type, max_capacity, selected_count, textbook, introduce, selectable, status, version)
SELECT 'GE7001', '大学计算机基础', @sem, 1, 6, '2.0', 32, 'PUBLIC', 'CHECK', 120, 0, '大学计算机基础配套教材', '大学计算机基础，注重理论联系实际，适合各专业学生选修。', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM course WHERE course_code = 'GE7001' AND semester_id = @sem);

INSERT INTO course_schedule (course_id, semester_id, classroom_id, day_of_week, start_section, end_section, start_week, end_week, week_type)
SELECT c.id, @sem, 1, 3, 5, 6, 1, 16, 'ALL' FROM course c
WHERE c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_schedule cs WHERE cs.course_id = c.id AND cs.semester_id = @sem);

-- GE7002 学术英语写作（教师ID=7）
INSERT INTO course (course_code, course_name, semester_id, dept_id, teacher_id, credit, hours, course_type, exam_type, max_capacity, selected_count, textbook, introduce, selectable, status, version)
SELECT 'GE7002', '学术英语写作', @sem, 3, 7, '2.0', 32, 'ELECTIVE', 'CHECK', 60, 0, '学术英语写作配套教材', '学术英语写作，注重理论联系实际，适合各专业学生选修。', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM course WHERE course_code = 'GE7002' AND semester_id = @sem);

INSERT INTO course_schedule (course_id, semester_id, classroom_id, day_of_week, start_section, end_section, start_week, end_week, week_type)
SELECT c.id, @sem, 2, 4, 7, 8, 1, 16, 'ALL' FROM course c
WHERE c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_schedule cs WHERE cs.course_id = c.id AND cs.semester_id = @sem);

-- GE7003 体育与健康（教师ID=8）
INSERT INTO course (course_code, course_name, semester_id, dept_id, teacher_id, credit, hours, course_type, exam_type, max_capacity, selected_count, textbook, introduce, selectable, status, version)
SELECT 'GE7003', '体育与健康', @sem, 5, 8, '1.0', 16, 'PUBLIC', 'CHECK', 150, 0, '体育与健康配套教材', '体育与健康，注重理论联系实际，适合各专业学生选修。', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM course WHERE course_code = 'GE7003' AND semester_id = @sem);

INSERT INTO course_schedule (course_id, semester_id, classroom_id, day_of_week, start_section, end_section, start_week, end_week, week_type)
SELECT c.id, @sem, 3, 5, 9, 10, 1, 16, 'ALL' FROM course c
WHERE c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_schedule cs WHERE cs.course_id = c.id AND cs.semester_id = @sem);

-- ---------------------------------------------------------------------
-- 2. 当前学期「已修完」选课记录（status=2）
--    原库当前学期只有 status=1（已选课），这里补 status=2，
--    使成绩 / 评价 / 待评价等页面有数据。
-- ---------------------------------------------------------------------
-- 2023010101 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-06 22:24:16', 2, '83.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-24 22:24:16', 2, '84.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-19 22:24:16', 2, '77.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-28 22:24:16', 2, '90.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-18 22:24:16', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-25 22:24:16', 2, '86.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-09 22:24:16', 2, '83.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-23 22:24:16', 2, '80.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-10 22:24:16', 2, '99.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-29 22:24:16', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-28 22:24:16', 2, '87.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010101 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-22 22:24:16', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-12 22:24:16', 2, '98.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-20 22:24:16', 2, '88.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-18 22:24:16', 2, '90.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-20 22:24:16', 2, '71.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-28 22:24:16', 2, '82.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-10 22:24:16', 2, '75.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-07 22:24:16', 2, '92.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-23 22:24:16', 2, '79.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-14 22:24:16', 2, '74.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-24 22:24:16', 2, '92.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-26 22:24:16', 2, '87.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-06 22:24:16', 2, '74.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-29 22:24:16', 2, '77.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-23 22:24:16', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010101 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-08 22:24:16', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-11 22:24:16', 2, '74.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-04 22:24:16', 2, '81.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-17 22:24:16', 2, '76.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-20 22:24:16', 2, '90.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-09 22:24:16', 2, '90.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-27 22:24:16', 2, '83.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-13 22:24:16', 2, '91.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-14 22:24:16', 2, '90.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-10 22:24:16', 2, '88.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-24 22:24:16', 2, '77.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-05 22:24:16', 2, '83.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-27 22:24:16', 2, '74.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-07 22:24:16', 2, '91.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-07 22:24:16', 2, '81.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-30 22:24:16', 2, '84.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-23 22:24:16', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-07 22:24:16', 2, '74.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-21 22:24:16', 2, '94.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-15 22:24:16', 2, '92.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-07 22:24:16', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-18 22:24:16', 2, '82.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-22 22:24:16', 2, '87.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-06-06 22:24:16', 2, '73.0', 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023020101 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-29 22:24:16', 2, '97.0', 1 FROM student st, course c
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-05-08 22:24:16', 2, '70.0', 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-24 00:00:00', 2, NULL, 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010101 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-25 00:00:00', 2, NULL, 1 FROM student st, course c
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010102 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-23 00:00:00', 2, NULL, 1 FROM student st, course c
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023010201 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-23 00:00:00', 2, NULL, 1 FROM student st, course c
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-29 00:00:00', 2, NULL, 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2023030101 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-22 00:00:00', 2, NULL, 1 FROM student st, course c
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-13 12:00:00', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-17 12:00:00', 2, '75.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-21 12:00:00', 2, '71.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010301 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-03 12:00:00', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020102 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-12 12:00:00', 2, '83.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020201 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-02 12:00:00', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030102 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-05 12:00:00', 2, '86.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030201 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-19 12:00:00', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026040101 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-24 12:00:00', 2, '88.0', 1 FROM student st, course c
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026050101 GE1001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-27 12:00:00', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE1001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-16 12:00:00', 2, '67.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010202 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-04 12:00:00', 2, '79.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-14 12:00:00', 2, '77.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-30 12:00:00', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-12 12:00:00', 2, '83.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020102 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-17 12:00:00', 2, '87.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020201 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-25 12:00:00', 2, '83.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030102 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-10 12:00:00', 2, '84.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030201 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-25 12:00:00', 2, '69.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026040101 GE1002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-08 12:00:00', 2, '94.0', 1 FROM student st, course c
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE1002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-05 12:00:00', 2, '81.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-24 12:00:00', 2, '82.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010301 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-06 12:00:00', 2, '77.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026050101 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-06 12:00:00', 2, '93.0', 1 FROM student st, course c
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-06 12:00:00', 2, '84.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-15 12:00:00', 2, '71.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020102 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-09 12:00:00', 2, '99.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020201 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-16 12:00:00', 2, '83.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030102 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-01 12:00:00', 2, '86.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030201 GE2001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-29 12:00:00', 2, '90.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE2001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-09 12:00:00', 2, '73.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-15 12:00:00', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010202 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-01 12:00:00', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010101 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-03 12:00:00', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-26 12:00:00', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-20 12:00:00', 2, '75.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010301 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-01 12:00:00', 2, '87.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026040101 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-09 12:00:00', 2, '97.0', 1 FROM student st, course c
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026050101 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-22 12:00:00', 2, '76.0', 1 FROM student st, course c
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE3001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-04 12:00:00', 2, '82.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE3001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-07 12:00:00', 2, '96.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-04 12:00:00', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010202 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-30 12:00:00', 2, '88.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-19 12:00:00', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-26 12:00:00', 2, '71.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010301 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-18 12:00:00', 2, '72.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020102 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-28 12:00:00', 2, '82.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030102 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-25 12:00:00', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030201 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-27 12:00:00', 2, '91.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026040101 GE3002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-10 12:00:00', 2, '76.0', 1 FROM student st, course c
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE3002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-03 12:00:00', 2, '87.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-16 12:00:00', 2, '79.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010202 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-28 12:00:00', 2, '94.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020201 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-27 12:00:00', 2, '73.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026050101 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-12 12:00:00', 2, '92.0', 1 FROM student st, course c
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-30 12:00:00', 2, '78.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-28 12:00:00', 2, '86.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010301 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-15 12:00:00', 2, '99.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026040101 GE4001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-25 12:00:00', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE4001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-23 12:00:00', 2, '98.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-14 12:00:00', 2, '81.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-24 12:00:00', 2, '70.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010202 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-30 12:00:00', 2, '93.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020102 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-10 12:00:00', 2, '88.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020201 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-26 12:00:00', 2, '88.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030201 GE4002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-06 12:00:00', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE4002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010101 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-27 12:00:00', 2, '79.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-16 12:00:00', 2, '99.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-08 12:00:00', 2, '84.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030102 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-31 12:00:00', 2, '84.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026050101 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-22 12:00:00', 2, '76.0', 1 FROM student st, course c
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-23 12:00:00', 2, '81.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-16 12:00:00', 2, '82.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010202 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-13 12:00:00', 2, '77.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010301 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-17 12:00:00', 2, '94.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020102 GE5001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-08 12:00:00', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE5001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010101 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-07 12:00:00', 2, '76.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-23 12:00:00', 2, '81.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-09 12:00:00', 2, '88.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-24 12:00:00', 2, '84.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020201 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-16 12:00:00', 2, '77.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030102 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-19 12:00:00', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030201 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-09 12:00:00', 2, '76.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-21 12:00:00', 2, '87.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-08 12:00:00', 2, '95.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010202 GE6001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-14 12:00:00', 2, '80.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE6001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-17 12:00:00', 2, '94.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026040101 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-13 12:00:00', 2, '68.0', 1 FROM student st, course c
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-05 12:00:00', 2, '76.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-26 12:00:00', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010301 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-19 12:00:00', 2, '93.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020102 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-23 12:00:00', 2, '78.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020201 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-02 12:00:00', 2, '96.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030102 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-07 12:00:00', 2, '90.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-29 12:00:00', 2, '71.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE6002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-04 12:00:00', 2, '76.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE6002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010101 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-21 12:00:00', 2, '79.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026050101 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-21 12:00:00', 2, '93.0', 1 FROM student st, course c
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-02 12:00:00', 2, '82.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-09 12:00:00', 2, '93.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030201 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-15 12:00:00', 2, '70.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026040101 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-20 12:00:00', 2, '82.0', 1 FROM student st, course c
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-07 12:00:00', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-21 12:00:00', 2, '96.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010202 GE7001
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-27 12:00:00', 2, '89.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE7001' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010101 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-27 12:00:00', 2, '92.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026050101 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-04 12:00:00', 2, '97.0', 1 FROM student st, course c
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-13 12:00:00', 2, '67.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-01 12:00:00', 2, '91.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010301 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-21 12:00:00', 2, '86.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020102 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-07 12:00:00', 2, '73.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020201 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-09 12:00:00', 2, '72.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030102 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-30 12:00:00', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030201 GE7002
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-26 12:00:00', 2, '92.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE7002' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010101 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-25 12:00:00', 2, '86.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026040101 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-01 12:00:00', 2, '85.0', 1 FROM student st, course c
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026050101 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-01 12:00:00', 2, '68.0', 1 FROM student st, course c
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010102 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-03 12:00:00', 2, '91.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010201 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-07-28 12:00:00', 2, '94.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026020101 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-21 12:00:00', 2, '77.0', 1 FROM student st, course c
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026030101 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-18 12:00:00', 2, '87.0', 1 FROM student st, course c
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010103 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-16 12:00:00', 2, '80.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- 2026010104 GE7003
INSERT INTO course_selection (student_id, course_id, semester_id, select_time, status, score, select_type)
SELECT st.id, c.id, @sem, '2026-08-19 12:00:00', 2, '86.0', 1 FROM student st, course c
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE7003' AND c.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_selection cs WHERE cs.student_id = st.id AND cs.course_id = c.id AND cs.semester_id = @sem);

-- ---------------------------------------------------------------------
-- 3. 成绩（status=1 已发布 / status=0 草稿）
-- ---------------------------------------------------------------------
INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '83.0', '83.0', '83.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-05 22:24:16', '2026-09-06 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '84.0', '84.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-08-23 22:24:16', '2026-08-24 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '77.0', '77.0', '77.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-18 22:24:16', '2026-09-19 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '90.0', '90.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-27 22:24:16', '2026-09-28 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-08-17 22:24:16', '2026-08-18 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '86.0', '86.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-08-24 22:24:16', '2026-08-25 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '81.0', '83.0', '83.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-08 22:24:16', '2026-09-09 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '76.0', '80.0', '80.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-08-22 22:24:16', '2026-08-23 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '100.0', '99.0', '99.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-08-09 22:24:16', '2026-08-10 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-08-28 22:24:16', '2026-08-29 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '87.0', '87.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-27 22:24:16', '2026-09-28 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-08-21 22:24:16', '2026-08-22 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '100.0', '98.0', '98.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-11 22:24:16', '2026-09-12 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '88.0', '88.0', '88.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-08-19 22:24:16', '2026-08-20 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '92.0', '90.0', '90.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-17 22:24:16', '2026-09-18 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '70.0', '71.0', '71.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-08-19 22:24:16', '2026-08-20 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '80.0', '82.0', '82.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-27 22:24:16', '2026-09-28 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '72.0', '75.0', '75.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-09 22:24:16', '2026-09-10 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '89.0', '92.0', '92.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-06 22:24:16', '2026-09-07 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '79.0', '79.0', '79.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-08-22 22:24:16', '2026-08-23 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '75.0', '74.0', '74.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-08-13 22:24:16', '2026-08-14 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '94.0', '92.0', '92.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-23 22:24:16', '2026-09-24 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '87.0', '87.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-08-25 22:24:16', '2026-08-26 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '76.0', '74.0', '74.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-05 22:24:16', '2026-09-06 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '75.0', '77.0', '77.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-28 22:24:16', '2026-09-29 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '89.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-08-22 22:24:16', '2026-08-23 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '99.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-08-07 22:24:16', '2026-08-08 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '72.0', '74.0', '74.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-08-10 22:24:16', '2026-08-11 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '84.0', '81.0', '81.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-03 22:24:16', '2026-09-04 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '76.0', '76.0', '76.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-08-16 22:24:16', '2026-08-17 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '90.0', '90.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-19 22:24:16', '2026-09-20 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '90.0', '90.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-08-08 22:24:16', '2026-08-09 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '83.0', '83.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-26 22:24:16', '2026-09-27 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '95.0', '91.0', '91.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-08-12 22:24:16', '2026-08-13 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '90.0', '90.0', '90.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-08-13 22:24:16', '2026-08-14 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '92.0', '88.0', '88.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-09 22:24:16', '2026-09-10 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '77.0', '77.0', '77.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-23 22:24:16', '2026-09-24 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '79.0', '83.0', '83.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-04 22:24:16', '2026-09-05 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '70.0', '74.0', '74.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-08-26 22:24:16', '2026-08-27 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '94.0', '91.0', '91.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-08-06 22:24:16', '2026-08-07 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '79.0', '81.0', '81.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-08-06 22:24:16', '2026-08-07 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '84.0', '84.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-09-29 22:24:16', '2026-09-30 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-08-22 22:24:16', '2026-08-23 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '74.0', '74.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-08-06 22:24:16', '2026-08-07 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '94.0', '94.0', '4.00', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-08-20 22:24:16', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '92.0', '92.0', '4.00', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-14 22:24:16', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '95.0', '95.0', '4.00', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-08-06 22:24:16', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '81.0', '82.0', '82.0', '3.30', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-17 22:24:16', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '84.0', '87.0', '87.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-10-29 00:00:00', '2026-10-30 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '73.0', '73.0', '73.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-10-08 00:00:00', '2026-10-09 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '100.0', '97.0', '97.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-09-28 00:00:00', '2026-09-29 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '66.0', '70.0', '70.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-11-26 00:00:00', '2026-11-27 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-27 12:00:00', '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '79.0', '75.0', '75.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-10-01 12:00:00', '2026-10-02 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '71.0', '71.0', '71.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-10-05 12:00:00', '2026-10-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '97.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-17 12:00:00', '2026-09-18 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '83.0', '83.0', '83.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-26 12:00:00', '2026-09-27 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '99.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-16 12:00:00', '2026-09-17 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '84.0', '86.0', '86.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-19 12:00:00', '2026-09-20 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '81.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-10-03 12:00:00', '2026-10-04 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '88.0', '88.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-07 12:00:00', '2026-09-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '95.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-10 12:00:00', '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '67.0', '67.0', '67.0', '1.50', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-30 12:00:00', '2026-10-01 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '79.0', '79.0', '79.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-18 12:00:00', '2026-09-19 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '77.0', '77.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-28 12:00:00', '2026-09-29 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-13 12:00:00', '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '82.0', '83.0', '83.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-26 12:00:00', '2026-09-27 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '89.0', '87.0', '87.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-10-01 12:00:00', '2026-10-02 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '81.0', '83.0', '83.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-08 12:00:00', '2026-09-09 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '84.0', '84.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-24 12:00:00', '2026-09-25 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '68.0', '69.0', '69.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-08 12:00:00', '2026-09-09 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '95.0', '94.0', '94.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-22 12:00:00', '2026-09-23 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '81.0', '81.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-19 12:00:00', '2026-09-20 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '82.0', '82.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-07 12:00:00', '2026-09-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '74.0', '77.0', '77.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-20 12:00:00', '2026-09-21 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '92.0', '93.0', '93.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-20 12:00:00', '2026-09-21 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '84.0', '84.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-20 12:00:00', '2026-09-21 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '71.0', '71.0', '71.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-29 12:00:00', '2026-09-30 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '100.0', '99.0', '99.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-23 12:00:00', '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '82.0', '83.0', '83.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-30 12:00:00', '2026-10-01 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '86.0', '86.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-15 12:00:00', '2026-09-16 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '90.0', '90.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 1), '2026-09-12 12:00:00', '2026-09-13 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '72.0', '73.0', '73.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-23 12:00:00', '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '99.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-29 12:00:00', '2026-09-30 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-15 12:00:00', '2026-09-16 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-17 12:00:00', '2026-09-18 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-09 12:00:00', '2026-09-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '72.0', '75.0', '75.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-10-04 12:00:00', '2026-10-05 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '87.0', '87.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-15 12:00:00', '2026-09-16 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '96.0', '97.0', '97.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-23 12:00:00', '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '72.0', '76.0', '76.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-05 12:00:00', '2026-09-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '82.0', '82.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-18 12:00:00', '2026-09-19 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '95.0', '96.0', '96.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-21 12:00:00', '2026-09-22 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '81.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-18 12:00:00', '2026-09-19 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '88.0', '88.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-13 12:00:00', '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '89.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-10-03 12:00:00', '2026-10-04 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '68.0', '71.0', '71.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-09 12:00:00', '2026-09-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '72.0', '72.0', '72.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-10-02 12:00:00', '2026-10-03 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '82.0', '82.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-11 12:00:00', '2026-09-12 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-08 12:00:00', '2026-09-09 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '91.0', '91.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-10 12:00:00', '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '76.0', '76.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 2), '2026-09-24 12:00:00', '2026-09-25 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '87.0', '87.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-17 12:00:00', '2026-09-18 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '82.0', '79.0', '79.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-30 12:00:00', '2026-10-01 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '98.0', '94.0', '94.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-11 12:00:00', '2026-09-12 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '77.0', '73.0', '73.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-10 12:00:00', '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '94.0', '92.0', '92.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-26 12:00:00', '2026-09-27 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '79.0', '78.0', '78.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-13 12:00:00', '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '86.0', '86.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-11 12:00:00', '2026-09-12 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '98.0', '99.0', '99.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-29 12:00:00', '2026-09-30 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-08 12:00:00', '2026-09-09 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '99.0', '98.0', '98.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-06 12:00:00', '2026-09-07 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '81.0', '81.0', '81.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-28 12:00:00', '2026-09-29 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '69.0', '70.0', '70.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-07 12:00:00', '2026-09-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '95.0', '93.0', '93.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-13 12:00:00', '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '90.0', '88.0', '88.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-24 12:00:00', '2026-09-25 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '88.0', '88.0', '88.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-09 12:00:00', '2026-09-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 3), '2026-09-20 12:00:00', '2026-09-21 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '77.0', '79.0', '79.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-10 12:00:00', '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '99.0', '99.0', '99.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-30 12:00:00', '2026-10-01 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '80.0', '84.0', '84.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-22 12:00:00', '2026-09-23 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '81.0', '84.0', '84.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-14 12:00:00', '2026-09-15 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '76.0', '76.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-05 12:00:00', '2026-09-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '81.0', '81.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-06 12:00:00', '2026-09-07 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '82.0', '82.0', '82.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-30 12:00:00', '2026-10-01 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '74.0', '77.0', '77.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-27 12:00:00', '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '94.0', '94.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-10-01 12:00:00', '2026-10-02 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 4), '2026-09-22 12:00:00', '2026-09-23 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '76.0', '76.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-21 12:00:00', '2026-09-22 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '81.0', '81.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-06 12:00:00', '2026-09-07 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '88.0', '88.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-23 12:00:00', '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '88.0', '84.0', '84.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-07 12:00:00', '2026-09-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '77.0', '77.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-30 12:00:00', '2026-10-01 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-10-03 12:00:00', '2026-10-04 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '77.0', '76.0', '76.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-23 12:00:00', '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '84.0', '87.0', '87.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-10-05 12:00:00', '2026-10-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '95.0', '95.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-22 12:00:00', '2026-09-23 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '80.0', '80.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-28 12:00:00', '2026-09-29 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '94.0', '94.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-10-01 12:00:00', '2026-10-02 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '66.0', '68.0', '68.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-27 12:00:00', '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '74.0', '76.0', '76.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-19 12:00:00', '2026-09-20 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-09 12:00:00', '2026-09-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '89.0', '93.0', '93.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-10-03 12:00:00', '2026-10-04 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '78.0', '78.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-06 12:00:00', '2026-09-07 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '92.0', '96.0', '96.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-16 12:00:00', '2026-09-17 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '90.0', '90.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-21 12:00:00', '2026-09-22 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '70.0', '71.0', '71.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-12 12:00:00', '2026-09-13 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '73.0', '76.0', '76.0', '2.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 5), '2026-09-18 12:00:00', '2026-09-19 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '79.0', '79.0', '3.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-10-05 12:00:00', '2026-10-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '96.0', '93.0', '93.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-10-05 12:00:00', '2026-10-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '82.0', '82.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-09-16 12:00:00', '2026-09-17 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '97.0', '93.0', '93.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-09-23 12:00:00', '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '69.0', '70.0', '70.0', '2.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-09-29 12:00:00', '2026-09-30 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '82.0', '82.0', '3.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-10-04 12:00:00', '2026-10-05 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-09-21 12:00:00', '2026-09-22 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '97.0', '96.0', '96.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-10-05 12:00:00', '2026-10-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '90.0', '89.0', '89.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 6), '2026-09-10 12:00:00', '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '92.0', '92.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-10 12:00:00', '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '97.0', '97.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-18 12:00:00', '2026-09-19 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '65.0', '67.0', '67.0', '1.50', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-27 12:00:00', '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '91.0', '91.0', '91.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-15 12:00:00', '2026-09-16 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '86.0', '86.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-10-05 12:00:00', '2026-10-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '72.0', '73.0', '73.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-21 12:00:00', '2026-09-22 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '72.0', '72.0', '72.0', '2.30', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-23 12:00:00', '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '86.0', '85.0', '85.0', '3.70', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-13 12:00:00', '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '94.0', '92.0', '92.0', '4.00', 1, 1, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 7), '2026-09-09 12:00:00', '2026-09-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '84.0', '86.0', '86.0', '3.70', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-09-08 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '88.0', '85.0', '85.0', '3.70', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-09-15 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '69.0', '68.0', '68.0', '2.00', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-09-15 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '89.0', '91.0', '91.0', '4.00', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-09-17 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '93.0', '94.0', '94.0', '4.00', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-09-11 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '78.0', '77.0', '77.0', '2.70', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-10-05 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '87.0', '87.0', '87.0', '3.70', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-10-02 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '84.0', '80.0', '80.0', '3.00', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-09-30 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

INSERT INTO course_grade (selection_id, student_id, course_id, semester_id, usual_score, exam_score, total_score, grade_point, is_pass, status, input_by, input_time, publish_time)
SELECT cs.id, cs.student_id, cs.course_id, cs.semester_id, '85.0', '86.0', '86.0', '3.70', 1, 0, (SELECT u.id FROM teacher t JOIN sys_user u ON u.id = t.user_id WHERE t.id = 8), '2026-10-03 12:00:00', NULL
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_grade g WHERE g.selection_id = cs.id);

-- ---------------------------------------------------------------------
-- 4. 教学评价（status=2 已公开；每门课留若干学生不评价，用于演示「待评价」）
-- ---------------------------------------------------------------------
INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 3, 5, 5, '4.5', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-09-07 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 3, 3, 3, '3.0', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-08-25 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 2, 2, 2, '2.5', '内容充实，希望多一些实际案例。', 1, 2, '2026-09-20 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 5, 4, '4.2', '老师很负责，课后答疑及时，推荐这门课。', 0, 2, '2026-08-19 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 5, 5, '4.2', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-08-26 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 5, '4.8', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-09-10 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-08-11 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 3, '4.2', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-08-30 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 4, 4, '4.2', '整体不错，但进度偏快，需要课后花时间消化。', 0, 2, '2026-09-29 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-09-13 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 5, 3, '4.0', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-08-21 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 4, 5, '4.5', '讲解由浅入深，案例贴近实际，收获很大。', 1, 2, '2026-09-19 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 4, 3, '4.0', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-09-29 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 2, 4, '3.2', '整体不错，但进度偏快，需要课后花时间消化。', 1, 2, '2026-09-11 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 5, '4.8', '讲解由浅入深，案例贴近实际，收获很大。', 1, 2, '2026-09-08 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 4, 4, '3.8', '课程有价值，就是作业有点多。', 1, 2, '2026-08-15 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 4, 4, '4.2', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-09-25 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 4, 5, '4.5', '老师很负责，课后答疑及时，推荐这门课。', 0, 2, '2026-08-27 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 2, 4, 2, 3, '2.8', '内容充实，希望多一些实际案例。', 1, 2, '2026-09-30 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 4, '4.5', '注重互动，课堂氛围轻松，知识点讲得透彻。', 0, 2, '2026-08-24 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023030101' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 5, '4.8', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-08-09 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 3, 5, '4.0', '内容前沿，能看出老师准备很充分。', 0, 2, '2026-09-05 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 4, 3, '3.5', '整体不错，但进度偏快，需要课后花时间消化。', 1, 2, '2026-08-18 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 4, '4.5', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-21 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 5, 5, 5, '4.5', '课程有价值，就是作业有点多。', 1, 2, '2026-09-28 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 5, 5, '4.5', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-08-14 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-08-15 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 2, 3, 2, 4, '2.8', '内容比较基础，与预期有差距。', 1, 2, '2026-09-25 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 3, 4, '4.0', '内容充实，希望多一些实际案例。', 1, 2, '2026-09-06 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 2, 4, 3, '3.0', '讲得清楚，不过考核方式可以再明确一点。', 1, 2, '2026-08-28 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 5, 3, 3, '3.5', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-08-08 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 5, 4, '4.0', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-10-01 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 5, 4, 3, '3.8', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-08-24 22:24:16'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 5, '4.8', '整体不错，节奏稍快，需要课后复习。', 0, 2, '2026-09-29 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 5, '4.8', '内容实用，老师讲解细致，课堂互动多。', 1, 2, '2026-10-17 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 4, '4.8', '课程安排合理，考核方式清晰，收获不错。', 1, 2, '2026-11-10 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 5, '4.8', '课程安排合理，考核方式清晰，收获不错。', 1, 2, '2026-11-01 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010102' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 3, 3, '3.2', '老师很耐心，课后答疑及时。', 0, 2, '2026-10-11 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023010201' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 5, '4.8', '内容实用，老师讲解细致，课堂互动多。', 1, 2, '2026-10-01 00:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2023020101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 3, 4, '3.5', '希望增加课堂练习与答疑时间。', 1, 2, '2026-10-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 5, 5, '4.5', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-22 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 3, 3, 4, '3.2', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-10-01 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 4, 5, '4.5', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-09-21 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 4, 3, '4.0', '课件质量高，复习时很有帮助。', 1, 2, '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 3, 5, 4, '3.8', '课件质量高，复习时很有帮助。', 0, 2, '2026-10-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 3, 3, 5, '4.0', '课件质量高，复习时很有帮助。', 0, 2, '2026-09-12 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-09-15 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE1001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 2, 2, 4, '2.8', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-09-23 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 4, 4, '4.0', '讲得清楚，不过考核方式可以再明确一点。', 1, 2, '2026-10-03 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 4, '4.5', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-09-18 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 3, '4.2', '课程有价值，就是作业有点多。', 1, 2, '2026-10-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 3, 4, '4.0', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-13 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 5, 4, 5, '4.2', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-29 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 3, 3, 2, '2.8', '节奏偏慢，希望增加实践环节。', 1, 2, '2026-09-13 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 4, '4.5', '注重互动，课堂氛围轻松，知识点讲得透彻。', 0, 2, '2026-09-27 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE1002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 3, 3, 3, '3.0', '讲得清楚，不过考核方式可以再明确一点。', 0, 2, '2026-09-25 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-09-25 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 3, '4.2', '课件质量高，复习时很有帮助。', 0, 2, '2026-09-25 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 2, 2, 4, 3, '2.8', '内容比较基础，与预期有差距。', 1, 2, '2026-10-04 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 4, 5, '4.5', '课堂节奏把控得好，重点突出，作业量适中。', 0, 2, '2026-10-05 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 3, 4, '3.5', '内容充实，希望多一些实际案例。', 1, 2, '2026-09-20 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 4, '4.5', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-09-17 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE2001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 2, 3, 2, 3, '2.5', '整体不错，但进度偏快，需要课后花时间消化。', 0, 2, '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 5, '4.8', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-10-04 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 2, 4, 2, '3.0', '内容比较基础，与预期有差距。', 1, 2, '2026-10-09 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 4, 4, '3.8', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-20 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 4, '4.8', '课件质量高，复习时很有帮助。', 1, 2, '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 2, 3, 4, 3, '3.0', '希望增加课堂练习与答疑时间。', 1, 2, '2026-09-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 4, '4.5', '课件质量高，复习时很有帮助。', 1, 2, '2026-09-23 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE3001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 5, '4.8', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-09-26 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 4, 5, '4.0', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-23 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 3, 3, '4.0', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-18 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 4, 5, '4.0', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-10-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 3, 2, 4, '3.0', '整体不错，但进度偏快，需要课后花时间消化。', 1, 2, '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 3, 3, 3, '3.5', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-09-13 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 4, 5, '4.5', '内容前沿，能看出老师准备很充分。', 0, 2, '2026-09-15 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 2, 3, '3.2', '希望增加课堂练习与答疑时间。', 1, 2, '2026-09-29 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE3002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 4, 5, '4.0', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-09-22 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 2, 2, 2, '2.2', '老师很负责，课后答疑及时，推荐这门课。', 0, 2, '2026-10-05 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 4, 5, '4.2', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-09-16 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 4, '4.8', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-10-01 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 2, 2, 4, '3.0', '希望增加课堂练习与答疑时间。', 1, 2, '2026-09-18 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 3, 3, 5, '3.5', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-16 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 5, '4.8', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-10-04 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE4001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 4, '4.5', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 4, 2, '3.2', '节奏偏慢，希望增加实践环节。', 1, 2, '2026-09-12 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 5, '4.8', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-18 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 4, 5, '4.0', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-29 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 4, 3, '3.5', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE4002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 4, 3, '3.5', '整体不错，但进度偏快，需要课后花时间消化。', 1, 2, '2026-09-15 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-10-05 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 3, 3, '4.0', '课程有价值，就是作业有点多。', 1, 2, '2026-09-27 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 5, 4, 5, '4.2', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 4, '4.5', '课程有价值，就是作业有点多。', 1, 2, '2026-10-05 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 2, 2, 2, 2, '2.0', '节奏偏慢，希望增加实践环节。', 1, 2, '2026-10-02 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '课堂节奏把控得好，重点突出，作业量适中。', 0, 2, '2026-10-06 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 3, 4, '3.8', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-27 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE5001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 3, 4, '3.5', '内容前沿，能看出老师准备很充分。', 0, 2, '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 3, 3, '4.0', '讲得清楚，不过考核方式可以再明确一点。', 0, 2, '2026-09-12 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 4, 3, '3.5', '课程有价值，就是作业有点多。', 1, 2, '2026-10-05 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 3, 3, '3.8', '课堂节奏把控得好，重点突出，作业量适中。', 0, 2, '2026-10-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 2, 3, 3, 2, '2.5', '课程有价值，就是作业有点多。', 1, 2, '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 4, 5, '4.0', '希望增加课堂练习与答疑时间。', 1, 2, '2026-10-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 5, '4.8', '老师很负责，课后答疑及时，推荐这门课。', 0, 2, '2026-09-27 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 3, 3, 5, '4.0', '老师很负责，课后答疑及时，推荐这门课。', 0, 2, '2026-10-03 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE6001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 2, 4, 2, '3.0', '整体不错，但进度偏快，需要课后花时间消化。', 1, 2, '2026-10-02 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 4, 2, '3.2', '内容充实，希望多一些实际案例。', 1, 2, '2026-09-24 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 5, 3, '4.0', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 5, '4.8', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-10-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010301' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 2, 3, 3, '3.0', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-11 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020102' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-09-21 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 4, '4.5', '课件质量高，复习时很有帮助。', 1, 2, '2026-09-26 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 2, 3, 3, '2.8', '内容充实，希望多一些实际案例。', 1, 2, '2026-09-17 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE6002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 2, 2, 4, '3.0', '注重互动，课堂氛围轻松，知识点讲得透彻。', 1, 2, '2026-10-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 4, '4.8', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-10-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 5, 3, '3.8', '整体不错，但进度偏快，需要课后花时间消化。', 1, 2, '2026-09-21 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 2, 4, 4, 4, '3.5', '内容充实，希望多一些实际案例。', 1, 2, '2026-10-04 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 4, 5, '4.5', '老师很负责，课后答疑及时，推荐这门课。', 1, 2, '2026-10-09 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026040101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 4, 5, '4.5', '整体不错，但进度偏快，需要课后花时间消化。', 1, 2, '2026-09-26 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 5, 3, 5, '4.0', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-15 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010202' AND c.course_code = 'GE7001' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 5, '4.8', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-09-15 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 5, 5, '5.0', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-09-23 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026050101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 2, 2, '2.8', '希望增加课堂练习与答疑时间。', 1, 2, '2026-10-02 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 4, 5, '4.8', '课堂节奏把控得好，重点突出，作业量适中。', 1, 2, '2026-09-20 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 2, 3, 2, '2.5', '节奏偏慢，希望增加实践环节。', 1, 2, '2026-09-28 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020201' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 3, '4.2', '希望增加课堂练习与答疑时间。', 1, 2, '2026-09-18 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030102' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 4, 5, '4.5', '讲解由浅入深，案例贴近实际项目，收获很大。', 1, 2, '2026-09-14 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030201' AND c.course_code = 'GE7002' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 3, 4, 3, 3, '3.2', '整体不错，但进度偏快，需要课后花时间消化。', 0, 2, '2026-09-13 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 4, 5, 4, '4.5', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-09-22 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010102' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 5, 5, 5, '4.8', '内容前沿，能看出老师准备很充分。', 1, 2, '2026-09-16 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010201' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 4, 2, '3.5', '讲得清楚，不过考核方式可以再明确一点。', 0, 2, '2026-10-10 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026020101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 5, 5, 3, 5, '4.5', '实验环节设计合理，动手能力提升明显。', 1, 2, '2026-10-07 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026030101' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 3, 5, 5, '4.2', '课件质量高，复习时很有帮助。', 1, 2, '2026-10-05 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010103' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

INSERT INTO course_review (selection_id, student_id, course_id, teacher_id, semester_id, score_content, score_teaching, score_attitude, score_gain, average_score, content, anonymous, status, submit_time)
SELECT cs.id, cs.student_id, cs.course_id, c.teacher_id, cs.semester_id, 4, 4, 4, 4, '4.0', '课程有价值，就是作业有点多。', 0, 2, '2026-10-08 12:00:00'
FROM course_selection cs JOIN student st ON st.id = cs.student_id JOIN course c ON c.id = cs.course_id
WHERE st.stu_no = '2026010104' AND c.course_code = 'GE7003' AND cs.semester_id = @sem
  AND NOT EXISTS (SELECT 1 FROM course_review r WHERE r.selection_id = cs.id);

-- ---------------------------------------------------------------------
-- 5. 公告（补一条置顶已发布、一条草稿、一条已下架，便于演示状态筛选）
-- ---------------------------------------------------------------------
-- 2026-2027学年第一学期期末考试安排（status=1）
INSERT INTO notice (title, content, notice_type, target_role, publisher_id, publisher, top_flag, view_count, status, publish_time)
SELECT '2026-2027学年第一学期期末考试安排', '期末考试将于第 17-18 周进行，具体时间地点请关注后续通知。请同学们提前做好复习准备。', 'EXAM', 'ALL', (SELECT id FROM sys_user WHERE username = 'admin'), '系统管理员', 1, 23, 1, '2026-10-02 22:24:16'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '2026-2027学年第一学期期末考试安排');

-- 关于开展本学期学生评教工作的通知（草稿）（status=0）
INSERT INTO notice (title, content, notice_type, target_role, publisher_id, publisher, top_flag, view_count, status, publish_time)
SELECT '关于开展本学期学生评教工作的通知（草稿）', '请各学院组织学生于第 15 周前完成网上评教，评教结果将作为教师考核参考。', 'SYSTEM', 'STUDENT', (SELECT id FROM sys_user WHERE username = 'admin'), '系统管理员', 0, 21, 0, NULL
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '关于开展本学期学生评教工作的通知（草稿）');

-- 关于选课系统维护的通知（已下架）（status=2）
INSERT INTO notice (title, content, notice_type, target_role, publisher_id, publisher, top_flag, view_count, status, publish_time)
SELECT '关于选课系统维护的通知（已下架）', '选课系统将于本周日凌晨进行维护，维护期间暂停服务。', 'SELECTION', 'ALL', (SELECT id FROM sys_user WHERE username = 'admin'), '系统管理员', 0, 5, 2, '2026-10-02 22:24:16'
WHERE NOT EXISTS (SELECT 1 FROM notice WHERE title = '关于选课系统维护的通知（已下架）');

-- ---------------------------------------------------------------------
-- 6. 操作日志（教学评价 / 成绩 / 选课 相关）
-- ---------------------------------------------------------------------
INSERT INTO sys_log (user_id, username, module, operation, method, request_uri, ip, cost_time, success, create_time)
SELECT 1, 'admin', '教学评价', '公开评价', 'POST', '/api/admin/reviews/1/status', '192.168.2.10', 12, 1, '2026-10-02 09:24:16'
WHERE NOT EXISTS (SELECT 1 FROM sys_log WHERE module = '教学评价' AND operation = '公开评价' AND request_uri = '/api/admin/reviews/1/status');

INSERT INTO sys_log (user_id, username, module, operation, method, request_uri, ip, cost_time, success, create_time)
SELECT 1, 'admin', '教学评价', '隐藏违规评价', 'POST', '/api/admin/reviews/9/status', '192.168.2.10', 18, 1, '2026-10-04 12:24:16'
WHERE NOT EXISTS (SELECT 1 FROM sys_log WHERE module = '教学评价' AND operation = '隐藏违规评价' AND request_uri = '/api/admin/reviews/9/status');

INSERT INTO sys_log (user_id, username, module, operation, method, request_uri, ip, cost_time, success, create_time)
SELECT 1, 'admin', '教学评价', '查询评价统计', 'POST', '/api/admin/reviews/statistics', '192.168.2.10', 35, 1, '2026-10-04 06:24:16'
WHERE NOT EXISTS (SELECT 1 FROM sys_log WHERE module = '教学评价' AND operation = '查询评价统计' AND request_uri = '/api/admin/reviews/statistics');

INSERT INTO sys_log (user_id, username, module, operation, method, request_uri, ip, cost_time, success, create_time)
SELECT 1, 'admin', '成绩管理', '发布课程成绩', 'POST', '/api/teacher/courses/3/grades/publish', '192.168.2.10', 46, 1, '2026-10-02 08:24:16'
WHERE NOT EXISTS (SELECT 1 FROM sys_log WHERE module = '成绩管理' AND operation = '发布课程成绩' AND request_uri = '/api/teacher/courses/3/grades/publish');

INSERT INTO sys_log (user_id, username, module, operation, method, request_uri, ip, cost_time, success, create_time)
SELECT 1, 'admin', '成绩管理', '录入课程成绩', 'POST', '/api/teacher/grades/input', '192.168.2.10', 88, 1, '2026-10-02 19:24:16'
WHERE NOT EXISTS (SELECT 1 FROM sys_log WHERE module = '成绩管理' AND operation = '录入课程成绩' AND request_uri = '/api/teacher/grades/input');

INSERT INTO sys_log (user_id, username, module, operation, method, request_uri, ip, cost_time, success, create_time)
SELECT 1, 'admin', '选课', '学生提交选课', 'POST', '/api/student/courses/2/select', '192.168.2.10', 23, 1, '2026-10-02 07:24:16'
WHERE NOT EXISTS (SELECT 1 FROM sys_log WHERE module = '选课' AND operation = '学生提交选课' AND request_uri = '/api/student/courses/2/select');

INSERT INTO sys_log (user_id, username, module, operation, method, request_uri, ip, cost_time, success, create_time)
SELECT 1, 'admin', '选课', '学生退选', 'POST', '/api/student/selections/9/drop', '192.168.2.10', 31, 1, '2026-10-02 01:24:16'
WHERE NOT EXISTS (SELECT 1 FROM sys_log WHERE module = '选课' AND operation = '学生退选' AND request_uri = '/api/student/selections/9/drop');

-- ---------------------------------------------------------------------
-- 7. 重算学生已获学分（按「已修完且及格」的课程学分汇总，可重复执行）
-- ---------------------------------------------------------------------
UPDATE student s SET s.total_credit = (
  SELECT IFNULL(SUM(CASE WHEN cs.score >= 60 THEN c.credit ELSE 0 END), 0)
  FROM course_selection cs JOIN course c ON c.id = cs.course_id
  WHERE cs.student_id = s.id AND cs.status = 2 AND cs.deleted = 0 AND c.deleted = 0
);

-- ---------------------------------------------------------------------
-- 8. 执行结果自检
-- ---------------------------------------------------------------------
SELECT '当前学期课程数' AS 项目, COUNT(*) AS 数量 FROM course WHERE semester_id = @sem AND deleted = 0
UNION ALL SELECT '当前学期已修完选课', COUNT(*) FROM course_selection WHERE semester_id = @sem AND status = 2 AND deleted = 0
UNION ALL SELECT '当前学期已发布成绩', COUNT(*) FROM course_grade WHERE semester_id = @sem AND status = 1 AND deleted = 0
UNION ALL SELECT '当前学期课程评价', COUNT(*) FROM course_review WHERE semester_id = @sem
UNION ALL SELECT '全库待评价记录', COUNT(*) FROM course_selection cs LEFT JOIN course_review r ON r.selection_id = cs.id WHERE cs.status = 2 AND r.id IS NULL
UNION ALL SELECT '没有任何课程的教师', COUNT(*) FROM teacher t WHERE NOT EXISTS (SELECT 1 FROM course c WHERE c.teacher_id = t.id AND c.deleted = 0);

-- 执行完成后，建议在管理端点击「课程管理 → 重建选课缓存」，让 Redis 余量与库内一致。