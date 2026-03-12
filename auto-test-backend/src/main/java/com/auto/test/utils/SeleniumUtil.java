package com.auto.test.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SeleniumUtil {

    private static final Path SCREENSHOT_DIR = Paths.get("screenshots");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final int DEFAULT_WAIT_SECONDS = 20;

    private SeleniumUtil() {
    }

    public static WebDriver getWebDriver(String browserType) {
        String type = (browserType == null || browserType.isBlank()) ? "chrome" : browserType.toLowerCase();

        try {
            Class.forName("io.github.bonigarcia.wdm.WebDriverManager");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("WebDriverManager类未找到", e);
        }

        try {
            String driverRootPath = "G:\\study\\test-management-system\\auto-test-backend\\src\\main\\resources\\drivers";
            File driverDir = new File(driverRootPath);
            if (!driverDir.exists()) {
                throw new RuntimeException("驱动目录不存在：" + driverRootPath);
            }

            String chromeDriverFullPath = driverRootPath + File.separator + "chromedriver.exe";
            String edgeDriverFullPath = driverRootPath + File.separator + "msedgedriver.exe";
            String firefoxDriverFullPath = driverRootPath + File.separator + "geckodriver.exe";

            WebDriver webDriver = null;
            switch (type) {
                case "chrome":
                    System.setProperty("webdriver.chrome.driver", chromeDriverFullPath);
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--remote-allow-origins=*");
                    chromeOptions.addArguments("--start-maximized");
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                    chromeOptions.addArguments("--disable-popup-blocking");
                    webDriver = new ChromeDriver(chromeOptions);
                    break;

                case "edge":
                    System.setProperty("webdriver.edge.driver", edgeDriverFullPath);
                    EdgeOptions edgeOptions = new EdgeOptions();
                    edgeOptions.addArguments("--remote-allow-origins=*");
                    edgeOptions.addArguments("--start-maximized");
                    edgeOptions.addArguments("--no-sandbox");
                    edgeOptions.addArguments("--disable-popup-blocking");
                    webDriver = new EdgeDriver(edgeOptions);
                    break;

                case "firefox":
                    System.setProperty("webdriver.gecko.driver", firefoxDriverFullPath);
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    firefoxOptions.setBinary("F:\\quarkload\\Firefox\\firefox.exe");
                    firefoxOptions.addArguments("--start-maximized");
                    firefoxOptions.addArguments("--no-sandbox");
                    firefoxOptions.addArguments("--disable-dev-shm-usage");
                    webDriver = new FirefoxDriver(firefoxOptions);
                    break;

                default:
                    throw new IllegalArgumentException("仅支持chrome/edge/firefox");
            }

            webDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
            bringBrowserToFront(webDriver);
            return webDriver;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IllegalStateException("Driver初始化失败", ex);
        }
    }

    private static void bringBrowserToFront(WebDriver driver) {
        if (driver == null) return;
        try {
            driver.switchTo().window(driver.getWindowHandle());
            driver.manage().window().maximize();
            driver.manage().window().setPosition(new Point(0, 0));
            ((JavascriptExecutor) driver).executeScript("window.focus();");
        } catch (Exception e) {
            System.out.println("[SeleniumUtil] 浏览器窗口置前失败：" + e.getMessage());
        }
    }

    public static void quitDriver(WebDriver driver) {
        if (driver == null) return;
        try {
            driver.quit();
        } catch (Exception ignored) {}
    }

    public static WebElement findElement(WebDriver driver, String locatorType, String locatorValue) {
        By by = buildByLocator(locatorType, locatorValue);
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
            return wait.until(ExpectedConditions.presenceOfElementLocated(by));
        } catch (Exception ex) {
            throw new IllegalArgumentException("元素定位失败：" + locatorType + "=" + locatorValue, ex);
        }
    }

    private static By buildByLocator(String locatorType, String locatorValue) {
        if (locatorType == null || locatorValue == null || locatorValue.isBlank()) {
            throw new RuntimeException("定位类型/值不能为空");
        }
        return switch (locatorType.toLowerCase()) {
            case "id" -> By.id(locatorValue.trim());
            case "name" -> By.name(locatorValue.trim());
            case "xpath" -> By.xpath(locatorValue.trim());
            case "css" -> By.cssSelector(locatorValue.trim());
            default -> throw new IllegalArgumentException("不支持的定位方式：" + locatorType);
        };
    }

    public static void performAction(WebDriver driver, WebElement element, String actionType, String inputData) {
        try {
            switch (actionType.toLowerCase()) {
                case "input":
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
                    ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", element);
                    Thread.sleep(300);

                    try {
                        element.clear();
                        element.sendKeys(inputData);
                    } catch (Exception e) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].value=arguments[1]", element, inputData);
                    }
                    break;

                case "click":
                    try {
                        element.click();
                    } catch (Exception e) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    }
                    break;

                case "enter":
                    try {
                        element.sendKeys(Keys.ENTER);
                    } catch (Exception e) {
                        ((JavascriptExecutor) driver).executeScript(
                                "arguments[0].dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true}))",
                                element);
                    }
                    break;
            }
        } catch (Exception e) {
        }
    }

    public static String takeScreenshot(WebDriver driver, String caseName) throws IOException {
        if (!Files.exists(SCREENSHOT_DIR)) Files.createDirectories(SCREENSHOT_DIR);
        String fileName = (caseName == null ? "case" : caseName.replaceAll("\\s+", "_"))
                + "_" + FORMATTER.format(LocalDateTime.now()) + ".png";
        Path filePath = SCREENSHOT_DIR.resolve(fileName);
        File srcFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        FileHandler.copy(srcFile, filePath.toFile());
        return filePath.toString();
    }
}