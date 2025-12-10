<template>
  <div class="error-page">
    <h1>500</h1>
    <p>服务器开小差了，请稍后重试</p>
    <div class="actions">
      <el-button type="primary" @click="reload">刷新页面</el-button>
      <el-button @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import router from "@/router";

function reload() {
  window.location.reload();
}

function goHome() {
  router.push("/home");
}
</script>

<style scoped lang="less">
.error-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #606266;
  gap: 12px;
}

.actions {
  display: flex;
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
