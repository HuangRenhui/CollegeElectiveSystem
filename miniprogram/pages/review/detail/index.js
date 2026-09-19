const app = getApp()
const reviewApi = require('../../../api/review')
const dict = require('../../../utils/dict')

Page({
  data: {
    courseId: null,
    courseName: '',
    loading: true,
    summary: null,
    /** 各维度得分（含百分比进度条宽度） */
    dimensions: [],
    /** 参评率 */
    participateRate: 0,
    reviews: [],
    pageNum: 1,
    pageSize: 10,
    hasMore: true,
    loadingMore: false
  },

  onLoad(options) {
    if (!app.isLoggedIn()) return
    this.setData({
      courseId: Number(options.courseId),
      courseName: decodeURIComponent(options.courseName || '')
    })
    this.loadSummary()
    this.loadReviews()
  },

  onPullDownRefresh() {
    this.setData({ pageNum: 1, reviews: [], hasMore: true })
    Promise.all([this.loadSummary(), this.loadReviews()])
      .finally(() => wx.stopPullDownRefresh())
  },

  /** 触底加载更多评价 */
  onReachBottom() {
    if (!this.data.hasMore || this.data.loadingMore) return
    this.setData({ pageNum: this.data.pageNum + 1 })
    this.loadReviews()
  },

  async loadSummary() {
    this.setData({ loading: true })
    try {
      const summary = await reviewApi.getCourseReviewSummary(this.data.courseId)
      if (!summary) return

      const dimensions = dict.REVIEW_DIMENSIONS.map((dim) => {
        const score = Number(summary[dim.key]) || 0
        return {
          key: dim.key,
          label: dim.label,
          score,
          /** 进度条宽度：满分 5 分换算成百分比 */
          percent: Math.round((score / 5) * 100)
        }
      })

      const studentCount = Number(summary.studentCount) || 0
      const reviewCount = Number(summary.reviewCount) || 0

      this.setData({
        summary,
        dimensions,
        courseName: summary.courseName || this.data.courseName,
        participateRate: studentCount
          ? Math.round((reviewCount / studentCount) * 100)
          : 0
      })
    } catch (err) {
      // 错误已由 request 统一 toast
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadReviews() {
    const { courseId, pageNum, pageSize } = this.data
    this.setData({ loadingMore: true })
    try {
      const result = await reviewApi.listCourseReviews(courseId, { pageNum, pageSize })
      const records = (result && result.records) || []
      // 匿名评价统一展示为「匿名同学」
      const formatted = records.map((item) => ({
        ...item,
        displayName: item.anonymous === 1
          ? '匿名同学'
          : dict.maskName(item.studentName),
        averageScore: dict.averageScore(item)
      }))

      this.setData({
        reviews: pageNum === 1 ? formatted : this.data.reviews.concat(formatted),
        hasMore: records.length >= pageSize
      })
    } catch (err) {
      this.setData({ hasMore: false })
    } finally {
      this.setData({ loadingMore: false })
    }
  }
})
