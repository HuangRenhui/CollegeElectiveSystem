<template>
  <el-container class="layout">
    <el-aside :width="appStore.sidebarCollapsed ? '64px' : '220px'" class="layout__aside">
      <SidebarMenu />
    </el-aside>

    <el-container class="layout__main">
      <el-header class="layout__header">
        <NavBar />
      </el-header>

      <el-main class="layout__content">
        <router-view v-slot="{ Component, route }">
          <transition name="fade-transform" mode="out-in">
            <keep-alive :max="10">
              <component :is="Component" :key="route.path" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { onMounted } from 'vue'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { getCurrentSemester, listNotices } from '@/api/common'
import SidebarMenu from './components/SidebarMenu.vue'
import NavBar from './components/NavBar.vue'

const appStore = useAppStore()
const userStore = useUserStore()

onMounted(async () => {
  try {
    const { data } = await getCurrentSemester()
    appStore.setCurrentSemester(data)
  } catch {
    // 忽略：可能尚未设置当前学期
  }

  // 先确定「已读记录」的归属用户，再写入公告列表（setNotices 会按列表裁剪已读记录）
  appStore.initNoticeRead(userStore.userInfo?.userId ?? userStore.userInfo?.username)

  try {
    // limit 取后端允许的最大值（50），保证铃铛未读角标与「公共信息」页面看到的是同一份全量列表
    const { data } = await listNotices({ limit: 50 })
    appStore.setNotices(data)
  } catch {
    // 忽略公告加载失败
  }
})
</script>

<style lang="scss" scoped>
.layout {
  height: 100%;

  &__aside {
    background: $sidebar-bg;
    transition: width 0.28s;
    overflow: hidden;
  }

  &__main {
    min-width: 0;
    background: $page-bg;
  }

  &__header {
    height: $header-height;
    padding: 0;
    background: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
    z-index: 10;
  }

  &__content {
    padding: 0;
    overflow-y: auto;
    background: $page-bg;
  }
}
</style>
