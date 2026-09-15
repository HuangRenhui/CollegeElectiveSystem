<template>
  <div class="page-container">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-card__content">
        <el-avatar :size="62" :src="userStore.userInfo.avatar">
          {{ userStore.realName?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="welcome-card__info">
          <h2>{{ greeting }}，{{ userStore.realName }}！</h2>
          <p class="text-muted">
            <el-tag :type="roleTagType" size="small" effect="dark">{{ roleText }}</el-tag>
            <span v-if="userStore.userInfo.stuNo">学号：{{ userStore.userInfo.stuNo }}</span>
            <span v-if="userStore.userInfo.teacherNo">工号：{{ userStore.userInfo.teacherNo }}</span>
            <span v-if="userStore.userInfo.className">班级：{{ userStore.userInfo.className }}</span>
            <span v-if="userStore.userInfo.deptName">院系：{{ userStore.userInfo.deptName }}</span>
          </p>
        </div>
      </div>
    </el-card>

    <!-- 学生概览 -->
    <template v-if="userStore.isStudent">
      <!-- 选课倒计时提醒 -->
      <el-alert
        v-if="selection.phase.value !== 'unset'"
        class="selection-banner"
        :type="selection.isOpen.value ? 'success' : selection.phase.value === 'before' ? 'warning' : 'info'"
        :closable="false"
        show-icon
      >
        <template #title>
          <div class="selection-banner__title">
            <span>{{ selection.phaseText.value }}</span>
            <el-tag v-if="!selection.isOpen.value" :type="selection.phaseTagType.value" size="small" effect="dark">
              {{ selection.countdownText.value }}
            </el-tag>
            <el-button
              v-if="selection.canSelect.value"
              type="primary"
              size="small"
              @click="$router.push('/student/courses')"
            >
              去选课
            </el-button>
          </div>
        </template>
        <div>{{ selection.tipText.value }}</div>
      </el-alert>

      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6" v-for="item in studentStats" :key="item.label">
          <el-card shadow="hover" class="stat-card">
            <el-icon :size="30" :color="item.color"><component :is="item.icon" /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ item.value }}</div>
              <div class="stat-card__label">{{ item.label }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :lg="14">
          <el-card shadow="never" class="mt-16">
            <template #header><span>本学期已选课程</span></template>
            <el-table :data="mySelections" size="small" v-loading="loading" empty-text="暂无选课记录">
              <el-table-column prop="courseCode" label="课程编号" width="110" />
              <el-table-column prop="courseName" label="课程名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="teacherName" label="教师" width="90" />
              <el-table-column prop="credit" label="学分" width="70" />
              <el-table-column prop="selectTime" label="选课时间" width="150" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :lg="10">
          <el-card shadow="never" class="mt-16">
            <template #header><span>最新公告</span></template>
            <el-empty v-if="!appStore.notices.length" description="暂无公告" :image-size="80" />
            <ul v-else class="notice-list">
              <li v-for="notice in appStore.notices.slice(0, 6)" :key="notice.id">
                <el-tag size="small" :type="noticeType(notice.noticeType)" effect="plain">
                  {{ noticeTypeText(notice.noticeType) }}
                </el-tag>
                <span class="notice-list__title">{{ notice.title }}</span>
              </li>
            </ul>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 教师概览 -->
    <template v-else-if="userStore.isTeacher">
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="6" v-for="item in teacherStats" :key="item.label">
          <el-card shadow="hover" class="stat-card">
            <el-icon :size="30" :color="item.color"><component :is="item.icon" /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ item.value }}</div>
              <div class="stat-card__label">{{ item.label }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-card shadow="never" class="mt-16">
        <template #header><span>我的授课课程</span></template>
        <el-table :data="myCourses" size="small" v-loading="loading" empty-text="本学期暂无授课">
          <el-table-column prop="courseCode" label="课程编号" width="110" />
          <el-table-column prop="courseName" label="课程名称" min-width="160" />
          <el-table-column prop="courseType" label="类型" width="100">
            <template #default="{ row }">{{ courseTypeText(row.courseType) }}</template>
          </el-table-column>
          <el-table-column label="选课情况" width="180">
            <template #default="{ row }">
              {{ row.selectedCount }} / {{ row.maxCapacity }} 人
              <el-progress
                :percentage="capacityPercent(row)"
                :stroke-width="6"
                :status="capacityPercent(row) >= 100 ? 'exception' : undefined"
                style="margin-top: 4px"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button link type="primary" @click="$router.push(`/teacher/grades/${row.id}`)">录入成绩</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <!-- 管理员概览 -->
    <template v-else>
      <el-row :gutter="16" class="stat-row">
        <el-col :xs="12" :sm="8" :md="4" v-for="item in adminStats" :key="item.label">
          <el-card shadow="hover" class="stat-card">
            <el-icon :size="30" :color="item.color"><component :is="item.icon" /></el-icon>
            <div class="stat-card__body">
              <div class="stat-card__value">{{ item.value }}</div>
              <div class="stat-card__label">{{ item.label }}</div>
            </div>
          </el-card>
        </el-col>
      </el-row>
      <el-row :gutter="16" class="mt-16">
        <el-col :lg="12">
          <el-card shadow="never">
            <template #header><span>热门课程 TOP 10</span></template>
            <el-table :data="statistics.hotCourses || []" size="small" empty-text="暂无数据">
              <el-table-column type="index" label="#" width="50" />
              <el-table-column prop="courseName" label="课程名称" min-width="150" show-overflow-tooltip />
              <el-table-column prop="teacherName" label="教师" width="90" />
              <el-table-column label="选课率" width="160">
                <template #default="{ row }">
                  <el-progress :percentage="Number(row.rate) || 0" :stroke-width="10" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :lg="12">
          <el-card shadow="never">
            <template #header><span>选课开关与快捷操作</span></template>
            <el-alert
              v-if="selection.phase.value !== 'unset'"
              class="mb-16"
              :type="selection.isOpen.value ? 'success' : selection.phase.value === 'before' ? 'warning' : 'info'"
              :closable="false"
              show-icon
            >
              <template #title>
                <div class="selection-banner__title">
                  <span>{{ selection.phaseText.value }}</span>
                  <el-tag v-if="!selection.isOpen.value" :type="selection.phaseTagType.value" size="small" effect="dark">
                    {{ selection.countdownText.value }}
                  </el-tag>
                </div>
              </template>
              <div>{{ selection.tipText.value }}</div>
            </el-alert>
            <el-space wrap size="large">
              <el-tag :type="switchOn ? 'success' : 'danger'" size="large">
                选课通道：{{ switchOn ? '已开启' : '已关闭' }}
              </el-tag>
              <el-button :type="switchOn ? 'danger' : 'success'" @click="toggleSwitch">
                {{ switchOn ? '关闭选课' : '开启选课' }}
              </el-button>
              <el-button type="primary" @click="openPreload">预热选课缓存</el-button>
              <el-button @click="openSync">同步选课人数</el-button>
              <el-button @click="$router.push('/admin/semesters')">设置选课时间</el-button>
            </el-space>
            <el-alert
              class="mt-16"
              type="info"
              :closable="false"
              title="操作说明"
              description="开放选课前建议先执行「预热选课缓存」，以保障选课期间余量数据的准确与响应速度；如发现余量与实际人数不一致，可执行「同步选课人数」进行校正。"
            />
          </el-card>
        </el-col>
      </el-row>
    </template>

    <!-- 缓存预热弹窗 -->
    <el-dialog v-model="preloadVisible" title="预热选课缓存" width="520px" :close-on-click-modal="false">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="预热说明"
        description="将本学期课程的余量与已选学生名单写入缓存，用于提升选课期间的响应速度。建议在每日开放选课前执行一次。"
      />

      <el-form label-width="90px" class="mt-16">
        <el-form-item label="目标学期">
          <el-select v-model="preloadSemesterId" placeholder="请选择学期" clearable style="width: 100%">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
          <div class="form-tip">不选择时默认使用当前学期（{{ appStore.currentSemester?.semesterName || '未设置' }}）。</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="preloadVisible = false">取消</el-button>
        <el-button type="primary" :loading="preloading" @click="doPreload">开始预热</el-button>
      </template>
    </el-dialog>

    <!-- 同步选课人数弹窗 -->
    <el-dialog v-model="syncVisible" title="同步选课人数" width="520px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="同步说明"
        description="将按选课记录重新统计各课程的已选人数，并覆盖缓存的余量数据。当页面显示的余量与实际情况不一致时使用。"
      />
      <p class="text-muted mt-16">同步过程可能耗时较长，请勿重复提交。</p>

      <template #footer>
        <el-button @click="syncVisible = false">取消</el-button>
        <el-button type="primary" :loading="syncing" @click="doSync">确定同步</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { listMySelections, getMyGradeReport } from '@/api/student'
import { listMyCourses } from '@/api/teacher'
import { getStatistics, getSelectionSwitch, switchSelection, preloadCourseCache, syncSelectionCount } from '@/api/admin'
import { listSemesterOptions } from '@/api/common'
import { useSelectionCountdown } from '@/utils/useSelectionCountdown'
import { courseTypeText, noticeTypeText } from '@/utils/dict'

const appStore = useAppStore()
const userStore = useUserStore()

/** 学生选课窗口倒计时（仅学生角色使用） */
const selection = useSelectionCountdown(computed(() => appStore.currentSemester))

const loading = ref(false)
const preloading = ref(false)
const syncing = ref(false)
const switchOn = ref(true)
const statistics = reactive({})
const mySelections = ref([])
const myCourses = ref([])
const semesters = ref([])
const studentSummary = reactive({ totalCredit: 0, earnedCredit: 0, averageScore: 0, courseCount: 0 })

const preloadVisible = ref(false)
const preloadSemesterId = ref(null)
const syncVisible = ref(false)

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const roleText = computed(() => ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }[userStore.role] || ''))
const roleTagType = computed(() => ({ STUDENT: 'success', TEACHER: 'warning', ADMIN: 'danger' }[userStore.role] || 'info'))

