<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="学号 / 姓名 / 班级" clearable style="width: 200px" @keyup.enter="handleSearch" />
      <el-select v-model="query.deptId" placeholder="院系" clearable style="width: 180px" @change="onDeptChange">
        <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-select v-model="query.majorId" placeholder="专业" clearable style="width: 180px" @change="handleSearch">
        <el-option v-for="m in majors" :key="m.id" :label="m.majorName" :value="m.id" />
      </el-select>
      <el-select v-model="query.gradeYear" placeholder="年级" clearable style="width: 110px" @change="handleSearch">
        <el-option v-for="year in [1, 2, 3, 4]" :key="year" :label="`${year}年级`" :value="year" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card shadow="never">
      <div class="table-toolbar">
        <el-button type="primary" :icon="Plus" @click="openForm()">新增学生</el-button>
        <span class="text-muted">共 {{ total }} 名学生（初始密码 123456）</span>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="stuNo" label="学号" width="130" />
        <el-table-column prop="realName" label="姓名" width="110" />
        <el-table-column label="性别" width="80">
          <template #default="{ row }">{{ genderText(row.gender) }}</template>
        </el-table-column>
        <el-table-column prop="deptName" label="院系" width="180" show-overflow-tooltip />
        <el-table-column prop="majorName" label="专业" width="150" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column prop="gradeYear" label="年级" width="80">
          <template #default="{ row }">{{ row.gradeYear }}年级</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="totalCredit" label="已获学分" width="100" />
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

    <el-dialog v-model="formVisible" :title="form.id ? '编辑学生' : '新增学生'" width="640px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学号" prop="stuNo">
              <el-input v-model="form.stuNo" placeholder="同时作为登录账号" :disabled="!!form.id" />
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
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="院系" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择院系" style="width: 100%" @change="onFormDeptChange">
                <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="专业" prop="majorId">
              <el-select v-model="form.majorId" placeholder="请选择专业" style="width: 100%">
                <el-option v-for="m in formMajors" :key="m.id" :label="m.majorName" :value="m.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行政班级" prop="className">
              <el-input v-model="form.className" placeholder="如 计科2601" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="gradeYear">
              <el-select v-model="form.gradeYear" style="width: 100%">
                <el-option v-for="year in [1, 2, 3, 4]" :key="year" :label="`${year}年级`" :value="year" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入学年份" prop="enrollYear">
              <el-input-number v-model="form.enrollYear" :min="2000" :max="2100" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  pageStudents, createStudent, updateStudent, deleteStudent, updateStudentStatus,
  listDepartments, listMajors
} from '@/api/admin'
import { genderText, userStatusText, userStatusTag, GENDER_OPTIONS } from '@/utils/dict'

defineOptions({ name: 'AdminStudents' })

const loading = ref(false)
const submitting = ref(false)
const formVisible = ref(false)

const list = ref([])
const total = ref(0)
const departments = ref([])
const majors = ref([])
const formMajors = ref([])

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', deptId: null, majorId: null, gradeYear: null })

const formRef = ref()
const form = reactive({
  id: null,
  stuNo: '',
  realName: '',
  gender: 1,
  phone: '',
  email: '',
  deptId: null,
  majorId: null,
  className: '',
  gradeYear: 1,
  enrollYear: new Date().getFullYear()
})

const rules = {
  stuNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择院系', trigger: 'change' }],
  majorId: [{ required: true, message: '请选择专业', trigger: 'change' }],
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
    const { data } = await pageStudents(query)
    list.value = data.records || []
    total.value = Number(data.total || 0)
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function onDeptChange() {
  query.majorId = null
  if (query.deptId) {
    const { data } = await listMajors({ deptId: query.deptId })
    majors.value = data || []
  } else {
    majors.value = []
  }
  handleSearch()
}

async function onFormDeptChange() {
  form.majorId = null
  if (form.deptId) {
    const { data } = await listMajors({ deptId: form.deptId })
    formMajors.value = data || []
  } else {
    formMajors.value = []
  }
}

function handleSearch() {
  query.pageNum = 1
  loadData()
}

function handleReset() {
  Object.assign(query, { pageNum: 1, keyword: '', deptId: null, majorId: null, gradeYear: null })
  majors.value = []
  loadData()
}

async function openForm(row) {
  formVisible.value = true
  if (!formMajors.value.length || form.deptId !== row?.deptId) {
    const deptId = row?.deptId || form.deptId
    if (deptId) {
      const { data } = await listMajors({ deptId })
      formMajors.value = data || []
    }
  }

  if (row) {
    Object.assign(form, {
      id: row.id,
      stuNo: row.stuNo,
      realName: row.realName,
      gender: row.gender ?? 1,
      phone: row.phone || '',
      email: row.email || '',
      deptId: row.deptId,
      majorId: row.majorId,
      className: row.className,
      gradeYear: row.gradeYear ?? 1,
      enrollYear: row.enrollYear ?? new Date().getFullYear()
    })
  } else {
    Object.assign(form, {
      id: null,
      stuNo: '',
      realName: '',
      gender: 1,
      phone: '',
      email: '',
      deptId: null,
      majorId: null,
      className: '',
      gradeYear: 1,
      enrollYear: new Date().getFullYear()
    })
    formMajors.value = []
  }
}

async function submitForm() {
  await formRef.value.validate()
  submitting.value = true
  try {
    if (form.id) {
      await updateStudent(form)
      ElMessage.success('学生信息修改成功')
    } else {
      await createStudent(form)
      ElMessage.success('学生创建成功，初始密码 123456')
    }
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await updateStudentStatus(row.id, next)
  ElMessage.success(next === 1 ? '账号已启用' : '账号已禁用')
  loadData()
}

function handleDelete(row) {
  ElMessageBox.confirm(`确定要删除学生「${row.realName}」吗？删除后该账号将无法登录。`, '删除确认', {
    type: 'warning'
  })
    .then(async () => {
      await deleteStudent(row.id)
      ElMessage.success('删除成功')
      loadData()
    })
    .catch(() => {})
}
</script>
