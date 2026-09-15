import { computed, onBeforeUnmount, ref, watch } from 'vue'

/**
 * 选课窗口倒计时。
 *
 * 依据学期配置的选课开放时间与截止时间，实时计算当前所处的选课阶段，
 * 并给出可用于按钮禁用与提示文案的状态。时间来源为服务端学期配置，
 * 前端仅做展示与交互控制，最终的选课资格仍由后端校验。
 *
 * @param {import('vue').Ref} semesterRef 学期对象（含 selectStartTime / selectEndTime）
 * @param {Object} [options]
 * @param {number} [options.tickMs] 计时刷新间隔，默认 1000 毫秒
 * @returns 选课窗口状态与倒计时文案
 */
export function useSelectionCountdown(semesterRef, options = {}) {
  const { tickMs = 1000 } = options

  /** 每秒递增，用于驱动倒计时重新计算 */
  const now = ref(Date.now())
  let timer = null

  function parse(value) {
    if (!value) return null
    // 兼容 "2026-09-15 08:00:00" 形式，Safari 对空格分隔解析不稳定
    const normalized = String(value).replace(/-/g, '/')
    const time = new Date(normalized).getTime()
    return Number.isNaN(time) ? null : time
  }

  function stop() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  function start() {
    stop()
    timer = setInterval(() => {
      now.value = Date.now()
    }, tickMs)
  }

  watch(
    semesterRef,
    () => {
      now.value = Date.now()
      start()
    },
    { immediate: true }
  )

  onBeforeUnmount(stop)

  /** 选课开放时间戳 */
  const startTime = computed(() => parse(semesterRef.value?.selectStartTime))
  /** 选课截止时间戳 */
  const endTime = computed(() => parse(semesterRef.value?.selectEndTime))

  /**
   * 当前阶段：
   * - unset    未配置选课时间
   * - before   选课尚未开始
   * - open     选课进行中
   * - closed   选课已结束
   */
  const phase = computed(() => {
    const start = startTime.value
    const end = endTime.value
    if (!start || !end) return 'unset'
    const current = now.value
    if (current < start) return 'before'
    if (current > end) return 'closed'
    return 'open'
  })

  /** 选课是否处于可提交状态 */
  const isOpen = computed(() => phase.value === 'open')

  /** 是否可以进入选课页面（未开始时仍可浏览课程） */
  const canSelect = computed(() => phase.value === 'open')

  /** 距离选课开始或结束的剩余毫秒数 */
  const remainMs = computed(() => {
    if (phase.value === 'before') return Math.max(startTime.value - now.value, 0)
    if (phase.value === 'open') return Math.max(endTime.value - now.value, 0)
    return 0
  })

  /** 结构化倒计时，便于按需组合展示 */
  const countdown = computed(() => {
    const total = Math.floor(remainMs.value / 1000)
    const days = Math.floor(total / 86400)
    const hours = Math.floor((total % 86400) / 3600)
    const minutes = Math.floor((total % 3600) / 60)
    const seconds = total % 60
    return {
      days,
      hours,
      minutes,
      seconds,
      padHours: String(hours).padStart(2, '0'),
      padMinutes: String(minutes).padStart(2, '0'),
      padSeconds: String(seconds).padStart(2, '0'),
      totalSeconds: total
    }
  })

  /** 距离选课开始不足该分钟数时，进入"即将开始"提醒状态 */
  const READY_MINUTES = 30
  const readySeconds = READY_MINUTES * 60

  /** 是否进入倒计时提醒区间（未开始且剩余不足 30 分钟） */
  const isReadySoon = computed(
    () => phase.value === 'before' && countdown.value.totalSeconds <= readySeconds
  )

  /** 状态标签类型 */
  const phaseTagType = computed(
    () => ({ open: 'success', before: 'warning', closed: 'info', unset: 'info' }[phase.value])
  )

  /** 面向学生的状态说明 */
  const phaseText = computed(() => {
    switch (phase.value) {
      case 'open':
        return '选课进行中'
      case 'before':
        return isReadySoon.value ? '选课即将开始' : '选课尚未开始'
      case 'closed':
        return '选课已结束'
      default:
        return '未配置选课时间'
    }
  })

  /** 倒计时完整文案，如 "02 天 03:15:20" */
  const countdownText = computed(() => {
    const { days, padHours, padMinutes, padSeconds } = countdown.value
    const time = `${padHours}:${padMinutes}:${padSeconds}`
    return days > 0 ? `${days} 天 ${time}` : time
  })

  /** 面向学生的倒计时提示语 */
  const tipText = computed(() => {
    switch (phase.value) {
      case 'before':
        return isReadySoon.value
          ? `选课通道将在 ${countdownText.value} 后开放，请提前准备`
          : `选课通道将于 ${formatTime(startTime.value)} 开放，剩余 ${countdownText.value}`
      case 'open':
        return `距离选课截止还有 ${countdownText.value}`
      case 'closed':
        return '本次选课已结束，如需调整请联系教务处'
      default:
        return '当前学期尚未配置选课开放时间，请联系管理员'
    }
  })

  return {
    phase,
    isOpen,
    canSelect,
    remainMs,
    countdown,
    countdownText,
    tipText,
    phaseText,
    phaseTagType,
    isReadySoon,
    startTime,
    endTime,
    READY_MINUTES
  }
}

/**
 * 将时间戳格式化为 "MM-DD HH:mm" 展示。
 */
export function formatTime(timestamp) {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getMonth() + 1}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
