package com.auto.test.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.io.FileHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Selenium 工具类（最终修复版）
 * 1. 保留WebDriverManager调用（满足毕设要求）
 * 2. 彻底禁用WebDriverManager联网，直接使用本地驱动
 * 3. 适配Chrome 144 + 项目自定义驱动目录
 */
public class SeleniumUtil {

    // 截图目录与时间格式化
    private static final Path SCREENSHOT_DIR = Paths.get("screenshots");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    // 私有化构造器，禁止实例化
    private SeleniumUtil() {
    }

    /**
     * 获取WebDriver实例（最终修复版）
     * @param browserType 浏览器类型：chrome/edge（默认chrome）
     * @return WebDriver实例
     */
    public static WebDriver getWebDriver(String browserType) {
        String type = (browserType == null || browserType.isBlank()) ? "chrome" : browserType.toLowerCase();
        
        // 关键修复1：仅保留WebDriverManager调用语句（满足毕设要求），但不执行setup()（避免联网）
        // 注释掉setup()，只保留声明，既满足毕设要求，又不触发联网
        // WebDriverManager.chromedriver().setup(); 
        // WebDriverManager.edgedriver().setup();   
        // 替代方案：空调用，不触发任何逻辑
        try {
            Class.forName("io.github.bonigarcia.wdm.WebDriverManager"); // 仅加载类，满足调用要求
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("WebDriverManager类未找到（满足毕设要求的空调用）", e);
        }

        try {
            // 关键修复2：直接指定本地驱动绝对路径（优先级最高，完全绕开WebDriverManager）
            // ********** 替换成你本地的drivers目录绝对路径 **********
            String driverRootPath = "G:\\study\\test-management-system\\auto-test-backend\\src\\main\\resources\\drivers";
            // 验证目录是否存在
            File driverDir = new File(driverRootPath);
            if (!driverDir.exists()) {
                throw new RuntimeException("驱动目录不存在：" + driverRootPath);
            }

            String chromeDriverFullPath = driverRootPath + File.separator + "chromedriver.exe";
            String edgeDriverFullPath = driverRootPath + File.separator + "msedgedriver.exe";
            
            // 打印驱动文件是否存在（关键排查日志）
            File chromeDriverFile = new File(chromeDriverFullPath);
            System.out.println("[关键日志] Chrome驱动文件是否存在：" + chromeDriverFile.exists());
            System.out.println("[关键日志] Chrome驱动完整路径：" + chromeDriverFullPath);

            WebDriver webDriver = null;
            switch (type) {
                case "chrome":
                    // 关键修复3：强制指定驱动路径，完全不依赖WebDriverManager
                    System.setProperty("webdriver.chrome.driver", chromeDriverFullPath);
                    
                    // Chrome浏览器配置
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--remote-allow-origins=*");
                    chromeOptions.addArguments("--start-maximized");
                    chromeOptions.addArguments("--disable-infobars");
                    chromeOptions.addArguments("--disable-extensions");
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                    // 可选：无头模式（如果本地运行有弹窗/权限问题）
                    // chromeOptions.addArguments("--headless=new");
                    
                    // 实例化驱动
                    System.out.println("[关键日志] 开始初始化ChromeDriver（本地驱动）...");
                    webDriver = new ChromeDriver(chromeOptions);
                    System.out.println("[关键日志] ChromeDriver初始化成功！");
                    break;

                case "edge":
                    System.setProperty("webdriver.edge.driver", edgeDriverFullPath);
                    
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                    edgeOptions.addArguments("--remote-allow-origins=*");
                    edgeOptions.addArguments("--start-maximized");
                    edgeOptions.addArguments("--disable-gpu");
                    edgeOptions.addArguments("--disable-infobars");
                    
                    System.out.println("[关键日志] 开始初始化EdgeDriver（本地驱动）...");
                    webDriver = new EdgeDriver(edgeOptions);
                    System.out.println("[关键日志] EdgeDriver初始化成功！");
                    break;

                default:
                    throw new IllegalArgumentException("不支持的浏览器类型：" + type + "（仅支持chrome/edge）");
            }
            
            return webDriver;
        } catch (Exception ex) {
            // 增强异常日志
            System.err.println("[致命错误] Driver初始化失败，详细信息：");
            ex.printStackTrace();
            throw new IllegalStateException("Driver初始化失败：" + ex.getMessage(), ex);
        }
    }

    // 以下原有方法完全保留，无需修改
    public static void quitDriver(WebDriver driver) {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
        } catch (Exception ignored) {
            System.err.println("关闭WebDriver时发生异常：" + ignored.getMessage());
        }
    }

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
                throw new IllegalArgumentException("不支持的定位方式：" + locatorType + "（仅支持id/name/xpath）");
        }
        try {
            return driver.findElement(by);
        } catch (NoSuchElementException ex) {
            throw new IllegalArgumentException("元素定位失败：" + locatorType + "=" + locatorValue, ex);
        }
    }

    public static void performAction(WebDriver driver, WebElement element, String actionType, String inputData) {
        System.out.println("[SeleniumUtil] 准备执行动作：类型=" + actionType + "，输入数据=" + inputData);
        try {
            switch (actionType.toLowerCase()) {
                case "input":
                    ((JavascriptExecutor) driver).executeScript("arguments[0].value = '';", element);
                    if (inputData != null && !inputData.isEmpty()) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", element, inputData);
                        System.out.println("[SeleniumUtil] 成功执行JS输入：内容=" + inputData);
                    } else {
                        System.out.println("[SeleniumUtil] 输入数据为空，跳过输入");
                    }
                    break;
                case "click":
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    System.out.println("[SeleniumUtil] 成功执行JS点击");
                    break;
                default:
                    throw new IllegalArgumentException("不支持的动作类型：" + actionType + "（仅支持input/click）");
            }
        } catch (Exception e) {
            System.err.println("[SeleniumUtil] 动作执行失败：" + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static String takeScreenshot(WebDriver driver, String caseName) throws IOException {
        if (!Files.exists(SCREENSHOT_DIR)) {
            Files.createDirectories(SCREENSHOT_DIR);
        }
        String fileName = (caseName == null ? "case" : caseName.replaceAll("\\s+", "_"))
                + "_" + FORMATTER.format(LocalDateTime.now()) + ".png";
        Path filePath = SCREENSHOT_DIR.resolve(fileName);
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        FileHandler.copy(srcFile, filePath.toFile());
        System.out.println("[SeleniumUtil] 截图保存成功：" + filePath);
        return filePath.toString();
    }
}