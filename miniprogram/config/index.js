/**
 * 小程序全局配置。
 *
 * ⚠️ 部署前必改：
 *   1. 把 ENV 改成 'prod'，并在 ENDPOINTS.prod 填入正式域名（真机调试不能写 localhost）；
 *   2. 微信开发者工具需勾选「不校验合法域名」，或在小程序后台配置 request 合法域名。
 */
const ENV = 'dev'

/**
 * 各环境的接口地址。
 *
 * 单独维护一份环境映射，而不是在代码里散落硬编码地址：
 * 切环境只改 ENV 一处，避免出现「部分接口指向测试机、部分指向正式机」的错配。
 */
const ENDPOINTS = {
  dev: 'http://localhost:8080/api',
  // TODO 部署前替换为正式域名（必须为 https，且已加入小程序后台的 request 合法域名）
  prod: 'https://your-domain.example.com/api'
}

module.exports = {
  /** 当前环境：dev / prod */
  env: ENV,

  /** 后端接口地址（与 Web 端 .env 的 VITE_API_BASE_URL 指向同一服务） */
  baseUrl: ENDPOINTS[ENV] || ENDPOINTS.dev,

  /** 本地缓存键名，统一收口避免各处硬编码不一致 */
  storageKeys: {
    token: 'mp_token',
    userInfo: 'mp_user_info'
  },

  /** 请求超时（毫秒） */
  timeout: 20000
}
