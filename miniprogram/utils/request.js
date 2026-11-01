/**
 * 网络请求封装。
 *
 * 与 Web 端 utils/request.js 保持同一套语义：
 *   - 自动携带 Authorization: Bearer {token}
 *   - code === 200 视为成功，直接返回 data
 *   - 401 / 2003 视为登录失效，清理登录态并跳转登录页
 *   - 其余业务错误统一 toast 后 reject
 */
const config = require('../config/index')

/** 登录失效处理中标记，避免并发请求重复跳转登录页 */
let isHandlingUnauthorized = false

/**
 * 发起请求。
 *
 * @param {object} options 请求配置
 * @param {string} options.url 接口路径（不含 baseUrl）
 * @param {string} [options.method] 请求方法，默认 GET
 * @param {object} [options.data] 请求参数
 * @param {boolean} [options.silent] 是否静默处理错误（不弹 toast），默认 false
 * @returns {Promise<any>} 成功时 resolve 业务数据（Result.data）
 */
function request(options) {
  const { url, method = 'GET', data = {}, silent = false } = options

  return new Promise((resolve, reject) => {
    // 已在处理登录失效：直接拒绝，不再发请求。
    // 否则跳转登录页的等待窗口里，后续请求仍会打到后端并各自弹出提示
    if (isHandlingUnauthorized) {
      reject(new Error('登录状态已失效'))
      return
    }

    const token = wx.getStorageSync(config.storageKeys.token)
    const header = { 'Content-Type': 'application/json;charset=UTF-8' }
    if (token) {
      header.Authorization = `Bearer ${token}`
    }

    wx.request({
      url: config.baseUrl + url,
      method,
      data,
      header,
      timeout: config.timeout,
      success(res) {
        const body = res.data

        // HTTP 层未授权（拦截器/网关直接返回 401）
        if (res.statusCode === 401) {
          handleUnauthorized()
          reject(new Error('登录状态已失效'))
          return
        }

        if (res.statusCode === 403) {
          // 后端 403 可能来自权限拦截（无 message）或业务码 2007（带更具体的说明），
          // 有就用后端文案，避免把所有越权都提示成同一句笼统的话
          const message = (body && body.message) || '没有操作权限'
          showError(silent, message)
          reject(new Error(message))
          return
        }

        if (res.statusCode !== 200) {
          // 后端按语义返回真实状态码，响应体里带有可读提示（限流、参数错误、系统异常等），
          // 优先透传，避免统一显示成「请求失败（400）」这种无信息量的文案
          const message = (body && body.message) ||
            (res.statusCode === 429 ? '操作过于频繁，请稍后再试' : `请求失败（${res.statusCode}）`)
          showError(silent, message)
          reject(new Error(message))
          return
        }

        // 非标准响应体，直接返回
        if (!body || typeof body !== 'object') {
          resolve(body)
          return
        }

        if (body.code === 200) {
          resolve(body.data)
          return
        }

        // 业务层的登录失效：401 或自定义 2003
        if (body.code === 401 || body.code === 2003) {
          handleUnauthorized()
          reject(new Error(body.message || '登录状态已失效'))
          return
        }

        const message = body.message || '请求失败'
        showError(silent, message)
        reject(new Error(message))
      },
      fail(err) {
        const message = err.errMsg && err.errMsg.indexOf('timeout') > -1
          ? '请求超时，请稍后重试'
          : '网络异常，请检查网络连接'
        showError(silent, message)
        reject(new Error(message))
      }
    })
  })
}

/** 统一错误提示 */
function showError(silent, message) {
  if (silent) return
  wx.showToast({ title: message, icon: 'none', duration: 2000 })
}

/**
 * 登录失效：清理缓存并跳转登录页。
 *
 * 用标记位防止多个并发请求同时触发跳转，造成页面栈异常。
 */
function handleUnauthorized() {
  if (isHandlingUnauthorized) return
  isHandlingUnauthorized = true

  wx.removeStorageSync(config.storageKeys.token)
  wx.removeStorageSync(config.storageKeys.userInfo)

  wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' })

  // 标记位在 reLaunch 完成前保持为 true（request 入口会直接拒绝），
  // 因此这里只需留出极短的提示时间，原 800ms 会让后续请求继续下发
  setTimeout(() => {
    wx.reLaunch({
      url: '/pages/login/index',
      complete() {
        isHandlingUnauthorized = false
      }
    })
  }, 300)
}

// ------------------------------ 语法糖 ------------------------------

function get(url, data, options) {
  return request({ url, method: 'GET', data, ...options })
}

function post(url, data, options) {
  return request({ url, method: 'POST', data, ...options })
}

function put(url, data, options) {
  return request({ url, method: 'PUT', data, ...options })
}

function del(url, data, options) {
  return request({ url, method: 'DELETE', data, ...options })
}

module.exports = { request, get, post, put, del }
