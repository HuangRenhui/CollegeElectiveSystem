const app = getApp()
const authApi = require('../../api/auth')
const config = require('../../config/index')

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onLoad() {
    // 已登录则直接进入首页，避免重复登录
    const token = wx.getStorageSync(config.storageKeys.token)
    if (token) {
      wx.reLaunch({ url: '/pages/review/list/index' })
    }
  },

  onInput(e) {
    const field = e.currentTarget.dataset.field
    this.setData({ [field]: e.detail.value })
  },

  /** 一键填充演示账号（与 Web 端登录页保持一致） */
  fillDemo(e) {
    const role = e.currentTarget.dataset.role
    const accounts = {
      student: { username: '2023010101', password: '123456' },
      teacher: { username: 'T1001', password: '123456' },
      admin: { username: 'admin', password: '123456' }
    }
    const account = accounts[role]
    if (account) {
      this.setData(account)
    }
  },

  async handleLogin() {
    const { username, password, loading } = this.data
    if (loading) return

    if (!username.trim()) {
      wx.showToast({ title: '请输入账号', icon: 'none' })
      return
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }

    this.setData({ loading: true })
    try {
      // 后端 LoginVO 为扁平结构：{ token, tokenType, expiresIn, userId, username, realName, role }
      const data = await authApi.login({
        username: username.trim(),
        password
      })

      // 小程序端当前仅面向学生开放，先校验角色再落缓存
      if (data.role && data.role !== 'STUDENT') {
        wx.showToast({ title: '小程序端目前仅供学生使用', icon: 'none', duration: 2500 })
        return
      }

      app.setLoginState(data.token, {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        role: data.role
      })

      wx.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        wx.reLaunch({ url: '/pages/review/list/index' })
      }, 600)
    } catch (err) {
      // 登录失败必须提示，request 已设为 silent，此处统一处理
      wx.showToast({ title: err.message || '登录失败', icon: 'none', duration: 2000 })
    } finally {
      this.setData({ loading: false })
    }
  }
})
