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

    <!-- 课程列表 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>可选课程（共 {{ total }} 门）</span>
          <el-button link type="primary" :icon="Refresh" @click="loadData">刷新</el-button>
        </div>
      </el-card>

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
                  :disabled="isFull(course) || !isSelectable(course)"
                  :loading="loadingId === course.id"
                  @click="handleSelect(course)"
                >
                  {{ isFull(course) ? '已满' : '选课' }}
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { listCourses, getCourseDetail, selectCourse } from '@/api/student'
import { listDepartments } from '@/api/admin'
import {
  COURSE_TYPE_OPTIONS, courseTypeText, courseTypeTag, examTypeText, weekTypeText
} from '@/utils/dict'

defineOptions({ name: 'StudentCourses' })

const loading = ref(false)
const loadingId = ref(null)
const list = ref([])
const total = ref(0)
const departments = ref([])
const creditLimit = ref(0)
const selectedCredit = ref(0)

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
  await Promise.all([loadData(), loadDepartments()])
})

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

async function handleSelect(course) {
  await ElMessageBox.confirm(
    `确定要选修《${course.courseName}》吗？该课程 ${course.credit} 学分。`,
    '选课确认',
    { type: 'info', confirmButtonText: '确定选课', cancelButtonText: '再想想' }
  )

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
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
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
