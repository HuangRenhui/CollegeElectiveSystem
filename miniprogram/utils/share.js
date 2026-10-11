/**
 * 分享配置工具。
 *
 * 微信通过页面 js 中的 onShareAppMessage 回调获取分享内容，
 * 回调是**同步**的，因此这里只做纯数据拼装，不发请求。
 *
 * 注意：
 *   - 必须返回 path，否则分享出去打开的是首页而非本页面；
 *   - imageUrl 需为 5:4 比例，不传则截取页面（效果较差）；
 *   - 朋友圈分享（onShareTimeline）不支持 path 带参数，只能用 query。
 */

/**
 * 分享封面图（需 5:4 比例）。
 *
 * 未放置图片时保持为空字符串，此时微信会自动截取当前页面作为封面——
 * 效果不如定制图，但不会报错。若要定制，将图片放入
 * `miniprogram/static/images/` 并在此填写路径即可。
 */
const SHARE_IMAGES = {
  timetable: '',
  grade: '',
  review: ''
}

/**
 * 构造好友分享配置。
 *
 * @param {string} title 分享标题
 * @param {string} path  分享后打开的页面路径（可带参数）
 * @param {string} [imageUrl] 封面图
 * @returns {object} 微信要求的分享配置对象
 */
function buildShare(title, path, imageUrl) {
  const config = { title, path }
  if (imageUrl) {
    config.imageUrl = imageUrl
  }
  return config
}

/** 课表分享 */
function shareTimetable() {
  return buildShare('我的课表', '/pages/timetable/index', SHARE_IMAGES.timetable)
}

/** 成绩分享 */
function shareGrade() {
  return buildShare('我的成绩单', '/pages/grade/index', SHARE_IMAGES.grade)
}

/** 评价页分享 */
function shareReview(courseId) {
  const path = courseId
    ? `/pages/review/detail/index?courseId=${courseId}`
    : '/pages/review/list/index'
  return buildShare('一起来评教', path, SHARE_IMAGES.review)
}

/** 通用分享（首页/公告等） */
function shareDefault() {
  return buildShare('选课助手 - 课表、成绩、评教一站式服务', '/pages/timetable/index')
}

module.exports = {
  buildShare,
  shareTimetable,
  shareGrade,
  shareReview,
  shareDefault
}
