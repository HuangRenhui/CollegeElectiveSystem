<template>
  <div class="page-container">
    <div class="search-bar">
      <el-select v-model="query.semesterId" placeholder="选择学期" clearable style="width: 220px" @change="loadData">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="选课状态" clearable style="width: 150px" @change="loadData">
        <el-option v-for="item in SELECTION_STATUS_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
      <div class="search-bar__spacer"></div>
      <el-statistic title="当前筛选学分合计" :value="totalCredit" :precision="1" />
    </div>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的选课记录（{{ total }} 条）</span>
          <el-button link type="primary" @click="loadData">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column type="index" label="#" width="56" />
        <el-table-column prop="courseCode" label="课程编号" width="110" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" show-overflow-tooltip />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="courseTypeTag(row.courseType)" size="small">{{ courseTypeText(row.courseType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column prop="credit" label="学分" width="80" />
        <el-table-column prop="selectTime" label="选课时间" width="165" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="selectionStatusTag(row.status)" size="small">
              {{ selectionStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="90">
          <template #default="{ row }">
            <span v-if="row.score !== null && row.score !== undefined" :style="{ color: scoreColor(row.score), fontWeight: 600 }">
              {{ row.score }}
            </span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 1"
              link
              type="danger"
              :loading="droppingId === row.courseId"
              @click="handleDrop(row)"
            >
              退课
            </el-button>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMySelections, dropCourse } from '@/api/student'
import { listSemesterOptions } from '@/api/common'
import {
  SELECTION_STATUS_OPTIONS, courseTypeText, courseTypeTag,
  selectionStatusText, selectionStatusTag, scoreColor
} from '@/utils/dict'

defineOptions({ name: 'StudentSelections' })

const loading = ref(false)
const droppingId = ref(null)
const list = ref([])
const total = ref(0)
const semesters = ref([])

const query = reactive({ pageNum: 1, pageSize: 10, semesterId: null, status: null })

const totalCredit = computed(() =>
  list.value
    .filter((item) => item.status === 1 || item.status === 2)
    .reduce((sum, item) => sum + Number(item.credit || 0), 0)
)

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
    const { data } = await listMySelections(query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function handleDrop(row) {
  await ElMessageBox.confirm(
    `确定要退选《${row.courseName}》吗？退课后学分将不再计入。`,
    '退课确认',
    { type: 'warning', confirmButtonText: '确定退课' }
  )
  droppingId.value = row.courseId
  try {
    await dropCourse(row.courseId)
    ElMessage.success('退课成功')
    await loadData()
  } finally {
    droppingId.value = null
  }
}
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
}
</style>
