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
        :unique-opened="true"
        background-color="#1f2d3d"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        router
      >
        <template v-for="item in menuRoutes" :key="item.path">
          <!-- 单个子菜单：直接渲染为菜单项 -->
          <el-menu-item v-if="item.children.length === 1" :index="resolvePath(item.path, item.children[0].path)">
            <el-icon>
              <component :is="item.children[0].meta.icon || item.meta.icon || 'Menu'" />
            </el-icon>
            <template #title>{{ item.children[0].meta.title }}</template>
          </el-menu-item>

          <!-- 多个子菜单：渲染为折叠组 -->
          <el-sub-menu v-else :index="item.path">
            <template #title>
              <el-icon>
                <component :is="item.meta.icon || 'Menu'" />
              </el-icon>
              <span>{{ item.meta.title }}</span>
            </template>
            <el-menu-item
              v-for="child in item.children"
              :key="child.path"
              :index="resolvePath(item.path, child.path)"
            >
              <el-icon>
                <component :is="child.meta.icon || 'Menu'" />
              </el-icon>
              <template #title>{{ child.meta.title }}</template>
            </el-menu-item>
          </el-sub-menu>
        </template>
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
 * 依据角色过滤出可见的菜单路由。
 */
const menuRoutes = computed(() => {
  const role = userStore.role
  const result = []

  for (const item of constantRoutes) {
    if (item.meta?.hidden || !item.children || item.children.length === 0) {
      continue
    }
    if (item.path === '/') {
      continue
    }

    const children = item.children.filter((child) => {
      if (child.meta?.hidden) {
        return false
      }
      const roles = child.meta?.roles
      return !Array.isArray(roles) || roles.length === 0 || roles.includes(role)
    })

    if (children.length === 0) {
      continue
    }

    const parentRoles = item.meta?.roles
    if (Array.isArray(parentRoles) && parentRoles.length > 0 && !parentRoles.includes(role)) {
      continue
    }

    result.push({ ...item, children })
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
