import request from '@/utils/request'

/** 公告列表（按角色过滤） */
export function listNotices(params) {
  return request({ url: '/common/notices', method: 'get', params })
}

/** 公告详情 */
export function getNoticeDetail(id) {
  return request({ url: `/common/notices/${id}`, method: 'get' })
}

/** 学期下拉列表 */
export function listSemesterOptions() {
  return request({ url: '/common/semesters', method: 'get' })
}

/** 当前学期 */
export function getCurrentSemester() {
  return request({ url: '/common/semesters/current', method: 'get' })
}
