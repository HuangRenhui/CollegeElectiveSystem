import { defineStore } from 'pinia'

/** 已读记录在 localStorage 中的 key 前缀（按用户维度隔离，避免多账号互相干扰） */
const READ_KEY_PREFIX = 'notice_read_'

/**
 * 读取某个用户的已读公告 ID 列表。
 * localStorage 内容可能被手工改坏，这里统一做容错，解析失败视为「全部未读」。
 */
function readIdsFromStorage(owner) {
  if (!owner) return []
  try {
    const parsed = JSON.parse(localStorage.getItem(READ_KEY_PREFIX + owner) || '[]')
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

/**
 * 应用级状态：布局、学期、公告。
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem('sidebar_collapsed') === 'true',
    currentSemester: null,
    /** 当前用户可见的全部公告（后端单次最多返回 50 条） */
    notices: [],
    /** 已读公告 ID：仅前端本地记录，用于计算未读角标 */
    readNoticeIds: [],
    /** readNoticeIds 归属的用户标识，用于识别「换了账号」 */
    readNoticeOwner: ''
  }),

  getters: {
    /** 未读公告列表 */
    unreadNotices: (state) =>
      state.notices.filter((item) => !state.readNoticeIds.includes(item.id)),

    /** 未读公告数量（顶部铃铛角标） */
    unreadNoticeCount() {
      return this.unreadNotices.length
    }
  },

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem('sidebar_collapsed', String(this.sidebarCollapsed))
    },

    setCurrentSemester(semester) {
      this.currentSemester = semester
    },

    /**
     * 写入公告列表。
     *
     * 同时剔除「已不在列表中」的已读 ID——公告被下架后本地记录不应继续保留，
     * 否则 localStorage 会随使用时间无限膨胀。
     */
    setNotices(notices) {
      this.notices = notices || []

      const alive = new Set(this.notices.map((item) => item.id))
      const kept = this.readNoticeIds.filter((id) => alive.has(id))
      if (kept.length !== this.readNoticeIds.length) {
        this.readNoticeIds = kept
        this.persistReadIds()
      }
    },

    /**
     * 按用户加载已读记录：登录后、刷新页面后调用。
     * 用户标识未变化时直接复用内存状态，避免重复读盘。
     */
    initNoticeRead(owner) {
      const key = owner == null ? '' : String(owner)
      if (this.readNoticeOwner === key) return
      this.readNoticeOwner = key
      this.readNoticeIds = readIdsFromStorage(key)
    },

    /** 指定公告是否已读 */
    isNoticeRead(id) {
      return this.readNoticeIds.includes(id)
    },

    /** 标记单条公告已读（点击阅读时调用） */
    markNoticeRead(id) {
      if (id == null || this.readNoticeIds.includes(id)) return
      this.readNoticeIds = [...this.readNoticeIds, id]
      this.persistReadIds()
    },

    /** 把当前列表内的公告全部标记为已读 */
    markAllNoticesRead() {
      const merged = new Set(this.readNoticeIds)
      this.notices.forEach((item) => merged.add(item.id))
      this.readNoticeIds = [...merged]
      this.persistReadIds()
    },

    /** 把已读记录写回 localStorage */
    persistReadIds() {
      if (!this.readNoticeOwner) return
      localStorage.setItem(
        READ_KEY_PREFIX + this.readNoticeOwner,
        JSON.stringify(this.readNoticeIds)
      )
    }
  }
})
