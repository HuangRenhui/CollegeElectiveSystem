<template>
  <el-config-provider :locale="zhCn" :size="size">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { ref } from 'vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const size = ref('default')
</script>

<style lang="scss">
#app {
  height: 100%;
}

// 全局滚动条美化
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-thumb {
  background: #c8ccd4;
  border-radius: 4px;

  &:hover {
    background: #a8adb8;
  }
}
::-webkit-scrollbar-track {
  background: transparent;
}
</style>
