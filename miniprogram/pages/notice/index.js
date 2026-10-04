const app = getApp()
const commonApi = require('../../api/common')
const dict = require('../../utils/dict')
const config = require('../../config/index')

/** 已读记录缓存键前缀（按用户隔离，换账号互不影响） */
const READ_KEY_PREFIX = 'mp_notice_read_'

Page({
  data: {
    loading: true,
    /** 渲染用列表（受「仅看未读」筛选影响） */
    notices: [],
    /** 未读数（始终基于全量列表统计） */
    unreadCount: 0,
    /** 是否只看未读 */
    onlyUnread: false,
    /** 当前展开的公告 ID */
    expandedId: null
  },

  onShow() {
    if (!app.isLoggedIn()) return
    this.loadList()
  },

  onPullDownRefresh() {
    this.loadList().finally(() => wx.stopPullDownRefresh())
  },

  /** 已读记录归属的用户标识 */
  readOwner() {
    const userInfo =
      app.globalData.userInfo || wx.getStorageSync(config.storageKeys.userInfo) || {}
    return String(userInfo.userId || userInfo.username || '')
  },

  /** 读取当前用户的已读公告 ID */
  readIds() {
    const owner = this.readOwner()
    if (!owner) return []
    const cached = wx.getStorageSync(READ_KEY_PREFIX + owner)
    return Array.isArray(cached) ? cached : []
  },

  /** 保存已读公告 ID */
  saveReadIds(ids) {
    const owner = this.readOwner()
    if (!owner) return
    wx.setStorageSync(READ_KEY_PREFIX + owner, ids)
  },

  async loadList() {
    this.setData({ loading: true })
    try {
      // limit 取后端上限（50），拿到当前角色可见的全部公告
      const list = (await commonApi.listNotices({ limit: 50 })) || []
      this.rawList = list
      this.render()
    } catch (err) {
      this.rawList = []
      this.render()
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 依据已读状态与筛选条件重新渲染列表 */
  render() {
    const readIds = this.readIds()
    const raw = this.rawList || []

    const notices = raw
      .map((item) => ({
        id: item.id,
        title: item.title,
        content: item.content || '（无正文）',
        publisher: item.publisher || '系统',
        publishTime: formatTime(item.publishTime),
        viewCount: item.viewCount || 0,
        topFlag: item.topFlag === 1,
        typeText: dict.noticeTypeText(item.noticeType),
        typeTheme: dict.noticeTypeTheme(item.noticeType),
        read: readIds.indexOf(item.id) > -1
      }))
      .filter((item) => (this.data.onlyUnread ? !item.read : true))

    this.setData({
      notices,
      unreadCount: raw.filter((item) => readIds.indexOf(item.id) === -1).length
    })
  },

  /**
   * 点击公告：标记已读 + 展开/收起正文。
   * 展开时再拉一次详情，后端会累加浏览量。
   */
  async handleTap(e) {
    const id = e.currentTarget.dataset.id
    const expanding = this.data.expandedId !== id

    this.markRead(id)
    this.setData({ expandedId: expanding ? id : null })
    this.render()

    if (!expanding) return

    try {
      const detail = await commonApi.getNoticeDetail(id)
      if (detail) {
        this.rawList = (this.rawList || []).map((item) =>
          item.id === id ? Object.assign({}, item, detail) : item
        )
        this.render()
      }
    } catch (err) {
      // 详情请求失败时保留列表里已有的正文
    }
  },

  /** 标记单条已读 */
  markRead(id) {
    const readIds = this.readIds()
    if (readIds.indexOf(id) > -1) return
    readIds.push(id)
    this.saveReadIds(readIds)
  },

  /** 把当前可见公告全部标记为已读 */
  markAllRead() {
    const raw = this.rawList || []
    if (!raw.length) return

    const readIds = this.readIds()
    raw.forEach((item) => {
      if (readIds.indexOf(item.id) === -1) {
        readIds.push(item.id)
      }
    })
    this.saveReadIds(readIds)
    this.render()
    wx.showToast({ title: '已全部标为已读', icon: 'none' })
  },

  /** 切换「全部 / 仅未读」 */
  toggleFilter() {
    this.setData({ onlyUnread: !this.data.onlyUnread })
    this.render()
  }
})

/** 2026-08-15T09:00:00 → 2026-08-15 09:00 */
function formatTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}
