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
  } catch (err) {
    localStorage.removeItem("token");
  }
  router.push("/login");
  ElMessage.success("退出成功");
}

function onMenuSelect(index) {
  router.push({ name: index }).catch(() => {});
}
</script>

<style scoped lang="less">
.layout-root {
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.layout-header {
  height: 60px;
  background: #1989fa;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.header-left {
  font-size: 18px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
}

.username {
  margin-right: 12px;
  color: #ffffff;
}

.layout-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.layout-aside {
  width: 200px;
  border-right: 1px solid #e5e7eb;
  overflow: auto;
}

.el-menu-vertical-demo {
  height: 100%;
}

.menu-icon {
  margin-right: 8px;
}

.layout-main {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: #f9fafb;
}

@media (max-width: 768px) {
  .layout-aside {
    width: 64px;
  }
}
</style>
