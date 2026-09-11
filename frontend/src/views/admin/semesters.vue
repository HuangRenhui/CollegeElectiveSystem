<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索学期名称" clearable style="width: 220px" @keyup.enter="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <div class="search-bar__spacer"></div>
      <el-button type="primary" :icon="Plus" @click="openForm()">新增学期</el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="semesterCode" label="学期编码" width="140" />
        <el-table-column prop="semesterName" label="学期名称" min-width="200" />
        <el-table-column label="起止日期" width="230">
          <template #default="{ row }">
            {{ row.startDate }} ~ {{ row.endDate }}
          </template>
        </el-table-column>
        <el-table-column label="选课时间" min-width="300">
          <template #default="{ row }">
            <span v-if="row.selectStartTime">
              {{ row.selectStartTime }} ~ {{ row.selectEndTime }}
            </span>
            <span v-else class="text-muted">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalWeeks" label="教学周" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="semesterStatusTag(row.status)" size="small">{{ semesterStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前学期" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isCurrent === 1" type="success" effect="dark" size="small">当前</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.isCurrent !== 1" link type="success" @click="setCurrent(row)">设为当前</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑学期' : '新增学期'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="学期编码" prop="semesterCode">
          <el-input v-model="form.semesterCode" placeholder="如 2026-2027-1" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="学期名称" prop="semesterName">
          <el-input v-model="form.semesterName" placeholder="如 2026-2027学年第一学期" />
        </el-form-item>
        <el-form-item label="起止日期" prop="dateRange">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="选课时间" prop="selectRange">
          <el-date-picker
            v-model="selectRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开放时间"
            end-placeholder="截止时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="总教学周数" prop="totalWeeks">
          <el-input-number v-model="form.totalWeeks" :min="1" :max="30" />
        </el-form-item>
        <el-form-item label="学期状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option v-for="item in SEMESTER_STATUS_OPTIONS" :key="item.value" v-bind="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="设为当前学期">
          <el-switch v-model="form.isCurrent" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { pageSemesters, saveSemester, setCurrentSemester, deleteSemester } from '@/api/admin'
import { semesterStatusText, semesterStatusTag, SEMESTER_STATUS_OPTIONS } from '@/utils/dict'

defineOptions({ name: 'AdminSemesters' })

const loading = ref(false)
const submitting = ref(false)
const formVisible = ref(false)

const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '' })

const formRef = ref()
const form = reactive({
  id: null,
  semesterCode: '',
  semesterName: '',
  startDate: '',
  endDate: '',
  selectStartTime: '',
  selectEndTime: '',
  totalWeeks: 20,
  status: 0,
  isCurrent: 0
})

const dateRange = ref([])
const selectRange = ref([])

const rules = {
  semesterCode: [{ required: true, message: '请输入学期编码', trigger: 'blur' }],
  semesterName: [{ required: true, message: '请输入学期名称', trigger: 'blur' }]
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const { data } = await pageSemesters(query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function openForm(row) {
  formVisible.value = true
  if (row) {
    Object.assign(form, {
      id: row.id,
      semesterCode: row.semesterCode,
      semesterName: row.semesterName,
      startDate: row.startDate,
      endDate: row.endDate,
      selectStartTime: row.selectStartTime,
      selectEndTime: row.selectEndTime,
      totalWeeks: row.totalWeeks ?? 20,
      status: row.status ?? 0,
      isCurrent: row.isCurrent ?? 0
    })
    dateRange.value = row.startDate && row.endDate ? [row.startDate, row.endDate] : []
    selectRange.value = row.selectStartTime && row.selectEndTime
      ? [row.selectStartTime, row.selectEndTime]
      : []
  } else {
    Object.assign(form, {
      id: null,
      semesterCode: '',
      semesterName: '',
      startDate: '',
      endDate: '',
      selectStartTime: '',
      selectEndTime: '',
      totalWeeks: 20,
      status: 0,
      isCurrent: 0
    })
    dateRange.value = []
    selectRange.value = []
  }
}

async function submitForm() {
  await formRef.value.validate()

  if (!dateRange.value?.length) {
    ElMessage.error('请选择学期起止日期')
    return
  }

  const payload = {
    ...form,
    startDate: dateRange.value[0],
    endDate: dateRange.value[1],
    selectStartTime: selectRange.value?.[0] || null,
    selectEndTime: selectRange.value?.[1] || null
  }

  submitting.value = true
  try {
    await saveSemester(payload)
    ElMessage.success('保存成功')
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function setCurrent(row) {
  await ElMessageBox.confirm(`确定将「${row.semesterName}」设为当前学期吗？`, '提示', { type: 'warning' })
  await setCurrentSemester(row.id)
  ElMessage.success('已设为当前学期')
  loadData()
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除学期「${row.semesterName}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteSemester(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}
</script>

<style lang="scss" scoped>
.search-bar__spacer {
  flex: 1;
}
</style>
