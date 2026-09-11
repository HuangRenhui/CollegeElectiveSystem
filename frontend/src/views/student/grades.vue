<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="semesterId" placeholder="选择学期（不选则查看全部）" clearable style="width: 260px" @change="loadData">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
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
import { onMounted, reactive, ref } from 'vue'
import { getMyGradeReport } from '@/api/student'
import { listSemesterOptions } from '@/api/common'
import { scoreColor } from '@/utils/dict'

defineOptions({ name: 'StudentGrades' })

const loading = ref(false)
const semesters = ref([])
const semesterId = ref(null)
const report = reactive({})

onMounted(async () => {
  try {
    const { data } = await listSemesterOptions()
    semesters.value = data || []
  } catch {
    semesters.value = []
  }
  loadData()
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
</style>
