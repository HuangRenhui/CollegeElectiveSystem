const app = getApp()
const reviewApi = require('../../../api/review')
const dict = require('../../../utils/dict')

/** 构造全空的五颗星，作为 dimensions 初始值 */
function buildDimensions() {
  return dict.REVIEW_DIMENSIONS.map((dim) => ({
    ...dim,
    score: 0,
    stars: [0, 1, 2, 3, 4].map((index) => ({ index, state: 'empty' }))
  }))
}

Page({
  data: {
    courseId: null,
    courseName: '',
    courseCode: '',
    teacherName: '',
    /** 编辑已有评价时携带 */
    reviewId: null,
    /** 被管理员隐藏时禁止修改 */
    hidden: false,

    dimensions: buildDimensions(),
    quickTags: dict.REVIEW_QUICK_TAGS,
    /** 已选中的快捷标签 */
    selectedTags: [],

    scoreContent: 0,
    scoreTeaching: 0,
    scoreAttitude: 0,
    scoreGain: 0,
    content: '',
    anonymous: true,

    overallScore: 0,
    submitting: false,
    loading: true,
    maxLength: 500
  },

  onLoad(options) {
    if (!app.isLoggedIn()) return
    const courseId = Number(options.courseId)
    this.setData({
      courseId,
      courseName: decodeURIComponent(options.courseName || '')
    })
    this.loadExistingReview()
  },

  /**
   * 载入已有评价。
   *
   * 支持两种入口：从待评价列表进入（无评价，表单为空），
   * 以及从「我的评价」点修改进入（带出原值）。
   */
  async loadExistingReview() {
    try {
      const review = await reviewApi.getMyReview(this.data.courseId)
      if (review) {
        const selectedTags = this.data.quickTags.filter(
          (tag) => review.content && review.content.indexOf(tag) > -1
        )
        this.setData({
          reviewId: review.id,
          courseCode: review.courseCode || '',
          teacherName: review.teacherName || '',
          scoreContent: Number(review.scoreContent) || 0,
          scoreTeaching: Number(review.scoreTeaching) || 0,
          scoreAttitude: Number(review.scoreAttitude) || 0,
          scoreGain: Number(review.scoreGain) || 0,
          content: review.content || '',
          anonymous: review.anonymous !== 0,
          hidden: review.status === 3,
          selectedTags
        })
      }
    } catch (err) {
      // 未评价过属正常情况，静默忽略
    } finally {
      // 无论是否已评价，都要刷新一次星星状态（未评价时为全空）
      this.calcOverall()
      this.setData({ loading: false })
    }
  },

  /**
   * 点击星级打分。
   *
   * 支持半星：点击星星左半边为 .5 分，右半边为整分。
   * 用 touch 位置计算，比引入第三方评分组件更轻。
   */
  onRate(e) {
    const { key, index } = e.currentTarget.dataset
    const starIndex = Number(index)

    wx.createSelectorQuery()
      .in(this)
      .select(`#rate-${key}-${starIndex}`)
      .boundingClientRect((rect) => {
        if (!rect) return
        const touchX = e.detail.x !== undefined ? e.detail.x : e.changedTouches[0].pageX
        const isLeftHalf = touchX - rect.left < rect.width / 2
        const score = starIndex + (isLeftHalf ? 0.5 : 1)
        this.setData({ [key]: score })
        this.calcOverall()
      })
      .exec()
  },

  /**
   * 计算综合评分与各维度星星展示状态。
   *
   * 星星状态在 JS 中预计算，避免 WXML 内联复杂三元表达式——
   * WXML 的表达式能力有限且可读性差，预计算后模板更清晰。
   */
  calcOverall() {
    const { scoreContent, scoreTeaching, scoreAttitude, scoreGain } = this.data

    const dimensions = dict.REVIEW_DIMENSIONS.map((dim) => {
      const score = Number(this.data[dim.key]) || 0
      const stars = [0, 1, 2, 3, 4].map((index) => {
        let state = 'empty'
        if (score >= index + 1) {
          state = 'full'
        } else if (score >= index + 0.5) {
          state = 'half'
        }
        return { index, state }
      })
      return { ...dim, score, stars }
    })

    const values = [scoreContent, scoreTeaching, scoreAttitude, scoreGain].filter((v) => v > 0)
    const overallScore = values.length
      ? Number((values.reduce((acc, v) => acc + v, 0) / values.length).toFixed(1))
      : 0

    this.setData({ dimensions, overallScore })
  },

  onContentInput(e) {
    this.setData({ content: e.detail.value })
  },

  /** 快捷标签：点选后自动拼接到文字评价 */
  toggleTag(e) {
    const tag = e.currentTarget.dataset.tag
    let { content, selectedTags } = this.data
    const isSelected = selectedTags.indexOf(tag) > -1

    if (isSelected) {
      selectedTags = selectedTags.filter((item) => item !== tag)
      content = content.replace(tag + '，', '').replace(tag, '')
    } else {
      selectedTags = selectedTags.concat(tag)
      content = content ? content + tag + '，' : tag + '，'
    }
    this.setData({ content, selectedTags })
  },

  onAnonymousChange(e) {
    this.setData({ anonymous: e.detail.value })
  },

  async handleSubmit() {
    const { submitting, hidden } = this.data
    if (submitting) return
    if (hidden) {
      wx.showToast({ title: '该评价已被隐藏，无法修改', icon: 'none' })
      return
    }

    const {
      scoreContent, scoreTeaching, scoreAttitude, scoreGain, content, courseId
    } = this.data

    // 前端预校验：与后端 6004 的规则对齐
    if (!scoreContent || !scoreTeaching || !scoreAttitude || !scoreGain) {
      wx.showToast({ title: '请完成四项评分', icon: 'none' })
      return
    }

    const trimmed = (content || '').trim()
    if (trimmed.length < 5) {
      wx.showToast({ title: '文字评价至少 5 个字', icon: 'none' })
      return
    }
    if (trimmed.length > 500) {
      wx.showToast({ title: '文字评价不能超过 500 字', icon: 'none' })
      return
    }

    const isEdit = !!this.data.reviewId
    const confirmed = await this.confirm(
      isEdit ? '确认更新评价？' : '确认提交评价？',
      '提交后管理员公开前仍可修改'
    )
    if (!confirmed) return

    this.setData({ submitting: true })
    try {
      await reviewApi.submitReview({
        id: this.data.reviewId,
        courseId,
        scoreContent,
        scoreTeaching,
        scoreAttitude,
        scoreGain,
        content: trimmed,
        anonymous: this.data.anonymous ? 1 : 0
      })

      wx.showToast({ title: isEdit ? '评价已更新' : '提交成功', icon: 'success' })
      setTimeout(() => {
        wx.navigateBack()
      }, 800)
    } catch (err) {
      // 错误已由 request 统一 toast
    } finally {
      this.setData({ submitting: false })
    }
  },

  /** Promise 化确认弹窗 */
  confirm(title, content) {
    return new Promise((resolve) => {
      wx.showModal({
        title,
        content,
        confirmColor: '#409eff',
        success(res) {
          resolve(res.confirm)
        },
        fail() {
          resolve(false)
        }
      })
    })
  },

  goDetail() {
    const { courseId, courseName } = this.data
    wx.navigateTo({
      url: `/pages/review/detail/index?courseId=${courseId}&courseName=${encodeURIComponent(courseName)}`
    })
  }
})
