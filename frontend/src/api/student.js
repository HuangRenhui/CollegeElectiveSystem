import request from '@/utils/request'

/** 分页查询可选课程（含实时余量与学生选课状态） */
export function listCourses(params) {
  return request({ url: '/student/courses', method: 'get', params })
}

/** 课程详情 */
export function getCourseDetail(id) {
  return request({ url: `/student/courses/${id}`, method: 'get' })
}

/** 选课（含重复选课校验、余量校验与上课时间冲突检测） */
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

// ============================ 课程 / 教师评价 ============================

/** 我的待评价课程（已修完且本学期未评价） */
export function listPendingReviews(params) {
  return request({ url: '/student/reviews/pending', method: 'get', params })
}

/** 提交课程评价（未公开前可修改） */
export function submitReview(data) {
  return request({ url: '/student/reviews', method: 'post', data })
}

/** 我的评价列表 */
export function listMyReviews(params) {
  return request({ url: '/student/reviews', method: 'get', params })
}

/** 查询我对某门课程的评价详情 */
export function getMyReview(courseId) {
  return request({ url: `/student/reviews/${courseId}`, method: 'get' })
}

/** 删除（撤回）我的评价 */
export function deleteMyReview(id) {
  return request({ url: `/student/reviews/${id}`, method: 'delete' })
}

/** 课程评价汇总（平均分、各维度得分、评价人数） */
export function getCourseReviewSummary(courseId) {
  return request({ url: `/student/courses/${courseId}/review-summary`, method: 'get' })
}

/** 课程公开评价列表（匿名） */
export function listCourseReviews(courseId, params) {
  return request({ url: `/student/courses/${courseId}/reviews`, method: 'get', params })
}
