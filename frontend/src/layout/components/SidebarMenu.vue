<template>
  <div class="sidebar">
    <div class="sidebar__logo">
      <el-icon :size="26" color="#409eff"><School /></el-icon>
      <span v-show="!appStore.sidebarCollapsed" class="sidebar__title">选修课管理</span>
    </div>

    <el-scrollbar class="sidebar__scroll">
      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        router
      >
        <!-- 所有页面同级平铺（不分组），顺序与路由表一致 -->
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="item.icon" />
          </el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { constantRoutes } from '@/router'

const route = useRoute()
const appStore = useAppStore()
const userStore = useUserStore()

/** 当前高亮菜单 */
const activeMenu = computed(() => route.meta?.activeMenu || route.path)

/**
 * 侧边栏菜单项：把路由表「拍平」成一级列表。
 *
 * 说明：本项目页面数量不多，分组会导致「先展开分组、再点子项」两次点击，
 * 因此这里不再渲染 el-sub-menu，所有可见页面同级平铺。
 * 顺序即路由表顺序，公共信息在路由表末尾，故固定显示在菜单最下方。
 */
const menuItems = computed(() => {
  const role = userStore.role
  const result = []

  for (const parent of constantRoutes) {
    // 跳过隐藏路由、首页分组（首页与个人中心不在侧边栏展示）以及没有子路由的节点
    if (parent.meta?.hidden || parent.path === '/' || !Array.isArray(parent.children)) {
      continue
    }

    const parentRoles = parent.meta?.roles
    if (Array.isArray(parentRoles) && parentRoles.length > 0 && !parentRoles.includes(role)) {
      continue
    }

    for (const child of parent.children) {
      if (child.meta?.hidden) {
        continue
      }
      const roles = child.meta?.roles
      if (Array.isArray(roles) && roles.length > 0 && !roles.includes(role)) {
        continue
      }
      result.push({
        path: resolvePath(parent.path, child.path),
        title: child.meta?.title,
        icon: child.meta?.icon || parent.meta?.icon || 'Menu'
      })
    }
  }

  return result
})

function resolvePath(parent, child) {
  if (!child) {
    return parent
  }
  if (child.startsWith('/')) {
    return child
  }
  return `${parent.replace(/\/$/, '')}/${child}`
}
</script>

<style lang="scss" scoped>
.sidebar {
  display: flex;
  flex-direction: column;
  height: 100%;

  &__logo {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    height: $header-height;
    flex-shrink: 0;
    color: #fff;
    background: #172432;
    white-space: nowrap;
    overflow: hidden;
  }

  &__title {
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 1px;
  }

  &__scroll {
    flex: 1;
    overflow-x: hidden;
  }

  :deep(.el-menu) {
    border-right: none;
  }

  :deep(.el-menu-item.is-active) {
    background: $primary-color !important;
  }

  :deep(.el-menu-item:hover),
  :deep(.el-sub-menu__title:hover) {
    background: #172432 !important;
  }
}
</style>
