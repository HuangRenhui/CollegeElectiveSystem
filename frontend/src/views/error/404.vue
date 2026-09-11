<template>
  <div class="error-page">
    <el-result icon="warning" title="404" sub-title="抱歉，您访问的页面不存在或已被移除">
      <template #extra>
        <el-button type="primary" @click="$router.replace('/')">返回首页</el-button>
        <el-button @click="$router.back()">返回上一页</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
defineOptions({ name: 'NotFound' })
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
