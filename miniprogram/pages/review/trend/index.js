const app = getApp()
const reviewApi = require('../../../api/review')
const dict = require('../../../utils/dict')

/** Canvas 逻辑尺寸（px），需与 wxml 中的 style 保持一致 */
const CANVAS_WIDTH = 340
const CANVAS_HEIGHT = 480

/** 雷达图几何参数 */
const RADAR = {
  centerX: 170,
  centerY: 150,
  radius: 100,
  /** 满分，用于把分数换算为半径比例 */
  fullScore: 5
}

/** 折线图几何参数 */
const LINE = {
  startX: 46,
  endX: 320,
  topY: 340,
  height: 110,
  /** 纵轴满分 */
  fullScore: 5
}

Page({
  data: {
    loading: true,
    /** 按学期升序的趋势数据 */
    trendList: [],
    /** 最近一个学期的四维分数，用于雷达图与维度条 */
    latestDimensions: [],
    /** 最新学期的综合评分 */
    latestScore: 0,
    /** 数据为空时的提示 */
    empty: false
  },

  onShow() {
    if (!app.isLoggedIn()) return
    this.loadTrend()
  },

  onPullDownRefresh() {
    this.loadTrend().finally(() => wx.stopPullDownRefresh())
  },

  async loadTrend() {
    this.setData({ loading: true })
    try {
      const list = (await reviewApi.getMyReviewTrend()) || []
      if (!list.length) {
        this.setData({ trendList: [], empty: true })
        return
      }

      // 雷达图取最近一个学期：趋势看变化，雷达看当前水平
      const latest = list[list.length - 1]
      const dimensions = dict.REVIEW_DIMENSIONS.map((dim) => {
        const score = Number(latest[dim.key]) || 0
        return {
          label: dim.label,
          // 数值型 score 供 Canvas 绘图使用
          score,
          // 文本型 scoreText 供 WXML 展示，避免在小程序模板里做格式化
          scoreText: score.toFixed(1),
          // 维度条宽度百分比，满分 5 分
          percent: Math.round((score / 5) * 100)
        }
      })

      this.setData({
        trendList: list,
        latestDimensions: dimensions,
        latestScore: Number(latest.averageScore || 0).toFixed(1),
        empty: false
      })

      // 等待 setData 渲染完成后再取 Canvas 节点，否则可能拿到 null
      wx.nextTick(() => this.drawCharts())
    } catch (err) {
      this.setData({ trendList: [], empty: true })
    } finally {
      this.setData({ loading: false })
    }
  },

  /** 绘制两张图 */
  async drawCharts() {
    try {
      const canvas = await this.queryCanvas('#trendCanvas')
      if (!canvas) return

      const ctx = canvas.getContext('2d')
      const dpr = wx.getSystemInfoSync().pixelRatio

      // 画布按像素比放大，再缩放上下文，避免高清屏下模糊
      canvas.width = CANVAS_WIDTH * dpr
      canvas.height = CANVAS_HEIGHT * dpr
      ctx.scale(dpr, dpr)

      this.drawRadar(ctx)
      this.drawLine(ctx)
    } catch (err) {
      // 绘制失败不阻断页面，图表区域留白
      console.error('[trend] 绘制失败', err)
    }
  },

  /**
   * 查询 Canvas 节点。
   *
   * @param {string} selector 选择器
   * @returns {Promise<object|null>}
   */
  queryCanvas(selector) {
    return new Promise((resolve) => {
      wx.createSelectorQuery()
        .in(this)
        .select(selector)
        .fields({ node: true, size: true })
        .exec((res) => resolve(res && res[0] ? res[0].node : null))
    })
  },

  /**
   * 绘制四维雷达图。
   *
   * 原理：给定中心点、半径、维度数，用三角函数算出每个维度的顶点位置。
   * 从正上方（-90°）开始均匀分布，视觉上更符合阅读习惯。
   */
  drawRadar(ctx) {
    const dimensions = this.data.latestDimensions
    if (!dimensions.length) return

    const count = dimensions.length
    const { centerX, centerY, radius, fullScore } = RADAR

    // 顶点坐标计算函数
    const vertex = (index, ratio) => {
      const angle = (Math.PI * 2 * index) / count - Math.PI / 2
      return {
        x: centerX + radius * ratio * Math.cos(angle),
        y: centerY + radius * ratio * Math.sin(angle)
      }
    }

    // 1. 满分参考线（正多边形边框）
    ctx.beginPath()
    for (let i = 0; i < count; i++) {
      const p = vertex(i, 1)
      i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)
    }
    ctx.closePath()
    ctx.strokeStyle = '#e4e7ed'
    ctx.lineWidth = 1
    ctx.stroke()

    // 2. 中间刻度线（3/5 与 4/5 位置），帮助读数
    ;[0.6, 0.8].forEach((ratio) => {
      ctx.beginPath()
      for (let i = 0; i < count; i++) {
        const p = vertex(i, ratio)
        i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)
      }
      ctx.closePath()
      ctx.strokeStyle = '#f0f2f5'
      ctx.stroke()
    })

    // 3. 从中心到各顶点的轴线
    ctx.beginPath()
    for (let i = 0; i < count; i++) {
      const p = vertex(i, 1)
      ctx.moveTo(centerX, centerY)
      ctx.lineTo(p.x, p.y)
    }
    ctx.strokeStyle = '#ebeef5'
    ctx.stroke()

    // 4. 数据区域（填充 + 描边）
    ctx.beginPath()
    dimensions.forEach((dim, i) => {
      const ratio = Math.max(0, Math.min(1, dim.score / fullScore))
      const p = vertex(i, ratio)
      i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)
    })
    ctx.closePath()
    ctx.fillStyle = 'rgba(64, 158, 255, 0.22)'
    ctx.fill()
    ctx.strokeStyle = '#409eff'
    ctx.lineWidth = 2
    ctx.stroke()

    // 5. 数据点
    dimensions.forEach((dim, i) => {
      const ratio = Math.max(0, Math.min(1, dim.score / fullScore))
      const p = vertex(i, ratio)
      ctx.beginPath()
      ctx.arc(p.x, p.y, 3, 0, Math.PI * 2)
      ctx.fillStyle = '#409eff'
      ctx.fill()
    })

    // 6. 维度标签与分数（绘制在满分顶点外侧）
    ctx.font = '12px sans-serif'
    dimensions.forEach((dim, i) => {
      const angle = (Math.PI * 2 * i) / count - Math.PI / 2
      const labelRadius = radius + 26
      const lx = centerX + labelRadius * Math.cos(angle)
      const ly = centerY + labelRadius * Math.sin(angle)

      ctx.fillStyle = '#606266'
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      ctx.fillText(dim.label, lx, ly)

      // 分数画在标签下一行，避免与标签重叠
      ctx.fillStyle = '#409eff'
      ctx.fillText(dim.score.toFixed(1), lx, ly + 15)
    })

    // 7. 标题
    ctx.fillStyle = '#303133'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'left'
    ctx.fillText('最新学期各维度评分', 16, 24)
  },

  /**
   * 绘制学期趋势折线图。
   *
   * 横轴为学期，纵轴为综合评分（0-5）。点太少（< 2）时画点不画线，
   * 因为单点无法体现趋势。
   */
  drawLine(ctx) {
    const list = this.data.trendList
    if (!list.length) return

    const { startX, endX, topY, height, fullScore } = LINE

    // 标题
    ctx.fillStyle = '#303133'
    ctx.font = 'bold 14px sans-serif'
    ctx.textAlign = 'left'
    ctx.fillText('综合评分趋势', 16, 300)

    // 纵轴刻度线（0 / 2.5 / 5）
    ctx.strokeStyle = '#f0f2f5'
    ctx.lineWidth = 1
    ;[0, 0.5, 1].forEach((ratio) => {
      const y = topY + height - ratio * height
      ctx.beginPath()
      ctx.moveTo(startX, y)
      ctx.lineTo(endX, y)
      ctx.stroke()
    })

    const count = list.length
    const stepX = count > 1 ? (endX - startX) / (count - 1) : 0

    const points = list.map((item, i) => {
      const score = Number(item.averageScore) || 0
      const ratio = Math.max(0, Math.min(1, score / fullScore))
      return {
        x: count > 1 ? startX + i * stepX : (startX + endX) / 2,
        y: topY + height - ratio * height,
        score,
        semesterName: item.semesterName
      }
    })

    // 折线（仅当有 2 个及以上数据点）
    if (points.length > 1) {
      ctx.beginPath()
      points.forEach((p, i) => {
        i === 0 ? ctx.moveTo(p.x, p.y) : ctx.lineTo(p.x, p.y)
      })
      ctx.strokeStyle = '#67c23a'
      ctx.lineWidth = 2
      ctx.stroke()
    }

    // 数据点与分数标签
    points.forEach((p) => {
      ctx.beginPath()
      ctx.arc(p.x, p.y, 3.5, 0, Math.PI * 2)
      ctx.fillStyle = '#67c23a'
      ctx.fill()

      ctx.fillStyle = '#606266'
      ctx.font = '11px sans-serif'
      ctx.textAlign = 'center'
      ctx.fillText(p.score.toFixed(1), p.x, p.y - 12)
    })

    // 横轴学期标签：只显示首尾，避免密集重叠
    ctx.fillStyle = '#909399'
    ctx.font = '10px sans-serif'
    ctx.textAlign = 'center'
    if (points.length) {
      ctx.textAlign = 'left'
      ctx.fillText(points[0].semesterName, startX, topY + height + 22)
      if (points.length > 1) {
        ctx.textAlign = 'right'
        ctx.fillText(points[points.length - 1].semesterName, endX, topY + height + 22)
      }
    }
  }
})
