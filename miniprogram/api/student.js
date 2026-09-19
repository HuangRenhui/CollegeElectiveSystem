/**
 * 学生端接口：课表与成绩。
 */
const { get } = require('../utils/request')

/** 我的课表 */
function getMyTimetable(params) {
  return get('/student/timetable', params)
}

/** 我的选课记录 */
function listMySelections(params) {
  return get('/student/selections', params)
}

/** 我的成绩单（含总学分、已获学分、平均分与平均绩点） */
function getMyGradeReport(params) {
  return get('/student/grades/report', params)
}

/** 我的成绩列表（仅已发布） */
function listMyGrades(params) {
  return get('/student/grades', params)
}

module.exports = { getMyTimetable, listMySelections, getMyGradeReport, listMyGrades }
