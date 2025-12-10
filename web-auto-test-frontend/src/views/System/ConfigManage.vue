<template>
  <div class="config-root">
    <h4>系统基础配置</h4>

    <el-card class="config-card">
      <el-form :model="form" label-width="140px" class="config-form">
        <el-form-item label="平台名称">
          <el-input v-model="form.platformName" placeholder="平台名称" />
        </el-form-item>

        <el-form-item label="超时时间(秒)">
          <el-input-number v-model="form.timeout" :min="1" :max="300" />
        </el-form-item>

        <el-form-item label="自动清理日志">
          <el-switch
            v-model="form.autoCleanLogs"
            active-text="开启"
            inactive-text="关闭"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="saveConfig">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { ElMessage } from "element-plus";

// 模拟的配置存储（页面内模拟），避免副作用
const savedConfig = ref(null);

const form = reactive({
  platformName: "Web自动化测试平台",
  timeout: 30,
  autoCleanLogs: true,
});

function loadConfig() {
  // 模拟加载，这里可替换为真实 API 调用
  const mock = {
    platformName: "Web自动化测试平台",
    timeout: 30,
    autoCleanLogs: true,
  };
  savedConfig.value = { ...mock };
  form.platformName = mock.platformName;
  form.timeout = mock.timeout;
  form.autoCleanLogs = mock.autoCleanLogs;
}

function saveConfig() {
  savedConfig.value = {
    platformName: form.platformName,
    timeout: form.timeout,
    autoCleanLogs: form.autoCleanLogs,
  };
  ElMessage.success("保存成功");
}

onMounted(() => {
  loadConfig();
});
</script>

<style scoped lang="less">
.config-root {
  padding: 20px;
}

.config-card {
  background: #fff;
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.config-form .el-form-item {
  margin-bottom: 12px;
}
</style>
