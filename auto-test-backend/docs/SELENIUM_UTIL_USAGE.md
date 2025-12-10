# SeleniumUtil 使用说明（WebDriverManager + 双浏览器）

## WebDriverManager 自动适配原理
- WebDriverManager 5.6.0 会探测本机已安装的 Chrome/Edge 版本（通过浏览器二进制或注册表/系统 PATH），解析出主版本号。
- 根据检测到的版本，自动从官方镜像下载匹配的 Driver（chromedriver.exe / msedgedriver.exe），下载后缓存在本地用户目录（通常在 `~/.cache/selenium`）。
- 首次启动会下载，后续复用缓存，不需要再次下载，也无需手工设置 `webdriver.chrome.driver` 或 `webdriver.edge.driver` 路径。

## 关键方法
- `createDriver(String driverPath)`：兼容旧签名，默认启动 Chrome，参数无需填写。
- `createDriver(String driverPath, String browserType)`：新增的双浏览器入口，`browserType` 支持 `chrome` / `edge`，空值时默认 `chrome`。
- `findElement`：支持 `id`/`name`/`xpath` 定位，定位失败抛出友好提示。
- `performAction`：支持 `input`（清空后输入）和 `click`。
- `takeScreenshot`：截图保存到项目根目录 `screenshots/`，返回相对路径。

## 服务层调用示例
```java
import org.openqa.selenium.WebDriver;
import com.auto.test.utils.SeleniumUtil;

// 1) 默认 Chrome（与旧代码兼容）
WebDriver driver = SeleniumUtil.createDriver(null);

// 2) 指定 Chrome（显式）
WebDriver chrome = SeleniumUtil.createDriver(null, "chrome");

// 3) 切换 Edge
WebDriver edge = SeleniumUtil.createDriver(null, "edge");
```

在现有 `TestCaseService` 中，如需暴露浏览器切换能力，可将 `runTestCase` 接口增加一个参数并传递给 `createDriver(null, browserType)`，其余逻辑无需变化。

## 常见问题
- **浏览器未安装或版本无法识别**：会抛出 `IllegalStateException`，请确认已安装 Chrome 或 Edge，并使用稳定版。
- **首次启动下载慢**：请检查网络；下载成功后会缓存，后续启动无需重复下载。
- **元素定位失败**：确认前端元素存在且定位方式/值正确（仅支持 id/name/xpath）。
- **无头模式**：可在 `createDriver` 中为对应 Options 增加 `--headless=new`。