const studentStats = computed(() => [
  { label: '已选课程', value: studentSummary.courseCount, icon: 'Notebook', color: '#409eff' },
  { label: '已获学分', value: studentSummary.earnedCredit, icon: 'Trophy', color: '#67c23a' },
  { label: '平均成绩', value: studentSummary.averageScore, icon: 'DataLine', color: '#e6a23c' },
  { label: '总学分', value: studentSummary.totalCredit, icon: 'Medal', color: '#f56c6c' }
])

const teacherStats = computed(() => {
  const courses = myCourses.value || []
  const totalStudents = courses.reduce((sum, c) => sum + (c.selectedCount || 0), 0)
  return [
    { label: '授课课程', value: courses.length, icon: 'Notebook', color: '#409eff' },
    { label: '选课学生', value: totalStudents, icon: 'User', color: '#67c23a' },
    { label: '总学分', value: courses.reduce((s, c) => s + Number(c.credit || 0), 0).toFixed(1), icon: 'Medal', color: '#e6a23c' },
    { label: '平均班额', value: courses.length ? (totalStudents / courses.length).toFixed(1) : 0, icon: 'DataAnalysis', color: '#f56c6c' }
  ]
})

const adminStats = computed(() => [
  { label: '学生总数', value: statistics.studentCount ?? 0, icon: 'User', color: '#409eff' },
  { label: '教师总数', value: statistics.teacherCount ?? 0, icon: 'Avatar', color: '#67c23a' },
  { label: '课程总数', value: statistics.courseCount ?? 0, icon: 'Notebook', color: '#e6a23c' },
  { label: '选课记录', value: statistics.selectionCount ?? 0, icon: 'List', color: '#f56c6c' },
  { label: '院系数量', value: statistics.deptCount ?? 0, icon: 'OfficeBuilding', color: '#909399' },
  { label: '本学期开课', value: statistics.currentSemesterCourseCount ?? 0, icon: 'Calendar', color: '#8e44ad' }
])

