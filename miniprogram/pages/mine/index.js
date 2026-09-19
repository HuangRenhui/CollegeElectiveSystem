const app = getApp()
const authApi = require('../../api/auth')
const config = require('../../config/index')

Page({
  data: {
    userInfo: {},
    /** 评价统计 */
    reviewStats: { pending: 0, reviewed: 0 }
  },

  onShow() {
    if (!app.isLoggedIn()) return
    this.setData({ userInfo: app.globalData.userInfo || {} })
    this.loadUserInfo()
  },

  /** 拉取最新用户信息，避免缓存过期 */
  async loadUserInfo() {
    try {
      const userInfo = await authApi.getUserInfo()
      if (userInfo) {
        this.setData({ userInfo })
        app.globalData.userInfo = userInfo
        wx.setStorageSync(config.storageKeys.userInfo, userInfo)
      }
    } catch (err) {
      // 失败则继续用缓存展示
    }
  },

  goReviewMine() {
    wx.navigateTo({ url: '/pages/review/mine/index' })
  },

  goReviewPending() {
    wx.reLaunch({ url: '/pages/review/list/index' })
  },

  goTimetable() {
    wx.switchTab({ url: '/pages/timetable/index' })
  },

  goGrade() {
    wx.switchTab({ url: '/pages/grade/index' })
  },

  handleLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确定要退出当前账号吗？',
      confirmColor: '#f56c6c',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await authApi.logout()
        } catch (err) {
          // 后端退出失败不影响本地清理
        }
        app.clearLoginState()
        wx.reLaunch({ url: '/pages/login/index' })
      }
    })
  }
})
