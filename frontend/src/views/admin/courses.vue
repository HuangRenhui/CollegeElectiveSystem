<template>
  <div class="page-container">
    <div class="search-bar">
      <el-input v-model="query.courseName" placeholder="课程名称" clearable style="width: 170px" @keyup.enter="handleSearch" />
      <el-input v-model="query.courseCode" placeholder="课程编号" clearable style="width: 140px" @keyup.enter="handleSearch" />
      <el-select v-model="query.semesterId" placeholder="学期" clearable style="width: 200px" @change="handleSearch">
        <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
      </el-select>
      <el-select v-model="query.deptId" placeholder="院系" clearable style="width: 180px" @change="handleSearch">
        <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
      </el-select>
      <el-select v-model="query.courseType" placeholder="类型" clearable style="width: 120px" @change="handleSearch">
        <el-option v-for="item in COURSE_TYPE_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px" @change="handleSearch">
        <el-option v-for="item in COURSE_STATUS_OPTIONS" :key="item.value" v-bind="item" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <el-card shadow="never">
      <div class="table-toolbar">
        <div>
          <el-button type="primary" :icon="Plus" @click="openForm()">新增课程</el-button>
          <el-button :icon="Refresh" @click="openPreload">预热选课缓存</el-button>
          <el-button @click="openSync">同步选课人数</el-button>
        </div>
        <span class="text-muted">共 {{ total }} 门课程</span>
      </div>

      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column prop="courseCode" label="课程编号" width="110" />
        <el-table-column prop="courseName" label="课程名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="teacherName" label="授课教师" width="100" />
        <el-table-column prop="deptName" label="开课院系" width="170" show-overflow-tooltip />
        <el-table-column prop="semesterName" label="学期" width="180" show-overflow-tooltip />
        <el-table-column label="类型" width="85">
          <template #default="{ row }">
            <el-tag :type="courseTypeTag(row.courseType)" size="small">{{ courseTypeText(row.courseType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="credit" label="学分" width="70" />
        <el-table-column prop="hours" label="学时" width="70" />
        <el-table-column label="选课情况" width="140">
          <template #default="{ row }">
            <div class="capacity-cell">
              <span>{{ row.selectedCount }} / {{ row.maxCapacity }}</span>
              <el-progress
                :percentage="capacityPercent(row)"
                :stroke-width="6"
                :show-text="false"
                :status="capacityPercent(row) >= 100 ? 'exception' : undefined"
              />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="courseStatusTag(row.status)" size="small">{{ courseStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-dropdown @command="(cmd) => handleCommand(cmd, row)">
              <el-button link type="primary">更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="detail">详情</el-dropdown-item>
                  <el-dropdown-item command="status-1">上架</el-dropdown-item>
                  <el-dropdown-item command="status-0">下架</el-dropdown-item>
                  <el-dropdown-item command="status-2">结课</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <!-- 课程表单 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑课程' : '新增课程'" width="880px" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="课程编号" prop="courseCode">
              <el-input v-model="form.courseCode" placeholder="如 GE1001" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程名称" prop="courseName">
              <el-input v-model="form.courseName" placeholder="请输入课程名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开课学期" prop="semesterId">
              <el-select v-model="form.semesterId" placeholder="请选择学期" style="width: 100%">
                <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="开课院系" prop="deptId">
              <el-select v-model="form.deptId" placeholder="请选择院系" style="width: 100%">
                <el-option v-for="d in departments" :key="d.id" :label="d.deptName" :value="d.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="授课教师" prop="teacherId">
              <el-select v-model="form.teacherId" placeholder="请选择教师" clearable filterable style="width: 100%">
                <el-option
                  v-for="t in teachers"
                  :key="t.id"
                  :label="`${t.realName}（${t.teacherNo}）`"
                  :value="t.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程类型" prop="courseType">
              <el-select v-model="form.courseType" style="width: 100%">
                <el-option v-for="item in COURSE_TYPE_OPTIONS" :key="item.value" v-bind="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="学分" prop="credit">
              <el-input-number v-model="form.credit" :min="0.5" :max="20" :step="0.5" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="总学时" prop="hours">
              <el-input-number v-model="form.hours" :min="1" :max="300" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="课程容量" prop="maxCapacity">
              <el-input-number v-model="form.maxCapacity" :min="1" :max="1000" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考核方式" prop="examType">
              <el-select v-model="form.examType" style="width: 100%">
                <el-option v-for="item in EXAM_TYPE_OPTIONS" :key="item.value" v-bind="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="课程状态" prop="status">
              <el-select v-model="form.status" style="width: 100%">
                <el-option v-for="item in COURSE_STATUS_OPTIONS" :key="item.value" v-bind="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="教材">
              <el-input v-model="form.textbook" placeholder="请输入教材名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="课程简介">
              <el-input v-model="form.introduce" type="textarea" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 排课信息 -->
        <el-divider content-position="left">
          排课信息
          <el-button link type="primary" :icon="Plus" @click="addSchedule">添加时段</el-button>
        </el-divider>

        <el-table :data="form.schedules" size="small" empty-text="请添加上课时段">
          <el-table-column label="星期" width="120">
            <template #default="{ row }">
              <el-select v-model="row.dayOfWeek" size="small" style="width: 100%">
                <el-option v-for="(label, index) in DAY_LABELS" :key="label" :label="label" :value="index + 1" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="开始节次" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.startSection" :min="1" :max="12" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="结束节次" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.endSection" :min="1" :max="12" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="起始周" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.startWeek" :min="1" :max="30" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="结束周" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.endWeek" :min="1" :max="30" size="small" controls-position="right" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="周次类型" width="110">
            <template #default="{ row }">
              <el-select v-model="row.weekType" size="small" style="width: 100%">
                <el-option v-for="item in WEEK_TYPE_OPTIONS" :key="item.value" v-bind="item" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="教室" min-width="160">
            <template #default="{ row }">
              <el-select v-model="row.classroomId" size="small" clearable filterable placeholder="选择教室" style="width: 100%">
                <el-option
                  v-for="room in classrooms"
                  :key="room.id"
                  :label="`${room.building}${room.roomNo}（${room.capacity}人）`"
                  :value="room.id"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.schedules.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-form>

      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>

    <!-- 课程详情 -->
    <el-dialog v-model="detailVisible" :title="detail.courseName" width="700px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="课程编号">{{ detail.courseCode }}</el-descriptions-item>
        <el-descriptions-item label="课程类型">{{ courseTypeText(detail.courseType) }}</el-descriptions-item>
        <el-descriptions-item label="授课教师">{{ detail.teacherName || '待定' }}</el-descriptions-item>
        <el-descriptions-item label="开课院系">{{ detail.deptName }}</el-descriptions-item>
        <el-descriptions-item label="学分">{{ detail.credit }}</el-descriptions-item>
        <el-descriptions-item label="学时">{{ detail.hours }}</el-descriptions-item>
        <el-descriptions-item label="选课情况">
          {{ detail.selectedCount || 0 }} / {{ detail.maxCapacity }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">{{ courseStatusText(detail.status) }}</el-descriptions-item>
        <el-descriptions-item label="教材" :span="2">{{ detail.textbook || '未指定' }}</el-descriptions-item>
        <el-descriptions-item label="课程简介" :span="2">{{ detail.introduce || '暂无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 缓存预热弹窗 -->
    <el-dialog v-model="preloadVisible" title="预热选课缓存" width="520px" :close-on-click-modal="false">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="预热说明"
        description="将所选学期课程的余量与已选学生名单写入缓存，用于提升选课期间的响应速度。建议在每日开放选课前执行一次。"
      />

      <el-form label-width="90px" class="mt-16">
        <el-form-item label="目标学期">
          <el-select v-model="preloadSemesterId" placeholder="请选择学期" clearable style="width: 100%">
            <el-option v-for="s in semesters" :key="s.id" :label="s.semesterName" :value="s.id" />
          </el-select>
          <div class="form-tip">
            默认使用列表当前筛选的学期；如未筛选则使用当前学期（{{ appStore.currentSemester?.semesterName || '未设置' }}）。
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="preloadVisible = false">取消</el-button>
        <el-button type="primary" :loading="preloading" @click="doPreload">开始预热</el-button>
      </template>
    </el-dialog>

    <!-- 同步选课人数弹窗 -->
    <el-dialog v-model="syncVisible" title="同步选课人数" width="520px" :close-on-click-modal="false">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="同步说明"
        description="将按选课记录重新统计各课程的已选人数，并覆盖缓存的余量数据。当页面显示的余量与实际人数不一致时使用。"
      />
      <p class="text-muted mt-16">同步过程可能耗时较长，请勿重复提交。</p>

      <template #footer>
        <el-button @click="syncVisible = false">取消</el-button>
        <el-button type="primary" :loading="syncing" @click="doSync">确定同步</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, ArrowDown } from '@element-plus/icons-vue'
import {
  pageCourses, createCourse, updateCourse, deleteCourse, updateCourseStatus,
  preloadCourseCache, syncSelectionCount, listDepartments, listTeacherOptions, listClassrooms,
  listSemesterOptions
} from '@/api/admin'
import {
  COURSE_TYPE_OPTIONS, COURSE_STATUS_OPTIONS, EXAM_TYPE_OPTIONS, WEEK_TYPE_OPTIONS, DAY_LABELS,
  courseTypeText, courseTypeTag, courseStatusText, courseStatusTag
} from '@/utils/dict'
import { useAppStore } from '@/store/modules/app'

defineOptions({ name: 'AdminCourses' })

const appStore = useAppStore()

const loading = ref(false)
const submitting = ref(false)
const formVisible = ref(false)
const detailVisible = ref(false)
const preloading = ref(false)
const syncing = ref(false)
const preloadVisible = ref(false)
const syncVisible = ref(false)
const preloadSemesterId = ref(null)

const list = ref([])
const total = ref(0)
const semesters = ref([])
const departments = ref([])
const teachers = ref([])
const classrooms = ref([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  courseName: '',
  courseCode: '',
  semesterId: null,
  deptId: null,
  courseType: '',
  status: null
})

const formRef = ref()
const form = reactive({
  id: null,
  courseCode: '',
  courseName: '',
  semesterId: null,
  deptId: null,
  teacherId: null,
  credit: 2,
  hours: 32,
  courseType: 'ELECTIVE',
  examType: 'CHECK',
  maxCapacity: 60,
  textbook: '',
  introduce: '',
  selectable: 1,
  status: 1,
  schedules: []
})

const rules = {
  courseCode: [{ required: true, message: '请输入课程编号', trigger: 'blur' }],
  courseName: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  semesterId: [{ required: true, message: '请选择开课学期', trigger: 'change' }],
  deptId: [{ required: true, message: '请选择开课院系', trigger: 'change' }],
  courseType: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  credit: [{ required: true, message: '请填写学分', trigger: 'blur' }],
  hours: [{ required: true, message: '请填写总学时', trigger: 'blur' }],
  maxCapacity: [{ required: true, message: '请填写课程容量', trigger: 'blur' }]
}

const detail = reactive({})

onMounted(async () => {
  await loadOptions()
  loadData()
})

async function loadOptions() {
  try {
    const [semesterRes, deptRes, teacherRes, roomRes] = await Promise.all([
      listSemesterOptions(),
      listDepartments({}),
      listTeacherOptions({}),
      listClassrooms({})
    ])
    semesters.value = semesterRes.data || []
    departments.value = deptRes.data || []
    teachers.value = teacherRes.data || []
    classrooms.value = roomRes.data || []
  } catch {
    // 忽略选项加载失败
  }
}

async function loadData() {
  loading.value = true
  try {
    const { data } = await pageCourses(query)
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
  Object.assign(query, {
    pageNum: 1,
    courseName: '',
    courseCode: '',
    semesterId: null,
    deptId: null,
    courseType: '',
    status: null
  })
  loadData()
}

function capacityPercent(row) {
  if (!row.maxCapacity) return 0
  return Math.min(Math.round(((row.selectedCount || 0) / row.maxCapacity) * 100), 100)
}

function addSchedule() {
  form.schedules.push({
    dayOfWeek: 1,
    startSection: 1,
    endSection: 2,
    startWeek: 1,
    endWeek: 16,
    weekType: 'ALL',
    classroomId: null
  })
}

async function openForm(row) {
  formVisible.value = true
  if (row) {
    const { data } = await pageCourses({ pageNum: 1, pageSize: 1, courseCode: row.courseCode })
    const target = data.records?.[0] || row
    Object.assign(form, {
      id: target.id,
      courseCode: target.courseCode,
      courseName: target.courseName,
      semesterId: target.semesterId,
      deptId: target.deptId,
      teacherId: target.teacherId,
      credit: Number(target.credit),
      hours: target.hours,
      courseType: target.courseType,
      examType: target.examType,
      maxCapacity: target.maxCapacity,
      textbook: target.textbook,
      introduce: target.introduce,
      selectable: target.selectable,
      status: target.status,
      schedules: []
    })
    // 加载已有排课
    try {
      const detailRes = await pageCourses({ pageNum: 1, pageSize: 1 })
      void detailRes
    } catch {
      // 忽略
    }
    if (Array.isArray(target.schedules)) {
      form.schedules = target.schedules.map((s) => ({
        dayOfWeek: s.dayOfWeek,
        startSection: s.startSection,
        endSection: s.endSection,
        startWeek: s.startWeek,
        endWeek: s.endWeek,
        weekType: s.weekType,
        classroomId: s.classroomId
      }))
    }
  } else {
    Object.assign(form, {
      id: null,
      courseCode: '',
      courseName: '',
      semesterId: appStore.currentSemester?.id || null,
      deptId: null,
      teacherId: null,
      credit: 2,
      hours: 32,
      courseType: 'ELECTIVE',
      examType: 'CHECK',
      maxCapacity: 60,
      textbook: '',
      introduce: '',
      selectable: 1,
      status: 1,
      schedules: []
    })
  }
}

async function submitForm() {
  await formRef.value.validate()

  for (const schedule of form.schedules) {
    if (schedule.startSection > schedule.endSection) {
      ElMessage.error('排课的开始节次不能大于结束节次')
      return
    }
  }

  submitting.value = true
  try {
    const payload = { ...form }
    if (form.id) {
      await updateCourse(payload)
      ElMessage.success('课程修改成功')
    } else {
      await createCourse(payload)
      ElMessage.success('课程创建成功')
    }
    formVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

function handleCommand(command, row) {
  if (command === 'detail') {
    Object.assign(detail, row)
    detailVisible.value = true
  } else if (command.startsWith('status-')) {
    const status = Number(command.split('-')[1])
    updateCourseStatus(row.id, status).then(() => {
      ElMessage.success('课程状态已更新')
      loadData()
    })
  } else if (command === 'delete') {
    ElMessageBox.confirm(`确定要删除课程《${row.courseName}》吗？`, '删除确认', { type: 'warning' })
      .then(async () => {
        await deleteCourse(row.id)
        ElMessage.success('删除成功')
        loadData()
      })
      .catch(() => {})
  }
}

function openPreload() {
  preloadSemesterId.value = query.semesterId || appStore.currentSemester?.id || null
  preloadVisible.value = true
}

async function doPreload() {
  preloading.value = true
  try {
    await preloadCourseCache(preloadSemesterId.value || appStore.currentSemester?.id)
    preloadVisible.value = false
    ElMessage.success('选课缓存预热完成')
    await loadData()
  } catch {
    // 未实现或执行失败已由拦截器提示，保留弹窗便于调整后重试
  } finally {
    preloading.value = false
  }
}

function openSync() {
  syncVisible.value = true
}

async function doSync() {
  syncing.value = true
  try {
    await syncSelectionCount()
    syncVisible.value = false
    ElMessage.success('选课人数同步完成')
    await loadData()
  } catch {
    // 未实现或执行失败已由拦截器提示
  } finally {
    syncing.value = false
  }
}
</script>

<style lang="scss" scoped>
.capacity-cell {
  span {
    font-size: 12.5px;
  }
}

.form-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}
</style>
