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
            <el-space wrap size="large">
              <el-tag :type="switchOn ? 'success' : 'danger'" size="large">
                选课通道：{{ switchOn ? '已开启' : '已关闭' }}
              </el-tag>
              <el-button :type="switchOn ? 'danger' : 'success'" @click="toggleSwitch">
                {{ switchOn ? '关闭选课' : '开启选课' }}
              </el-button>
              <el-button type="primary" :loading="preloading" @click="doPreload">预热选课缓存</el-button>
              <el-button @click="doSync">同步选课人数</el-button>
            </el-space>
            <el-alert
              class="mt-16"
              type="info"
              :closable="false"
              title="选课缓存说明"
              description="开启选课前建议先执行缓存预热，将课程余量写入 Redis，可显著提升高并发选课性能。"
            />
          </el-card>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { listMySelections, getMyGradeReport } from '@/api/student'
import { listMyCourses } from '@/api/teacher'
import { getStatistics, getSelectionSwitch, switchSelection, preloadCourseCache, syncSelectionCount } from '@/api/admin'
import { courseTypeText, noticeTypeText } from '@/utils/dict'

const appStore = useAppStore()
const userStore = useUserStore()

const loading = ref(false)
const preloading = ref(false)
const switchOn = ref(true)
const statistics = reactive({})
const mySelections = ref([])
const myCourses = ref([])
const studentSummary = reactive({ totalCredit: 0, earnedCredit: 0, averageScore: 0, courseCount: 0 })

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
    }
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    loading.value = false
  }
}

async function toggleSwitch() {
  const next = switchOn.value ? 'off' : 'on'
  await switchSelection(next)
  switchOn.value = next === 'on'
  ElMessage.success(next === 'on' ? '选课通道已开启' : '选课通道已关闭')
}

async function doPreload() {
  preloading.value = true
  try {
    await preloadCourseCache(appStore.currentSemester?.id)
    ElMessage.success('缓存预热完成')
  } finally {
    preloading.value = false
  }
}

async function doSync() {
  await syncSelectionCount()
  ElMessage.success('选课人数同步完成')
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

.mt-16 {
  margin-top: 16px;
}
</style>
