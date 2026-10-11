const app = getApp()
const authApi = require('../../api/auth')
const wxApi = require('../../api/wx')
const config = require('../../config/index')

/** 小程序端面向的角色 */
const ALLOWED_ROLE = 'STUDENT'

Page({
  data: {
    username: '',
    password: '',
    loading: false,

    // ---------- 微信一键登录 ----------
    /** 后端是否配置了微信能力（未配置时隐藏按钮，避免点了才报错） */
    wxEnabled: false,
    /** 是否处于「首次使用需绑定账号」状态 */
    needBind: false,
    /** 待绑定的 OpenID，由后端下发，前端不可自行构造 */
    pendingOpenId: ''
  },

  async onLoad() {
    // 已登录则直接进入首页，避免重复登录
    const token = wx.getStorageSync(config.storageKeys.token)
    if (token) {
      wx.reLaunch({ url: '/pages/review/list/index' })
      return
    }

    // 探测后端微信能力；失败按不可用处理，不影响密码登录
    const enabled = await wxApi.isWxEnabled()
    this.setData({ wxEnabled: enabled === true })
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

      if (!this.checkRole(data.role)) return
      this.afterLoginSuccess(data)
    } catch (err) {
      // 登录失败必须提示，request 已设为 silent，此处统一处理
      wx.showToast({ title: err.message || '登录失败', icon: 'none', duration: 2000 })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 微信一键登录。
   *
   * 流程：wx.login 取 code → 调后端换取登录态。
   * 已绑定直接登录；未绑定则展示绑定表单，要求输入学号密码。
   *
   * <p>安全要点：未绑定的微信身份<b>不能直接登录</b>——
   * 否则任意微信用户都能进入系统。必须校验系统账号密码后才能建立绑定。</p>
   */
  async handleWxLogin() {
    const { loading } = this.data
    if (loading) return

    this.setData({ loading: true })
    try {
      const code = await this.getWxCode()
      const data = await wxApi.wxLogin(code)

      if (data.bound) {
        // 已绑定：直接登录
        if (!this.checkRole(data.role)) return
        this.afterLoginSuccess(data)
        return
      }

      // 未绑定：引导用户输入学号密码完成绑定
      this.setData({
        needBind: true,
        pendingOpenId: data.openId,
        username: '',
        password: ''
      })
      wx.showToast({ title: '首次使用请先绑定学号', icon: 'none', duration: 2500 })
    } catch (err) {
      wx.showToast({ title: err.message || '微信登录失败', icon: 'none', duration: 2000 })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 完成微信账号绑定。
   *
   * 使用密码登录表单中已输入的账号密码，校验通过后建立绑定关系。
   */
  async handleBind() {
    const { username, password, pendingOpenId, loading } = this.data
    if (loading) return

    if (!username.trim()) {
      wx.showToast({ title: '请输入学号', icon: 'none' })
      return
    }
    if (!password) {
      wx.showToast({ title: '请输入密码', icon: 'none' })
      return
    }
    if (!pendingOpenId) {
      wx.showToast({ title: '绑定信息已失效，请重新发起微信登录', icon: 'none' })
      this.setData({ needBind: false })
      return
    }

    this.setData({ loading: true })
    try {
      const data = await wxApi.bindAccount({
        openId: pendingOpenId,
        username: username.trim(),
        password
      })

      if (!this.checkRole(data.role)) return
      wx.showToast({ title: '绑定成功', icon: 'success' })
      this.afterLoginSuccess(data)
    } catch (err) {
      wx.showToast({ title: err.message || '绑定失败', icon: 'none', duration: 2000 })
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 取消绑定，回到普通登录 */
  cancelBind() {
    this.setData({ needBind: false, pendingOpenId: '' })
  },

  /**
   * 获取微信临时登录凭证。
   *
   * @returns {Promise<string>} code
   */
  getWxCode() {
    return new Promise((resolve, reject) => {
      wx.login({
        success(res) {
          if (res.code) resolve(res.code)
          else reject(new Error('获取微信登录凭证失败'))
        },
        fail() {
          reject(new Error('获取微信登录凭证失败'))
        }
      })
    })
  },

  /**
   * 校验角色。
   *
   * 小程序端当前仅面向学生开放，教师与管理员请使用 Web 端。
   *
   * @returns {boolean} 是否允许继续
   */
  checkRole(role) {
    if (role && role !== ALLOWED_ROLE) {
      wx.showToast({ title: '小程序端目前仅供学生使用', icon: 'none', duration: 2500 })
      return false
    }
    return true
  },

  /** 登录成功后的统一处理：写缓存并跳转首页 */
  afterLoginSuccess(data) {
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
  }
})
