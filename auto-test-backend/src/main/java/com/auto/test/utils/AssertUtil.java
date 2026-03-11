package com.auto.test.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.time.Duration;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class AssertUtil {
    private static final int DEFAULT_WAIT_SECONDS = 20;

    public static void assertByConfig(WebDriver driver,
                                     String assertType,
                                     String locatorType,
                                     String locatorValue,
                                     String expectedValue) {
        By elementLocator = buildByLocator(locatorType, locatorValue);
        String expected = expectedValue == null ? "" : expectedValue.trim();

        // 自动切换到最新打开的标签页
        try {
            String currentHandle = driver.getWindowHandle();
            for (String handle : driver.getWindowHandles()) {
                if (!handle.equals(currentHandle)) {
                    driver.switchTo().window(handle);
                }
            }
        } catch (Exception e) {
            System.out.println("[AssertUtil] 切换到新标签页失败: " + e.getMessage());
        }

        switch (assertType.toUpperCase()) {
            case "ELEMENT":
            case "EXISTS":
                waitForElementExist(driver, elementLocator);
                break;
            case "TEXT":
                String actualText = "";
                try {
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
                    WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(elementLocator));
                    wait.until(ExpectedConditions.elementToBeClickable(elementLocator));
                    actualText = element.getText().trim();
                    if (!actualText.contains(expected)) {
                        throw new RuntimeException(
                                String.format("元素文本断言失败！预期包含：%s，实际：%s", expected, actualText)
                        );
                    }
                } catch (TimeoutException e) {
                    throw new RuntimeException("断言元素定位/可点击超时：" + locatorType + "=" + locatorValue, e);
                }
                break;
            default:
                throw new RuntimeException("不支持的断言类型：" + assertType);
        }
    }

    public static void assertSearchSuccess(WebDriver driver,
                                          String keyword,
                                          String resultAreaLocatorType,
                                          String resultAreaLocatorValue) {
        String cleanKeyword = keyword.trim();
        String encodedKeyword = URLEncoder.encode(cleanKeyword, StandardCharsets.UTF_8);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS));

        String currentUrl = driver.getCurrentUrl();
        if (!currentUrl.contains(cleanKeyword) && !currentUrl.contains(encodedKeyword)) {
            throw new RuntimeException(
                    String.format("URL未包含目标关键词！当前URL：%s，预期包含：%s（或编码版：%s）",
                            currentUrl, cleanKeyword, encodedKeyword)
            );
        }
        System.out.println("[通用搜索断言] 条件1通过：URL包含关键词");

        By resultAreaLocator = buildByLocator(resultAreaLocatorType, resultAreaLocatorValue);
        WebElement resultArea = wait.until(ExpectedConditions.visibilityOfElementLocated(resultAreaLocator));
        String resultAreaText = resultArea.getText().trim();

        if (resultAreaText.isEmpty() || !resultAreaText.contains(cleanKeyword)) {
            String textPreview = resultAreaText.substring(0, Math.min(resultAreaText.length(), 200));
            throw new RuntimeException(
                    String.format("结果区域未包含关键词！预期：%s，结果区域文本片段：%s",
                            cleanKeyword, textPreview)
            );
        }
        System.out.println("[通用搜索断言] 条件2通过：结果区域包含关键词");
    }

    private static By buildByLocator(String locatorType, String locatorValue) {
        if (locatorType == null || locatorValue == null || locatorValue.trim().isEmpty()) {
            throw new RuntimeException("定位类型/定位值不能为空！");
        }
        return switch (locatorType.toLowerCase()) {
            case "id" -> By.id(locatorValue.trim());
            case "xpath" -> By.xpath(locatorValue.trim());
            case "name" -> By.name(locatorValue.trim());
            case "css" -> By.cssSelector(locatorValue.trim());
            default -> throw new RuntimeException("不支持的定位类型：" + locatorType);
        };
    }

    private static void waitForElementExist(WebDriver driver, By locator) {
        new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_WAIT_SECONDS))
                .until(ExpectedConditions.presenceOfElementLocated(locator));
    }
}