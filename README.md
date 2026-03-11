Web 自动化测试管理系统
本项目是基于 SpringBoot + Vue + Selenium 开发的前后端分离 Web 自动化测试平台，专为解决传统手动测试效率低、回归测试重复工作量大的痛点设计，支持可视化用例配置、自动化脚本执行、权限控制、任务调度、测试报告自动统计等核心功能，已适配 Spring Security 6.x 版本，解决跨域登录、路径匹配等实际技术问题。
🔧 技术栈
后端
核心框架：Spring Boot 4.0.0、Spring Security（权限控制）、MyBatis-Plus（数据访问）
安全机制：JWT 无状态认证、BCrypt 密码加密
测试工具：
单元测试：JUnit 5（基于spring-boot-starter-test）
Web 自动化测试：Selenium 4.x
接口测试：rest-assured
数据处理：EasyExcel（Excel 导入导出）、HikariCP（高性能数据库连接池）、DecimalFormat（数据格式化）
日志管理：Logback
数据库：MySQL 8.0+
构建工具：Maven
辅助工具：Hutool（工具类库）、Jsoup（HTML 解析）、Lombok（简化代码）
前端
核心框架：Vue 3、Element Plus（组件库）
网络请求：Axios（请求封装、拦截器处理）
工程化：npm、vue.config.js（代理配置）
样式：Less
可视化：ECharts 适配（预留测试报告可视化能力）
自动化测试
核心工具：Selenium 4.x（Web 元素定位、自动化交互）
功能支持：
多浏览器兼容（Chrome/Edge/Firefox）
显式等待优化、JS 操作增强
驱动复用（批量执行用例仅初始化一次浏览器，提升执行效率）
失败自动重试（批量执行时用例失败重试 1 次，提升稳定性）
任务 - 用例结果关联（执行结果绑定任务 ID，支撑测试报告统计）
🚀 快速启动
前置条件
本地安装以下环境：
JDK 21+
MySQL 8.0+
Node.js 14+
Maven 3.6+
克隆仓库
运行
git clone https://github.com/2258954940/test-management-system.git
cd test-management-system
后端启动（auto-test-backend）
进入后端目录
运行
cd auto-test-backend
配置数据库（必做）
本地 MySQL 新建数据库：auto_test（编码格式 UTF-8）
修改src/main/resources/application.yml中的数据库配置：
yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/auto_test?useSSL=false&serverTimezone=Asia/Shanghai
    username: 你的 MySQL 账号（如 root）
    password: 你的 MySQL 密码（如 123456）
执行数据库字段新增语句（支撑测试报告统计）：
sql
ALTER TABLE auto_test.test_result ADD COLUMN task_id BIGINT COMMENT '关联任务 ID，绑定执行结果到具体任务';
启动项目
运行
mvn spring-boot:run
启动成功后端口：8000
接口访问示例：
登录接口：http://localhost:8000/api/user/login
任务列表接口：http://localhost:8000/api/task/list
已完成任务接口（支撑测试报告）：http://localhost:8000/api/task/finished-list
前端启动（web-auto-test-frontend）
进入前端目录
运行
cd web-auto-test-frontend
安装依赖（首次启动必做）
运行
npm install
启动项目
运行
npm run serve
启动成功后端口：8080
访问地址：http://localhost:8080
登录信息（默认账号）
用户名：admin
密码：123456
角色权限：admin（支持所有功能操作）
✨ 核心功能
1. 用户与权限模块
登录认证：账号密码校验、JWT 令牌生成与存储
权限控制：基于 Spring Security 实现接口访问权限管控，系统用户接口仅 admin 可访问
2. 用例管理模块
用例配置：可视化编辑测试用例（支持输入测试 URL、操作步骤、预期结果）
动态断言：支持 TEXT 等断言类型，可视化配置断言规则，无需编写代码
数据管理：测试用例增删改查、批量导出 Excel
3. 自动化执行模块
脚本执行：点击「执行」按钮触发 Selenium 自动化脚本，支持 Web 场景测试
稳定性优化：显式等待 + JS 操作增强，解决 10 + 类元素定位失败问题
结果反馈：执行结果实时提示，成功 / 失败状态清晰展示
批量执行：支持单 / 批量执行切换，执行结果截图自动保存、验证详情结构化存储
4. 任务调度模块
任务创建：可视化创建执行任务，支持关联多个测试用例（逗号分隔 ID）
执行方式：支持立即执行，预留定时执行（Cron 表达式）能力
任务管理：任务状态实时更新（pending / 运行中 /finished/failed）、任务强制终止、日志查看
批量优化：批量执行用例时复用浏览器驱动，减少启动 / 关闭开销，提升执行效率
5. 测试报告模块
数据统计：自动拉取已完成任务，统计配置用例数、实际执行数、成功数、失败数
成功率计算：按实际执行用例数统计真实成功率，避免配置数与执行数不一致导致的计算异常
结果展示：任务级执行数据列表展示，支撑用例详情、失败原因钻取
数据溯源：所有执行结果绑定任务 ID，实现任务 - 用例 - 执行结果全链路溯源
6. 日志与排查模块
日志输出：Logback 配置日志级别（INFO/ERROR），支持控制台 + 文件双输出
问题定位：通过日志快速排查接口报错、自动化执行失败等问题
📌 已解决核心技术问题
Spring Security 6.x 适配：移除废弃 API，用 HttpMethod 规范请求匹配，解决「pattern must start with a /」启动报错
跨域问题：配置 CORS 允许前端跨域请求，支持带凭证（Token/Cookie）访问
登录 403 问题：修复前端路径重复拼接（/api/api/user/login），精准放行登录接口
前端报错：解决 ResizeObserver 循环报错，优化页面稳定性
任务 / 报告模块问题：
解决任务调度 / 测试报告页面 500 错误：Task 实体类动态统计字段添加@TableField(exist=false)注解，避免 MyBatis-Plus 生成无效 SQL
修复测试报告成功率计算异常：将计算分母改为「实际执行用例数」，增加任务 ID 空值校验
解决参数类型不匹配问题：任务 ID 转 Long 类型适配数据库 BIGINT 字段
批量执行效率问题：实现浏览器驱动复用，解决多次启动浏览器导致的执行缓慢问题
空值处理：增加用例 ID、任务 ID、执行结果的空值校验，避免空指针异常
📝 备注
本项目为毕业设计，仅用于学习交流，请勿用于商业场景
核心亮点：无代码化配置测试用例、Selenium 自动化执行、完整的前后端权限链路、任务调度与测试报告联动、驱动复用提升执行效率
如有问题可通过 GitHub Issues 反馈，或联系邮箱：2258954940@qq.com