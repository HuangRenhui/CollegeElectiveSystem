<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.semesterId" placeholder="选择学期" clearable style="width: 230px" @change="loadData">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <div class="search-bar__spacer"></div>
      <el-tag type="info" effect="plain">共 {{ total }} 门课程</el-tag>
    </div>

    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="course in list" :key="course.id" :xs="24" :sm="12" :lg="8" style="margin-bottom: 16px">
        <el-card shadow="hover" class="course-card">
          <div class="course-card__header">
            <span class="course-card__title" :title="course.courseName">{{ course.courseName }}</span>
            <el-tag :type="courseTypeTag(course.courseType)" size="small" effect="dark">
              {{ courseTypeText(course.courseType) }}
            </el-tag>
          </div>
          <div class="course-card__code">
            {{ course.courseCode }} · {{ course.credit }} 学分 · {{ course.hours }} 学时
          </div>

          <div class="course-card__schedule">
            <el-icon><Calendar /></el-icon>
            <span>{{ course.scheduleText || '暂未排课' }}</span>
          </div>

          <div class="course-card__capacity">
            <div class="course-card__capacity-text">
              <span>选课人数</span>
              <strong>{{ course.selectedCount }} / {{ course.maxCapacity }}</strong>
            </div>
            <el-progress
              :percentage="capacityPercent(course)"
              :stroke-width="8"
              :status="capacityPercent(course) >= 100 ? 'exception' : undefined"
            />
          </div>

          <div class="course-card__footer">
            <el-button size="small" @click="$router.push(`/teacher/students/${course.id}`)">
              <el-icon><UserFilled /></el-icon>学生名单
            </el-button>
            <el-button type="primary" size="small" @click="$router.push(`/teacher/grades/${course.id}`)">
              <el-icon><EditPen /></el-icon>成绩录入
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && !list.length" description="本学期暂无授课安排" />

    <div class="pagination-wrapper" v-if="total > query.pageSize">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { listMyCourses } from '@/api/teacher'
import { listSemesterOptions } from '@/api/common'
import { courseTypeText, courseTypeTag } from '@/utils/dict'

defineOptions({ name: 'TeacherWorkbench' })

const loading = ref(false)
const list = ref([])
const total = ref(0)
const semesters = ref([])

const query = reactive({ pageNum: 1, pageSize: 12, semesterId: null })

onMounted(async () => {
  try {
    const { data } = await listSemesterOptions()
    semesters.value = data || []
    const currentSemester = (data || []).find((item) => item.isCurrent === 1)
    if (currentSemester) {
      query.semesterId = currentSemester.id
    }
  } catch {
    semesters.value = []
  }
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const { data } = await listMyCourses(query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function capacityPercent(course) {
  if (!course.maxCapacity) return 0
  return Math.min(Math.round(((course.selectedCount || 0) / course.maxCapacity) * 100), 100)
}
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
}

.course-card {
  &__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 8px;
  }

  &__title {
    flex: 1;
    font-size: 16px;
    font-weight: 600;
    color: #1f2d3d;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__code {
    margin: 6px 0 12px;
    font-size: 12.5px;
    color: #909399;
  }

  &__schedule {
    display: flex;
    align-items: center;
    gap: 5px;
    padding: 8px 10px;
    font-size: 12px;
    color: #606266;
    background: #f5f7fa;
    border-radius: 4px;
    min-height: 34px;
  }

  &__capacity {
    margin-top: 14px;

    &-text {
      display: flex;
      justify-content: space-between;
      margin-bottom: 6px;
      font-size: 12.5px;
      color: #909399;

      strong {
        color: #303133;
        font-size: 13.5px;
      }
    }
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 16px;
    padding-top: 14px;
    border-top: 1px solid #f0f0f0;
  }
}
</style>
