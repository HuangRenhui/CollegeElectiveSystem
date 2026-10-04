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

/** 公告列表（后端按当前登录角色过滤：ALL + 本角色） */
function listNotices(data) {
  return get('/common/notices', data)
}

/** 公告详情（后端会累加浏览量） */
function getNoticeDetail(id) {
  return get('/common/notices/' + id)
}

module.exports = { listSemesterOptions, getCurrentSemester, listNotices, getNoticeDetail }
