<template>
  <div class="system-root">
    <h3>系统管理</h3>

    <el-card class="tabs-card">
      <el-tabs v-model="activeName" type="card">
        <el-tab-pane label="用户管理" name="user">
          <UserManage />
        </el-tab-pane>

        <el-tab-pane label="系统配置" name="config">
          <ConfigManage />
        </el-tab-pane>

        <el-tab-pane label="操作日志" name="log">
          <LogManage />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from "vue";
import UserManage from "@/views/System/UserManage.vue";
import ConfigManage from "@/views/System/ConfigManage.vue";
import LogManage from "@/views/System/LogManage.vue";

const activeName = ref("user");
</script>

<style scoped lang="less">
.system-root {
  padding: 20px;
}

.tabs-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

@media (max-width: 768px) {
  .tabs-card {
    padding: 12px;
  }
}
</style>
