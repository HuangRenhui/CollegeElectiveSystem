<template>
  <div class="page-container">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div>
            <el-page-header @back="$router.push('/teacher/workbench')">
              <template #content>
                <span style="font-size: 15px; font-weight: 600">课程学生名单</span>
              </template>
            </el-page-header>
          </div>
          <div>
            <el-input v-model="query.keyword" placeholder="搜索姓名/学号" clearable size="small" style="width: 200px" @keyup.enter="loadData" />
            <el-button type="primary" size="small" style="margin-left: 8px" @click="loadData">查询</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" stripe empty-text="暂无学生选课">
        <el-table-column type="index" label="#" width="56" />
        <el-table-column prop="stuNo" label="学号" width="140" />
        <el-table-column prop="studentName" label="姓名" width="120" />
        <el-table-column prop="className" label="行政班级" width="140" />
        <el-table-column prop="credit" label="学分" width="90" />
        <el-table-column prop="selectTime" label="选课时间" width="170" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="selectionStatusTag(row.status)" size="small">
              {{ selectionStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="100" align="center">
          <template #default="{ row }">
            <span v-if="row.score !== null && row.score !== undefined" :style="{ color: scoreColor(row.score), fontWeight: 600 }">
              {{ row.score }}
            </span>
            <span v-else class="text-muted">未录入</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { listCourseStudents } from '@/api/teacher'
import { selectionStatusText, selectionStatusTag, scoreColor } from '@/utils/dict'

defineOptions({ name: 'TeacherStudents' })

const route = useRoute()
const courseId = Number(route.params.courseId)

const loading = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ pageNum: 1, pageSize: 20, keyword: '' })

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const { data } = await listCourseStudents(courseId, query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}
</script>
