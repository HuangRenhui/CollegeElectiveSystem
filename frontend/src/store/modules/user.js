import { defineStore } from 'pinia'
import { login, logout, getUserInfo } from '@/api/auth'
import {
  getToken, setToken, removeToken,
  getStoredUser, setStoredUser, removeStoredUser
} from '@/utils/auth'

/**
 * 用户状态：令牌、角色、个人资料。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: getToken() || '',
    userInfo: getStoredUser() || {},
    roles: getStoredUser()?.role ? [getStoredUser().role] : []
  }),

  getters: {
    isLogin: (state) => !!state.token,
    role: (state) => state.userInfo.role || '',
    realName: (state) => state.userInfo.realName || '',
    isStudent: (state) => state.userInfo.role === 'STUDENT',
    isTeacher: (state) => state.userInfo.role === 'TEACHER',
    isAdmin: (state) => state.userInfo.role === 'ADMIN',
    homePath: (state) => {
      switch (state.userInfo.role) {
        case 'ADMIN':
          return '/admin/dashboard'
        case 'TEACHER':
          return '/teacher/workbench'
        default:
          return '/student/courses'
      }
    }
  },

  actions: {
    /**
     * 登录并缓存令牌。
     */
    async login(loginForm) {
      const { data } = await login(loginForm)
      const { token } = data
      this.token = token
      setToken(token, loginForm.remember)

      const user = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        role: data.role,
        avatar: data.avatar
      }
      this.userInfo = user
      this.roles = [data.role]
      setStoredUser(user, loginForm.remember)
      return data
    },

    /**
     * 拉取最新用户资料（用于刷新页面后恢复状态）。
     */
    async fetchUserInfo() {
      const { data } = await getUserInfo()
      this.userInfo = { ...this.userInfo, ...data }
      this.roles = [data.role]
      setStoredUser(this.userInfo)
      return data
    },

    /**
     * 退出登录。
     */
    async logout() {
      try {
        await logout()
      } catch {
        // 忽略退出接口异常，保证本地状态一定被清理
      } finally {
        this.resetState()
      }
    },

    /**
     * 重置本地登录状态。
     */
    resetState() {
      this.token = ''
      this.userInfo = {}
      this.roles = []
      removeToken()
      removeStoredUser()
    }
  }
})
