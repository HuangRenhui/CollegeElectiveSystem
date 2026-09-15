/**
 * 全局字典：枚举值与显示文本的映射，统一前后端语义。
 */

// ---------------------------- 课程类型 ----------------------------
export const COURSE_TYPE_MAP = {
  REQUIRED: { text: '必修', type: 'danger' },
  ELECTIVE: { text: '选修', type: 'success' },
  PUBLIC: { text: '公选', type: 'warning' }
}

export const COURSE_TYPE_OPTIONS = Object.entries(COURSE_TYPE_MAP).map(([value, item]) => ({
  value,
  label: item.text
}))

export function courseTypeText(value) {
  return COURSE_TYPE_MAP[value]?.text || value || '-'
}

export function courseTypeTag(value) {
  return COURSE_TYPE_MAP[value]?.type || 'info'
}

// ---------------------------- 课程状态 ----------------------------
export const COURSE_STATUS_MAP = {
  0: { text: '已下架', type: 'info' },
  1: { text: '可选中', type: 'success' },
  2: { text: '已结课', type: 'warning' }
}

export const COURSE_STATUS_OPTIONS = Object.entries(COURSE_STATUS_MAP).map(([value, item]) => ({
  value: Number(value),
  label: item.text
}))

export function courseStatusText(value) {
  return COURSE_STATUS_MAP[value]?.text || '-'
}

export function courseStatusTag(value) {
  return COURSE_STATUS_MAP[value]?.type || 'info'
}

// ---------------------------- 考核方式 ----------------------------
export const EXAM_TYPE_MAP = { EXAM: '考试', CHECK: '考查' }

export const EXAM_TYPE_OPTIONS = Object.entries(EXAM_TYPE_MAP).map(([value, label]) => ({ value, label }))

export function examTypeText(value) {
  return EXAM_TYPE_MAP[value] || value || '-'
}

// ---------------------------- 选课状态 ----------------------------
export const SELECTION_STATUS_MAP = {
  0: { text: '已退选', type: 'info' },
  1: { text: '已选课', type: 'success' },
  2: { text: '已修完', type: 'primary' }
}

export const SELECTION_STATUS_OPTIONS = Object.entries(SELECTION_STATUS_MAP).map(([value, item]) => ({
  value: Number(value),
  label: item.text
}))

export function selectionStatusText(value) {
  return SELECTION_STATUS_MAP[value]?.text || '-'
}

export function selectionStatusTag(value) {
  return SELECTION_STATUS_MAP[value]?.type || 'info'
}

// ---------------------------- 选课方式 ----------------------------
export const SELECT_TYPE_MAP = {
  1: { text: '正常选课', type: 'info' },
  2: { text: '管理员代选', type: 'warning' }
}

export const SELECT_TYPE_OPTIONS = Object.entries(SELECT_TYPE_MAP).map(([value, item]) => ({
  value: Number(value),
  label: item.text
}))

export function selectTypeText(value) {
  return SELECT_TYPE_MAP[value]?.text || '正常选课'
}

export function selectTypeTag(value) {
  return SELECT_TYPE_MAP[value]?.type || 'info'
}

// ---------------------------- 成绩状态 ----------------------------
export const GRADE_STATUS_MAP = {
  0: { text: '草稿', type: 'info' },
  1: { text: '已发布', type: 'success' },
  2: { text: '已归档', type: 'warning' }
}

export const GRADE_STATUS_OPTIONS = Object.entries(GRADE_STATUS_MAP).map(([value, item]) => ({
  value: Number(value),
  label: item.text
}))

export function gradeStatusText(value) {
  return GRADE_STATUS_MAP[value]?.text || '-'
}

export function gradeStatusTag(value) {
  return GRADE_STATUS_MAP[value]?.type || 'info'
}

// ---------------------------- 学期状态 ----------------------------
export const SEMESTER_STATUS_MAP = {
  0: { text: '未开始', type: 'info' },
  1: { text: '选课中', type: 'success' },
  2: { text: '进行中', type: 'primary' },
  3: { text: '已结束', type: 'warning' }
}

export const SEMESTER_STATUS_OPTIONS = Object.entries(SEMESTER_STATUS_MAP).map(([value, item]) => ({
  value: Number(value),
  label: item.text
}))

export function semesterStatusText(value) {
  return SEMESTER_STATUS_MAP[value]?.text || '-'
}

export function semesterStatusTag(value) {
  return SEMESTER_STATUS_MAP[value]?.type || 'info'
}

