<template>
  <div class="navbar">
    <div class="navbar__left">
      <el-icon class="navbar__collapse" @click="appStore.toggleSidebar()">
        <component :is="appStore.sidebarCollapsed ? 'Expand' : 'Fold'" />
      </el-icon>

      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: userStore.homePath }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
          {{ item.meta.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="navbar__right">
      <el-tag v-if="appStore.currentSemester" type="success" effect="plain" size="small" class="navbar__semester">
        {{ appStore.currentSemester.semesterName }}
      </el-tag>

      <el-badge :value="appStore.notices.length" :hidden="!appStore.notices.length" class="navbar__notice">
        <el-popover placement="bottom-end" :width="360" trigger="click">
          <template #reference>
            <el-icon :size="19"><Bell /></el-icon>
          </template>
          <div class="notice-panel">
            <div class="notice-panel__title">最新公告</div>
            <el-empty v-if="!appStore.notices.length" description="暂无公告" :image-size="70" />
            <ul v-else class="notice-panel__list">
              <li v-for="notice in appStore.notices" :key="notice.id" @click="openNotice(notice)">
                <span class="notice-panel__item-title">{{ notice.title }}</span>
                <span class="text-muted">{{ formatDate(notice.publishTime) }}</span>
              </li>
            </ul>
          </div>
        </el-popover>
      </el-badge>

      <el-dropdown @command="handleCommand">
        <div class="navbar__user">
          <el-avatar :size="30" :src="userStore.userInfo.avatar">
            {{ userStore.realName?.charAt(0) || 'U' }}
          </el-avatar>
          <span class="navbar__name">{{ userStore.realName }}</span>
          <el-tag :type="roleTagType" size="small" effect="dark">{{ roleText }}</el-tag>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>个人中心
            </el-dropdown-item>
            <el-dropdown-item command="password">
              <el-icon><Lock /></el-icon>修改密码
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="440px">
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="6-64 位字符" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>

    <!-- 公告详情弹窗 -->
    <el-dialog v-model="noticeDialogVisible" :title="currentNotice.title" width="620px">
      <div class="text-muted" style="margin-bottom: 12px">
        {{ currentNotice.publisher }} · {{ formatDate(currentNotice.publishTime) }}
      </div>
      <div style="line-height: 1.8; white-space: pre-wrap">{{ currentNotice.content }}</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'
import { changePassword } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const breadcrumbs = computed(() =>
  route.matched.filter((item) => item.meta?.title && item.path !== '/')
)

const roleText = computed(() => {
  const map = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[userStore.role] || '未知'
})

const roleTagType = computed(() => {
  const map = { STUDENT: 'success', TEACHER: 'warning', ADMIN: 'danger' }
  return map[userStore.role] || 'info'
})

// ---------------------------- 修改密码 ----------------------------
const passwordDialogVisible = ref(false)
const submitting = ref(false)
const passwordFormRef = ref()
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
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

async function submitPassword() {
  await passwordFormRef.value.validate()
  submitting.value = true
  try {
    const { message } = await changePassword(passwordForm)
    ElMessage.success(message || '密码修改成功，请重新登录')
    passwordDialogVisible.value = false
    await userStore.logout()
    router.replace('/login')
  } finally {
    submitting.value = false
  }
}

// ---------------------------- 公告 ----------------------------
const noticeDialogVisible = ref(false)
const currentNotice = ref({})

function openNotice(notice) {
  currentNotice.value = notice
  noticeDialogVisible.value = true
}

function handleCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'password') {
    passwordDialogVisible.value = true
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await userStore.logout()
        ElMessage.success('已安全退出')
        router.replace('/login')
      })
      .catch(() => {})
  }
}

function formatDate(value) {
  return value ? dayjs(value).format('MM-DD HH:mm') : ''
}
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $header-height;
  padding: 0 20px;
  background: #fff;

  &__left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  &__collapse {
    font-size: 20px;
    cursor: pointer;
    color: #5a5e66;

    &:hover {
      color: $primary-color;
    }
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 20px;
  }

  &__semester {
    font-weight: 600;
  }

  &__notice {
    display: flex;
    align-items: center;
    cursor: pointer;
    color: #5a5e66;

    &:hover {
      color: $primary-color;
    }
  }

  &__user {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    outline: none;
  }

  &__name {
    font-weight: 500;
  }
}

.notice-panel {
  &__title {
    font-weight: 600;
    margin-bottom: 10px;
    padding-bottom: 8px;
    border-bottom: 1px solid #ebeef5;
  }

  &__list {
    list-style: none;
    margin: 0;
    padding: 0;
    max-height: 320px;
    overflow-y: auto;

    li {
      display: flex;
      justify-content: space-between;
      gap: 12px;
      padding: 9px 4px;
      border-bottom: 1px dashed #f0f0f0;
      cursor: pointer;
      font-size: 13px;

      &:hover {
        background: #f5f7fa;
      }
    }
  }

  &__item-title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
</style>
