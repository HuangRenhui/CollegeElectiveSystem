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
              <!-- 历史平均分：后端无已发布成绩时返回 null，此时不渲染该标签
                   （若显示 0 会被误读为「该课平均分 0」） -->
              <span v-if="course.avgScore != null">
                <el-icon><TrendCharts /></el-icon>均分 {{ course.avgScore }}
              </span>
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
        <el-descriptions-item v-if="detail.avgScore != null" label="历史平均分">
          {{ detail.avgScore }}
        </el-descriptions-item>
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

    <!-- 选课排队提示：高峰期限流，避免直接失败 -->
    <el-dialog
      v-model="queueVisible"
      title="选课排队中"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
    >
      <el-progress
        :percentage="queueProgress"
        :stroke-width="10"
        :status="queueAdmitted ? 'success' : undefined"
      />
      <div class="queue-info">
        <p class="queue-message">{{ queueStatus.message || '正在为你排队，请稍候' }}</p>
        <p v-if="queueStatus.queuedCount" class="text-muted">
          当前排队 {{ queueStatus.queuedCount }} 人
        </p>
        <p class="text-muted">令牌剩余 {{ queueStatus.tokenTtlSeconds || 0 }} 秒</p>
      </div>
      <template #footer>
        <el-button @click="handleCancelQueue">退出排队</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, TrendCharts } from '@element-plus/icons-vue'
import {
  listCourses, getCourseDetail, selectCourse, checkConflict,
  requestQueue, queryQueue, cancelQueue
} from '@/api/student'
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
const creditLimit = ref(0)
const selectedCredit = ref(0)

/**
 * 开课院系筛选项。
 *
 * 直接从课程列表里累积去重生成，不再调用 /admin/departments——
 * 该接口仅管理员可用，学生调用会返回「没有操作权限」（登录后进入本页即弹红条）。
 * 累积而非每次重建，避免筛选中某个院系后选项被裁剪掉、无法切换。
 */
const departmentMap = reactive({})
const departments = computed(() => Object.values(departmentMap))

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

// ---------- 选课排队（高峰期削峰） ----------
/** 排队提示弹窗是否可见 */
const queueVisible = ref(false)
/** 当前排队状态，取值见 QueueStatusVO */
const queueStatus = ref({})
/** 轮询定时器：固定节奏查询叫号进度 */
let queueTimer = null
/** 看门狗定时器：令牌耗尽时兜底停止轮询，避免定时器泄漏 */
let queueWatchdog = null

onMounted(async () => {
  await Promise.all([loadData(), loadSemester()])
})