function capacityPercent(row) {
  if (!row.maxCapacity) return 0
  return Math.min(Math.round(((row.selectedCount || 0) / row.maxCapacity) * 100), 100)
}

function noticeType(type) {
  return { SYSTEM: 'info', SELECTION: 'success', EXAM: 'warning' }[type] || 'info'
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    if (userStore.isStudent) {
      const [selectionRes, reportRes] = await Promise.all([
        listMySelections({ pageNum: 1, pageSize: 10, status: 1 }),
        getMyGradeReport({})
      ])
      mySelections.value = selectionRes.data.records || []
      studentSummary.courseCount = reportRes.data.grades?.length || 0
      studentSummary.totalCredit = reportRes.data.totalCredit || 0
      studentSummary.earnedCredit = reportRes.data.earnedCredit || 0
      studentSummary.averageScore = reportRes.data.averageScore || 0
    } else if (userStore.isTeacher) {
      const { data } = await listMyCourses({ pageNum: 1, pageSize: 20 })
      myCourses.value = data.records || []
    } else {
      const [statRes, switchRes] = await Promise.all([getStatistics({}), getSelectionSwitch()])
      Object.assign(statistics, statRes.data || {})
      switchOn.value = switchRes.data !== 'off'
      loadSemesters()
    }
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    loading.value = false
  }
}

