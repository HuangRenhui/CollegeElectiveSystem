/**
 * 小程序全局配置。
 *
 * ⚠️ 部署前必改：
 *   1. baseUrl 改为你的后端地址（真机调试不能写 localhost）；
 *   2. 微信开发者工具需勾选「不校验合法域名」，或在小程序后台配置 request 合法域名。
 */
module.exports = {
  /** 后端接口地址（与 Web 端 .env 的 VITE_API_BASE_URL 指向同一服务） */
  baseUrl: 'http://localhost:8080/api',

  /** 本地缓存键名，统一收口避免各处硬编码不一致 */
  storageKeys: {
    token: 'mp_token',
    userInfo: 'mp_user_info'
  },

  /** 请求超时（毫秒） */
  timeout: 20000
}
