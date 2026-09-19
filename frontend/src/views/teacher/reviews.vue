<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.semesterId" placeholder="选择学期" clearable style="width: 230px" @change="loadSummary">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="loadSummary">查询</el-button>
      <div class="search-bar__spacer"></div>
      <el-tag type="info" effect="plain">共 {{ summaryList.length }} 门课程</el-tag>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="查看说明"
      description="此处仅展示学生已公开的评价（匿名）。评价结果用于教学改进，不参与任何考核排序。"
    />

    <el-row v-loading="loading" :gutter="16" class="mt-16">
      <el-col
        v-for="item in summaryList"
        :key="item.courseId"
        :xs="24"
        :sm="12"
        :lg="8"
        style="margin-bottom: 16px"
      >
        <el-card shadow="hover" class="summary-card">
          <div class="summary-card__header">
            <span class="summary-card__title" :title="item.courseName">{{ item.courseName }}</span>
            <el-tag :type="scoreTagType(item.averageScore)" size="small" effect="dark">
              {{ item.averageScore || 0 }} 分
            </el-tag>
          </div>
          <div class="summary-card__meta text-muted">
            {{ item.courseCode }} · {{ item.semesterName }}
          </div>

          <div class="summary-card__body">
            <div class="summary-card__row" v-for="dim in REVIEW_DIMENSIONS" :key="dim.key">
              <span>{{ dim.label }}</span>
              <el-rate :model-value="Number(item[dim.key]) || 0" disabled allow-half size="small" />
              <span class="summary-card__value">{{ item[dim.key] ?? '-' }}</span>
            </div>
          </div>

          <div class="summary-card__footer">
            <span class="text-muted">
              参评 {{ item.reviewCount || 0 }} / {{ item.studentCount || 0 }} 人
            </span>
            <el-button link type="primary" size="small" @click="openDetail(item)">查看评价</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && !summaryList.length" description="本学期暂无可查看的评价数据" />

    <!-- 评价明细 -->
    <el-dialog v-model="detailVisible" :title="`${current.courseName || ''} · 评价明细`" width="720px">
      <el-descriptions :column="3" border size="small" class="mb-16">
        <el-descriptions-item label="平均分">
          <el-tag :type="scoreTagType(current.averageScore)" effect="dark">{{ current.averageScore || 0 }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="已公开评价">{{ detailList.length }} 条</el-descriptions-item>
        <el-descriptions-item label="参评率">{{ participateRate }}%</el-descriptions-item>
      </el-descriptions>

      <el-empty v-if="!detailList.length" description="暂无学生公开评价" :image-size="80" />
      <div v-else class="review-list">
        <div v-for="review in detailList" :key="review.id" class="review-list__item">
          <div class="review-list__head">
            <span class="review-list__user">{{ review.anonymous === 1 ? '匿名同学' : maskName(review.studentName) }}</span>
            <el-rate :model-value="averageScore(review)" disabled allow-half size="small" />
            <span class="review-list__time text-muted">{{ review.submitTime }}</span>
          </div>
          <div class="review-list__content">{{ review.content || '（未填写文字评价）' }}</div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { listMyReviewSummary, getCourseReviews } from '@/api/teacher'
import { listSemesterOptions } from '@/api/common'
import { REVIEW_DIMENSIONS, averageScore, scoreTagType, maskName } from '@/utils/dict'

defineOptions({ name: 'TeacherReviews' })

const loading = ref(false)
const summaryList = ref([])
const semesters = ref([])
const query = reactive({ semesterId: null })

const detailVisible = ref(false)
const current = ref({})
const detailList = ref([])

const participateRate = computed(() => {
  const total = Number(current.value.studentCount || 0)
  if (!total) return 0
  return Math.round((detailList.value.length / total) * 100)
})

onMounted(async () => {
  try {
    const { data } = await listSemesterOptions()
    semesters.value = data || []
    const cur = (data || []).find((item) => item.isCurrent === 1)
    if (cur) {
      query.semesterId = cur.id
    }
  } catch {
    semesters.value = []
  }
  loadSummary()
})

async function loadSummary() {
  loading.value = true
  try {
    const { data } = await listMyReviewSummary(query)
    summaryList.value = data || []
  } catch {
    summaryList.value = []
  } finally {
    loading.value = false
  }
}

async function openDetail(item) {
  current.value = item
  detailVisible.value = true
  try {
    const { data } = await getCourseReviews(item.courseId)
    // 兼容两种返回：仅明细数组，或 { summary, reviews }
    detailList.value = Array.isArray(data) ? data : data?.reviews || []
    if (data && !Array.isArray(data) && data.summary) {
      current.value = { ...item, ...data.summary }
    }
  } catch {
    detailList.value = []
    ElMessage.error('评价明细加载失败')
  }
}
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
}

.summary-card {
  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
  }

  &__title {
    flex: 1;
    font-size: 15px;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__meta {
    margin-top: 5px;
    font-size: 12.5px;
  }

  &__body {
    margin-top: 12px;
    padding: 10px 12px;
    background: #f8f9fb;
    border-radius: 4px;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    font-size: 12.5px;
    color: #606266;
    min-height: 26px;
  }

  &__value {
    width: 30px;
    text-align: right;
    font-weight: 600;
    color: #303133;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
    padding-top: 10px;
    border-top: 1px solid #f0f0f0;
    font-size: 12.5px;
  }
}

.review-list {
  max-height: 420px;
  overflow-y: auto;

  &__item {
    padding: 12px 4px;
    border-bottom: 1px dashed #ebeef5;

    &:last-child {
      border-bottom: none;
    }
  }

  &__head {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  &__user {
    font-weight: 600;
    font-size: 13px;
  }

  &__time {
    margin-left: auto;
    font-size: 12px;
  }

  &__content {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.7;
    color: #606266;
    white-space: pre-wrap;
  }
}

.mt-16 {
  margin-top: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
