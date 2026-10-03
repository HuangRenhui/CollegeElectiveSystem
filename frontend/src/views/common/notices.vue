<template>
  <div class="page-container">
    <!-- 概览 + 操作 -->
    <div class="search-bar notice-overview">
      <div>
        <div class="notice-overview__title">公共信息</div>
        <div class="text-muted">
          共 {{ appStore.notices.length }} 条可见公告，其中未读
          <span :class="{ 'text-danger': appStore.unreadNoticeCount > 0 }">
            {{ appStore.unreadNoticeCount }}
          </span>
          条
        </div>
      </div>
      <div class="notice-overview__actions">
        <el-button :icon="Refresh" circle :loading="loading" @click="loadNotices" />
        <el-button
          type="primary"
          plain
          :disabled="!appStore.unreadNoticeCount"
          @click="markAllRead"
        >
          全部标为已读
        </el-button>
      </div>
    </div>

    <!-- 筛选：阅读状态 / 类型 / 标题关键字 -->
    <div class="search-bar">
      <el-radio-group v-model="readFilter">
        <el-radio-button value="all">全部公告</el-radio-button>
        <el-radio-button value="unread">仅未读</el-radio-button>
      </el-radio-group>

      <el-select v-model="typeFilter" placeholder="公告类型" clearable style="width: 150px">
        <el-option
          v-for="item in NOTICE_TYPE_OPTIONS"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>

      <el-input
        v-model="keyword"
        placeholder="搜索公告标题"
        clearable
        :prefix-icon="Search"
        style="width: 220px"
      />
    </div>

    <!-- 公告列表 -->
    <div v-loading="loading" class="page-card notice-card">
      <el-empty v-if="!filteredNotices.length" description="暂无公告" />
      <ul v-else class="notice-list">
        <li
          v-for="item in filteredNotices"
          :key="item.id"
          class="notice-item"
          :class="{ 'is-read': appStore.isNoticeRead(item.id) }"
          @click="openDetail(item)"
        >
          <div class="notice-item__main">
            <span
              class="notice-item__dot"
              :class="{ 'is-unread': !appStore.isNoticeRead(item.id) }"
            />
            <el-tag v-if="item.topFlag === 1" size="small" type="danger" effect="plain">置顶</el-tag>
            <el-tag size="small" :type="noticeTypeTag(item.noticeType)" effect="plain">
              {{ noticeTypeText(item.noticeType) }}
            </el-tag>
            <span class="notice-item__title">{{ item.title }}</span>
            <el-tag
              size="small"
              :type="appStore.isNoticeRead(item.id) ? 'info' : 'danger'"
              effect="plain"
            >
              {{ appStore.isNoticeRead(item.id) ? '已读' : '未读' }}
            </el-tag>
          </div>

          <div class="notice-item__meta text-muted">
            <span>{{ item.publisher || '系统' }}</span>
            <span>{{ formatTime(item.publishTime) }}</span>
            <span>浏览 {{ item.viewCount || 0 }}</span>
          </div>
        </li>
      </ul>
    </div>

    <!-- 公告详情 -->
    <el-dialog v-model="detailVisible" :title="detail.title" width="680px">
      <div class="notice-detail__meta">
        <el-tag size="small" :type="noticeTypeTag(detail.noticeType)" effect="plain">
          {{ noticeTypeText(detail.noticeType) }}
        </el-tag>
        <span class="text-muted">
          {{ detail.publisher || '系统' }} · {{ formatTime(detail.publishTime) }} · 浏览
          {{ detail.viewCount || 0 }}
        </span>
      </div>
      <div class="notice-detail__content">{{ detail.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import { useAppStore } from '@/store/modules/app'
import { getNoticeDetail, listNotices } from '@/api/common'
import { NOTICE_TYPE_OPTIONS, noticeTypeTag, noticeTypeText } from '@/utils/dict'

const appStore = useAppStore()

const loading = ref(false)
const readFilter = ref('all')
const typeFilter = ref('')
const keyword = ref('')

const detailVisible = ref(false)
const detail = ref({})

/** 前端二次筛选：阅读状态 + 公告类型 + 标题关键字 */
const filteredNotices = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return appStore.notices.filter((item) => {
    if (readFilter.value === 'unread' && appStore.isNoticeRead(item.id)) {
      return false
    }
    if (typeFilter.value && item.noticeType !== typeFilter.value) {
      return false
    }
    if (kw && !String(item.title || '').toLowerCase().includes(kw)) {
      return false
    }
    return true
  })
})

onMounted(loadNotices)

/**
 * 拉取全部可见公告并写回 store。
 * 与顶部铃铛共用同一份数据，因此这里刷新后角标也会同步。
 */
async function loadNotices() {
  loading.value = true
  try {
    const { data } = await listNotices({ limit: 50 })
    appStore.setNotices(data)
  } finally {
    loading.value = false
  }
}

/**
 * 打开公告详情：先落地「已读」（角标与列表状态立即更新），再拉取详情。
 * 详情接口会累加浏览量；失败时降级展示列表里已有的内容。
 */
async function openDetail(item) {
  appStore.markNoticeRead(item.id)
  detail.value = { ...item }
  detailVisible.value = true

  try {
    const { data } = await getNoticeDetail(item.id)
    if (data) {
      detail.value = data
    }
  } catch {
    // 忽略：列表数据已可展示
  }
}

function markAllRead() {
  appStore.markAllNoticesRead()
}

function formatTime(value) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-'
}
</script>

<style lang="scss" scoped>
.notice-overview {
  justify-content: space-between;

  &__title {
    margin-bottom: 4px;
    font-size: 16px;
    font-weight: 600;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.notice-card {
  padding: 8px 16px;
}

.notice-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.notice-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 4px;
  border-bottom: 1px dashed #f0f0f0;
  cursor: pointer;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: #f5f7fa;
  }

  // 已读条目弱化标题，方便一眼扫出未读
  &.is-read .notice-item__title {
    color: #909399;
  }

  &__main {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  &__title {
    overflow: hidden;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    display: flex;
    flex-shrink: 0;
    gap: 14px;
    font-size: 12px;
  }

  &__dot {
    flex-shrink: 0;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: transparent;

    &.is-unread {
      background: $danger-color;
    }
  }
}

.notice-detail {
  &__meta {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 12px;
    font-size: 13px;
  }

  &__content {
    line-height: 1.9;
    white-space: pre-wrap;
  }
}
</style>
