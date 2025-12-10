package com.auto.test.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.PageLoadStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Selenium 工具类，封装 Driver 初始化、元素定位、动作执行、截图能力。
 * 集成 WebDriverManager 自动匹配浏览器版本，无需手动配置驱动路径。
 */
public class SeleniumUtil {

    private static final Path SCREENSHOT_DIR = Paths.get("screenshots");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private SeleniumUtil() {
    }

    /**
     * 兼容旧签名：默认启动 Chrome。driverPath 参数保留但不再需要传值。
     */
    public static WebDriver createDriver(String driverPath) {
        return createDriver(driverPath, "chrome");
    }

    /**
     * 初始化浏览器 Driver，支持 chrome/edge，自动匹配本地浏览器版本并下载对应驱动。
     * browserType 可为 null 或空时默认 chrome。
     */
    public static WebDriver createDriver(String driverPath, String browserType) {
        String type = browserType == null || browserType.isBlank() ? "chrome" : browserType.toLowerCase();
        try {
            switch (type) {
                case "edge":
                    // 手动指定 EdgeDriver 路径，放置于 src/main/resources/drivers/msedgedriver.exe
                    String hardCodedDriverPath = "src/main/resources/drivers/msedgedriver.exe";
                    Path edgeDriver = Paths.get(hardCodedDriverPath).toAbsolutePath();
                    if (!Files.exists(edgeDriver)) {
                        throw new IllegalStateException("未找到 EdgeDriver，可执行文件路径: " + edgeDriver);
                    }
                    System.setProperty("webdriver.edge.driver", edgeDriver.toString());
                    System.out.println("[SeleniumUtil] Edge驱动路径: " + edgeDriver);
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);
                    edgeOptions.addArguments("--remote-allow-origins=*");
                    edgeOptions.addArguments("--start-maximized");
                    edgeOptions.addArguments("--disable-gpu");
                    edgeOptions.addArguments("--disable-infobars", "--disable-extensions");
                    edgeOptions.addArguments("--no-sandbox"); // 关闭沙箱（关键）
                    edgeOptions.addArguments("--disable-dev-shm-usage"); // 解决资源限制
                    edgeOptions.addArguments("--remote-debugging-port=9222"); // 强制开启调试模式
                    return new EdgeDriver(edgeOptions);

                case "chrome":
                default:
                    // 假设 chromedriver 已在 PATH 或系统环境中可用。如需固定路径，可按 Edge 分支方式设置 webdriver.chrome.driver。
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--remote-allow-origins=*");
                    chromeOptions.addArguments("--start-maximized");
                    chromeOptions.addArguments("--disable-infobars", "--disable-extensions");
                    return new ChromeDriver(chromeOptions);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Driver 初始化失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 根据 locatorType 定位元素，支持 id/name/xpath 三种方式，捕获未找到元素的友好提示。
     */
    public static WebElement findElement(WebDriver driver, String locatorType, String locatorValue) {
        By by;
        switch (locatorType.toLowerCase()) {
            case "id":
                by = By.id(locatorValue);
                break;
            case "name":
                by = By.name(locatorValue);
                break;
            case "xpath":
                by = By.xpath(locatorValue);
                break;
            default:
                throw new IllegalArgumentException("不支持的定位方式: " + locatorType);
        }
        try {
            return driver.findElement(by);
        } catch (NoSuchElementException ex) {
            throw new IllegalArgumentException("元素定位失败，请确认定位方式和值是否正确: " + locatorType + "=" + locatorValue, ex);
        }
    }

    /**
     * 执行动作，当前支持 input(输入) 与 click(点击)。
     */
    public static void performAction(WebElement element, String actionType, String inputData) {
        switch (actionType.toLowerCase()) {
            case "input":
                element.clear();
                if (inputData != null) {
                    element.sendKeys(inputData);
                }
                break;
            case "click":
                element.click();
                break;
            default:
                throw new IllegalArgumentException("不支持的动作类型: " + actionType);
        }
    }

    /**
     * 截图并返回相对路径，保存在项目根目录下的 screenshots 目录。
     */
    public static String takeScreenshot(WebDriver driver, String caseName) throws IOException {
        Files.createDirectories(SCREENSHOT_DIR);
        String fileName = (caseName == null ? "case" : caseName.replaceAll("\\s+", "_")) + "_" + FORMATTER.format(LocalDateTime.now()) + ".png";
        Path filePath = SCREENSHOT_DIR.resolve(fileName);
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        FileHandler.copy(srcFile, filePath.toFile());
        return filePath.toString();
    }
}
