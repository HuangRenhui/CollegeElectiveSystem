import request from '@/utils/request'

// ============================ 课程管理 ============================

export function pageCourses(params) {
  return request({ url: '/admin/courses', method: 'get', params })
}

export function getCourseDetail(id) {
  return request({ url: `/admin/courses/${id}`, method: 'get' })
}

export function createCourse(data) {
  return request({ url: '/admin/courses', method: 'post', data })
}

export function updateCourse(data) {
  return request({ url: '/admin/courses', method: 'put', data })
}

export function deleteCourse(id) {
  return request({ url: `/admin/courses/${id}`, method: 'delete' })
}

export function updateCourseStatus(id, status) {
  return request({ url: `/admin/courses/${id}/status/${status}`, method: 'put' })
}

export function preloadCourseCache(semesterId) {
  return request({ url: '/admin/courses/cache/preload', method: 'post', params: { semesterId } })
}

export function syncSelectionCount() {
  return request({ url: '/admin/courses/selection/sync', method: 'post' })
}

export function getStatistics(params) {
  return request({ url: '/admin/courses/statistics', method: 'get', params })
}

// ============================ 学生管理 ============================

export function pageStudents(params) {
  return request({ url: '/admin/students', method: 'get', params })
}

export function getStudentDetail(id) {
  return request({ url: `/admin/students/${id}`, method: 'get' })
}

export function createStudent(data) {
  return request({ url: '/admin/students', method: 'post', data })
}

export function updateStudent(data) {
  return request({ url: '/admin/students', method: 'put', data })
}

export function deleteStudent(id) {
  return request({ url: `/admin/students/${id}`, method: 'delete' })
}

export function updateStudentStatus(id, status) {
  return request({ url: `/admin/students/${id}/status/${status}`, method: 'put' })
}

// ============================ 教师管理 ============================

export function pageTeachers(params) {
  return request({ url: '/admin/teachers', method: 'get', params })
}

export function listTeacherOptions(params) {
  return request({ url: '/admin/teachers/options', method: 'get', params })
}

export function getTeacherDetail(id) {
  return request({ url: `/admin/teachers/${id}`, method: 'get' })
}

export function createTeacher(data) {
  return request({ url: '/admin/teachers', method: 'post', data })
}

export function updateTeacher(data) {
  return request({ url: '/admin/teachers', method: 'put', data })
}

export function deleteTeacher(id) {
  return request({ url: `/admin/teachers/${id}`, method: 'delete' })
}

export function updateTeacherStatus(id, status) {
  return request({ url: `/admin/teachers/${id}/status/${status}`, method: 'put' })
}

// ============================ 院系 / 专业 / 教室 ============================

export function listDepartments(params) {
  return request({ url: '/admin/departments', method: 'get', params })
}

export function saveDepartment(data) {
  return request({ url: '/admin/departments', method: 'post', data })
}

export function deleteDepartment(id) {
  return request({ url: `/admin/departments/${id}`, method: 'delete' })
}

export function listMajors(params) {
  return request({ url: '/admin/majors', method: 'get', params })
}

export function saveMajor(data) {
  return request({ url: '/admin/majors', method: 'post', data })
}

export function deleteMajor(id) {
  return request({ url: `/admin/majors/${id}`, method: 'delete' })
}

export function listClassrooms(params) {
  return request({ url: '/admin/classrooms', method: 'get', params })
}

export function saveClassroom(data) {
  return request({ url: '/admin/classrooms', method: 'post', data })
}

export function deleteClassroom(id) {
  return request({ url: `/admin/classrooms/${id}`, method: 'delete' })
}

// ============================ 学期管理 ============================

export function pageSemesters(params) {
  return request({ url: '/admin/semesters', method: 'get', params })
}

export function listSemesterOptions() {
  return request({ url: '/admin/semesters/options', method: 'get' })
}

export function getCurrentSemester() {
  return request({ url: '/admin/semesters/current', method: 'get' })
}

export function saveSemester(data) {
  return request({ url: '/admin/semesters', method: 'post', data })
}

export function setCurrentSemester(id) {
  return request({ url: `/admin/semesters/${id}/current`, method: 'put' })
}

export function deleteSemester(id) {
  return request({ url: `/admin/semesters/${id}`, method: 'delete' })
}

// ============================ 公告管理 ============================

export function pageNotices(params) {
  return request({ url: '/admin/notices', method: 'get', params })
}

export function saveNotice(data) {
  return request({ url: '/admin/notices', method: 'post', data })
}

export function updateNoticeStatus(id, status) {
  return request({ url: `/admin/notices/${id}/status/${status}`, method: 'put' })
}

export function deleteNotice(id) {
  return request({ url: `/admin/notices/${id}`, method: 'delete' })
}

// ============================ 系统管理 ============================

export function pageLogs(params) {
  return request({ url: '/admin/system/logs', method: 'get', params })
}

export function cleanLogs(days = 90) {
  return request({ url: '/admin/system/logs', method: 'delete', params: { days } })
}

export function resetUserPassword(userId, newPassword = '123456') {
  return request({ url: `/admin/system/users/${userId}/password`, method: 'put', params: { newPassword } })
}

// ============================ 教务代选（管理员） ============================

/** 查询指定学生的选课记录（供代选弹窗展示与退选操作） */
export function listStudentSelections(studentId, params) {
  return request({ url: `/admin/students/${studentId}/selections`, method: 'get', params })
}

/** 教务代选：为学生添加课程（selectType = 2） */
export function addSelectionForStudent(studentId, courseId, reason) {
  return request({
    url: `/admin/students/${studentId}/selections`,
    method: 'post',
    params: { courseId, reason }
  })
}

/** 教务代退选：撤销学生的选课记录（已录入成绩的不可退） */
export function removeSelectionForStudent(studentId, courseId, reason) {
  return request({
    url: `/admin/students/${studentId}/selections/${courseId}`,
    method: 'delete',
    params: { reason }
  })
}

// ============================ 评价管理（管理员） ============================

/** 分页查询课程评价（支持学期 / 课程 / 状态筛选） */
export function pageReviews(params) {
  return request({ url: '/admin/reviews', method: 'get', params })
}

/** 变更评价公开状态（1-已公开 3-已隐藏） */
export function updateReviewStatus(id, status, reason) {
  return request({
    url: `/admin/reviews/${id}/status/${status}`,
    method: 'put',
    params: { reason }
  })
}

/** 删除违规评价 */
export function deleteReview(id, reason) {
  return request({ url: `/admin/reviews/${id}`, method: 'delete', params: { reason } })
}

/** 全校课程评价统计（平均分排行、参评率、状态分布） */
export function getReviewStatistics(params) {
  return request({ url: '/admin/reviews/statistics', method: 'get', params })
}

export function switchSelection(state) {
  return request({ url: `/admin/system/selection-switch/${state}`, method: 'put' })
}

export function getSelectionSwitch() {
  return request({ url: '/admin/system/selection-switch', method: 'get' })
}
