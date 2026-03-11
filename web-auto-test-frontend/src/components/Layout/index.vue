<template>
  <div class="layout-root">
    <header class="layout-header">
      <div class="header-left">Web自动化测试平台</div>
      <div class="header-right">
        <span class="username">{{ username }}</span>
        <el-button type="text" @click="handleLogout">退出登录</el-button>
      </div>
    </header>

    <div class="layout-body">
      <aside class="layout-aside">
        <el-menu
          :default-active="currentRouteName"
          :collapse="isCollapse"
          class="el-menu-vertical-demo"
          @select="onMenuSelect"
        >
          <el-menu-item
            v-for="item in menuItems"
            :key="item.name"
            :index="item.name"
          >
            <component :is="iconMap[item.icon] || 'span'" class="menu-icon" />
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </aside>

      <main class="layout-main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  HomeFilled,
  Files,
  Monitor,
  Timer,
  PieChart,
  Setting,
} from "@element-plus/icons-vue";
import { useUserStore } from "@/store";
import menuConfig from "@/router/menu";

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const username = computed(() => userStore.username || "");
const role = computed(
  () => userStore.role || localStorage.getItem("role") || ""
);
const isAdmin = computed(() => role.value === "admin");

const iconMap = {
  HomeFilled,
  Files,
  Monitor,
  Timer,
  PieChart,
  Setting,
};

const menuItems = computed(() =>
  menuConfig.filter((item) => !item.requiresAdmin || isAdmin.value)
);

const currentRouteName = computed(() => route.name || "");
const isCollapse = ref(window.innerWidth < 768);

function onResize() {
  isCollapse.value = window.innerWidth < 768;
}

onMounted(() => {
  window.addEventListener("resize", onResize);
});

onBeforeUnmount(() => {
  window.removeEventListener("resize", onResize);
});

function handleLogout() {
  try {
    userStore.logout();
  } catch (e) {
    // eslint-disable-next-line no-unused-vars
    // 兜底清理，避免旧数据残留
    ["token", "role", "username", "userInfo"].forEach((k) => {
      try {
        localStorage.removeItem(k);
      } catch (e) {
        // eslint-disable-next-line no-unused-vars
        // 忽略localStorage清理异常（如浏览器禁用存储）
      }
      try {
        sessionStorage.removeItem(k);
      } catch (e) {
        // eslint-disable-next-line no-unused-vars
        // 忽略sessionStorage清理异常（如浏览器禁用存储）
      }
    });
  }
  // 额外清理可能的会话缓存
  try {
    sessionStorage.clear();
  } catch (e) {
    // eslint-disable-next-line no-unused-vars
    // 忽略sessionStorage清空异常（如浏览器禁用存储）
  }
  router.push("/login");
  ElMessage.success("退出成功");
}

function onMenuSelect(index) {
  router
    .push({ name: index })
    // eslint-disable-next-line no-unused-vars
    .catch((e) => {
      // 忽略路由跳转异常（如路由不存在/重复跳转）
    });
}
</script>

<style scoped lang="less">
.layout-root {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(180deg, #f4f7fb 0%, #eef3f9 100%);
}

.layout-header {
  height: 64px;
  background: linear-gradient(90deg, #2f7df4 0%, #4a90ff 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 8px 24px rgba(47, 125, 244, 0.22);
  backdrop-filter: blur(6px);
}

.header-left {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  margin-right: 2px;
  color: #ffffff;
  font-size: 24px;
  font-weight: 600;
}

// 👇 1. 修改退出登录按钮样式
.header-right {
  :deep(.el-button) {
    background-color: #00b42a; // 绿色背景
    color: #ffffff !important; // 白色文字
    border: none;
    border-radius: 8px;
    padding: 4px 12px;
    font-weight: 500;
    transition: all 0.3s ease;

    &:hover {
      background-color: #009624; // 深一点的绿色悬停
      transform: scale(1.05);
    }
  }
}

.layout-body {
  display: flex;
  flex: 1;
  overflow: hidden;
  padding: 12px;
  gap: 12px;
}

.layout-aside {
  width: 230px;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  box-shadow: 0 10px 24px rgba(16, 24, 40, 0.06);
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.layout-aside::-webkit-scrollbar {
  width: 0;
  height: 0;
  display: none;
}

.el-menu-vertical-demo {
  height: 100%;
  border-right: none;
  padding: 14px 10px;
  background: transparent;
}

:deep(.el-menu-item) {
  height: 72px;
  line-height: 72px;
  margin: 12px 8px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 0 14px !important;
  font-size: 22px;
  font-weight: 500;
  transition: all 0.2s ease;
}

:deep(.el-menu-item:hover) {
  background-color: #edf4ff;
  transform: translateX(2px);
}

:deep(.el-menu-item.is-active) {
  background: #e8f1ff;
  color: #2f7df4;
  font-weight: 600;
  box-shadow: inset 0 0 0 1px #d3e4ff;
}

.menu-icon {
  margin-right: 0;
  font-size: 32px;
  min-width: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

:deep(.menu-icon svg) {
  width: 32px;
  height: 32px;
  display: block;
}

.layout-main {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 10px 24px rgba(16, 24, 40, 0.04);
}

@media (max-width: 768px) {
  .layout-aside {
    width: 64px;
  }

  :deep(.el-menu-item) {
    height: 58px;
    line-height: 58px;
    margin: 8px 4px;
    padding: 0 8px !important;
    justify-content: center;
  }

  .menu-icon {
    min-width: auto;
    font-size: 26px;
  }

  :deep(.menu-icon svg) {
    width: 26px;
    height: 26px;
  }
}
</style>
