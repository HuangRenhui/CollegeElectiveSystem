const app = getApp()
const reviewApi = require('../../../api/review')
const dict = require('../../../utils/dict')

Page({
  data: {
    loading: true,
    list: []
  },

  onShow() {
    if (!app.isLoggedIn()) return
    this.loadList()
  },

  onPullDownRefresh() {
    this.loadList().finally(() => wx.stopPullDownRefresh())
  },

  async loadList() {
    this.setData({ loading: true })
    try {
      const list = (await reviewApi.listMyReviews()) || []
      // 附加展示字段：综合评分、状态文案
      const formatted = list.map((item) => ({
        ...item,
        averageScore: dict.averageScore(item),
        statusText: dict.reviewStatusText(item.status),
        statusTheme: dict.reviewStatusTheme(item.status),
        /** 已隐藏的评价不允许修改与撤回 */
        locked: item.status === 3
      }))
      this.setData({ list: formatted })
    } catch (err) {
      this.setData({ list: [] })
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 修改评价 */
  goEdit(e) {
    const { courseId, courseName, locked } = e.currentTarget.dataset
    if (locked) {
      wx.showToast({ title: '该评价已被隐藏，无法修改', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: `/pages/review/form/index?courseId=${courseId}&courseName=${encodeURIComponent(courseName || '')}`
    })
  },

  /** 撤回评价 */
  handleDelete(e) {
    const { id, courseName } = e.currentTarget.dataset

    wx.showModal({
      title: '撤回确认',
      content: `确定撤回对《${courseName}》的评价吗？撤回后需重新填写。`,
      confirmText: '确定撤回',
      confirmColor: '#f56c6c',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await reviewApi.deleteMyReview(id)
          wx.showToast({ title: '已撤回', icon: 'success' })
          this.loadList()
        } catch (err) {
          // 错误已由 request 统一 toast
        }
      }
    })
  },

  goPending() {
    wx.reLaunch({ url: '/pages/review/list/index' })
  }
})
