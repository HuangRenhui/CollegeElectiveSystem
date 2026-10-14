import request from '@/utils/request'

/** 我的站内信列表 */
export function listMyMessages(params) {
  return request({ url: '/message', method: 'get', params })
}

/** 我的未读消息数（导航栏红点） */
export function countMyUnread() {
  return request({ url: '/message/unread-count', method: 'get' })
}

/** 标记单条已读 */
export function markMessageRead(id) {
  return request({ url: `/message/${id}/read`, method: 'post' })
}

/** 全部标记为已读 */
export function markAllMessagesRead() {
  return request({ url: '/message/read-all', method: 'post' })
}
