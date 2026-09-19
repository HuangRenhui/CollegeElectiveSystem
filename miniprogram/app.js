/**
 * 小程序入口。
 *
 * 登录态校验集中在 onLaunch：已登录直接进入首页，未登录跳转登录页。
 * 各页面无需重复判断，从登录页进入时由 reLaunch 保证页面栈干净。
 */
const config = require('./config/index')

App({
  globalData: {
    /** 当前登录用户信息（含 role、realName、studentId 等） */
    userInfo: null
  },

  onLaunch() {
    const token = wx.getStorageSync(config.storageKeys.token)
    const userInfo = wx.getStorageSync(config.storageKeys.userInfo)

    if (token && userInfo) {
      this.globalData.userInfo = userInfo
    }
  },

  /**
   * 判断是否已登录。
   *
   * 供页面 onShow 调用：未登录时跳转登录页并返回 false。
   */
  isLoggedIn() {
    const token = wx.getStorageSync(config.storageKeys.token)
    if (!token) {
      wx.reLaunch({ url: '/pages/login/index' })
      return false
    }
    return true
  },

  /** 保存登录态 */
  setLoginState(token, userInfo) {
    wx.setStorageSync(config.storageKeys.token, token)
    wx.setStorageSync(config.storageKeys.userInfo, userInfo)
    this.globalData.userInfo = userInfo
  },

  /** 清除登录态 */
  clearLoginState() {
    wx.removeStorageSync(config.storageKeys.token)
    wx.removeStorageSync(config.storageKeys.userInfo)
    this.globalData.userInfo = null
  }
})
