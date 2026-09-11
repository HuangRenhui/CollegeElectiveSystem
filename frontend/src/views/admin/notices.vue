<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索标题" clearable style="width: 200px" @keyup.enter="handleSearch" />
      <el-select v-model="query.noticeType" placeholder="公告类型" clearable style="width: 140px" @change="handleSearch">
        <el-option v-for="item in NOTICE_TYPE_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="handleSearch">
        <el-option v-for="item in NOTICE_STATUS_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openForm()">发布公告</el-button>
        <span class="text-muted">共 {{ total }} 条公告</span>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="置顶" width="70" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.topFlag === 1" type="danger" size="small" effect="dark">顶</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="noticeTypeTag(row.noticeType)" size="small">{{ noticeTypeText(row.noticeType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="目标角色" width="110">
          <template #default="{ row }">{{ targetRoleText(row.targetRole) }}</template>
        </el-table-column>
        <el-table-column prop="publisher" label="发布人" width="100" />
        <el-table-column prop="publishTime" label="发布时间" width="165" />
        <el-table-column prop="viewCount" label="浏览量" width="90" />
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <el-tag :type="noticeStatusTag(row.status)" size="small">{{ noticeStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button
              v-if="row.status !== 1"
              link
              type="success"
              @click="changeStatus(row, 1)"
            >
              发布
            </el-button>
            <el-button v-else link type="warning" @click="changeStatus(row, 2)">下架</el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑公告' : '发布公告'" width="700px" top="6vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="150" show-word-limit />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="公告类型" prop="noticeType">
              <el-select v-model="form.noticeType" style="width: 100%">
                <el-option v-for="item in NOTICE_TYPE_OPTIONS" :key="item.value" v-bind="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="目标角色" prop="targetRole">
              <el-select v-model="form.targetRole" style="width: 100%">
                <el-option v-for="item in TARGET_ROLE_OPTIONS" :key="item.value" v-bind="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="置顶" prop="topFlag">
              <el-switch v-model="form.topFlag" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="请输入公告内容" maxlength="3000" show-word-limit />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="0">保存为草稿</el-radio>
            <el-radio :value="1">立即发布</el-radio>
          </el-radio-group>
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
import { pageNotices, saveNotice, updateNoticeStatus, deleteNotice } from '@/api/admin'
import {
  NOTICE_TYPE_OPTIONS, NOTICE_STATUS_OPTIONS, TARGET_ROLE_OPTIONS,
  noticeTypeText, noticeTypeTag, noticeStatusText, noticeStatusTag
} from '@/utils/dict'

defineOptions({ name: 'AdminNotices' })

const loading = ref(false)
const submitting = ref(false)
const formVisible = ref(false)

const list = ref([])
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', noticeType: '', status: null })

const formRef = ref()
const form = reactive({
  id: null,
  title: '',
  content: '',
  noticeType: 'SYSTEM',
  targetRole: 'ALL',
  topFlag: 0,
  status: 1
})

const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
}

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const { data } = await pageNotices(query)
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

function handleReset() {
  Object.assign(query, { pageNum: 1, keyword: '', noticeType: '', status: null })
  loadData()
}

function targetRoleText(role) {
  return { ALL: '全部角色', STUDENT: '仅学生', TEACHER: '仅教师' }[role] || role
}

function openForm(row) {
  formVisible.value = true
  if (row) {
    Object.assign(form, {
      id: row.id,
      title: row.title,
      content: row.content,
      noticeType: row.noticeType,
      targetRole: row.targetRole,
      topFlag: row.topFlag ?? 0,
      status: row.status ?? 1
    })
  } else {
    Object.assign(form, {
      id: null,
      title: '',
      content: '',
      noticeType: 'SYSTEM',
      targetRole: 'ALL',
      topFlag: 0,
      status: 1
    })
  }
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    await saveNotice(form)
    ElMessage.success('公告保存成功')
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function changeStatus(row, status) {
  await updateNoticeStatus(row.id, status)
  ElMessage.success(status === 1 ? '公告已发布' : '公告已下架')
  loadData()
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteNotice(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}
</script>
