# 项目全局说明（Web自动化测试平台-前端）
## 1. 项目背景
基于SpringBoot+Selenium的Web自动化测试平台前端，用于测试人员可视化配置测试用例、管理页面元素、调度执行任务、查看测试报告。

## 2. 技术栈
- 核心框架：Vue3（Composition API + script setup）
- UI组件库：Element Plus（含图标库@element-plus/icons-vue）
- 接口请求：Axios（已封装request.js，统一拦截器）
- 状态管理：Pinia（已创建userStore，管理用户信息和Token）
- 路由：Vue Router（已配置路由守卫，需登录访问业务页面）
- CSS预处理器：Less
- 可视化：ECharts（用于测试报告图表展示）

## 3. 目录结构
src/
├── api/          # 接口请求封装（如user.js、case.js）
├── assets/       # 静态资源（图片、全局CSS）
├── components/   # 公共组件（Layout布局组件、通用查询表单等）
├── router/       # 路由配置
├── store/        # Pinia状态管理
├── utils/        # 工具类（request.js、常量等）
├── views/        # 业务页面（Login、Home、CaseManage等）
├── App.vue       # 根组件
└── main.js       # 入口文件

## 4. 接口约定
- 后端基础地址：/api（已配置跨域代理）
- 返回格式：{ code: 200（成功）/401（Token失效）/500（错误）, msg: "提示信息", data: {} }
- 请求头：需要登录的接口，自动携带Authorization: Bearer {Token}（Axios拦截器已处理）

## 5. 开发规范
- 组件命名：大驼峰（如CaseForm.vue），页面组件放在views对应目录下
- 代码风格：ESLint+Prettier，保存自动格式化
- 样式：scoped样式隔离，公共样式放在assets/css/global.less
- 注释：关键业务逻辑、接口参数需加注释