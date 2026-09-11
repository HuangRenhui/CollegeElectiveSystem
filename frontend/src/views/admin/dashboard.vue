<template>
  <div class="page-container">
    <el-row :gutter="16" class="stat-row">
      <el-col v-for="item in statCards" :key="item.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="hover" class="stat-card">
          <el-icon :size="32" :color="item.color"><component :is="item.icon" /></el-icon>
          <div class="stat-card__body">
            <div class="stat-card__value">{{ item.value }}</div>
            <div class="stat-card__label">{{ item.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt-16">
      <el-col :lg="12">
        <el-card shadow="never" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>各院系开课分布</span>
              <el-tag size="small" type="info" effect="plain">按课程数量统计</el-tag>
            </div>
          </template>
          <div ref="deptChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :lg="12">
        <el-card shadow="never" v-loading="loading">
          <template #header>
            <div class="card-header">
              <span>成绩分布</span>
              <el-tag size="small" type="info" effect="plain">按总评分数段统计</el-tag>
            </div>
          </template>
          <div ref="scoreChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="mt-16" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>热门课程排行 TOP 10</span>
          <el-button link type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>
      <el-table :data="statistics.hotCourses || []" stripe empty-text="暂无数据">
        <el-table-column type="index" label="排名" width="70" />
        <el-table-column prop="courseName" label="课程名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="授课教师" width="120" />
        <el-table-column label="选课情况" width="200">
          <template #default="{ row }">
            {{ row.selectedCount }} / {{ row.maxCapacity }}
            <el-progress
              :percentage="Number(row.rate) || 0"
              :stroke-width="8"
              :status="Number(row.rate) >= 100 ? 'exception' : undefined"
              style="margin-top: 4px"
            />
          </template>
        </el-table-column>
        <el-table-column label="选课率" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="rateTag(Number(row.rate))" effect="dark">{{ row.rate }}%</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { getStatistics } from '@/api/admin'
import { useAppStore } from '@/store/modules/app'

defineOptions({ name: 'AdminDashboard' })

const appStore = useAppStore()
const loading = ref(false)
const statistics = reactive({})
const deptChartRef = ref()
const scoreChartRef = ref()

let deptChart = null
let scoreChart = null

const statCards = computed(() => [
  { label: '学生总数', value: statistics.studentCount ?? 0, icon: 'User', color: '#409eff' },
  { label: '教师总数', value: statistics.teacherCount ?? 0, icon: 'Avatar', color: '#67c23a' },
  { label: '课程总数', value: statistics.courseCount ?? 0, icon: 'Notebook', color: '#e6a23c' },
  { label: '选课记录', value: statistics.selectionCount ?? 0, icon: 'List', color: '#f56c6c' },
  { label: '院系数量', value: statistics.deptCount ?? 0, icon: 'OfficeBuilding', color: '#909399' },
  { label: '本学期开课', value: statistics.currentSemesterCourseCount ?? 0, icon: 'Calendar', color: '#8e44ad' }
])

function rateTag(rate) {
  if (rate >= 100) return 'danger'
  if (rate >= 80) return 'warning'
  if (rate >= 50) return 'primary'
  return 'success'
}

onMounted(async () => {
  await loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  deptChart?.dispose()
  scoreChart?.dispose()
})

watch(
  () => appStore.currentSemester,
  () => loadData()
)

async function loadData() {
  loading.value = true
  try {
    const { data } = await getStatistics({ semesterId: appStore.currentSemester?.id })
    Object.assign(statistics, data || {})
    await nextTick()
    renderCharts()
  } catch {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  // 院系分布
  if (deptChartRef.value) {
    deptChart = deptChart || echarts.init(deptChartRef.value)
    const deptData = statistics.deptDistribution || []
    deptChart.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '6%', bottom: '3%', top: 20, containLabel: true },
      xAxis: { type: 'value', name: '开课数', minInterval: 1 },
      yAxis: {
        type: 'category',
        data: deptData.map((item) => item.name),
        axisLabel: { formatter: (value) => (value.length > 10 ? `${value.slice(0, 10)}…` : value) }
      },
      series: [
        {
          name: '课程数',
          type: 'bar',
          data: deptData.map((item) => item.value),
          itemStyle: { color: '#409eff', borderRadius: [0, 4, 4, 0] },
          barMaxWidth: 24,
          label: { show: true, position: 'right' }
        }
      ]
    }, true)
    deptChart.resize()
  }

  // 成绩分布
  if (scoreChartRef.value) {
    scoreChart = scoreChart || echarts.init(scoreChartRef.value)
    const scoreData = statistics.scoreDistribution || []
    scoreChart.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} 人 ({d}%)' },
      legend: { bottom: 0, icon: 'circle' },
      color: ['#67c23a', '#409eff', '#e6a23c', '#f56c6c', '#909399'],
      series: [
        {
          name: '成绩分布',
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '45%'],
          avoidLabelOverlap: true,
          label: { formatter: '{b}\n{c} 人' },
          data: scoreData.map((item) => ({ name: item.name, value: item.value }))
        }
      ]
    }, true)
    scoreChart.resize()
  }
}

function resizeCharts() {
  deptChart?.resize()
  scoreChart?.resize()
}
</script>

<style lang="scss" scoped>
.stat-card {
  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  &__value {
    font-size: 23px;
    font-weight: 700;
    line-height: 1.2;
    color: #1f2d3d;
  }

  &__label {
    font-size: 13px;
    color: #909399;
  }
}

.stat-row {
  margin-bottom: 0;
}

.chart {
  height: 300px;
}

.mt-16 {
  margin-top: 16px;
}
</style>