// 组件卸载时清理定时器，防止切换页面后仍在后台轮询
onBeforeUnmount(() => {
  if (queueTimer) clearInterval(queueTimer)
  if (queueWatchdog) clearTimeout(queueWatchdog)
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

    // 顺带累积「开课院系」筛选项（课程列表已带 deptId / deptName）
    list.value.forEach((course) => {
      if (course.deptId != null && !departmentMap[course.deptId]) {
        departmentMap[course.deptId] = {
          id: course.deptId,
          deptName: course.deptName || '未知院系'
        }
      }
    })
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
    // ---------- 排队准入（高峰期削峰） ----------
    // 未启用排队时后端直接返回「已叫号」状态，此处近乎零开销；
    // 启用后若尚未叫到，则转入轮询等待，叫到后再提交选课
    const admitted = await ensureQueueAdmitted(course.id)
    if (!admitted) return

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
 * 确保已叫到号，可提交选课。
 *
 * <p>流程：取号 → 若已叫到则立即返回 true；否则弹出排队提示并每 2.5 秒轮询一次，
 * 叫到或令牌失效时停止轮询。</p>
 *
 * <p><b>为什么用 setInterval 而非 setTimeout 递归</b>：递归 setTimeout 会把
 * 上一次请求的耗时累加到等待间隔里，网络慢时轮询频率会越来越低；
 * setInterval 由浏览器保证固定节奏。</p>
 *
 * @param {number} courseId 课程ID
 * @returns {Promise<boolean>} 是否可以继续提交选课
 */
async function ensureQueueAdmitted(courseId) {
  const { data: status } = await requestQueue(courseId)

  // 已叫号（或未启用排队），直接放行
  if (status.mySeq <= status.currentSeq) return true

  queueVisible.value = true
  queueStatus.value = status

  return new Promise((resolve) => {
    queueTimer = setInterval(async () => {
      try {
        const { data } = await queryQueue(courseId)
        queueStatus.value = data
        if (data.mySeq <= data.currentSeq) {
          stopQueuePolling()
          resolve(true)
        } else if (!data.tokenValid) {
          stopQueuePolling()
          ElMessage.warning(data.message || '排队令牌已失效，请重新选课')
          resolve(false)
        }
      } catch {
        // 单次轮询失败不中断整体等待，继续重试直到令牌耗尽
      }
    }, 2500)

    // 令牌最长有效期兜底，避免用户关闭弹窗后定时器一直运行
    queueWatchdog = setTimeout(() => {
      stopQueuePolling()
      resolve(false)
    }, (status.tokenTtlSeconds + 5) * 1000)
  })
}

function stopQueuePolling() {
  if (queueTimer) {
    clearInterval(queueTimer)
    queueTimer = null
  }
  if (queueWatchdog) {
    clearTimeout(queueWatchdog)
    queueWatchdog = null
  }
  queueVisible.value = false
}

/** 用户主动退出排队 */
async function handleCancelQueue() {
  try {
    await cancelQueue(queueStatus.value.courseId)
    ElMessage.info('已退出排队')
  } finally {
    stopQueuePolling()
  }
}

/**
 * 将冲突课程渲染为确认弹窗中的 HTML 列表。
 *
 * 后端 ConflictVO 将时间拆分为节次与周次两个字段，此处拼接后展示，
 * 便于学生判断冲突的具体时间段是否真的无法协调。
 */
function escapeHtml(text) {
  return String(text ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function buildConflictHtml(course, conflicts) {
  const lines = conflicts
    .map((item) => {
      const timeText = [item.sectionText, item.weekText].filter(Boolean).join(' ')
      const name = item.conflictCourseName || item.courseName || '已选课程'
      return `《${escapeHtml(name)}》${timeText ? ' ' + escapeHtml(timeText) : ''}`
    })
    .join('<br/>')
  return `<p>《${escapeHtml(course.courseName)}》与以下已选课程上课时间重叠：</p>
    <p style="color:#f56c6c;line-height:1.9">${lines}</p>
    <p style="color:#909399;font-size:12px">继续选课可能导致无法正常上课，请确认后再操作。</p>`
}

/** 是否已叫号（可立即提交选课） */
const queueAdmitted = computed(
  () => queueStatus.value.mySeq <= queueStatus.value.currentSeq
)

/**
 * 排队进度百分比。
 *
 * <p>以「我的排队号 / 当前叫号位置」衡量——叫号位置越过我的号即完成，
 * 因此进度到达 100% 等价于已放行。若尚无排队号（undefined）则显示 0。</p>
 */
const queueProgress = computed(() => {
  const mySeq = queueStatus.value.mySeq || 0
  const currentSeq = queueStatus.value.currentSeq || 0
  if (mySeq <= currentSeq) return 100
  // 用 mySeq 而非 currentSeq 作分母，保证进度随叫号推进而稳步增长
  return Math.min(99, Math.round((currentSeq / mySeq) * 100))
})
</script>

<style lang="scss" scoped>
.queue-info {
  margin-top: 16px;
  text-align: center;

  .queue-message {
    margin-bottom: 8px;
    font-size: 14px;
    color: #409eff;
  }

  p {
    margin: 4px 0;
    font-size: 12px;
  }
}

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

/* 历史平均分标签：与其它信息项视觉区分，弱化为次要信息 */
.course-card__info span:last-child {
  color: #909399;
}

.schedule-line {
  line-height: 1.9;
}
</style>
