<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="title">Web自动化测试平台</div>

      <el-form
        :model="form"
        :rules="rules"
        ref="loginFormRef"
        class="login-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="loading"
            class="login-button"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import router from "@/router";
import { useUserStore } from "@/store";
import { ElMessage } from "element-plus";
import { login } from "@/api/user";

// 初始化用户仓库
const userStore = useUserStore();

// 登录表单数据
const form = reactive({
  username: "",
  password: "",
});

// 表单校验规则
const rules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, message: "用户名长度需>=3", trigger: "blur" },
  ],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, message: "密码长度需>=6", trigger: "blur" },
  ],
};

// 表单引用 & 加载状态
const loginFormRef = ref(null);
const loading = ref(false);

// 登录处理逻辑
async function handleLogin() {
  try {
    await loginFormRef.value.validate();
  } catch (err) {
    ElMessage.error("请完善表单信息后提交");
    return;
  }

  if (loading.value) return;
  loading.value = true;

  try {
    const res = await login({
      username: form.username,
      password: form.password,
    });

    // ========== 核心修改：适配响应拦截器的返回格式 ==========
    // 现在res就是后端的data字段（{token, role, username}）
    if (res?.token) {
      // 存储用户信息到Pinia
      userStore.login({
        username: res.username,
        role: res.role,
        token: res.token,
      });
      // 回退存储到localStorage
      localStorage.setItem("token", res.token);
      localStorage.setItem("role", res.role);
      localStorage.setItem("username", res.username);

      ElMessage.success("登录成功");
      router.push("/home");
    } else {
      ElMessage.error("登录失败：未获取到有效登录信息");
    }
  } catch (err) {
    const errorMsg =
      err?.response?.data?.msg || err.message || "登录失败，请重试";
    ElMessage.error(errorMsg);
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped lang="less">
.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.login-card {
  width: 30%;
  min-width: 320px;
  padding: 32px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.title {
  text-align: center;
  font-size: 20px;
  color: #1989fa;
  margin-bottom: 20px;
  font-weight: 600;
}

.login-form .el-form-item {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
}

@media (max-width: 768px) {
  .login-card {
    width: 90%;
  }
}
</style>
