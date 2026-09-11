import request from '@/utils/request'

/** 登录 */
export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

/** 退出登录 */
export function logout() {
  return request({ url: '/auth/logout', method: 'post' })
}

/** 获取当前用户信息 */
export function getUserInfo() {
  return request({ url: '/auth/info', method: 'get' })
}

/** 修改密码 */
export function changePassword(data) {
  return request({ url: '/auth/password', method: 'put', data })
}

/** 修改个人资料 */
export function updateProfile(data) {
  return request({ url: '/auth/profile', method: 'put', data })
}
