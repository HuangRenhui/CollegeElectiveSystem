<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="账号 / 操作 / 接口" clearable style="width: 220px" @keyup.enter="handleSearch" />
      <el-input v-model="query.module" placeholder="业务模块" clearable style="width: 160px" @keyup.enter="handleSearch" />
      <el-select v-model="query.success" placeholder="执行结果" clearable style="width: 140px" @change="handleSearch">
        <el-option label="成功" :value="1" />
        <el-option label="失败" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
      <div class="search-bar__spacer"></div>
      <el-button type="danger" plain :icon="Delete" @click="handleClean">清理 90 天前日志</el-button>
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>操作日志（{{ total }} 条）</span>
          <el-button link type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="username" label="操作人" width="110" />
        <el-table-column prop="module" label="模块" width="110" />
        <el-table-column prop="operation" label="操作" width="150" />
        <el-table-column prop="requestUri" label="请求地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="method" label="方法" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-danger': row.costTime > 1000 }">{{ row.costTime }} ms</span>
          </template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="165" />
        <el-table-column label="详情" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="日志详情" size="620px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="操作人">
          {{ current.username }}（ID: {{ current.userId || '-' }}）
        </el-descriptions-item>
        <el-descriptions-item label="业务模块">{{ current.module }}</el-descriptions-item>
        <el-descriptions-item label="操作描述">{{ current.operation }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ current.method }}</el-descriptions-item>
        <el-descriptions-item label="请求地址">{{ current.requestUri }}</el-descriptions-item>
        <el-descriptions-item label="操作 IP">{{ current.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ current.costTime }} ms</el-descriptions-item>
        <el-descriptions-item label="执行结果">
          <el-tag :type="current.success === 1 ? 'success' : 'danger'" size="small">
            {{ current.success === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ current.createTime }}</el-descriptions-item>
        <el-descriptions-item label="请求参数">
          <pre class="log-pre">{{ current.requestParam || '无' }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="current.errorMsg" label="异常信息">
          <pre class="log-pre text-danger">{{ current.errorMsg }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { pageLogs, cleanLogs } from '@/api/admin'

defineOptions({ name: 'AdminLogs' })

const loading = ref(false)
const detailVisible = ref(false)
const list = ref([])
const total = ref(0)
const current = ref({})

const query = reactive({ pageNum: 1, pageSize: 20, keyword: '', module: '', success: null })

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const { data } = await pageLogs(query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  Object.assign(query, { pageNum: 1, keyword: '', module: '', success: null })
  loadData()
}

function openDetail(row) {
  current.value = row
  detailVisible.value = true
}

function handleClean() {
  ElMessageBox.confirm('确定清理 90 天前的历史操作日志吗？该操作不可恢复。', '清理确认', { type: 'warning' })
    .then(async () => {
      await cleanLogs(90)
      ElMessage.success('日志清理任务已提交')
      loadData()
    })
    .catch(() => {})
}
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
}

.log-pre {
  max-height: 240px;
  margin: 0;
  overflow: auto;
  font-size: 12px;
  font-family: Consolas, Monaco, monospace;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