async function loadSemesters() {
  try {
    const { data } = await listSemesterOptions()
    semesters.value = data || []
    const current = (data || []).find((item) => item.isCurrent === 1)
    preloadSemesterId.value = current ? current.id : null
  } catch {
    semesters.value = []
  }
}

async function toggleSwitch() {
  const next = switchOn.value ? 'off' : 'on'
  await ElMessageBox.confirm(
    next === 'on'
      ? '开启后学生即可提交选课申请，确定开启选课通道吗？'
      : '关闭后学生将无法提交选课申请，确定关闭选课通道吗？',
    next === 'on' ? '开启选课确认' : '关闭选课确认',
    { type: 'warning', confirmButtonText: next === 'on' ? '确定开启' : '确定关闭' }
  )
  await switchSelection(next)
  switchOn.value = next === 'on'
  ElMessage.success(next === 'on' ? '选课通道已开启' : '选课通道已关闭')
}

function openPreload() {
  preloadVisible.value = true
}

async function doPreload() {
  preloading.value = true
  try {
    await preloadCourseCache(preloadSemesterId.value || appStore.currentSemester?.id)
    preloadVisible.value = false
    ElMessage.success('缓存预热完成')
    await loadData()
  } catch {
    // 未实现或执行失败已由拦截器提示，保留弹窗便于调整后重试
  } finally {
    preloading.value = false
  }
}

function openSync() {
  syncVisible.value = true
}

async function doSync() {
  syncing.value = true
  try {
    await syncSelectionCount()
    syncVisible.value = false
    ElMessage.success('选课人数同步完成')
    await loadData()
  } catch {
    // 未实现或执行失败已由拦截器提示
  } finally {
    syncing.value = false
  }
}
</script>

<style lang="scss" scoped>
.welcome-card {
  margin-bottom: 16px;
  border: none;
  background: linear-gradient(120deg, #e8f1ff 0%, #f5f9ff 100%);

  &__content {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  &__info {
    h2 {
      margin: 0 0 8px;
      font-size: 20px;
    }

    p {
      display: flex;
      flex-wrap: wrap;
      gap: 14px;
      margin: 0;
      font-size: 13px;
    }
  }
}

.stat-row {
  margin-bottom: 0;
}

.selection-banner {
  margin-bottom: 16px;

  &__title {
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.mb-16 {
  margin-bottom: 16px;
}

.stat-card {
  display: flex;
  align-items: center;

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 14px;
    width: 100%;
  }

  &__value {
    font-size: 22px;
    font-weight: 700;
    color: #1f2d3d;
    line-height: 1.2;
  }

  &__label {
    font-size: 13px;
    color: #909399;
  }
}

.notice-list {
  list-style: none;
  margin: 0;
  padding: 0;

  li {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 9px 0;
    border-bottom: 1px dashed #f0f0f0;
    font-size: 13px;

    &:last-child {
      border-bottom: none;
    }
  }

  &__title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.mt-16 {
  margin-top: 16px;
}
</style>
