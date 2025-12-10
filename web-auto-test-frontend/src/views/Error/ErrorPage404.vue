<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面未找到</p>
    <el-button type="primary" @click="goHome">返回首页</el-button>
  </div>
</template>

<script setup>
import router from "@/router";

function goHome() {
  router.push("/home");
}
</script>

<style scoped lang="less">
.not-found {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #606266;
  gap: 12px;
}

h1 {
  font-size: 64px;
  margin: 0;
}

p {
  margin: 0;
}
</style>
