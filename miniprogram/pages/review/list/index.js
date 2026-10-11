const app = getApp()
const reviewApi = require('../../../api/review')
const commonApi = require('../../../api/common')
const share = require('../../../utils/share')

Page({
  data: {
    loading: true,
    semesterId: null,
    semesters: [],
    semesterIndex: 0,
    /** 待评价课程 */
    pendingList: [],
    /** 已提交评价数量，用于展示「已评 X / 共 Y」 */
    reviewedCount: 0
  },

  onShow() {
    if (!app.isLoggedIn()) return
    // 学期列表只在首次进入时加载，避免用户切换学期后返回本页被重置成当前学期
    if (!this.data.semesters.length) {
      this.loadSemesters().then(() => this.loadAll())
    } else {
      this.loadAll()
    }
  },

  onPullDownRefresh() {
    this.loadAll().finally(() => wx.stopPullDownRefresh())
  },

  /** 加载学期下拉，默认选中当前学期 */
  async loadSemesters() {
    try {
      const semesters = (await commonApi.listSemesterOptions()) || []
      let semesterIndex = 0
      const currentIndex = semesters.findIndex((item) => item.isCurrent === 1)
      if (currentIndex > -1) {
        semesterIndex = currentIndex
      }
      this.setData({
        semesters,
        semesterIndex,
        semesterId: semesters.length ? semesters[semesterIndex].id : null
      })
    } catch (err) {
      this.setData({ semesters: [] })
    }
  },

  async loadAll() {
    this.setData({ loading: true })
    try {
      await Promise.all([this.loadPending(), this.loadReviewedCount()])
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 加载待评价课程 */
  async loadPending() {
    const params = this.data.semesterId ? { semesterId: this.data.semesterId } : {}
    try {
      const list = (await reviewApi.listPendingReviews(params)) || []
      this.setData({ pendingList: list })
    } catch (err) {
      this.setData({ pendingList: [] })
    }
  },

  /** 统计已评价数量，用于顶部进度展示 */
  async loadReviewedCount() {
    const params = this.data.semesterId ? { semesterId: this.data.semesterId } : {}
    try {
      const list = (await reviewApi.listMyReviews(params)) || []
      this.setData({ reviewedCount: list.length })
    } catch (err) {
      this.setData({ reviewedCount: 0 })
    }
  },

  onSemesterChange(e) {
    const index = Number(e.detail.value)
    const semester = this.data.semesters[index]
    this.setData({
      semesterIndex: index,
      semesterId: semester ? semester.id : null
    })
    this.loadAll()
  },

  /** 去评价 */
  goForm(e) {
    const { courseId, courseName } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/review/form/index?courseId=${courseId}&courseName=${encodeURIComponent(courseName || '')}`
    })
  },

  /** 查看课程评价汇总 */
  goDetail(e) {
    const { courseId, courseName } = e.currentTarget.dataset
    wx.navigateTo({
      url: `/pages/review/detail/index?courseId=${courseId}&courseName=${encodeURIComponent(courseName || '')}`
    })
  },

  goMine() {
    wx.navigateTo({ url: '/pages/review/mine/index' })
  },

  /** 查看我的评价趋势（雷达图 + 学期折线） */
  goTrend() {
    wx.navigateTo({ url: '/pages/review/trend/index' })
  },

  // ---------------- 分享 ----------------

  /** 分享给好友 */
  onShareAppMessage() {
    return share.shareReview()
  },

  /** 分享到朋友圈（不支持 path 带参数） */
  onShareTimeline() {
    return {
      title: '一起来评教',
      query: ''
    }
  }
})
