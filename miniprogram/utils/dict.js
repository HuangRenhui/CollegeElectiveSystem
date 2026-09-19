/**
 * 全局字典：与 Web 端 frontend/src/utils/dict.js 保持一致的语义。
 *
 * 小程序侧只保留实际用到的部分，避免无用代码占用包体积。
 */

// ---------------------------- 评价状态 ----------------------------
const REVIEW_STATUS_MAP = {
  0: { text: '草稿', theme: 'info' },
  1: { text: '已提交', theme: 'warning' },
  2: { text: '已公开', theme: 'success' },
  3: { text: '已隐藏', theme: 'danger' }
}

function reviewStatusText(value) {
  const item = REVIEW_STATUS_MAP[value]
  return item ? item.text : '-'
}

function reviewStatusTheme(value) {
  const item = REVIEW_STATUS_MAP[value]
  return item ? item.theme : 'info'
}

// ---------------------------- 评价维度 ----------------------------
/** 四维评分定义，与后端 Constants.REVIEW_DIMENSION_COUNT 对应 */
const REVIEW_DIMENSIONS = [
  { key: 'scoreContent', label: '教学内容', tip: '内容充实、条理清晰' },
  { key: 'scoreTeaching', label: '教学方法', tip: '讲解透彻、启发性强' },
  { key: 'scoreAttitude', label: '教学态度', tip: '认真负责、答疑及时' },
  { key: 'scoreGain', label: '学习收获', tip: '学有所获、提升明显' }
]

/** 文字评价快捷标签，点选自动拼接 */
const REVIEW_QUICK_TAGS = [
  '讲解清晰', '条理分明', '案例丰富', '重点突出',
  '作业适中', '给分友好', '答疑及时', '课堂氛围好',
  '进度偏快', '内容偏难', '考核偏严', '互动较少'
]

/** 综合评分：四维平均，保留 1 位小数 */
function averageScore(review) {
  const values = REVIEW_DIMENSIONS
    .map((item) => Number(review && review[item.key]))
    .filter((value) => !isNaN(value) && value > 0)
  if (!values.length) return 0
  const sum = values.reduce((acc, value) => acc + value, 0)
  return Number((sum / values.length).toFixed(1))
}

/** 评分对应的展示色（小程序用 class 后缀而非 tag type） */
function scoreTheme(score) {
  const value = Number(score)
  if (value >= 4.5) return 'success'
  if (value >= 3.5) return 'primary'
  if (value >= 2.5) return 'warning'
  return 'danger'
}

/** 匿名展示名：张*远 */
function maskName(name) {
  if (!name) return '匿名同学'
  const chars = String(name)
  if (chars.length <= 1) return chars + '*'
  if (chars.length === 2) return chars[0] + '*'
  return chars[0] + '*'.repeat(chars.length - 2) + chars[chars.length - 1]
}

// ---------------------------- 选课 / 成绩 ----------------------------
const SELECTION_STATUS_MAP = {
  0: { text: '已退选', theme: 'info' },
  1: { text: '已选课', theme: 'success' },
  2: { text: '已修完', theme: 'primary' }
}

function selectionStatusText(value) {
  const item = SELECTION_STATUS_MAP[value]
  return item ? item.text : '-'
}

const GRADE_STATUS_MAP = {
  0: { text: '草稿', theme: 'info' },
  1: { text: '已发布', theme: 'success' },
  2: { text: '已归档', theme: 'warning' }
}

function gradeStatusText(value) {
  const item = GRADE_STATUS_MAP[value]
  return item ? item.text : '-'
}

/** 成绩颜色 */
function scoreColor(score) {
  if (score === null || score === undefined) return '#909399'
  const value = Number(score)
  if (value >= 90) return '#67c23a'
  if (value >= 80) return '#409eff'
  if (value >= 60) return '#e6a23c'
  return '#f56c6c'
}

// ---------------------------- 课表 ----------------------------
const DAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

/** 节次时间，与 Web 端保持一致 */
const SECTION_TIMES = [
  '08:00-08:50', '08:55-09:45', '10:05-10:55', '11:00-11:50',
  '14:00-14:50', '14:55-15:45', '16:05-16:55', '17:00-17:50',
  '19:00-19:50', '19:55-20:45', '20:50-21:40', '21:45-22:35'
]

const WEEK_TYPE_MAP = { ALL: '每周', ODD: '单周', EVEN: '双周' }

function weekTypeText(value) {
  return WEEK_TYPE_MAP[value] || '每周'
}

module.exports = {
  REVIEW_STATUS_MAP,
  reviewStatusText,
  reviewStatusTheme,
  REVIEW_DIMENSIONS,
  REVIEW_QUICK_TAGS,
  averageScore,
  scoreTheme,
  maskName,
  SELECTION_STATUS_MAP,
  selectionStatusText,
  GRADE_STATUS_MAP,
  gradeStatusText,
  scoreColor,
  DAY_LABELS,
  SECTION_TIMES,
  WEEK_TYPE_MAP,
  weekTypeText
}
