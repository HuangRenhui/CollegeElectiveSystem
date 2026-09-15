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
        <div>
          <el-button type="primary" :icon="Plus" @click="openForm()">新增学生</el-button>
          <span class="text-muted" style="margin-left: 12px">新建账号的初始密码为 123456</span>
        </div>
        <span class="text-muted">共 {{ total }} 名学生</span>
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
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-button link type="success" @click="openSelection(row)">代选课程</el-button>
            <el-button link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
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

    <!-- 重置密码 -->
    <el-dialog v-model="pwdVisible" title="重置登录密码" width="520px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="重置说明"
        description="重置后该学生当前登录状态会立即失效，需使用新密码重新登录。请通过安全渠道将新密码告知学生本人。"
      />

      <el-form label-width="90px" class="mt-16">
        <el-form-item label="学生">
          <span>{{ pwdTarget.realName }}（学号：{{ pwdTarget.stuNo }}）</span>
        </el-form-item>
        <el-form-item label="重置方式">
          <el-radio-group v-model="pwdMode">
            <el-radio value="default">重置为初始密码</el-radio>
            <el-radio value="custom">自定义新密码</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="pwdMode === 'default'" label="初始密码">
          <el-tag type="info" size="large">{{ DEFAULT_PASSWORD }}</el-tag>
          <div class="form-tip">与新建账号时下发的初始密码一致。</div>
        </el-form-item>
        <el-form-item v-else label="新密码" prop="newPassword">
          <el-input
            v-model="pwdForm.newPassword"
            type="password"
            show-password
            placeholder="请输入 6-20 位新密码"
            maxlength="20"
          />
          <div class="form-tip">长度 6-20 位，建议包含字母与数字组合。</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="submitResetPassword">确定重置</el-button>
      </template>
    </el-dialog>

    <!-- 教务代选 -->
    <el-dialog
      v-model="selectionVisible"
      :title="`代选课程 - ${selectionTarget.realName || ''}`"
      width="900px"
      :close-on-click-modal="false"
    >
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="教务代选说明"
        description="代选用于处理学生错过选课时间、重修插班等特殊情况，不受选课时间窗口限制，但仍会校验学分上限、上课时间冲突、课程容量与重复选课。代选记录将标记为「管理员代选」以便审计，请如实填写代选原因。"
      />

      <el-descriptions :column="3" border class="mt-16">
        <el-descriptions-item label="学号">{{ selectionTarget.stuNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ selectionTarget.realName }}</el-descriptions-item>
        <el-descriptions-item label="班级">{{ selectionTarget.className || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前学期">{{ currentSemesterName }}</el-descriptions-item>
        <el-descriptions-item label="已选学分">
          <span :class="{ 'text-danger': selectedCredit > creditLimit }">
            {{ selectedCredit }} / {{ creditLimit || '不限' }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="已选门数">{{ mySelections.length }}</el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="selectionTab" class="mt-16">
        <!-- 已选课程 -->
        <el-tab-pane :label="`已选课程（${mySelections.length}）`" name="selected">
          <el-table :data="mySelections" size="small" v-loading="selectionLoading" empty-text="该学生本学期暂无选课记录">
            <el-table-column prop="courseCode" label="课程编号" width="110" />
            <el-table-column prop="courseName" label="课程名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="teacherName" label="教师" width="90" />
            <el-table-column prop="credit" label="学分" width="70" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="selectionStatusTag(row.status)" size="small">
                  {{ selectionStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="选课方式" width="110">
              <template #default="{ row }">
                <el-tag :type="selectTypeTag(row.selectType)" size="small" effect="plain">
                  {{ selectTypeText(row.selectType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成绩" width="80" align="center">
              <template #default="{ row }">
                <span v-if="row.score !== null && row.score !== undefined">{{ row.score }}</span>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 1"
                  link
                  type="danger"
                  :loading="operatingId === `drop-${row.courseId}`"
                  @click="handleAdminDrop(row)"
                >
                  退选
                </el-button>
                <el-tooltip v-else-if="row.status === 2" content="已修完的课程不可退选" placement="top">
                  <span class="text-muted">-</span>
                </el-tooltip>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 可选课程 -->
        <el-tab-pane :label="`代选课程（${availableCourses.length}）`" name="available">
          <div class="selection-toolbar">
            <el-input
              v-model="courseKeyword"
              placeholder="课程名称 / 编号"
              clearable
              size="small"
              style="width: 200px"
              @keyup.enter="loadAvailableCourses"
            />
            <el-select v-model="courseDeptId" placeholder="开课院系" clearable size="small" style="width: 180px" @change="loadAvailableCourses">
              <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
            </el-select>
            <el-checkbox v-model="onlyAvailable" @change="loadAvailableCourses">仅看有余量</el-checkbox>
            <el-button size="small" type="primary" @click="loadAvailableCourses">查询</el-button>
            <span class="text-muted" style="margin-left: auto">共 {{ availableTotal }} 门可选</span>
          </div>

          <el-table :data="availableCourses" size="small" v-loading="courseLoading" empty-text="没有符合条件的课程">
            <el-table-column prop="courseCode" label="课程编号" width="110" />
            <el-table-column prop="courseName" label="课程名称" min-width="140" show-overflow-tooltip />
            <el-table-column prop="teacherName" label="教师" width="90" />
            <el-table-column prop="credit" label="学分" width="70" />
            <el-table-column label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="courseTypeTag(row.courseType)" size="small">
                  {{ courseTypeText(row.courseType) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="余量" width="100">
              <template #default="{ row }">
                <span :class="{ 'text-danger': isFull(row) }">
                  {{ row.remainingCapacity ?? '-' }} / {{ row.maxCapacity }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag v-if="row.selected" type="success" size="small" effect="plain">已选</el-tag>
                <el-tag v-else-if="isFull(row)" type="danger" size="small" effect="plain">已满</el-tag>
                <el-tag v-else type="info" size="small" effect="plain">可选</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :disabled="row.selected"
                  :loading="operatingId === `add-${row.id}`"
                  @click="handleAdminAdd(row)"
                >
                  代选
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="courseQuery.pageNum"
              v-model:page-size="courseQuery.pageSize"
              :page-sizes="[10, 20, 50]"
              :total="availableTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="loadAvailableCourses"
              @current-change="loadAvailableCourses"
            />
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="selectionVisible = false">关闭</el-button>
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
  listDepartments, listMajors, resetUserPassword,
  listStudentSelections, addSelectionForStudent, removeSelectionForStudent
} from '@/api/admin'
import { listCourses } from '@/api/student'
import { getCurrentSemester } from '@/api/common'
import {
  genderText, userStatusText, userStatusTag, GENDER_OPTIONS,
  selectionStatusText, selectionStatusTag, selectTypeText, selectTypeTag, courseTypeText, courseTypeTag
} from '@/utils/dict'

defineOptions({ name: 'AdminStudents' })

/** 新建账号与重置时使用的初始密码 */
const DEFAULT_PASSWORD = '123456'

const loading = ref(false)
const submitting = ref(false)
const resetting = ref(false)
const formVisible = ref(false)

const pwdVisible = ref(false)
const pwdMode = ref('default')
const pwdTarget = reactive({})
const pwdForm = reactive({ newPassword: '' })

// ---------------- 教务代选 ----------------
const selectionVisible = ref(false)
const selectionTab = ref('selected')
const selectionLoading = ref(false)
const courseLoading = ref(false)
const operatingId = ref(null)
const selectionTarget = reactive({})
const mySelections = ref([])
const availableCourses = ref([])
const availableTotal = ref(0)
const creditLimit = ref(0)
const currentSemesterName = ref('-')
const courseKeyword = ref('')
const courseDeptId = ref(null)
const onlyAvailable = ref(true)

const courseQuery = reactive({ pageNum: 1, pageSize: 10 })

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

function handleResetPassword(row) {
  if (!row.userId) {
    ElMessage.warning('该学生未关联登录账号，无法重置密码')
    return
  }
  Object.assign(pwdTarget, row)
  pwdMode.value = 'default'
  pwdForm.newPassword = ''
  pwdVisible.value = true
}

async function submitResetPassword() {
  const newPassword = pwdMode.value === 'default' ? DEFAULT_PASSWORD : pwdForm.newPassword.trim()

  if (newPassword.length < 6 || newPassword.length > 20) {
    ElMessage.warning('密码长度需为 6-20 位')
    return
  }

  resetting.value = true
  try {
    await resetUserPassword(pwdTarget.userId, newPassword)
    pwdVisible.value = false
    ElMessage.success(
      pwdMode.value === 'default'
        ? `已重置为初始密码 ${DEFAULT_PASSWORD}，请告知学生及时修改`
        : '密码重置成功，请告知学生及时修改'
    )
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    resetting.value = false
  }
}

// ============================ 教务代选 ============================

/** 已选学分（仅统计在修课程，已退选不计入） */
const selectedCredit = computed(() =>
  mySelections.value
    .filter((item) => item.status === 1 || item.status === 2)
    .reduce((sum, item) => sum + Number(item.credit || 0), 0)
)

async function loadSemesterInfo() {
  try {
    const { data } = await getCurrentSemester()
    currentSemesterName.value = data?.semesterName || '-'
    courseQuery.semesterId = data?.id || null
  } catch {
    currentSemesterName.value = '-'
  }
}

function openSelection(row) {
  Object.assign(selectionTarget, row)
  selectionTab.value = 'selected'
  courseKeyword.value = ''
  courseDeptId.value = null
  courseQuery.pageNum = 1
  selectionVisible.value = true
  loadSemesterInfo()
  loadMySelections()
}

async function loadMySelections() {
  selectionLoading.value = true
  try {
    const { data } = await listStudentSelections(selectionTarget.id, {})
    mySelections.value = data?.records || data || []
  } catch {
    mySelections.value = []
  } finally {
    selectionLoading.value = false
  }
}

async function loadAvailableCourses() {
  courseLoading.value = true
  try {
    const { data } = await listCourses({
      ...courseQuery,
      courseName: courseKeyword.value,
      deptId: courseDeptId.value,
      onlyAvailable: onlyAvailable.value,
      semesterId: courseQuery.semesterId
    })
    availableCourses.value = data?.records || []
    availableTotal.value = Number(data?.total || 0)
  } catch {
    availableCourses.value = []
    availableTotal.value = 0
  } finally {
    courseLoading.value = false
  }
}

function isFull(course) {
  const remaining = course.remainingCapacity
  return remaining !== null && remaining !== undefined ? remaining <= 0 : false
}

/**
 * 代选课程。先请求冲突预检，有冲突则要求管理员二次确认后再提交。
 */
async function handleAdminAdd(course) {
  let reason
  try {
    const result = await ElMessageBox.prompt(
      `即将为「${selectionTarget.realName}」代选《${course.courseName}》（${course.credit} 学分）。\n请填写代选原因，该信息将记入操作日志以便审计。`,
      '代选原因',
      {
        inputPlaceholder: '如：学生错过选课时间，已提交书面申请',
        inputValidator: (value) => (value && value.trim().length >= 2) || '请填写不少于 2 个字的代选原因',
        confirmButtonText: '提交代选',
        type: 'warning',
        inputType: 'textarea'
      }
    )
    reason = result.value.trim()
  } catch {
    return
  }

  operatingId.value = `add-${course.id}`
  try {
    await addSelectionForStudent(selectionTarget.id, course.id, reason)
    ElMessage.success(`已为 ${selectionTarget.realName} 代选《${course.courseName}》`)
    await Promise.all([loadMySelections(), loadAvailableCourses()])
    selectionTab.value = 'selected'
  } catch {
    // 学分超限、时间冲突、容量已满等错误已由拦截器提示
    await Promise.all([loadMySelections(), loadAvailableCourses()])
  } finally {
    operatingId.value = null
  }
}

/**
 * 代退选。已录入成绩的课程不可退选。
 */
async function handleAdminDrop(row) {
  if (row.score !== null && row.score !== undefined) {
    ElMessage.warning('该课程已录入成绩，无法代退选')
    return
  }

  let reason
  try {
    const result = await ElMessageBox.prompt(
      `即将为「${selectionTarget.realName}」退选《${row.courseName}》。\n退选不删除记录，将保留痕迹以便审计。`,
      '代退选原因',
      {
        inputPlaceholder: '如：学生申请退课，已提交书面材料',
        inputValidator: (value) => (value && value.trim().length >= 2) || '请填写不少于 2 个字的退选原因',
        confirmButtonText: '确认退选',
        type: 'warning',
        inputType: 'textarea'
      }
    )
    reason = result.value.trim()
  } catch {
    return
  }

  operatingId.value = `drop-${row.courseId}`
  try {
    await removeSelectionForStudent(selectionTarget.id, row.courseId, reason)
    ElMessage.success(`已退选《${row.courseName}》`)
    await Promise.all([loadMySelections(), loadAvailableCourses()])
  } catch {
    // 错误已由请求拦截器提示
  } finally {
    operatingId.value = null
  }
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

<style lang="scss" scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.selection-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}
</style>
