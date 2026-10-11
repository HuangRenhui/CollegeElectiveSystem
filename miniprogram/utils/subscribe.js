/**
 * 订阅消息授权工具。
 *
 * ## 微信的硬限制
 *
 * 用户**每次授权只能收到一条消息**，用完即失效。这与公众号模板消息
 * （可长期推送）完全不同，因此必须先理解再使用：
 *
 *   - 授权一次 → 可发送 1 条；
 *   - 发送后次数归零，需再次引导用户授权；
 *   - 授权有效期 7 天。
 *
 * ## 续期策略（重要）
 *
 * **不要**在小程序启动时弹窗索权——用户会习惯性拒绝，且拉低好感度。
 * 正确做法是在用户**明确需要该通知**的时刻再引导，例如：
 *
 *   - 提交评价成功后 → 询问「成绩发布时通知我」；
 *   - 查看成绩单时   → 询问「成绩发布时通知我」。
 *
 * ## 使用方式
 *
 * ```js
 * const { requestSubscribe } = require('../../utils/subscribe')
 *
 * async handleSubmit() {
 *   await submitReview(...)
 *   // 提交成功后顺带引导，此时用户对通知有真实需求
 *   await requestSubscribe('GRADE_PUBLISH')
 * }
 * ```
 */
const wxApi = require('../api/wx')
const { SCENES } = require('./dict')

/** 模板ID缓存，避免每次授权都请求后端 */
let templateCache = null

/**
 * 拉取已配置模板的场景与模板ID。
 *
 * 未配置模板的场景不会返回，因此调用方无需关心「哪些场景可用」——
 * 直接调用 requestSubscribe，不可用时会静默返回 false。
 *
 * @returns {Promise<Array<{scene: string, templateId: string}>>}
 */
async function loadTemplates() {
  if (templateCache) return templateCache
  const list = await wxApi.listTemplates()
  templateCache = Array.isArray(list) ? list : []
  return templateCache
}

/**
 * 请求订阅授权。
 *
 * **必须由用户点击触发**，不能在 onLoad/onShow 中自动调用，
 * 否则微信会直接返回失败。
 *
 * @param {string} scene 场景：REVIEW_OPEN 或 GRADE_PUBLISH
 * @returns {Promise<boolean>} 用户是否同意授权
 */
async function requestSubscribe(scene) {
  const templates = await loadTemplates()
  const target = templates.find(item => item.scene === scene)

  // 后端未配置该场景的模板ID，静默跳过（不算失败）
  if (!target || !target.templateId) {
    return false
  }

  const accepted = await new Promise(resolve => {
    wx.requestSubscribeMessage({
      tmplIds: [target.templateId],
      success(res) {
        // res[templateId] === 'accept' 表示用户同意；'reject' / 'ban' 表示拒绝
        resolve(res[target.templateId] === 'accept')
      },
      fail() {
        // 基础库过低或用户拒绝弹窗
        resolve(false)
      }
    })
  })

  if (!accepted) {
    return false
  }

  // 上报后端：剩余可发送次数 +1
  try {
    await wxApi.reportSubscribe(scene)
  } catch (e) {
    // 上报失败不影响已获得的授权，仅本地记录不足
    return false
  }
  return true
}

/**
 * 查询某场景剩余可发送次数。
 *
 * 用于判断是否需要再次引导授权：每次进入相关页面时检查，
 * 归零则提示用户重新授权（而非等消息发不出去才发现）。
 *
 * @param {string} scene 场景
 * @returns {Promise<number>}
 */
async function getRemainCount(scene) {
  const count = await wxApi.getRemainCount(scene)
  return Number(count) || 0
}

/**
 * 在用户操作成功后，友好地引导订阅。
 *
 * 用轻提示而非强制弹窗，用户拒绝也不打断主流程。
 *
 * @param {string} scene 场景
 * @param {string} tip   引导文案
 */
async function guideSubscribe(scene, tip) {
  const templates = await loadTemplates()
  if (!templates.some(item => item.scene === scene)) {
    return
  }

  const remain = await getRemainCount(scene)
  if (remain > 0) {
    // 仍有可用次数，无需打扰用户
    return
  }

  const confirmed = await new Promise(resolve => {
    wx.showModal({
      title: '开启消息提醒',
      content: tip,
      confirmText: '开启',
      cancelText: '暂不',
      success(res) {
        resolve(!!res.confirm)
      },
      fail() {
        resolve(false)
      }
    })
  })

  if (confirmed) {
    await requestSubscribe(scene)
  }
}

module.exports = {
  SCENES,
  loadTemplates,
  requestSubscribe,
  getRemainCount,
  guideSubscribe
}
