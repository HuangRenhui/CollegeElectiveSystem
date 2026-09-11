import { defineStore } from 'pinia'

/**
 * 应用级状态：布局、学期、公告。
 */
export const useAppStore = defineStore('app', {
  state: () => ({
    sidebarCollapsed: localStorage.getItem('sidebar_collapsed') === 'true',
    currentSemester: null,
    notices: []
  }),

  actions: {
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem('sidebar_collapsed', String(this.sidebarCollapsed))
    },

    setCurrentSemester(semester) {
      this.currentSemester = semester
    },

    setNotices(notices) {
      this.notices = notices || []
    }
  }
})
