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
import { getCurrentSemester, listNotices } from '@/api/common'
import SidebarMenu from './components/SidebarMenu.vue'
import NavBar from './components/NavBar.vue'

const appStore = useAppStore()

onMounted(async () => {
  try {
    const { data } = await getCurrentSemester()
    appStore.setCurrentSemester(data)
  } catch {
    // 忽略：可能尚未设置当前学期
  }
  try {
    const { data } = await listNotices({ limit: 10 })
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
