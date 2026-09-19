/**
 * 公共接口：学期、公告。
 */
const { get } = require('../utils/request')

/** 学期下拉列表 */
function listSemesterOptions() {
  return get('/common/semesters')
}

/** 当前学期 */
function getCurrentSemester() {
  return get('/common/semesters/current')
}

/** 公告列表 */
function listNotices(data) {
  return get('/common/notices', data)
}

module.exports = { listSemesterOptions, getCurrentSemester, listNotices }
