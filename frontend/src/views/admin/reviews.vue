<template>
  <div class="page-container">
    <!-- 统计概览 -->
    <el-row v-loading="statLoading" :gutter="16" class="stat-row">
      <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-icon :size="30" :color="item.color"><component :is="item.icon" /></el-icon>
          <div class="stat-card__body">
            <div class="stat-card__value">{{ item.value }}</div>
            <div class="stat-card__label">{{ item.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="search-bar mt-16">
      <el-input v-model="query.keyword" placeholder="课程名称 / 编号 / 教师" clearable style="width: 210px" @keyup.enter="handleSearch" />
      <el-select v-model="query.semesterId" placeholder="学期" clearable style="width: 210px" @change="handleSearch">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="handleSearch">
        <el-option v-for="item in REVIEW_STATUS_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-select v-model="query.scoreLevel" placeholder="综合评分" clearable style="width: 150px" @change="handleSearch">
        <el-option v-for="item in SCORE_LEVEL_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card shadow="never">
      <div class="table-toolbar">
        <span class="text-muted">
          共 {{ total }} 条评价。管理员可隐藏违规内容或删除；删除后学生需重新填写。
        </span>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="courseCode" label="课程编号" width="110" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column label="评价人" width="130">
          <template #default="{ row }">
            <span v-if="row.anonymous === 1">匿名同学</span>
            <span v-else>{{ row.studentName }}（{{ row.stuNo }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="综合评分" width="170">
          <template #default="{ row }">
            <el-rate :model-value="averageScore(row)" disabled allow-half size="small" />
            <span class="score-text">{{ averageScore(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="评价内容" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '（未填写文字评价）' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="reviewStatusTag(row.status)" size="small">{{ reviewStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="165" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-button
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)"
            >
              {{ row.status === 1 ? '隐藏' : '公开' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 评价详情 -->
    <el-dialog v-model="detailVisible" title="评价详情" width="620px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="课程">{{ detail.courseCode }} {{ detail.courseName }}</el-descriptions-item>
        <el-descriptions-item label="授课教师">{{ detail.teacherName || '待定' }}</el-descriptions-item>
        <el-descriptions-item label="评价人">
          {{ detail.anonymous === 1 ? '匿名同学' : `${detail.studentName}（${detail.stuNo}）` }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="reviewStatusTag(detail.status)" size="small">{{ reviewStatusText(detail.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-for="dim in REVIEW_DIMENSIONS" :key="dim.key" :label="dim.label">
          {{ detail[dim.key] ?? '-' }} 分
        </el-descriptions-item>
        <el-descriptions-item label="综合评分" :span="2">
          {{ averageScore(detail) }} 分
        </el-descriptions-item>
        <el-descriptions-item label="评价内容" :span="2">
          <div style="white-space: pre-wrap; line-height: 1.7">{{ detail.content || '（未填写文字评价）' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ detail.submitTime }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageReviews, updateReviewStatus, deleteReview, getReviewStatistics } from '@/api/admin'
import { listSemesterOptions } from '@/api/common'
import {
  REVIEW_STATUS_OPTIONS, REVIEW_DIMENSIONS,
  reviewStatusText, reviewStatusTag, averageScore
} from '@/utils/dict'

defineOptions({ name: 'AdminReviews' })

const SCORE_LEVEL_OPTIONS = [
  { value: 'high', label: '高分（≥4.5）' },
  { value: 'good', label: '良好（3.5-4.4）' },
  { value: 'normal', label: '一般（2.5-3.4）' },
  { value: 'low', label: '偏低（<2.5）' }
]

const loading = ref(false)
const statLoading = ref(false)
const list = ref([])
const total = ref(0)
const semesters = ref([])
const statistics = reactive({})

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  semesterId: null,
  status: null,
  scoreLevel: null
})

const detailVisible = ref(false)
const detail = ref({})

const statCards = computed(() => [
  { label: '评价总数', value: statistics.reviewCount ?? 0, icon: 'ChatDotSquare', color: '#409eff' },
  { label: '已公开', value: statistics.publishedCount ?? 0, icon: 'View', color: '#67c23a' },
  { label: '已隐藏', value: statistics.hiddenCount ?? 0, icon: 'Hide', color: '#f56c6c' },
  { label: '平均分', value: statistics.averageScore ?? 0, icon: 'Star', color: '#e6a23c' },
  { label: '参评率', value: `${statistics.participateRate ?? 0}%`, icon: 'DataLine', color: '#8e44ad' },
  { label: '覆盖课程', value: statistics.courseCount ?? 0, icon: 'Notebook', color: '#909399' }
])

onMounted(async () => {
  try {
    const { data } = await listSemesterOptions()
    semesters.value = data || []
    const current = (data || []).find((item) => item.isCurrent === 1)
    if (current) {
      query.semesterId = current.id
    }
  } catch {
    semesters.value = []
  }
  loadAll()
})

function loadAll() {
  loadData()
  loadStatistics()
}

async function loadData() {
  loading.value = true
  try {
    const { data } = await pageReviews(query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadStatistics() {
  statLoading.value = true
  try {
    const { data } = await getReviewStatistics({ semesterId: query.semesterId })
    Object.assign(statistics, data || {})
  } catch {
    Object.assign(statistics, {})
  } finally {
    statLoading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  Object.assign(query, { pageNum: 1, keyword: '', semesterId: null, status: null, scoreLevel: null })
  loadAll()
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 3 : 1
  const { value } = await ElMessageBox.prompt(
    next === 3
      ? `隐藏后学生端与教师端将不再展示该评价，并记录操作原因。请填写隐藏原因：`
      : '公开后该评价将在教师评价页对学生与教师可见。请填写恢复原因：',
    next === 3 ? '隐藏评价' : '公开评价',
    {
      inputPlaceholder: '不少于 2 个字符',
      inputValidator: (val) => (val && val.trim().length >= 2 ? true : '原因不少于 2 个字符'),
      type: 'warning'
    }
  )
  await updateReviewStatus(row.id, next, value.trim())
  ElMessage.success(next === 3 ? '评价已隐藏' : '评价已公开')
  loadData()
}

async function handleDelete(row) {
  const { value } = await ElMessageBox.prompt(
    `删除后学生需重新填写《${row.courseName}》的评价，且不可恢复。请填写删除原因：`,
    '删除评价',
    {
      inputPlaceholder: '不少于 2 个字符',
      inputValidator: (val) => (val && val.trim().length >= 2 ? true : '原因不少于 2 个字符'),
      type: 'warning',
      confirmButtonText: '确定删除'
    }
  )
  await deleteReview(row.id, value.trim())
  ElMessage.success('评价已删除')
  loadAll()
}
</script>

<style lang="scss" scoped>
.stat-row {
  margin-bottom: 0;
}

.stat-card {
  display: flex;
  align-items: center;

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 12px;
    width: 100%;
  }

  &__value {
    font-size: 21px;
    font-weight: 700;
    line-height: 1.2;
    color: #1f2d3d;
  }

  &__label {
    font-size: 12.5px;
    color: #909399;
  }
}

.score-text {
  margin-left: 8px;
  font-weight: 600;
  color: #e6a23c;
}

.mt-16 {
  margin-top: 16px;
}
</style>
