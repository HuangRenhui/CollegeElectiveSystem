/**
 * 认证接口。
 *
 * 与 Web 端共用同一套后端账号体系，无需为小程序单独开设接口。
 */
const { get, post, put } = require('../utils/request')

/** 登录，返回 { token, userInfo } 等 */
function login(data) {
  return post('/auth/login', data, { silent: true })
}

/** 退出登录 */
function logout() {
  return post('/auth/logout')
}

/** 当前登录用户信息 */
function getUserInfo() {
  return get('/auth/info')
}

/** 修改密码 */
function changePassword(data) {
  return put('/auth/password', data)
}

module.exports = { login, logout, getUserInfo, changePassword }
