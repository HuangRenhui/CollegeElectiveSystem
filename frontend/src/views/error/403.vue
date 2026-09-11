<template>
  <div class="error-page">
    <el-result icon="error" title="403" sub-title="抱歉，您当前的角色没有访问该页面的权限">
      <template #extra>
        <el-button type="primary" @click="$router.replace('/')">返回首页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
defineOptions({ name: 'Forbidden' })
</script>

<style lang="scss" scoped>
.error-page {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 60vh;
}
</style>
