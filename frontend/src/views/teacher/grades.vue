<template>
  <div class="page-container">
    <!-- 课程信息 -->
    <el-card shadow="never" class="course-info">
      <div class="course-info__row">
        <div>
          <el-page-header @back="$router.push('/teacher/workbench')">
            <template #content>
              <span style="font-size: 16px; font-weight: 600">
                {{ course.courseName || '成绩录入' }}
              </span>
              <el-tag v-if="course.courseCode" size="small" style="margin-left: 10px">
                {{ course.courseCode }}
              </el-tag>
            </template>
          </el-page-header>
        </div>
        <div class="course-info__actions">
          <el-tag type="info" effect="plain">{{ course.credit }} 学分</el-tag>
          <el-tag type="success" effect="plain">已选 {{ sheet.length }} 人</el-tag>
          <el-button type="primary" :loading="saving" @click="handleSave">
            <el-icon><DocumentAdd /></el-icon>保存草稿
          </el-button>
          <el-button type="success" :loading="publishing" @click="handlePublish">
            <el-icon><Promotion /></el-icon>发布成绩
          </el-button>
          <el-button type="warning" plain @click="handleRevoke">撤回发布</el-button>
        </div>
      </div>

      <el-alert
        type="info"
        :closable="false"
        show-icon
        style="margin-top: 14px"
        title="成绩计算规则"
        description="总评成绩 = 平时成绩 × 30% + 期末成绩 × 70%。成绩发布后学生即可查询，如需修改请先撤回发布。"
      />
    </el-card>

    <el-card shadow="never" class="mt-16" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>成绩录入单</span>
          <div>
            <el-input v-model="keyword" placeholder="搜索姓名/学号" clearable size="small" style="width: 180px" />
            <el-button size="small" style="margin-left: 8px" @click="fillAllWith">批量填充</el-button>
          </div>
        </div>
      </template>

      <el-table :data="filteredSheet" stripe :default-sort="{ prop: 'stuNo', order: 'ascending' }" empty-text="暂无学生选课">
        <el-table-column type="index" label="#" width="56" />
        <el-table-column prop="stuNo" label="学号" width="130" sortable />
        <el-table-column prop="studentName" label="姓名" width="110" />
        <el-table-column prop="className" label="班级" width="120" />
        <el-table-column label="平时成绩（30%）" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.usualScore"
              :min="0"
              :max="100"
              :precision="1"
              :step="1"
              size="small"
              controls-position="right"
              style="width: 120px"
              :disabled="isPublished(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="期末成绩（70%）" width="160">
          <template #default="{ row }">
            <el-input-number
              v-model="row.examScore"
              :min="0"
              :max="100"
              :precision="1"
              :step="1"
              size="small"
              controls-position="right"
              style="width: 120px"
              :disabled="isPublished(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="总评" width="90" align="center">
          <template #default="{ row }">
            <span :style="{ color: scoreColor(calcTotal(row)), fontWeight: 700 }">
              {{ calcTotal(row) ?? '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="gradeStatusTag(row.status)" size="small">
              {{ gradeStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="140">
          <template #default="{ row }">
            <el-input v-model="row.remark" size="small" placeholder="可选" :disabled="isPublished(row)" />
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getGradeSheet, inputGrades, publishGrades, revokeGrades } from '@/api/teacher'
import { getCourseDetail } from '@/api/student'
import { gradeStatusText, gradeStatusTag, scoreColor } from '@/utils/dict'

defineOptions({ name: 'TeacherGrades' })

const route = useRoute()
const courseId = Number(route.params.courseId)

const loading = ref(false)
const saving = ref(false)
const publishing = ref(false)
const sheet = ref([])
const course = ref({})
const keyword = ref('')

const filteredSheet = computed(() => {
  const key = keyword.value.trim()
  if (!key) return sheet.value
  return sheet.value.filter(
    (item) => (item.studentName || '').includes(key) || (item.stuNo || '').includes(key)
  )
})

onMounted(async () => {
  await Promise.all([loadCourse(), loadSheet()])
})

async function loadCourse() {
  try {
    const { data } = await getCourseDetail(courseId)
    course.value = data || {}
  } catch {
    course.value = {}
  }
}

async function loadSheet() {
  loading.value = true
  try {
    const { data } = await getGradeSheet(courseId)
    // 后端返回已有成绩记录；对未录入的学生补充空白行
    sheet.value = (data || []).map((item) => ({
      ...item,
      usualScore: item.usualScore !== null && item.usualScore !== undefined ? Number(item.usualScore) : null,
      examScore: item.examScore !== null && item.examScore !== undefined ? Number(item.examScore) : null,
      status: item.status ?? 0
    }))
  } catch {
    sheet.value = []
  } finally {
    loading.value = false
  }
}

function isPublished(row) {
  return row.status === 1 || row.status === 2
}

function calcTotal(row) {
  const { usualScore, examScore } = row
  if (usualScore === null || usualScore === undefined) {
    return examScore === null || examScore === undefined ? null : Number(examScore).toFixed(1)
  }
  if (examScore === null || examScore === undefined) {
    return Number(usualScore).toFixed(1)
  }
  return (Number(usualScore) * 0.3 + Number(examScore) * 0.7).toFixed(1)
}

function fillAllWith() {
  ElMessageBox.prompt('请输入要批量填充的分数（0-100）', '批量填充', {
    inputPattern: /^(\d{1,3}(\.\d)?)$/,
    inputErrorMessage: '请输入 0-100 之间的数字',
    inputValue: '85'
  })
    .then(({ value }) => {
      const score = Number(value)
      if (score < 0 || score > 100) {
        ElMessage.error('分数必须在 0-100 之间')
        return
      }
      let count = 0
      sheet.value.forEach((row) => {
        if (!isPublished(row)) {
          row.usualScore = score
          row.examScore = score
          count++
        }
      })
      ElMessage.success(`已填充 ${count} 条未发布的成绩`)
    })
    .catch(() => {})
}

async function handleSave() {
  const items = sheet.value
    .filter((row) => !isPublished(row) && (row.usualScore !== null || row.examScore !== null))
    .map((row) => ({
      selectionId: row.selectionId,
      studentId: row.studentId,
      usualScore: row.usualScore,
      examScore: row.examScore,
      remark: row.remark
    }))

  if (!items.length) {
    ElMessage.warning('没有需要保存的成绩')
    return
  }

  saving.value = true
  try {
    await inputGrades({ courseId, items })
    ElMessage.success('成绩已保存为草稿')
    await loadSheet()
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  await ElMessageBox.confirm(
    '发布后学生即可查看成绩，且无法直接修改（需先撤回）。确定发布吗？',
    '发布确认',
    { type: 'warning' }
  )
  publishing.value = true
  try {
    await publishGrades(courseId)
    ElMessage.success('成绩发布成功')
    await loadSheet()
  } finally {
    publishing.value = false
  }
}

async function handleRevoke() {
  await ElMessageBox.confirm('撤回后成绩将变为草稿状态，学生无法查看。确定撤回吗？', '撤回确认', {
    type: 'warning'
  })
  await revokeGrades(courseId)
  ElMessage.success('已撤回成绩发布')
  await loadSheet()
}
</script>

<style lang="scss" scoped>
.course-info {
  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 12px;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;
  }
}

.mt-16 {
  margin-top: 16px;
}
</style>
