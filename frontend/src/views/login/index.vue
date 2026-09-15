<template>
  <div class="login">
    <div class="login__bg-decoration login__bg-decoration--1"></div>
    <div class="login__bg-decoration login__bg-decoration--2"></div>

    <div class="login__wrapper">
      <!-- 左侧品牌区 -->
      <div class="login__brand">
        <el-icon :size="52" color="#fff"><School /></el-icon>
        <h1 class="login__brand-title">高校选修课管理系统</h1>
        <p class="login__brand-desc">
          高性能选课 · 智能冲突检测 · 全流程教务管理
        </p>
        <ul class="login__feature-list">
          <li><el-icon><Select /></el-icon> 学生在线选课退课</li>
          <li><el-icon><Select /></el-icon> 教师课程与成绩管理</li>
          <li><el-icon><Select /></el-icon> 管理员教务数据统筹</li>
        </ul>
      </div>

      <!-- 右侧表单区 -->
      <div class="login__form-panel">
        <div class="login__form-header">
          <h2>欢迎登录</h2>
          <p class="text-muted">请使用学号 / 工号 / 管理员账号登录</p>
        </div>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="请输入账号" clearable>
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="login__form-options">
            <el-checkbox v-model="form.remember">记住登录状态</el-checkbox>
            <span class="text-muted">忘记密码请联系管理员</span>
          </div>

          <el-button type="primary" size="large" class="login__submit" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form>

        <template v-if="showDemoAccounts">
          <el-divider>演示账号（密码均为 123456）</el-divider>
          <div class="login__demo">
            <el-tag
              v-for="account in demoAccounts"
              :key="account.username"
              :type="account.type"
              effect="plain"
              class="login__demo-tag"
              @click="fillAccount(account)"
            >
              {{ account.label }}：{{ account.username }}
            </el-tag>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 是否展示演示账号：仅开发环境开启，可通过环境变量 VITE_SHOW_DEMO_ACCOUNTS 控制 */
const showDemoAccounts = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS === 'true'

const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  remember: false
})

const rules = {
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度需在 6-64 之间', trigger: 'blur' }
  ]
}

const demoAccounts = [
  { label: '管理员', username: 'admin', type: 'danger' },
  { label: '教师', username: 'T2026001', type: 'warning' },
  { label: '学生', username: '2026010101', type: 'success' }
]

function fillAccount(account) {
  form.username = account.username
  form.password = '123456'
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功，正在进入系统…')
    const redirect = route.query.redirect
    router.replace(redirect ? String(redirect) : userStore.homePath)
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  overflow: hidden;
  background: linear-gradient(135deg, #1f3a68 0%, #2b5c9b 45%, #3f8ac9 100%);

  &__bg-decoration {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);

    &--1 {
      width: 520px;
      height: 520px;
      top: -160px;
      right: -120px;
    }

    &--2 {
      width: 360px;
      height: 360px;
      bottom: -140px;
      left: -80px;
    }
  }

  &__wrapper {
    position: relative;
    z-index: 2;
    display: flex;
    width: 940px;
    max-width: 94vw;
    min-height: 540px;
    background: #fff;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 24px 60px rgba(0, 0, 0, 0.28);
  }

  &__brand {
    display: flex;
    flex-direction: column;
    justify-content: center;
    width: 47%;
    padding: 48px 40px;
    color: #fff;
    background: linear-gradient(160deg, #2b5c9b 0%, #1f3a68 100%);
  }

  &__brand-title {
    margin: 20px 0 12px;
    font-size: 25px;
    letter-spacing: 1px;
  }

  &__brand-desc {
    margin: 0 0 30px;
    font-size: 13px;
    line-height: 1.9;
    color: rgba(255, 255, 255, 0.78);
  }

  &__feature-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 13px;
      font-size: 13.5px;
      color: rgba(255, 255, 255, 0.9);
    }
  }

  &__form-panel {
    display: flex;
    flex-direction: column;
    justify-content: center;
    flex: 1;
    padding: 44px 46px;
  }

  &__form-header {
    margin-bottom: 26px;

    h2 {
      margin: 0 0 6px;
      font-size: 24px;
      color: #1f2d3d;
    }

    p {
      margin: 0;
      font-size: 13px;
    }
  }

  &__form-options {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 20px;
    font-size: 13px;
  }

  &__submit {
    width: 100%;
    letter-spacing: 4px;
    font-weight: 600;
  }

  &__demo {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  &__demo-tag {
    cursor: pointer;
    transition: transform 0.15s;

    &:hover {
      transform: translateY(-2px);
    }
  }
}

@media (max-width: 768px) {
  .login__brand {
    display: none;
  }

  .login__wrapper {
    width: 92vw;
    min-height: auto;
  }
}
</style>