// ---------------------------- 公告类型 ----------------------------
export const NOTICE_TYPE_MAP = {
  SYSTEM: { text: '系统', type: 'info' },
  SELECTION: { text: '选课', type: 'success' },
  EXAM: { text: '考试', type: 'warning' }
}

export const NOTICE_TYPE_OPTIONS = Object.entries(NOTICE_TYPE_MAP).map(([value, item]) => ({
  value,
  label: item.text
}))

export function noticeTypeText(value) {
  return NOTICE_TYPE_MAP[value]?.text || '系统'
}

export function noticeTypeTag(value) {
  return NOTICE_TYPE_MAP[value]?.type || 'info'
}

// ---------------------------- 公告状态 ----------------------------
export const NOTICE_STATUS_MAP = {
  0: { text: '草稿', type: 'info' },
  1: { text: '已发布', type: 'success' },
  2: { text: '已下架', type: 'warning' }
}

export const NOTICE_STATUS_OPTIONS = Object.entries(NOTICE_STATUS_MAP).map(([value, item]) => ({
  value: Number(value),
  label: item.text
}))

export function noticeStatusText(value) {
  return NOTICE_STATUS_MAP[value]?.text || '-'
}

export function noticeStatusTag(value) {
  return NOTICE_STATUS_MAP[value]?.type || 'info'
}

// ---------------------------- 公告目标角色 ----------------------------
export const TARGET_ROLE_OPTIONS = [
  { value: 'ALL', label: '全部角色' },
  { value: 'STUDENT', label: '仅学生' },
  { value: 'TEACHER', label: '仅教师' }
]

// ---------------------------- 教室类型 ----------------------------
export const ROOM_TYPE_MAP = {
  NORMAL: { text: '普通教室', type: 'info' },
  MULTIMEDIA: { text: '多媒体教室', type: 'success' },
  LAB: { text: '实验室', type: 'warning' },
  GYM: { text: '体育场', type: 'danger' }
}

export const ROOM_TYPE_OPTIONS = Object.entries(ROOM_TYPE_MAP).map(([value, item]) => ({
  value,
  label: item.text
}))

export function roomTypeText(value) {
  return ROOM_TYPE_MAP[value]?.text || value || '-'
}

export function roomTypeTag(value) {
  return ROOM_TYPE_MAP[value]?.type || 'info'
}

// ---------------------------- 职称 ----------------------------
export const TITLE_OPTIONS = ['助教', '讲师', '副教授', '教授'].map((label) => ({ value: label, label }))

// ---------------------------- 性别 ----------------------------
export const GENDER_MAP = { 0: '未知', 1: '男', 2: '女' }

export const GENDER_OPTIONS = Object.entries(GENDER_MAP).map(([value, label]) => ({
  value: Number(value),
  label
}))

export function genderText(value) {
  return GENDER_MAP[value] ?? '未知'
}

// ---------------------------- 账号状态 ----------------------------
export const USER_STATUS_MAP = {
  0: { text: '禁用', type: 'danger' },
  1: { text: '正常', type: 'success' }
}

export function userStatusText(value) {
  return USER_STATUS_MAP[value]?.text || '-'
}

export function userStatusTag(value) {
  return USER_STATUS_MAP[value]?.type || 'info'
}

// ---------------------------- 周次类型 ----------------------------
export const WEEK_TYPE_MAP = { ALL: '每周', ODD: '单周', EVEN: '双周' }

export const WEEK_TYPE_OPTIONS = Object.entries(WEEK_TYPE_MAP).map(([value, label]) => ({ value, label }))

export function weekTypeText(value) {
  return WEEK_TYPE_MAP[value] || '每周'
}

// ---------------------------- 节次时间 ----------------------------
export const SECTION_TIMES = [
  '08:00-08:50',
  '08:55-09:45',
  '10:05-10:55',
  '11:00-11:50',
  '14:00-14:50',
  '14:55-15:45',
  '16:05-16:55',
  '17:00-17:50',
  '19:00-19:50',
  '19:55-20:45',
  '20:50-21:40',
  '21:45-22:35'
]

export const DAY_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// ---------------------------- 成绩颜色 ----------------------------
export function scoreColor(score) {
  if (score === null || score === undefined) return '#909399'
  const value = Number(score)
  if (value >= 90) return '#67c23a'
  if (value >= 80) return '#409eff'
  if (value >= 60) return '#e6a23c'
  return '#f56c6c'
}
