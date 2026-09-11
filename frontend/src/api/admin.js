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

export function switchSelection(state) {
  return request({ url: `/admin/system/selection-switch/${state}`, method: 'put' })
}

export function getSelectionSwitch() {
  return request({ url: '/admin/system/selection-switch', method: 'get' })
}
