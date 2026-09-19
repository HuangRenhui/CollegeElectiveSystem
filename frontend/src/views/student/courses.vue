<template>
  <div class="page-container">
    <!-- 搜索条件 -->
    <div class="search-bar">
      <el-input v-model="query.courseName" placeholder="课程名称" clearable style="width: 190px" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.courseType" placeholder="课程类型" clearable style="width: 140px" @change="handleSearch">
        <el-option v-for="item in COURSE_TYPE_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-select v-model="query.deptId" placeholder="开课院系" clearable style="width: 190px" @change="handleSearch">
        <el-option v-for="dept in departments" :key="dept.id" :label="dept.deptName" :value="dept.id" />
      </el-select>
      <el-checkbox v-model="query.onlyAvailable" @change="handleSearch">仅看有余量</el-checkbox>
      <el-button type="primary" @click="handleSearch">
        <el-icon><Search /></el-icon>查询
      </el-button>
      <el-button @click="handleReset">重置</el-button>
      <div class="search-bar__spacer"></div>
      <el-tag v-if="creditLimit" type="warning" effect="plain">
        本学期已选 {{ selectedCredit }} / {{ creditLimit }} 学分
      </el-tag>
    </div>

    <!-- 选课窗口状态与倒计时 -->
    <el-alert
      v-if="phase !== 'unset'"
      class="selection-banner"
      :type="isOpen ? 'success' : phase === 'before' ? 'warning' : 'info'"
      :closable="false"
      show-icon
    >
      <template #title>
        <div class="selection-banner__title">
          <span>{{ phaseText }}</span>
          <el-tag v-if="phase !== 'open'" :type="phaseTagType" size="small" effect="dark">
            {{ countdownText }}
          </el-tag>
        </div>
      </template>
      <div class="selection-banner__tip">{{ tipText }}</div>
      <div v-if="semesterStartText" class="selection-banner__range">
        选课时段：{{ semesterStartText }} ~ {{ semesterEndText }}
      </div>
    </el-alert>

    <!-- 课程列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>可选课程（共 {{ total }} 门）</span>
          <el-button link type="primary" :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-row v-loading="loading" :gutter="16">
        <el-col v-for="course in list" :key="course.id" :xs="24" :sm="12" :lg="8" :xl="6" style="margin-bottom: 16px">
          <el-card shadow="hover" class="course-card" :class="{ 'course-card--full': isFull(course) }">
            <div class="course-card__header">
              <div class="course-card__title" :title="course.courseName">
                {{ course.courseName }}
              </div>
              <el-tag :type="courseTypeTag(course.courseType)" size="small" effect="dark">
                {{ courseTypeText(course.courseType) }}
              </el-tag>
            </div>

            <div class="course-card__code">{{ course.courseCode }}</div>

            <div class="course-card__info">
              <span><el-icon><User /></el-icon>{{ course.teacherName || '待定' }}</span>
              <span><el-icon><Medal /></el-icon>{{ course.credit }} 学分</span>
              <span><el-icon><Clock /></el-icon>{{ course.hours }} 学时</span>
              <span><el-icon><EditPen /></el-icon>{{ examTypeText(course.examType) }}</span>
            </div>

            <div class="course-card__schedule">
              <el-icon><Calendar /></el-icon>
              <span>{{ course.scheduleText || '暂未排课' }}</span>
            </div>

            <div class="course-card__capacity">
              <div class="course-card__capacity-text">
                <span>余量</span>
                <strong :class="capacityClass(course)">
                  {{ course.remainingCapacity ?? '-' }} / {{ course.maxCapacity }}
                </strong>
              </div>
              <el-progress
                :percentage="capacityPercent(course)"
                :stroke-width="7"
                :show-text="false"
                :status="capacityPercent(course) >= 100 ? 'exception' : undefined"
              />
            </div>

            <div class="course-card__footer">
              <el-button link type="primary" size="small" @click="openDetail(course)">课程详情</el-button>
              <div>
                <el-button
                  v-if="!course.selected"
                  type="primary"
                  size="small"
                  :disabled="isFull(course) || !isSelectable(course) || !canSelect"
                  :loading="loadingId === course.id"
                  @click="handleSelect(course)"
                >
                  {{ selectButtonText(course) }}
                </el-button>
                <el-tag v-else type="success" effect="plain" size="large">已选</el-tag>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && !list.length" description="没有找到符合条件的课程" />

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[12, 24, 48]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- 课程详情 -->
    <el-dialog v-model="detailVisible" :title="detail.courseName" width="680px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="课程编号">{{ detail.courseCode }}</el-descriptions-item>
        <el-descriptions-item label="课程类型">{{ courseTypeText(detail.courseType) }}</el-descriptions-item>
        <el-descriptions-item label="授课教师">{{ detail.teacherName || '待定' }}</el-descriptions-item>
        <el-descriptions-item label="开课院系">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="学分">{{ detail.credit }}</el-descriptions-item>
        <el-descriptions-item label="学时">{{ detail.hours }}</el-descriptions-item>
        <el-descriptions-item label="考核方式">{{ examTypeText(detail.examType) }}</el-descriptions-item>
        <el-descriptions-item label="选课情况">
          {{ detail.selectedCount || 0 }} / {{ detail.maxCapacity }}（余 {{ detail.remainingCapacity ?? '-' }}）
        </el-descriptions-item>
        <el-descriptions-item label="教材" :span="2">{{ detail.textbook || '未指定' }}</el-descriptions-item>
        <el-descriptions-item label="上课安排" :span="2">
          <div v-for="(s, index) in detail.schedules || []" :key="index" class="schedule-line">
            周{{ '一二三四五六日'[s.dayOfWeek - 1] }} 第 {{ s.startSection }}-{{ s.endSection }} 节
            <span class="text-muted">
              （{{ s.startWeek }}-{{ s.endWeek }} 周 {{ weekTypeText(s.weekType) }}）
            </span>
            <span class="text-muted"> · {{ s.building }}{{ s.roomNo }}</span>
          </div>
          <span v-if="!detail.schedules?.length" class="text-muted">暂未排课</span>
        </el-descriptions-item>
        <el-descriptions-item label="课程简介" :span="2">
          {{ detail.introduce || '暂无简介' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listCourses, getCourseDetail, selectCourse, checkConflict } from '@/api/student'
import { listDepartments } from '@/api/admin'
import { getCurrentSemester } from '@/api/common'
import { useAppStore } from '@/store/modules/app'
import { useSelectionCountdown } from '@/utils/useSelectionCountdown'
import {
  COURSE_TYPE_OPTIONS, courseTypeText, courseTypeTag, examTypeText, weekTypeText
} from '@/utils/dict'

defineOptions({ name: 'StudentCourses' })

const appStore = useAppStore()

const loading = ref(false)
const loadingId = ref(null)
const list = ref([])
const total = ref(0)
const departments = ref([])
const creditLimit = ref(0)
const selectedCredit = ref(0)

/** 当前学期（优先取全局缓存，缺失时回退到接口） */
const semester = ref(appStore.currentSemester)

const {
  phase,
  canSelect,
  countdownText,
  tipText,
  phaseText,
  phaseTagType,
  isOpen,
  startTime,
  endTime
} = useSelectionCountdown(semester)

const semesterStartText = computed(() => formatMoment(startTime.value))
const semesterEndText = computed(() => formatMoment(endTime.value))

const query = reactive({
  pageNum: 1,
  pageSize: 12,
  courseName: '',
  courseType: '',
  deptId: null,
  onlyAvailable: false,
  status: 1
})

const detailVisible = ref(false)
const detail = reactive({})

onMounted(async () => {
  await Promise.all([loadData(), loadDepartments(), loadSemester()])
})

async function loadSemester() {
  // 全局 store 通常已由布局加载，此处仅在缺失时补充请求
  if (semester.value?.selectStartTime !== undefined) return
  try {
    const { data } = await getCurrentSemester()
    if (data) {
      semester.value = data
      appStore.setCurrentSemester(data)
    }
  } catch {
    // 未设置当前学期时保持为空，由倒计时状态统一提示
  }
}

/** 时间戳格式化为 "YYYY-MM-DD HH:mm" */
function formatMoment(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadData() {
  loading.value = true
  try {
    const { data } = await listCourses(query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
    selectedCredit.value = list.value
      .filter((c) => c.selected)
      .reduce((sum, c) => sum + Number(c.credit || 0), 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadDepartments() {
  try {
    const { data } = await listDepartments({})
    departments.value = data || []
  } catch {
    departments.value = []
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  query.courseName = ''
  query.courseType = ''
  query.deptId = null
  query.onlyAvailable = false
  handleSearch()
}

function isFull(course) {
  const remaining = course.remainingCapacity
  return remaining !== null && remaining !== undefined ? remaining <= 0 : false
}

function isSelectable(course) {
  return course.status === 1
}

function capacityPercent(course) {
  if (!course.maxCapacity) return 0
  const selected = course.maxCapacity - (course.remainingCapacity ?? course.maxCapacity)
  return Math.min(Math.round((selected / course.maxCapacity) * 100), 100)
}

function capacityClass(course) {
  const remaining = course.remainingCapacity
  if (remaining === 0) return 'text-danger'
  if (remaining !== undefined && remaining !== null && remaining <= 5) return 'text-warning'
  return 'text-success'
}

async function openDetail(course) {
  const { data } = await getCourseDetail(course.id)
  Object.assign(detail, data)
  detailVisible.value = true
}

/** 选课按钮文案：满员优先，其次反映选课窗口状态 */
function selectButtonText(course) {
  if (isFull(course)) return '已满'
  if (!isSelectable(course)) return '不可选'
  if (phase.value === 'before') return '未开始'
  if (phase.value === 'closed') return '已结束'
  if (phase.value === 'unset') return '未开放'
  return '选课'
}

async function handleSelect(course) {
  // 双保险：按钮已禁用，但仍防御极端情况下的误触发
  if (!canSelect.value) {
    ElMessage.warning(tipText.value)
    return
  }

  loadingId.value = course.id
  let conflicts = []
  try {
    // 先做时间冲突预检，冲突时引导确认而非直接提交
    const { data } = await checkConflict(course.id)
    conflicts = data || []
  } catch {
    // 预检接口不可用时仍允许继续选课，由后端在提交阶段校验
    conflicts = []
  } finally {
    loadingId.value = null
  }

  if (conflicts.length) {
    try {
      await ElMessageBox.confirm(buildConflictHtml(course, conflicts), '上课时间冲突', {
        type: 'warning',
        dangerouslyUseHTMLString: true,
        confirmButtonText: '仍然选课',
        cancelButtonText: '放弃选课'
      })
    } catch {
      return
    }
  } else {
    try {
      await ElMessageBox.confirm(
        `确定要选修《${course.courseName}》吗？该课程 ${course.credit} 学分。`,
        '选课确认',
        { type: 'info', confirmButtonText: '确定选课', cancelButtonText: '再想想' }
      )
    } catch {
      return
    }
  }

  loadingId.value = course.id
  try {
    const { data } = await selectCourse(course.id)
    ElMessage.success(data.message || '选课成功')
    await loadData()
  } catch {
    // 冲突或已满等错误已由拦截器提示
    await loadData()
  } finally {
    loadingId.value = null
  }
}

/**
 * 将冲突课程渲染为确认弹窗中的 HTML 列表。
 *
 * 后端 ConflictVO 将时间拆分为节次与周次两个字段，此处拼接后展示，
 * 便于学生判断冲突的具体时间段是否真的无法协调。
 */
function buildConflictHtml(course, conflicts) {
  const lines = conflicts
    .map((item) => {
      const timeText = [item.sectionText, item.weekText].filter(Boolean).join(' ')
      const name = item.conflictCourseName || item.courseName || '已选课程'
      return `《${name}》${timeText ? ' ' + timeText : ''}`
    })
    .join('<br/>')
  return `<p>《${course.courseName}》与以下已选课程上课时间重叠：</p>
    <p style="color:#f56c6c;line-height:1.9">${lines}</p>
    <p style="color:#909399;font-size:12px">继续选课可能导致无法正常上课，请确认后再操作。</p>`
}
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
}

.selection-banner {
  margin-bottom: 16px;

  &__title {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  &__tip {
    margin-top: 2px;
  }

  &__range {
    margin-top: 4px;
    font-size: 12px;
    opacity: 0.85;
  }
}

.course-card {
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: transform 0.18s;

  &:hover {
    transform: translateY(-3px);
  }

  &--full {
    opacity: 0.78;
  }

  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
  }

  &__title {
    flex: 1;
    font-size: 15.5px;
    font-weight: 600;
    color: #1f2d3d;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__code {
    margin: 4px 0 10px;
    font-size: 12px;
    color: #909399;
  }

  &__info {
    display: flex;
    flex-wrap: wrap;
    gap: 6px 14px;
    font-size: 12.5px;
    color: #606266;

    span {
      display: flex;
      align-items: center;
      gap: 3px;
    }
  }

  &__schedule {
    display: flex;
    align-items: center;
    gap: 5px;
    margin-top: 10px;
    padding: 7px 9px;
    font-size: 12px;
    color: #606266;
    background: #f5f7fa;
    border-radius: 4px;
    min-height: 32px;
  }

  &__capacity {
    margin-top: 12px;

    &-text {
      display: flex;
      justify-content: space-between;
      font-size: 12px;
      color: #909399;
      margin-bottom: 5px;

      strong {
        font-size: 13px;
      }
    }
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 14px;
    padding-top: 12px;
    border-top: 1px solid #f0f0f0;
  }
}

.text-warning {
  color: #e6a23c;
}

.schedule-line {
  line-height: 1.9;
}
</style>
