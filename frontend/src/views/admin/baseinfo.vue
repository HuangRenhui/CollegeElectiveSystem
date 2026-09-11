<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 院系 -->
      <el-tab-pane label="院系管理" name="department">
        <div class="table-toolbar">
          <el-input v-model="deptKeyword" placeholder="搜索院系" clearable size="small" style="width: 200px" @keyup.enter="loadDepartments" />
          <el-button type="primary" size="small" :icon="Plus" @click="openDeptForm()">新增院系</el-button>
        </div>
        <el-table v-loading="loading" :data="departments" stripe border>
          <el-table-column prop="deptCode" label="院系编码" width="120" />
          <el-table-column prop="deptName" label="院系名称" min-width="200" />
          <el-table-column prop="deanName" label="负责人" width="120" />
          <el-table-column prop="contactPhone" label="联系电话" width="150" />
          <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDeptForm(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDeleteDept(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 专业 -->
      <el-tab-pane label="专业管理" name="major">
        <div class="table-toolbar">
          <div style="display: flex; gap: 10px">
            <el-select v-model="majorDeptId" placeholder="按院系筛选" clearable size="small" style="width: 200px" @change="loadMajors">
              <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
            </el-select>
            <el-input v-model="majorKeyword" placeholder="搜索专业" clearable size="small" style="width: 200px" @keyup.enter="loadMajors" />
          </div>
          <el-button type="primary" size="small" :icon="Plus" @click="openMajorForm()">新增专业</el-button>
        </div>
        <el-table v-loading="loading" :data="majors" stripe border>
          <el-table-column prop="majorCode" label="专业编码" width="120" />
          <el-table-column prop="majorName" label="专业名称" min-width="180" />
          <el-table-column label="所属院系" min-width="180">
            <template #default="{ row }">{{ deptName(row.deptId) }}</template>
          </el-table-column>
          <el-table-column prop="degreeType" label="培养层次" width="110" />
          <el-table-column prop="duration" label="学制(年)" width="100" />
          <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMajorForm(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDeleteMajor(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 教室 -->
      <el-tab-pane label="教室管理" name="classroom">
        <div class="table-toolbar">
          <el-input v-model="roomKeyword" placeholder="搜索教室/教学楼" clearable size="small" style="width: 220px" @keyup.enter="loadClassrooms" />
          <el-button type="primary" size="small" :icon="Plus" @click="openRoomForm()">新增教室</el-button>
        </div>
        <el-table v-loading="loading" :data="classrooms" stripe border>
          <el-table-column prop="roomNo" label="教室编号" width="130" />
          <el-table-column prop="building" label="教学楼" min-width="150" />
          <el-table-column prop="capacity" label="容纳人数" width="110" />
          <el-table-column label="类型" width="130">
            <template #default="{ row }">
              <el-tag :type="roomTypeTag(row.roomType)" size="small">{{ roomTypeText(row.roomType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '可用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openRoomForm(row)">编辑</el-button>
              <el-button link type="danger" @click="handleDeleteRoom(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 院系表单 -->
    <el-dialog v-model="deptFormVisible" :title="deptForm.id ? '编辑院系' : '新增院系'" width="520px">
      <el-form ref="deptFormRef" :model="deptForm" :rules="deptRules" label-width="90px">
        <el-form-item label="院系编码" prop="deptCode">
          <el-input v-model="deptForm.deptCode" placeholder="如 CS" />
        </el-form-item>
        <el-form-item label="院系名称" prop="deptName">
          <el-input v-model="deptForm.deptName" placeholder="请输入院系名称" />
        </el-form-item>
        <el-form-item label="负责人" prop="deanName">
          <el-input v-model="deptForm.deanName" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="deptForm.contactPhone" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="deptForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="deptForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitDept">保存</el-button>
      </template>
    </el-dialog>

    <!-- 专业表单 -->
    <el-dialog v-model="majorFormVisible" :title="majorForm.id ? '编辑专业' : '新增专业'" width="520px">
      <el-form ref="majorFormRef" :model="majorForm" :rules="majorRules" label-width="90px">
        <el-form-item label="专业编码" prop="majorCode">
          <el-input v-model="majorForm.majorCode" placeholder="如 CS01" />
        </el-form-item>
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="majorForm.majorName" />
        </el-form-item>
        <el-form-item label="所属院系" prop="deptId">
          <el-select v-model="majorForm.deptId" style="width: 100%">
            <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="培养层次" prop="degreeType">
          <el-select v-model="majorForm.degreeType" style="width: 100%">
            <el-option label="本科" value="本科" />
            <el-option label="专科" value="专科" />
            <el-option label="研究生" value="研究生" />
          </el-select>
        </el-form-item>
        <el-form-item label="学制(年)" prop="duration">
          <el-input-number v-model="majorForm.duration" :min="1" :max="8" />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="majorForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="majorFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMajor">保存</el-button>
      </template>
    </el-dialog>

    <!-- 教室表单 -->
    <el-dialog v-model="roomFormVisible" :title="roomForm.id ? '编辑教室' : '新增教室'" width="520px">
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="90px">
        <el-form-item label="教室编号" prop="roomNo">
          <el-input v-model="roomForm.roomNo" placeholder="如 A101" />
        </el-form-item>
        <el-form-item label="教学楼" prop="building">
          <el-input v-model="roomForm.building" placeholder="如 第一教学楼" />
        </el-form-item>
        <el-form-item label="容纳人数" prop="capacity">
          <el-input-number v-model="roomForm.capacity" :min="1" :max="1000" />
        </el-form-item>
        <el-form-item label="教室类型" prop="roomType">
          <el-select v-model="roomForm.roomType" style="width: 100%">
            <el-option v-for="item in ROOM_TYPE_OPTIONS" :key="item.value" v-bind="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roomForm.status">
            <el-radio :value="1">可用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roomFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitRoom">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  listDepartments, saveDepartment, deleteDepartment,
  listMajors, saveMajor, deleteMajor,
  listClassrooms, saveClassroom, deleteClassroom
} from '@/api/admin'
import { roomTypeText, roomTypeTag, ROOM_TYPE_OPTIONS } from '@/utils/dict'

defineOptions({ name: 'AdminBaseInfo' })

const activeTab = ref('department')
const loading = ref(false)
const submitting = ref(false)

const departments = ref([])
const majors = ref([])
const classrooms = ref([])

const deptKeyword = ref('')
const majorKeyword = ref('')
const majorDeptId = ref(null)
const roomKeyword = ref('')

const deptFormVisible = ref(false)
const deptFormRef = ref()
const deptForm = reactive({ id: null, deptCode: '', deptName: '', deanName: '', contactPhone: '', sort: 0, description: '' })
const deptRules = {
  deptCode: [{ required: true, message: '请输入院系编码', trigger: 'blur' }],
  deptName: [{ required: true, message: '请输入院系名称', trigger: 'blur' }]
}

const majorFormVisible = ref(false)
const majorFormRef = ref()
const majorForm = reactive({ id: null, majorCode: '', majorName: '', deptId: null, degreeType: '本科', duration: 4, description: '' })
const majorRules = {
  majorCode: [{ required: true, message: '请输入专业编码', trigger: 'blur' }],
  majorName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }],
  deptId: [{ required: true, message: '请选择所属院系', trigger: 'change' }]
}

const roomFormVisible = ref(false)
const roomFormRef = ref()
const roomForm = reactive({ id: null, roomNo: '', building: '', capacity: 60, roomType: 'NORMAL', status: 1 })
const roomRules = {
  roomNo: [{ required: true, message: '请输入教室编号', trigger: 'blur' }],
  building: [{ required: true, message: '请输入教学楼', trigger: 'blur' }]
}

onMounted(async () => {
  await loadDepartments()
  await Promise.all([loadMajors(), loadClassrooms()])
})

async function loadDepartments() {
  loading.value = true
  try {
    const { data } = await listDepartments({ keyword: deptKeyword.value })
    departments.value = data || []
  } finally {
    loading.value = false
  }
}

async function loadMajors() {
  loading.value = true
  try {
    const { data } = await listMajors({ deptId: majorDeptId.value, keyword: majorKeyword.value })
    majors.value = data || []
  } finally {
    loading.value = false
  }
}

async function loadClassrooms() {
  loading.value = true
  try {
    const { data } = await listClassrooms({ keyword: roomKeyword.value })
    classrooms.value = data || []
  } finally {
    loading.value = false
  }
}

function deptName(deptId) {
  return departments.value.find((d) => d.id === deptId)?.deptName || '-'
}



// ---------------------------- 院系 ----------------------------
function openDeptForm(row) {
  deptFormVisible.value = true
  Object.assign(deptForm, row
    ? { ...row }
    : { id: null, deptCode: '', deptName: '', deanName: '', contactPhone: '', sort: 0, description: '' })
}

async function submitDept() {
  await deptFormRef.value.validate()
  submitting.value = true
  try {
    await saveDepartment(deptForm)
    ElMessage.success('保存成功')
    deptFormVisible.value = false
    await loadDepartments()
  } finally {
    submitting.value = false
  }
}

function handleDeleteDept(row) {
  ElMessageBox.confirm(`确定删除院系「${row.deptName}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteDepartment(row.id)
      ElMessage.success('删除成功')
      await loadDepartments()
    })
    .catch(() => {})
}

// ---------------------------- 专业 ----------------------------
function openMajorForm(row) {
  majorFormVisible.value = true
  Object.assign(majorForm, row
    ? { ...row }
    : { id: null, majorCode: '', majorName: '', deptId: null, degreeType: '本科', duration: 4, description: '' })
}

async function submitMajor() {
  await majorFormRef.value.validate()
  submitting.value = true
  try {
    await saveMajor(majorForm)
    ElMessage.success('保存成功')
    majorFormVisible.value = false
    await loadMajors()
  } finally {
    submitting.value = false
  }
}

function handleDeleteMajor(row) {
  ElMessageBox.confirm(`确定删除专业「${row.majorName}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteMajor(row.id)
      ElMessage.success('删除成功')
      await loadMajors()
    })
    .catch(() => {})
}

// ---------------------------- 教室 ----------------------------
function openRoomForm(row) {
  roomFormVisible.value = true
  Object.assign(roomForm, row
    ? { ...row }
    : { id: null, roomNo: '', building: '', capacity: 60, roomType: 'NORMAL', status: 1 })
}

async function submitRoom() {
  await roomFormRef.value.validate()
  submitting.value = true
  try {
    await saveClassroom(roomForm)
    ElMessage.success('保存成功')
    roomFormVisible.value = false
    await loadClassrooms()
  } finally {
    submitting.value = false
  }
}

function handleDeleteRoom(row) {
  ElMessageBox.confirm(`确定删除教室「${row.building}${row.roomNo}」吗？`, '删除确认', { type: 'warning' })
    .then(async () => {
      await deleteClassroom(row.id)
      ElMessage.success('删除成功')
      await loadClassrooms()
    })
    .catch(() => {})
}
</script>
