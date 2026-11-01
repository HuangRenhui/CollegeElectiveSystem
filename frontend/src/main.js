import { createApp } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import ElementPlus from 'element-plus'

import App from './App.vue'
import router, { prefetchRouteComponents } from './router'
import './router/guard'
import pinia from './store'

import 'element-plus/dist/index.css'
import './styles/index.scss'

const app = createApp(App)

// ------------------------------ 全局异常兜底 ------------------------------
// 没有这些钩子时，组件渲染异常或未捕获的 Promise 拒绝只会让页面「白屏」，
// 控制台之外没有任何线索，线上排查只能靠用户描述。
function reportError(source, error, detail) {
  // 统一出口：后续接入前端监控（Sentry 等）只需改这一处
  console.error(`[${source}]`, error, detail || '')
}

app.config.errorHandler = (error, instance, info) => {
  reportError('vue', error, info)
}

window.addEventListener('unhandledrejection', (event) => {
  reportError('promise', event.reason)
})

window.addEventListener('error', (event) => {
  reportError('runtime', event.error || event.message, event.filename)
})

// 全局注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale: zhCn, size: 'default' })

app.mount('#app')

// 启动后预热各页面 chunk，避免首次点击左侧菜单时因按需加载而「看起来没反应」
prefetchRouteComponents()
