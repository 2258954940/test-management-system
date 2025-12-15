Web 自动化测试管理系统
本项目是基于 SpringBoot + Vue + Selenium 开发的前后端分离 Web 自动化测试平台，专为解决传统手动测试效率低、回归测试重复工作量大的痛点设计，支持可视化用例配置、自动化脚本执行、权限控制等核心功能，已适配 Spring Security 6.x 版本，解决跨域登录、路径匹配等实际技术问题。
🔧 技术栈
后端
核心框架：Spring Boot 6.x、Spring Security（权限控制）、MyBatis-Plus（数据访问）
安全机制：JWT 无状态认证、BCrypt 密码加密
测试工具：TestNG（接口测试框架）、Logback（日志管理）、EasyExcel（Excel 导入导出）
数据库：MySQL（数据存储）
构建工具：Maven
前端
核心框架：Vue 3、Element UI（组件库）
网络请求：Axios（请求封装、拦截器处理）
工程化：npm、vue.config.js（代理配置）
样式：Less
自动化测试
核心工具：Selenium 4.x（Web 元素定位、自动化交互）
功能支持：多浏览器兼容、显式等待优化、JS 操作增强
🚀 快速启动
前置条件
本地安装 JDK 11+、MySQL 8.0+、Node.js 14+、Maven 3.6+
克隆仓库到本地：git clone https://github.com/2258954940/test-management-system.git
1. 后端启动（auto-test-backend）
bash
运行
# 进入后端目录
cd test-management-system/auto-test-backend

# 配置数据库（必做）
1. 本地 MySQL 新建数据库：auto_test（编码格式 UTF-8）
2. 修改 src/main/resources/application.yml 中的数据库配置：
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/auto_test?useSSL=false&serverTimezone=Asia/Shanghai
       username: 你的MySQL账号（如root）
       password: 你的MySQL密码（如123456）

# 启动项目
mvn spring-boot:run
启动成功后端口：8000
接口访问示例：http://localhost:8000/api/user/login（登录接口）
2. 前端启动（web-auto-test-frontend）
bash
运行
# 进入前端目录
cd test-management-system/web-auto-test-frontend

# 安装依赖（首次启动必做）
npm install

# 启动项目
npm run serve
启动成功后端口：8080
访问地址：http://localhost:8080（直接打开浏览器即可）
3. 登录信息（默认账号）
用户名：admin
密码：123456
角色权限：admin（支持所有功能操作）
✨ 核心功能
用户与权限模块
登录认证：账号密码校验、JWT 令牌生成与存储
权限控制：基于 Spring Security 实现接口访问权限管控，系统用户接口仅 admin 可访问
用例管理模块
用例配置：可视化编辑测试用例（支持输入测试 URL、操作步骤、预期结果）
数据管理：测试用例增删改查、批量导出 Excel
自动化执行模块
脚本执行：点击「执行」按钮触发 Selenium 自动化脚本，支持百度搜索等 Web 场景测试
稳定性优化：显式等待 + JS 操作增强，解决 10+ 类元素定位失败问题
结果反馈：执行结果实时提示，成功 / 失败状态清晰展示
日志与排查模块
日志输出：Logback 配置日志级别（INFO/ERROR），支持控制台 + 文件双输出
问题定位：通过日志快速排查接口报错、自动化执行失败等问题
📌 已解决核心技术问题
Spring Security 6.x 适配：移除废弃 API，用 HttpMethod 规范请求匹配，解决「pattern must start with a /」启动报错
跨域问题：配置 CORS 允许前端跨域请求，支持带凭证（Token/Cookie）访问
登录 403 问题：修复前端路径重复拼接（/api/api/user/login），精准放行登录接口
前端报错：解决 ResizeObserver 循环报错，优化页面稳定性
📝 备注
本项目为毕业设计，仅用于学习交流，请勿用于商业场景
核心亮点：无代码化配置测试用例、Selenium 自动化执行、完整的前后端权限链路
如有问题可通过 GitHub Issues 反馈，或联系邮箱：2258954940@qq.com