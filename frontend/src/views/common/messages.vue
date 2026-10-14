<template>
  <div class="messages-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>消息中心</span>
          <div class="card-header__actions">
            <el-tag v-if="unreadCount > 0" type="danger" effect="plain" size="small">
              {{ unreadCount }} 条未读
            </el-tag>
            <el-button
              link
              type="primary"
              :disabled="!unreadCount"
              @click="handleReadAll"
            >
              全部已读
            </el-button>
          </div>
        </div>
      </template>

      <div class="toolbar">
        <el-radio-group v-model="readFilter" @change="handleFilterChange">
          <el-radio-button value="ALL">全部</el-radio-button>
          <el-radio-button value="UNREAD">未读</el-radio-button>
          <el-radio-button value="READ">已读</el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="message-list">
        <div
          v-for="item in list"
          :key="item.id"
          class="message-item"
          :class="{ 'message-item--unread': item.readFlag === 0 }"
          @click="handleOpen(item)"
        >
          <div class="message-item__dot" v-if="item.readFlag === 0" />
          <div class="message-item__body">
            <div class="message-item__title">
              {{ item.title }}
              <el-tag :type="msgTypeTag(item.msgType)" size="small" effect="plain">
                {{ msgTypeText(item.msgType) }}
              </el-tag>
            </div>
            <div class="message-item__content">{{ item.content }}</div>
            <div class="message-item__time">{{ formatTime(item.createTime) }}</div>
          </div>
        </div>

        <el-empty v-if="!loading && !list.length" description="暂无消息" />
      </div>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMyMessages, countMyUnread, markMessageRead, markAllMessagesRead } from '@/api/message'

defineOptions({ name: 'MyMessages' })

const loading = ref(false)
const list = ref([])
const total = ref(0)
const unreadCount = ref(0)

const query = reactive({ pageNum: 1, pageSize: 10 })
/**
 * 已读筛选。
 *
 * <p>用字符串字面量而不用 null/0/1：Element Plus 的 RadioButton 内部取值为
 * {@code props.value ?? props.label}，传 {@code null} 会回退到未定义的 label，
 * DOM 上的 value 变成字符串 "undefined"，导致「全部」永远无法选中。
 * 统一用字符串哨兵值，在请求时再映射为后端的 null/0/1。</p>
 */
const readFilter = ref('ALL')

onMounted(() => {
  loadData()
  loadUnread()
})

async function loadData() {
  loading.value = true
  try {
    const { data } = await listMyMessages({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      readFlag: toReadFlagParam(readFilter.value)
    })
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/**
 * 把界面上的哨兵值映射为后端参数：ALL → null（不筛选），UNREAD → 0，READ → 1。
 */
function toReadFlagParam(filter) {
  if (filter === 'UNREAD') return 0
  if (filter === 'READ') return 1
  return null
}

async function loadUnread() {
  try {
    const { data } = await countMyUnread()
    unreadCount.value = Number(data || 0)
  } catch {
    unreadCount.value = 0
  }
}

function handleFilterChange() {
  query.pageNum = 1
  loadData()
}

/** 点击消息：标记已读，并同步刷新未读数 */
async function handleOpen(item) {
  if (item.readFlag === 1) return
  try {
    await markMessageRead(item.id)
    item.readFlag = 1
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  } catch {
    // 标记失败不阻塞阅读，静默即可
  }
}

async function handleReadAll() {
  try {
    await markAllMessagesRead()
    ElMessage.success('已全部标记为已读')
    unreadCount.value = 0
    loadData()
  } catch {
    // 错误已由拦截器提示
  }
}

/** 后端返回 "yyyy-MM-dd HH:mm:ss" 或 LocalDateTime 序列化字符串，直接展示 */
function formatTime(time) {
  return time ? String(time).replace('T', ' ') : ''
}

function msgTypeText(type) {
  const map = {
    SYSTEM: '系统',
    SELECTION: '选课',
    GRADE: '成绩',
    REVIEW: '评价'
  }
  return map[type] || '系统'
}

function msgTypeTag(type) {
  const map = {
    SELECTION: 'success',
    GRADE: 'warning',
    REVIEW: 'primary',
    SYSTEM: 'info'
  }
  return map[type] || 'info'
}
</script>

<style lang="scss" scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  &__actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.toolbar {
  margin-bottom: 12px;
}

.message-list {
  min-height: 200px;
}

.message-item {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;

  &:hover {
    background: #fafafa;
  }

  &--unread {
    .message-item__title {
      font-weight: 600;
    }
  }

  &__dot {
    flex-shrink: 0;
    width: 8px;
    height: 8px;
    margin-top: 6px;
    border-radius: 50%;
    background: #f56c6c;
  }

  &__body {
    flex: 1;
    min-width: 0;
  }

  &__title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    color: #1f2d3d;
  }

  &__content {
    margin-top: 4px;
    font-size: 12.5px;
    line-height: 1.6;
    color: #606266;
    word-break: break-all;
  }

  &__time {
    margin-top: 4px;
    font-size: 12px;
    color: #909399;
  }
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
