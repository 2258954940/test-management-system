# 自动化测试平台后端运行说明

## 环境要求
- JDK 21
- Maven（已附带 mvnw，可直接使用）
- MySQL 8+，已创建数据库 `auto_test`
- Chrome 浏览器与匹配版本的 chromedriver，可将路径写入 `SeleniumUtil.DEFAULT_CHROME_DRIVER_PATH`

## 数据库准备
1. 启动 MySQL，确保账号密码与 `application.yaml` 一致（默认 root/123456）。
2. 执行 `src/main/resources/schema.sql` 创建 `test_case`、`test_result` 表。

## ChromeDriver 配置
- 找到本机与 Chrome 版本匹配的 `chromedriver` 可执行文件。
- 打开 `src/main/java/com/auto/test/utils/SeleniumUtil.java`，将 `DEFAULT_CHROME_DRIVER_PATH` 替换为绝对路径，例如：
  - Windows: `C:/drivers/chromedriver.exe`
  - macOS/Linux: `/usr/local/bin/chromedriver`
- 如需无头运行，可在 `createDriver` 方法中为 `ChromeOptions` 添加 `--headless=new`。

## 启动步骤（VS Code）
1. 终端执行 `./mvnw.cmd spring-boot:run`（Windows）或 `./mvnw spring-boot:run`（Linux/macOS）。
2. 服务启动后监听 `http://localhost:8000`。
3. 截图会保存到项目根目录 `screenshots/`，前端可通过 `http://localhost:8000/screenshots/<文件名>` 直接访问。

## 前端请求示例（兼容 Vue/axios）

### 新增用例
```javascript
// POST http://localhost:8000/api/addCase
axios.post('http://localhost:8000/api/addCase', {
  name: '搜索示例',
  description: '在搜索框输入内容并点击',
  url: 'https://www.example.com',
  locatorType: 'id',
  locatorValue: 'search-input',
  actionType: 'input',
  inputData: '自动化',
  expectedResult: '搜索结果出现'
}).then(res => console.log(res.data)).catch(err => console.error(err));
```

### 执行用例
```javascript
// POST http://localhost:8000/api/runCase
axios.post('http://localhost:8000/api/runCase', {
  caseId: 1
}).then(res => console.log(res.data)).catch(err => console.error(err));
```

## 典型返回格式
```json
{
  "code": 200,
  "msg": "执行完毕，状态: PASS",
  "data": {
    "id": 3,
    "caseId": 1,
    "status": "PASS",
    "message": "执行成功，截图已保存",
    "screenshotPath": "screenshots/case_20241206_120000_000.png",
    "runTime": "2024-12-06T12:00:00",
    "durationMs": 1523
  }
}
```

## 常见问题
- 如果提示 `请在 SeleniumUtil.DEFAULT_CHROME_DRIVER_PATH 中配置本地 chromedriver 路径`，请按上文替换路径。
- 如果元素定位失败，确认前端页面是否存在对应元素且定位方式填写正确（仅支持 id/name/xpath）。
- 端口占用可修改 `application.yaml` 中的 `server.port`。
