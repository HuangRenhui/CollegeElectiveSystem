import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import router from './index'
import { useUserStore } from '@/store/modules/user'
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false, minimum: 0.15 })

/** 免登录白名单 */
const WHITE_LIST = ['/login', '/404', '/403']

/** 应用标题 */
const APP_TITLE = import.meta.env.VITE_APP_TITLE || '高校选修课管理系统'

/**
 * 全局前置守卫：登录校验 + 角色权限校验。
 */
router.beforeEach(async (to, from, next) => {
  NProgress.start()
  document.title = to.meta?.title ? `${to.meta.title} - ${APP_TITLE}` : APP_TITLE

  const token = getToken()

  // 未登录
  if (!token) {
    if (WHITE_LIST.includes(to.path)) {
      next()
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
    NProgress.done()
    return
  }

  // 已登录访问登录页 → 跳转首页
  if (to.path === '/login') {
    const userStore = useUserStore()
    next(userStore.homePath)
    NProgress.done()
    return
  }

  const userStore = useUserStore()

  // 刷新页面后，state 被重置，需要重新拉取用户信息
  if (!userStore.userInfo?.role) {
    try {
      await userStore.fetchUserInfo()
    } catch {
      userStore.resetState()
      next({ path: '/login', query: { redirect: to.fullPath } })
      NProgress.done()
      return
    }
  }

  // 角色权限校验：取路由链上最靠下的 roles 配置
  const requiredRoles = resolveRequiredRoles(to)
  if (requiredRoles.length > 0 && !requiredRoles.includes(userStore.role)) {
    next('/403')
    NProgress.done()
    return
  }

  next()
})

router.afterEach(() => {
  NProgress.done()
})

/**
 * 从匹配链中解析出需要校验的角色数组。
 */
function resolveRequiredRoles(route) {
  const records = route.matched || []
  for (let i = records.length - 1; i >= 0; i--) {
    const roles = records[i].meta?.roles
    if (Array.isArray(roles) && roles.length > 0) {
      return roles
    }
  }
  return []
}
