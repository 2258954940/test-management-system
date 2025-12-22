package com.auto.test.utils; // 必须和SeleniumUtil同包

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;

/**
 * 自动化测试通用断言工具类（适配任意网页的动态断言）
 */
public class AssertUtil {
    // 默认等待时间（10秒，和你的SeleniumUtil保持一致）
    private static final int DEFAULT_WAIT_SECONDS = 10;

    /**
     * 动态断言核心方法：从用例配置读取规则，验证任意网页的元素/文本
     * @param driver 浏览器驱动（复用你的SeleniumUtil创建的driver）
     * @param assertType 断言类型：TEXT（文本匹配）/ELEMENT（元素存在）
     * @param locatorType 定位类型：id/xpath/name（和你的Element实体一致）
     * @param locatorValue 定位值（比如元素ID、XPath）
     * @param expectedValue 预期值（文本断言时用，元素断言时传null即可）
     */
    public static void assertByConfig(WebDriver driver, 
                                     String assertType, 
                                     String locatorType, 
                                     String locatorValue, 
                                     String expectedValue) {
        // 1. 构建定位符（适配你的Element实体的定位类型）
        By elementLocator = buildByLocator(locatorType, locatorValue);

        // 2. 根据断言类型执行验证
        switch (assertType.toUpperCase()) {
            case "ELEMENT":
                // 断言：元素存在（比如购物车图标、登录按钮）
                waitForElementExist(driver, elementLocator);
                break;
            case "TEXT":
                // 断言：元素文本包含预期内容（修改点1：从严格相等→包含匹配）
                String actualText = getElementText(driver, elementLocator);
                // 核心修改：equals → contains
                if (!actualText.trim().contains(expectedValue.trim())) {
                    throw new RuntimeException(
                        // 修改点2：异常提示说明“包含”
                        String.format("文本断言失败！预期包含：%s，实际：%s", expectedValue, actualText)
                    );
                }
                break;
            default:
                throw new RuntimeException("不支持的断言类型：" + assertType);
        }
    }

    // 私有方法：构建定位符（抽离出来，方便复用）
    private static By buildByLocator(String locatorType, String locatorValue) {
        if (locatorType == null || locatorValue == null) {
            throw new RuntimeException("定位类型/定位值不能为空！");
        }
        return switch (locatorType.toLowerCase()) {
            case "id" -> By.id(locatorValue);
            case "xpath" -> By.xpath(locatorValue);
            case "name" -> By.name(locatorValue);
            default -> throw new RuntimeException("不支持的定位类型：" + locatorType);
        };
    }

    // 私有方法：等待元素存在（封装WebDriverWait，避免重复写）
    private static void waitForElementExist(WebDriver driver, By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
            .until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    // 私有方法：获取元素文本（确保元素可见后再获取）
    private static String getElementText(WebDriver driver, By locator) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
            .until(ExpectedConditions.visibilityOfElementLocated(locator));
        return element.getText();
    }
}