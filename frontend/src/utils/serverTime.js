import { reactive, readonly } from 'vue'

/**
 * 服务器时间基准。
 *
 * ## 为什么需要
 *
 * 选课倒计时本质上是在做时间比较。若前端直接使用浏览器本地时间（`Date.now()`），
 * 用户修改操作系统时间即可让倒计时提前归零或延后，从而绕过前端的按钮禁用——
 * 虽然后端仍会拦截（`Semester#inSelectionPeriod` 是最终防线），但体验上会出现
 * 「按钮亮了却选不上课」的错觉。
 *
 * ## 工作方式
 *
 * 以「偏移量」而非「绝对时间」做修正：
 *
 * ```text
 * 偏移量 = 服务器时刻 + 单程网络耗时估算 - 本地时刻
 * 校正后的当前时刻 = Date.now() + 偏移量
 * ```
 *
 * 之所以用偏移量而不是每次都信任某个时间戳，是因为用户可以在两次请求之间再次
 * 改动系统时间；只要保留本地单调递增的计时器（`setInterval`）并叠加固定偏移，
 * 改本地时间不会影响倒计时进度。
 *
 * ## 精度说明
 *
 * 偏移量在每次接口响应时自动重算（见 `request.js` 的响应拦截器），
 * 因此网络波动不会累积误差；请求间隔内的微小漂移可忽略不计。
 */

/** 偏移量状态：服务器时刻 - 本地时刻（毫秒） */
const state = reactive({ offset: 0, calibrated: false })

/**
 * 用一次请求的往返时间校正偏移量。
 *
 * @param {number} serverTime 服务器返回的 epoch 毫秒
 * @param {number} sentAt 本地发起请求的时刻（epoch 毫秒）
 * @param {number} receivedAt 本地收到响应的时刻（epoch 毫秒）
 */
export function calibrate(serverTime, sentAt, receivedAt) {
  if (!Number.isFinite(serverTime) || serverTime <= 0) return
  if (!Number.isFinite(sentAt) || !Number.isFinite(receivedAt)) return

  // 用往返时间的一半估算单程延迟，比只用响应到达时刻更接近真实服务器时刻
  const roundTrip = Math.max(receivedAt - sentAt, 0)
  const estimatedServer = serverTime + roundTrip / 2

  state.offset = estimatedServer - receivedAt
  state.calibrated = true
}

/**
 * 当前经服务器基准校正后的时刻（epoch 毫秒）。
 *
 * 未完成首次校正前退化为本地时间，保证组件在冷启动时也能正常渲染。
 */
export function serverNow() {
  return Date.now() + state.offset
}

/**
 * 是否已完成服务器时间校准。
 *
 * 组件可据此提示「时间已与服务器同步」，或在未校准时采取更保守的策略。
 */
export function isCalibrated() {
  return state.calibrated
}

/** 仅供测试与调试使用 */
export const serverTimeState = readonly(state)