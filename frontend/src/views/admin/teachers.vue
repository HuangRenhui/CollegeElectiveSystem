<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="工号 / 姓名" clearable style="width: 200px" @keyup.enter="handleSearch" />
      <el-select v-model="query.deptId" placeholder="院系" clearable style="width: 190px" @change="handleSearch">
        <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-select v-model="query.title" placeholder="职称" clearable style="width: 140px" @change="handleSearch">
        <el-option v-for="item in TITLE_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openForm()">新增教师</el-button>
        <span class="text-muted">共 {{ total }} 名教师（初始密码 123456）</span>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="teacherNo" label="工号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="所属院系" width="200" show-overflow-tooltip />
        <el-table-column prop="title" label="职称" width="100" />
        <el-table-column prop="researchArea" label="研究方向" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="userStatusTag(row.status)" size="small">
              {{ userStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑教师' : '新增教师'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工号" prop="teacherNo">
              <el-input v-model="form.teacherNo" placeholder="同时作为登录账号" :disabled="!!form.id" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio v-for="item in GENDER_OPTIONS" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="院系" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择院系" style="width: 100%">
                <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称" prop="title">
              <el-select v-model="form.title" placeholder="请选择职称" clearable style="width: 100%">
                <el-option v-for="item in TITLE_OPTIONS" :key="item.value" v-bind="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="研究方向" prop="researchArea">
              <el-input v-model="form.researchArea" type="textarea" :rows="2" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
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
import {
  pageTeachers, createTeacher, updateTeacher, deleteTeacher, updateTeacherStatus, listDepartments
} from '@/api/admin'
import { genderText, userStatusText, userStatusTag, GENDER_OPTIONS, TITLE_OPTIONS } from '@/utils/dict'

defineOptions({ name: 'AdminTeachers' })

const loading = ref(false)
const submitting = ref(false)
const formVisible = ref(false)

const list = ref([])
const total = ref(0)
const departments = ref([])

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', deptId: null, title: '' })

const formRef = ref()
const form = reactive({
  id: null,
  teacherNo: '',
  realName: '',
  gender: 1,
  phone: '',
  email: '',
  deptId: null,
  title: '',
  researchArea: ''
})

const rules = {
  teacherNo: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择院系', trigger: 'change' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

onMounted(async () => {
  try {
    const { data } = await listDepartments({})
    departments.value = data || []
  } catch {
    departments.value = []
  }
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const { data } = await pageTeachers(query)
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
  Object.assign(query, { pageNum: 1, keyword: '', deptId: null, title: '' })
  loadData()
}

function openForm(row) {
  formVisible.value = true
  if (row) {
    Object.assign(form, {
      id: row.id,
      teacherNo: row.teacherNo,
      realName: row.realName,
      gender: row.gender ?? 1,
      phone: row.phone || '',
      email: row.email || '',
      deptId: row.deptId,
      title: row.title || '',
      researchArea: row.researchArea || ''
    })
  } else {
    Object.assign(form, {
      id: null,
      teacherNo: '',
      realName: '',
      gender: 1,
      phone: '',
      email: '',
      deptId: null,
      title: '',
      researchArea: ''
    })
  }
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateTeacher(form)
      ElMessage.success('教师信息修改成功')
    } else {
      await createTeacher(form)
      ElMessage.success('教师创建成功，初始密码 123456')
    }
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await updateTeacherStatus(row.id, next)
  ElMessage.success(next === 1 ? '账号已启用' : '账号已禁用')
  loadData()
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除教师「${row.realName}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteTeacher(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}
</script>
