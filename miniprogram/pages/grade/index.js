const app = getApp()
const studentApi = require('../../api/student')
const commonApi = require('../../api/common')
const dict = require('../../utils/dict')

Page({
  data: {
    loading: true,
    /** 学期下拉，首位为「全部学期」 */
    semesters: [],
    semesterIndex: 0,
    semesterId: null,
    report: {},
    /** 平均值对应的展示色，在 JS 中算好避免 WXML 内联调用 */
    scoreColorValue: '#909399',
    /** 成绩明细（附加展示字段） */
    grades: []
  },

  onShow() {
    if (!app.isLoggedIn()) return
    if (!this.data.semesters.length) {
      this.loadSemesters().then(() => this.loadReport())
    } else {
      this.loadReport()
    }
  },

  onPullDownRefresh() {
    this.loadReport().finally(() => wx.stopPullDownRefresh())
  },

  async loadSemesters() {
    try {
      const list = (await commonApi.listSemesterOptions()) || []
      // 成绩页允许查看全部学期，因此插入一个空选项
      const semesters = [{ id: null, semesterName: '全部学期' }].concat(list)
      const currentIndex = list.findIndex((item) => item.isCurrent === 1)
      const semesterIndex = currentIndex > -1 ? currentIndex + 1 : 0

      this.setData({
        semesters,
        semesterIndex,
        semesterId: semesters[semesterIndex].id
      })
    } catch (err) {
      this.setData({ semesters: [{ id: null, semesterName: '全部学期' }] })
    }
  },

  async loadReport() {
    this.setData({ loading: true })
    const params = this.data.semesterId ? { semesterId: this.data.semesterId } : {}
    try {
      const report = (await studentApi.getMyGradeReport(params)) || {}
      const grades = (report.grades || []).map((item) => ({
        ...item,
        totalScoreColor: dict.scoreColor(item.totalScore),
        passText: item.pass ? '通过' : '未通过'
      }))
      this.setData({
        report,
        grades,
        scoreColorValue: dict.scoreColor(report.averageScore)
      })
    } catch (err) {
      this.setData({ report: {}, grades: [], scoreColorValue: '#909399' })
    } finally {
      this.setData({ loading: false })
    }
  },

  onSemesterChange(e) {
    const index = Number(e.detail.value)
    const semester = this.data.semesters[index]
    this.setData({
      semesterIndex: index,
      semesterId: semester ? semester.id : null
    })
    this.loadReport()
  }
})
