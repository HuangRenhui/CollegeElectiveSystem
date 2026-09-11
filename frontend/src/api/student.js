import request from '@/utils/request'

/** 分页查询可选课程（含 Redis 实时余量与学生选课状态） */
export function listCourses(params) {
  return request({ url: '/student/courses', method: 'get', params })
}

/** 课程详情 */
export function getCourseDetail(id) {
  return request({ url: `/student/courses/${id}`, method: 'get' })
}

/** 选课（Redis Lua 原子预占：查重、余量校验、时间冲突检测） */
export function selectCourse(courseId) {
  return request({ url: `/student/courses/${courseId}/select`, method: 'post' })
}

/** 退课 */
export function dropCourse(courseId) {
  return request({ url: `/student/courses/${courseId}/drop`, method: 'post' })
}

/** 选课时间冲突预检 */
export function checkConflict(courseId) {
  return request({ url: `/student/courses/${courseId}/conflict`, method: 'get' })
}

/** 我的选课记录 */
export function listMySelections(params) {
  return request({ url: '/student/selections', method: 'get', params })
}

/** 我的课表 */
export function getMyTimetable(params) {
  return request({ url: '/student/timetable', method: 'get', params })
}

/** 我的成绩单（含总学分、已获学分、平均分与平均绩点） */
export function getMyGradeReport(params) {
  return request({ url: '/student/grades/report', method: 'get', params })
}

/** 我的成绩列表（仅已发布） */
export function listMyGrades(params) {
  return request({ url: '/student/grades', method: 'get', params })
}
