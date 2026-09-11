<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="semesterId" placeholder="选择学期" clearable style="width: 230px" @change="loadData">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="loadData">刷新课表</el-button>
      <div class="search-bar__spacer"></div>
      <el-tag type="info" effect="plain">共 {{ timetableList.length }} 个上课时段</el-tag>
    </div>

    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>我的课表</span>
          <span class="text-muted" style="font-size: 12px">点击课程卡片可查看详情</span>
        </div>
      </template>

      <div class="timetable-grid">
        <div class="timetable-grid__header">节次</div>
        <div v-for="day in DAY_LABELS" :key="day" class="timetable-grid__header">{{ day }}</div>

        <template v-for="section in maxSection" :key="section">
          <div class="timetable-grid__section">
            <div>
              <div>第{{ section }}节</div>
              <div style="font-size: 10px; font-weight: 400">{{ SECTION_TIMES[section - 1] }}</div>
            </div>
          </div>
          <div v-for="day in 7" :key="`${day}-${section}`" class="timetable-grid__cell">
            <div
              v-if="cellCourse(day, section)"
              class="timetable-grid__course"
              :style="{ background: courseColor(cellCourse(day, section).courseId) }"
              @click="openDetail(cellCourse(day, section))"
            >
              <strong>{{ cellCourse(day, section).courseName }}</strong>
              <div>{{ cellCourse(day, section).teacherName }}</div>
              <div>{{ cellCourse(day, section).location }}</div>
            </div>
          </div>
        </template>
      </div>

      <el-empty v-if="!loading && !timetableList.length" description="本学期暂无课程安排，快去选课吧~" />
    </el-card>

    <el-dialog v-model="detailVisible" :title="current.courseName" width="480px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="课程编号">{{ current.courseCode }}</el-descriptions-item>
        <el-descriptions-item label="授课教师">{{ current.teacherName || '待定' }}</el-descriptions-item>
        <el-descriptions-item label="学分">{{ current.credit }}</el-descriptions-item>
        <el-descriptions-item label="上课时间">
          周{{ '一二三四五六日'[current.dayOfWeek - 1] }} 第 {{ current.startSection }}-{{ current.endSection }} 节
        </el-descriptions-item>
        <el-descriptions-item label="上课周次">
          {{ current.startWeek }}-{{ current.endWeek }} 周（{{ weekTypeText(current.weekType) }}）
        </el-descriptions-item>
        <el-descriptions-item label="上课地点">{{ current.location || '待定' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMyTimetable } from '@/api/student'
import { listSemesterOptions } from '@/api/common'
import { DAY_LABELS, SECTION_TIMES, weekTypeText } from '@/utils/dict'

defineOptions({ name: 'StudentTimetable' })

const loading = ref(false)
const timetableList = ref([])
const semesters = ref([])
const semesterId = ref(null)
const maxSection = ref(12)

const detailVisible = ref(false)
const current = ref({})

const COLOR_PALETTE = [
  '#5b8ff9', '#61ddaa', '#657797', '#f6bd16',
  '#7262fd', '#78d3f8', '#9661bc', '#f6903d',
  '#008685', '#f08bb4'
]

onMounted(async () => {
  try {
    const { data } = await listSemesterOptions()
    semesters.value = data || []
    const currentSemester = (data || []).find((item) => item.isCurrent === 1)
    if (currentSemester) {
      semesterId.value = currentSemester.id
    }
  } catch {
    semesters.value = []
  }
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const { data } = await getMyTimetable({ semesterId: semesterId.value })
    timetableList.value = data || []
    const max = timetableList.value.reduce((m, item) => Math.max(m, item.endSection || 0), 10)
    maxSection.value = Math.min(Math.max(max, 10), 12)
  } catch {
    timetableList.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 查找指定星期与节次对应的课程（同格多课程时取第一条）。
 */
function cellCourse(day, section) {
  return timetableList.value.find(
    (item) =>
      item.dayOfWeek === day &&
      section >= item.startSection &&
      section <= item.endSection
  )
}

function courseColor(courseId) {
  const index = Number(courseId) % COLOR_PALETTE.length
  return COLOR_PALETTE[index]
}

function openDetail(course) {
  current.value = course
  detailVisible.value = true
}
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
}
</style>
