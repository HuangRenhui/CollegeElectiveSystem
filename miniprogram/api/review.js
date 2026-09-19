/**
 * 教学评价接口（学生端）。
 *
 * 对应后端 StudentReviewController，共 7 个接口，全部已实现。
 */
const { get, post, del } = require('../utils/request')

/** 待评价课程列表（已修完且尚未评价） */
function listPendingReviews(params) {
  return get('/student/reviews/pending', params)
}

/** 提交课程评价（带 id 表示修改） */
function submitReview(data) {
  return post('/student/reviews', data)
}

/** 我的评价列表 */
function listMyReviews(params) {
  return get('/student/reviews', params)
}

/** 我对某门课程的评价 */
function getMyReview(courseId) {
  return get(`/student/reviews/${courseId}`, {}, { silent: true })
}

/** 撤回我的评价 */
function deleteMyReview(id) {
  return del(`/student/reviews/${id}`)
}

/** 课程评价汇总（平均分、各维度得分、参评人数） */
function getCourseReviewSummary(courseId) {
  return get(`/student/courses/${courseId}/review-summary`)
}

/** 课程公开评价列表（匿名） */
function listCourseReviews(courseId, params) {
  return get(`/student/courses/${courseId}/reviews`, params)
}

module.exports = {
  listPendingReviews,
  submitReview,
  listMyReviews,
  getMyReview,
  deleteMyReview,
  getCourseReviewSummary,
  listCourseReviews
}
