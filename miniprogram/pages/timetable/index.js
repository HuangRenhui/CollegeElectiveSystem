const app = getApp()
const studentApi = require('../../api/student')
const commonApi = require('../../api/common')
const dict = require('../../utils/dict')

Page({
  data: {
    loading: true,
    semesterId: null,
    semesters: [],
    semesterIndex: 0,
    /** 按星期分组后的课表 [{ day, dayLabel, items: [] }] */
    groups: [],
    /** 课程总数 */
    total: 0
  },

  onShow() {
    if (!app.isLoggedIn()) return
    if (!this.data.semesters.length) {
      this.loadSemesters().then(() => this.loadTimetable())
    } else {
      this.loadTimetable()
    }
  },

  onPullDownRefresh() {
    this.loadTimetable().finally(() => wx.stopPullDownRefresh())
  },

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

  async loadTimetable() {
    this.setData({ loading: true })
    const params = this.data.semesterId ? { semesterId: this.data.semesterId } : {}
    try {
      const list = (await studentApi.getMyTimetable(params)) || []
      this.setData({
        groups: this.groupByDay(list),
        total: list.length
      })
    } catch (err) {
      this.setData({ groups: [], total: 0 })
    } finally {
      this.setData({ loading: false })
    }
  },

  /**
   * 按星期分组并格式化。
   *
   * 小程序屏幕窄，网格课表可读性差，改为「按天分组的列表」，
   * 每天内按开始节次排序，学生一眼能看到当天要上什么课。
   */
  groupByDay(list) {
    const groups = []
    for (let day = 1; day <= 7; day++) {
      const items = list
        .filter((item) => Number(item.dayOfWeek) === day)
        .sort((a, b) => Number(a.startSection) - Number(b.startSection))
        .map((item) => ({
          ...item,
          sectionText: this.formatSection(item.startSection, item.endSection),
          weekText: this.formatWeek(item),
          timeText: this.resolveTime(item.startSection)
        }))

      if (items.length) {
        groups.push({
          day,
          dayLabel: dict.DAY_LABELS[day - 1],
          items
        })
      }
    }
    return groups
  },

  /** 节次区间文案：1-2 节 */
  formatSection(start, end) {
    if (!start) return '-'
    if (!end || start === end) return `${start} 节`
    return `${start}-${end} 节`
  },

  /** 周次文案：1-16 周（单周） */
  formatWeek(item) {
    const start = item.startWeek || 1
    const end = item.endWeek || 16
    const weekTypeText = item.weekType && item.weekType !== 'ALL'
      ? dict.weekTypeText(item.weekType)
      : ''
    return `${start}-${end} 周${weekTypeText ? `（${weekTypeText}）` : ''}`
  },

  /** 用节次反查上课时间，取自 dict.SECTION_TIMES */
  resolveTime(startSection) {
    const index = Number(startSection) - 1
    if (index < 0 || index >= dict.SECTION_TIMES.length) return ''
    return dict.SECTION_TIMES[index]
  },

  onSemesterChange(e) {
    const index = Number(e.detail.value)
    const semester = this.data.semesters[index]
    this.setData({
      semesterIndex: index,
      semesterId: semester ? semester.id : null
    })
    this.loadTimetable()
  }
})
