import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import router from '@/router'
import { useUserStore } from '@/store/modules/user'

NProgress.configure({ showSpinner: false, minimum: 0.15 })

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json;charset=UTF-8' }
})

/** 是否已弹出登录失效提示，避免并发请求重复弹窗 */
let isReloginPrompted = false

// ------------------------------ 请求拦截 ------------------------------
service.interceptors.request.use(
  (config) => {
    NProgress.start()
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    NProgress.done()
    return Promise.reject(error)
  }
)

// ------------------------------ 响应拦截 ------------------------------
service.interceptors.response.use(
  (response) => {
    NProgress.done()
    const { data } = response

    // 二进制流（文件下载）直接返回
    if (response.config.responseType === 'blob') {
      return response
    }

    if (!data || typeof data !== 'object') {
      return data
    }

    if (data.code === 200) {
      return data
    }

    // 未登录 / 登录过期
    if (data.code === 401 || data.code === 2003) {
      handleUnauthorized(data.message)
      return Promise.reject(new Error(data.message || '登录状态已失效'))
    }

    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    NProgress.done()
    const status = error.response?.status

    if (status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    if (status === 403) {
      ElMessage.error('没有操作权限，请联系管理员')
      return Promise.reject(error)
    }
    if (status === 404) {
      ElMessage.error('请求的接口不存在')
      return Promise.reject(error)
    }
    if (status === 500) {
      ElMessage.error('服务器内部错误，请联系管理员')
      return Promise.reject(error)
    }

    const message = error.code === 'ECONNABORTED'
      ? '请求超时，请稍后重试'
      : error.message || '网络异常，请检查网络连接'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

/**
 * 统一处理登录失效：提示后跳转登录页。
 */
function handleUnauthorized(message) {
  if (isReloginPrompted) {
    return
  }
  isReloginPrompted = true
  ElMessageBox.confirm(message || '登录状态已失效，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  })
    .then(() => {
      const userStore = useUserStore()
      userStore.resetState()
      redirectToLogin()
    })
    .catch(() => {
      const userStore = useUserStore()
      userStore.resetState()
      redirectToLogin()
    })
    .finally(() => {
      isReloginPrompted = false
    })
}

function redirectToLogin() {
  const currentPath = router.currentRoute.value.fullPath
  router.replace({
    path: '/login',
    query: currentPath && currentPath !== '/login' ? { redirect: currentPath } : {}
  })
}

export default service
