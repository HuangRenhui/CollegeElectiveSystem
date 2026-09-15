import request from '@/utils/request'

/** 我的授课课程 */
export function listMyCourses(params) {
  return request({ url: '/teacher/courses', method: 'get', params })
}

/** 课程学生名单（分页） */
export function listCourseStudents(courseId, params) {
  return request({ url: `/teacher/courses/${courseId}/students`, method: 'get', params })
}

/** 课程选课记录列表 */
export function listCourseSelections(courseId) {
  return request({ url: `/teacher/courses/${courseId}/selections`, method: 'get' })
}

/** 成绩录入单（返回该课程全部学生的当前成绩状态） */
export function getGradeSheet(courseId) {
  return request({ url: `/teacher/courses/${courseId}/grade-sheet`, method: 'get' })
}

/** 批量录入成绩 */
export function inputGrades(data) {
  return request({ url: '/teacher/grades/input', method: 'post', data })
}

/** 发布课程成绩 */
export function publishGrades(courseId) {
  return request({ url: `/teacher/courses/${courseId}/grades/publish`, method: 'post' })
}

/** 撤回成绩发布 */
export function revokeGrades(courseId) {
  return request({ url: `/teacher/courses/${courseId}/grades/revoke`, method: 'post' })
}

/** 教师课表 */
export function getMyTimetable(params) {
  return request({ url: '/teacher/timetable', method: 'get', params })
}
