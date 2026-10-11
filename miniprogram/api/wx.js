/**
 * 微信小程序专用接口。
 *
 * 包含微信一键登录与订阅消息授权两组能力。
 * 未配置 AppID/AppSecret 时后端会返回 enabled=false，
 * 前端据此隐藏入口，避免用户点了才报错。
 */
const { get, post } = require('../utils/request')

/**
 * 查询微信登录能力是否可用。
 *
 * 该接口不返回业务错误，失败时按「不可用」处理，
 * 保证登录页在微信配置缺失时仍可正常使用密码登录。
 */
function isWxEnabled() {
  return get('/wx/enabled', {}, { silent: true }).catch(() => false)
}

/**
 * 微信一键登录。
 *
 * @param {string} code wx.login() 得到的临时登录凭证
 * @returns {Promise<object>} bound=true 时携带 token；false 时携带 openId
 */
function wxLogin(code) {
  return post('/wx/login', { code }, { silent: true })
}

/**
 * 绑定系统账号（首次使用）。
 *
 * @param {object} data { openId, username, password }
 */
function bindAccount(data) {
  return post('/wx/bind', data)
}

/** 已配置模板的订阅场景列表 */
function listTemplates() {
  return get('/wx/subscribe/templates', {}, { silent: true }).catch(() => [])
}

/**
 * 上报订阅授权（用户同意后调用）。
 *
 * @param {string} scene REVIEW_OPEN 或 GRADE_PUBLISH
 */
function reportSubscribe(scene) {
  return post('/wx/subscribe', { scene })
}

/** 查询某场景的剩余订阅次数，归零时需重新引导授权 */
function getRemainCount(scene) {
  return get(`/wx/subscribe/${scene}/remain`, {}, { silent: true }).catch(() => 0)
}

module.exports = {
  isWxEnabled,
  wxLogin,
  bindAccount,
  listTemplates,
  reportSubscribe,
  getRemainCount
}
