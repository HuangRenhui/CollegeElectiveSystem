<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :md="8">
        <el-card shadow="never" class="profile-card">
          <el-avatar :size="86" :src="userInfo.avatar">
            {{ userInfo.realName?.charAt(0) || 'U' }}
          </el-avatar>
          <h3>{{ userInfo.realName }}</h3>
          <el-tag :type="roleTagType" effect="dark">{{ roleText }}</el-tag>
          <el-divider />
          <ul class="profile-card__meta">
            <li><span>登录账号</span><strong>{{ userInfo.username }}</strong></li>
            <li v-if="userInfo.stuNo"><span>学号</span><strong>{{ userInfo.stuNo }}</strong></li>
            <li v-if="userInfo.teacherNo"><span>工号</span><strong>{{ userInfo.teacherNo }}</strong></li>
            <li v-if="userInfo.deptName"><span>院系</span><strong>{{ userInfo.deptName }}</strong></li>
            <li v-if="userInfo.majorName"><span>专业</span><strong>{{ userInfo.majorName }}</strong></li>
            <li v-if="userInfo.className"><span>班级</span><strong>{{ userInfo.className }}</strong></li>
            <li v-if="userInfo.title"><span>职称</span><strong>{{ userInfo.title }}</strong></li>
            <li v-if="userInfo.totalCredit !== undefined">
              <span>已获学分</span><strong>{{ userInfo.totalCredit }}</strong>
            </li>
          </ul>
        </el-card>
      </el-col>

      <el-col :md="16">
        <el-card shadow="never">
          <template #header><span>资料维护</span></template>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" style="max-width: 520px">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" />
            </el-form-item>
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio v-for="item in GENDER_OPTIONS" :key="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="submit">保存修改</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" class="mt-16">
          <template #header><span>安全设置</span></template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="登录密码">
              <span class="text-muted">建议定期更换密码，确保账号安全</span>
              <el-button link type="primary" style="margin-left: 12px" @click="passwordVisible = true">
                修改密码
              </el-button>
            </el-descriptions-item>
            <el-descriptions-item label="令牌有效期">
              <span class="text-muted">2 小时（活跃操作自动续期）</span>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="passwordVisible" title="修改密码" width="440px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { updateProfile, changePassword } from '@/api/auth'
import { GENDER_OPTIONS } from '@/utils/dict'

const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => userStore.userInfo)
const roleText = computed(() => ({ STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }[userInfo.value.role] || ''))
const roleTagType = computed(() => ({ STUDENT: 'success', TEACHER: 'warning', ADMIN: 'danger' }[userInfo.value.role] || 'info'))

const formRef = ref()
const saving = ref(false)
const form = reactive({ realName: '', gender: 0, phone: '', email: '' })

const rules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

const passwordVisible = ref(false)
const passwordFormRef = ref()
const submitting = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const passwordRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6-64 之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) =>
        value === passwordForm.newPassword ? callback() : callback(new Error('两次输入的密码不一致')),
      trigger: 'blur'
    }
  ]
}

onMounted(resetForm)

function resetForm() {
  form.realName = userInfo.value.realName || ''
  form.gender = userInfo.value.gender ?? 0
  form.phone = userInfo.value.phone || ''
  form.email = userInfo.value.email || ''
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await updateProfile(form)
    ElMessage.success('资料更新成功')
    await userStore.fetchUserInfo()
  } finally {
    saving.value = false
  }
}

async function submitPassword() {
  await passwordFormRef.value.validate()
  submitting.value = true
  try {
    await changePassword(passwordForm)
    ElMessage.success('密码修改成功，请重新登录')
    passwordVisible.value = false
    await userStore.logout()
    router.replace('/login')
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss" scoped>
.profile-card {
  text-align: center;

  h3 {
    margin: 14px 0 8px;
  }

  &__meta {
    list-style: none;
    margin: 0;
    padding: 0;
    text-align: left;

    li {
      display: flex;
      justify-content: space-between;
      padding: 9px 0;
      border-bottom: 1px dashed #f0f0f0;
      font-size: 13px;

      &:last-child {
        border-bottom: none;
      }

      span {
        color: #909399;
      }

      strong {
        color: #303133;
        font-weight: 500;
      }
    }
  }
}

.mt-16 {
  margin-top: 16px;
}
</style>
