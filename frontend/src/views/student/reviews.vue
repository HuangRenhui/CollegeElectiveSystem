<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.semesterId" placeholder="选择学期" clearable style="width: 230px" @change="loadAll">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="loadAll">刷新</el-button>
      <div class="search-bar__spacer"></div>
      <el-tag type="warning" effect="plain">待评价 {{ pendingList.length }} 门</el-tag>
      <el-tag type="success" effect="plain">已评价 {{ myReviews.length }} 门</el-tag>
    </div>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="评价说明"
      description="仅可评价本人已修完的课程；每位学生每门课程仅一份评价，公开前可随时修改。评价对学生之间匿名，请客观真实填写。"
    />

    <el-tabs v-model="activeTab" class="mt-16">
      <!-- 待评价 -->
      <el-tab-pane :label="`待评价课程（${pendingList.length}）`" name="pending">
        <el-row v-loading="loading" :gutter="16">
          <el-col
            v-for="course in pendingList"
            :key="course.courseId"
            :xs="24"
            :sm="12"
            :lg="8"
            style="margin-bottom: 16px"
          >
            <el-card shadow="hover" class="review-card">
              <div class="review-card__header">
                <span class="review-card__title" :title="course.courseName">{{ course.courseName }}</span>
                <el-tag type="warning" size="small" effect="dark">待评价</el-tag>
              </div>
              <div class="review-card__meta">
                {{ course.courseCode }} · {{ course.credit }} 学分 · {{ course.teacherName || '待定' }}
              </div>
              <div class="review-card__meta text-muted">
                {{ course.semesterName }} · 总评 {{ course.totalScore ?? '-' }}
              </div>
              <div class="review-card__footer">
                <el-button type="primary" size="small" @click="openForm(course)">
                  <el-icon><EditPen /></el-icon>去评价
                </el-button>
              </div>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="!loading && !pendingList.length" description="暂无待评价课程，感谢你的反馈~" />
      </el-tab-pane>

      <!-- 我的评价 -->
      <el-tab-pane :label="`我的评价（${myReviews.length}）`" name="mine">
        <el-card shadow="never" v-loading="loading">
          <el-table :data="myReviews" stripe empty-text="暂无评价记录">
            <el-table-column prop="courseCode" label="课程编号" width="120" />
            <el-table-column prop="courseName" label="课程名称" min-width="170" show-overflow-tooltip />
            <el-table-column prop="teacherName" label="授课教师" width="110" />
            <el-table-column label="综合评分" width="180">
              <template #default="{ row }">
                <el-rate :model-value="averageScore(row)" disabled allow-half size="small" />
                <span class="score-text">{{ averageScore(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="评价内容" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                {{ row.content || '（未填写文字评价）' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="reviewStatusTag(row.status)" size="small">
                  {{ reviewStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="165" />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" :disabled="row.status === 3" @click="openForm(row, true)">
                  修改
                </el-button>
                <el-button link type="danger" @click="handleDelete(row)">撤回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 评价表单 -->
    <el-dialog v-model="formVisible" :title="formTitle" width="640px" :close-on-click-modal="false">
      <el-alert
        v-if="form.status === 3"
        type="error"
        :closable="false"
        show-icon
        title="该评价已被管理员隐藏，暂时无法修改"
        style="margin-bottom: 16px"
      />

      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-descriptions :column="2" border size="small" class="mb-16">
          <el-descriptions-item label="课程">{{ form.courseCode }} {{ form.courseName }}</el-descriptions-item>
          <el-descriptions-item label="教师">{{ form.teacherName || '待定' }}</el-descriptions-item>
        </el-descriptions>

        <el-form-item
          v-for="dim in REVIEW_DIMENSIONS"
          :key="dim.key"
          :label="dim.label"
          :prop="dim.key"
        >
          <el-rate v-model="form[dim.key]" :max="5" allow-half show-score score-template="{value} 分" />
          <span class="form-tip">{{ dim.tip }}</span>
        </el-form-item>

        <el-form-item label="综合评分">
          <el-tag :type="scoreTagType(overallScore)" effect="dark" size="large">{{ overallScore }} 分</el-tag>
          <span class="form-tip">由以上四个维度自动计算</span>
        </el-form-item>

        <el-form-item label="文字评价" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="可从教学组织、课堂氛围、作业与考核、收获与建议等方面填写（5-500 字）"
          />
        </el-form-item>

        <el-form-item label="匿名提交">
          <el-switch v-model="form.anonymous" :active-value="1" :inactive-value="0" />
          <span class="form-tip">开启后，教师与同学看到的评价将隐藏你的姓名</span>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">提交评价</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listPendingReviews, listMyReviews, submitReview, deleteMyReview } from '@/api/student'
import { listSemesterOptions } from '@/api/common'
import {
  REVIEW_DIMENSIONS, reviewStatusText, reviewStatusTag, averageScore, scoreTagType
} from '@/utils/dict'

defineOptions({ name: 'StudentReviews' })

const loading = ref(false)
const submitting = ref(false)
const activeTab = ref('pending')
const semesters = ref([])
const pendingList = ref([])
const myReviews = ref([])

const query = reactive({ semesterId: null })

// ---------------------------- 表单 ----------------------------
const formVisible = ref(false)
const formRef = ref()
const form = reactive(emptyForm())

const rules = {
  scoreContent: [{ required: true, message: '请为教学内容打分', trigger: 'change' }],
  scoreTeaching: [{ required: true, message: '请为教学方法打分', trigger: 'change' }],
  scoreAttitude: [{ required: true, message: '请为教学态度打分', trigger: 'change' }],
  scoreGain: [{ required: true, message: '请为学习收获打分', trigger: 'change' }],
  content: [
    { required: true, message: '请填写文字评价', trigger: 'blur' },
    { min: 5, max: 500, message: '文字评价需 5-500 字', trigger: 'blur' }
  ]
}

function emptyForm() {
  return {
    id: null,
    courseId: null,
    courseCode: '',
    courseName: '',
    teacherName: '',
    scoreContent: 0,
    scoreTeaching: 0,
    scoreAttitude: 0,
    scoreGain: 0,
    content: '',
    anonymous: 1,
    status: 1
  }
}

const formTitle = computed(() => (form.id ? '修改课程评价' : '提交课程评价'))

const overallScore = computed(() => {
  const values = REVIEW_DIMENSIONS.map((item) => Number(form[item.key])).filter((v) => v > 0)
  if (!values.length) return 0
  return Number((values.reduce((sum, v) => sum + v, 0) / values.length).toFixed(1))
})

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

async function loadAll() {
  await Promise.all([loadPending(), loadMine()])
}

async function loadPending() {
  loading.value = true
  try {
    const { data } = await listPendingReviews(query)
    pendingList.value = data || []
  } catch {
    pendingList.value = []
  } finally {
    loading.value = false
  }
}

async function loadMine() {
  try {
    const { data } = await listMyReviews(query)
    myReviews.value = data || []
  } catch {
    myReviews.value = []
  }
}

/**
 * 打开评价表单。
 * @param {object} row 待评价课程或已有评价记录
 */
function openForm(row, isEdit = false) {
  const source = isEdit
    ? { ...row, courseId: row.courseId }
    : { ...row, id: null, content: '', anonymous: 1, status: 1 }

  Object.assign(form, emptyForm(), {
    id: isEdit ? row.id : null,
    courseId: source.courseId,
    courseCode: source.courseCode,
    courseName: source.courseName,
    teacherName: source.teacherName,
    scoreContent: Number(source.scoreContent) || 0,
    scoreTeaching: Number(source.scoreTeaching) || 0,
    scoreAttitude: Number(source.scoreAttitude) || 0,
    scoreGain: Number(source.scoreGain) || 0,
    content: source.content || '',
    anonymous: source.anonymous === 0 ? 0 : 1,
    status: source.status ?? 1
  })
  formVisible.value = true
  formRef.value?.clearValidate()
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await submitReview({
      id: form.id,
      courseId: form.courseId,
      scoreContent: form.scoreContent,
      scoreTeaching: form.scoreTeaching,
      scoreAttitude: form.scoreAttitude,
      scoreGain: form.scoreGain,
      content: form.content,
      anonymous: form.anonymous
    })
    ElMessage.success(form.id ? '评价已更新' : '评价提交成功，感谢你的反馈')
    formVisible.value = false
    activeTab.value = 'mine'
    await loadAll()
  } catch {
    // 错误已由拦截器提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定撤回对《${row.courseName}》的评价吗？撤回后需重新填写。`,
    '撤回确认',
    { type: 'warning', confirmButtonText: '确定撤回' }
  )
  await deleteMyReview(row.id)
  ElMessage.success('评价已撤回')
  await loadAll()
}
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
}

.review-card {
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
    margin-top: 8px;
    font-size: 12.5px;
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }
}

.score-text {
  margin-left: 8px;
  font-weight: 600;
  color: #e6a23c;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
}

.mt-16 {
  margin-top: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
