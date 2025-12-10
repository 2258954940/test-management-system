import { createRouter, createWebHashHistory } from "vue-router";
import { ElMessage } from "element-plus";
import Login from "@/views/Login/index.vue";
import Layout from "@/components/Layout/index.vue";
import menuConfig from "./menu";
import { useUserStore } from "@/store";

const viewMap = {
  Home: () => import("@/views/Home/index.vue"),
  CaseManage: () => import("@/views/CaseManage/index.vue"),
  ElementManage: () => import("@/views/ElementManage/index.vue"),
  TaskSchedule: () => import("@/views/TaskSchedule/index.vue"),
  Report: () => import("@/views/Report/index.vue"),
  System: () => import("@/views/System/index.vue"),
};

const layoutChildren = menuConfig
  .map((item) => {
    const component = viewMap[item.name];
    if (!component) return null;
    return {
      path: item.path.replace(/^\//, ""),
      name: item.name,
      component,
      meta: {
        title: item.title,
        requiresAdmin: Boolean(item.requiresAdmin),
      },
    };
  })
  .filter(Boolean);

const routes = [
  {
    path: "/login",
    name: "Login",
    component: Login,
    meta: { title: "登录" },
  },
  {
    path: "/",
    component: Layout,
    redirect: "/home",
    children: layoutChildren,
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: () => import("@/views/Error/ErrorPage404.vue"),
    meta: { title: "404" },
  },
];

const router = createRouter({
  history: createWebHashHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

router.beforeEach((to, from, next) => {
  const userStore = useUserStore();
  if (userStore?.loadFromLocal) {
    userStore.loadFromLocal();
  }
  const token = userStore?.token || localStorage.getItem("token");

  if (!token && to.name !== "Login") {
    next({ name: "Login" });
    return;
  }

  if (token && to.name === "Login") {
    next({ path: "/home" });
    return;
  }

  if (to.meta.requiresAdmin && userStore.role !== "admin") {
    ElMessage.warning("仅管理员可访问该页面");
    next({ name: "Home" });
    return;
  }

  document.title = to.meta?.title
    ? `${to.meta.title} - Web自动化测试平台`
    : "Web自动化测试平台";

  next();
});

export default router;
