<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="semesterId" placeholder="选择学期（不选则查看全部）" clearable style="width: 260px" @change="loadData">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <!-- 导出与当前筛选的学期保持一致：选了学期就导出该学期，未选则导出全部 -->
      <el-button
        :loading="exporting"
        :disabled="!report.grades?.length"
        @click="handleExport"
      >
        导出成绩单
      </el-button>
    </div>

    <!-- 统计概览 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__value">{{ report.totalCredit || 0 }}</div>
          <div class="stat-card__label">总学分</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__value text-success">{{ report.earnedCredit || 0 }}</div>
          <div class="stat-card__label">已获学分</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__value" :style="{ color: scoreColor(report.averageScore) }">
            {{ report.averageScore || 0 }}
          </div>
          <div class="stat-card__label">平均成绩</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-card__value" style="color: #e6a23c">{{ report.averageGradePoint || 0 }}</div>
          <div class="stat-card__label">平均绩点</div>
        </el-card>
      </el-col>
    </el-row>

    <!--
      GPA 趋势：仅在至少两个学期有已发布成绩时才渲染图表。
      只有一个学期时折线图退化为一个点，没有趋势意义，此时改文案提示。
    -->
    <el-card shadow="never" class="mt-16" v-if="trend.points?.length >= 2">
      <template #header>
        <div class="card-header">
          <span>绩点趋势</span>
          <el-tag v-if="trend.bestSemester" type="warning" effect="plain">
            最高 {{ trend.bestPoint }} · {{ trend.bestSemester }}
          </el-tag>
        </div>
      </template>
      <div ref="trendRef" class="trend-chart" v-loading="trendLoading" />
    </el-card>
    <el-card shadow="never" class="mt-16" v-else-if="trend.points?.length === 1">
      <div class="trend-tip">
        仅「{{ trend.points[0].semesterName }}」一个学期有已发布成绩，
        暂无法呈现趋势；后续学期发布成绩后此处将展示绩点走势。
      </div>
    </el-card>

    <el-card shadow="never" class="mt-16" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>成绩明细 {{ report.semesterName ? `— ${report.semesterName}` : '' }}</span>
          <el-tag v-if="report.grades?.length" type="success" effect="plain">
            共 {{ report.grades.length }} 门课程
          </el-tag>
        </div>
      </template>

      <el-table :data="report.grades || []" stripe empty-text="暂无已发布成绩">
        <el-table-column type="index" label="#" width="56" />
        <el-table-column prop="courseCode" label="课程编号" width="120" />
        <el-table-column prop="courseName" label="课程名称" min-width="170" show-overflow-tooltip />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="usualScore" label="平时成绩" width="100" align="center" />
        <el-table-column prop="examScore" label="期末成绩" width="100" align="center" />
        <el-table-column label="总评成绩" width="110" align="center">
          <template #default="{ row }">
            <span :style="{ color: scoreColor(row.totalScore), fontWeight: 700, fontSize: '15px' }">
              {{ row.totalScore ?? '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="gradePoint" label="绩点" width="90" align="center" />
        <el-table-column label="结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.pass ? 'success' : 'danger'" size="small" effect="dark">
              {{ row.pass ? '通过' : '未通过' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && !report.grades?.length" description="暂无成绩数据，成绩发布后可在此查看" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, nextTick, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyGradeReport, getMyGpaTrend, exportMyGrades } from '@/api/student'
import { listSemesterOptions } from '@/api/common'
import { scoreColor } from '@/utils/dict'
import { downloadBlob } from '@/utils/download'
import * as echarts from 'echarts'

defineOptions({ name: 'StudentGrades' })

const loading = ref(false)
const exporting = ref(false)
const semesters = ref([])
const semesterId = ref(null)
const report = reactive({})

// GPA 趋势：数据与图表实例分开管理，卸载时必须销毁实例，
// 否则切页后定时器/事件残留会报 "Can't access property of null"
const trendLoading = ref(false)
const trend = reactive({})
const trendRef = ref(null)
let trendChart = null
let resizeHandler = null

onMounted(async () => {
  try {
    const { data } = await listSemesterOptions()
    semesters.value = data || []
  } catch {
    semesters.value = []
  }
  loadData()
  loadTrend()
})

onBeforeUnmount(() => {
  // 与页面内其他地方一致：先移除 resize 监听，再 dispose 图表
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  trendChart?.dispose()
  trendChart = null
})

async function loadData() {
  loading.value = true
  try {
    const { data } = await getMyGradeReport({ semesterId: semesterId.value })
    Object.assign(report, data || {})
  } catch {
    Object.assign(report, {})
  } finally {
    loading.value = false
  }
}

/** 导出当前学期（或全部）的成绩单 */
async function handleExport() {
  exporting.value = true
  try {
    const response = await exportMyGrades(semesterId.value)
    downloadBlob(response, '成绩单.xlsx')
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    exporting.value = false
  }
}

async function loadTrend() {
  trendLoading.value = true
  try {
    const { data } = await getMyGpaTrend()
    Object.assign(trend, data || {})
  } catch {
    Object.assign(trend, {})
  } finally {
    trendLoading.value = false
  }
  // 必须等 Vue 把 DOM 刷完：卡片由 v-if="trend.points?.length >= 2" 控制，
  // 数据赋值只是"标记需要渲染"，同步执行到这里 trendRef 仍为 null，
  // 图表会静默不渲染（不报错，最难排查的一类问题）。
  await nextTick()
  renderTrend()
}

/**
 * 渲染绩点趋势折线图。
 *
 * <p>调用前必须已 await nextTick()，否则 v-if 控制的容器尚未挂载。</p>
 */
function renderTrend() {
  if (trend.points?.length < 2 || !trendRef.value) return

  trendChart = echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v) => (v == null ? '-' : v) },
    grid: { left: 48, right: 24, top: 28, bottom: 32 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.points.map((p) => p.semesterName || '未知学期'),
      axisLabel: { interval: 0, rotate: trend.points.length > 6 ? 30 : 0 },
    },
    yAxis: { type: 'value', min: 0, max: 4, name: '绩点' },
    series: [
      {
        name: '平均绩点',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        data: trend.points.map((p) => p.gradePoint),
        areaStyle: { opacity: 0.15 },
        lineStyle: { width: 3 },
        label: { show: true, formatter: '{c}' },
      },
    ],
  })

  resizeHandler = () => trendChart?.resize()
  window.addEventListener('resize', resizeHandler)
}
</script>

<style lang="scss" scoped>
.stat-card {
  text-align: center;

  &__value {
    font-size: 26px;
    font-weight: 700;
  }

  &__label {
    margin-top: 4px;
    font-size: 13px;
    color: #909399;
  }
}

.stat-row {
  margin-bottom: 0;
}

.mt-16 {
  margin-top: 16px;
}

/* 绩点趋势图容器：高度写死，echarts 需要明确的容器尺寸 */
.trend-chart {
  width: 100%;
  height: 300px;
}

/* 只有一个学期数据时的提示文案 */
.trend-tip {
  padding: 4px 0;
  font-size: 13px;
  color: #909399;
}
</style>
