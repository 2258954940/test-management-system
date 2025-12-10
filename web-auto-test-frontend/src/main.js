// Vue 3 应用入口文件
// - 创建应用实例，挂载 Pinia 与 Router
// - 导入 Element Plus 全局样式，并全局注册 @element-plus/icons-vue 中的所有图标组件
// 代码遵循项目 ESLint 规范，添加必要注释以提高可维护性。

import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import { createPinia } from "pinia";
import "element-plus/dist/index.css";
import ElementPlus from "element-plus";
import * as ElementPlusIconsVue from "@element-plus/icons-vue";

// 创建应用实例
const app = createApp(App);

// 全局注册 Element Plus 图标
// 使用 Object.entries 可以同时拿到组件名与组件对象，便于注册
Object.entries(ElementPlusIconsVue).forEach(([iconName, iconComponent]) => {
  app.component(iconName, iconComponent);
});

// 挂载状态管理与路由
const pinia = createPinia();
app.use(ElementPlus);
app.use(pinia);
app.use(router);

// 挂载应用到 DOM
app.mount("#app");

// 如果以后需要全局注册 Element Plus 组件（按需或全部导入），
// 可以在这里引入并使用：
// import ElementPlus from 'element-plus';
// app.use(ElementPlus);
