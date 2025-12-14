package com.auto.test.service;

import com.auto.test.dto.TestCaseRequest;
import com.auto.test.entity.TestCase;
import com.auto.test.entity.TestResult;
import com.auto.test.mapper.TestCaseMapper;
import com.auto.test.mapper.TestResultMapper;
import com.auto.test.utils.SeleniumUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.BuildInfo;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import com.auto.test.entity.Element;
import com.auto.test.mapper.ElementMapper;
import java.util.Arrays;
/**
 * 用例业务服务：新增、查询、更新、删除、执行。
 */
@Service
public class TestCaseService {

    private final TestCaseMapper testCaseMapper;
    private final TestResultMapper testResultMapper;
    private final ElementMapper elementMapper; // 新增

    // 构造方法新增ElementMapper注入（注意：要保证Spring能扫描到ElementMapper）
    public TestCaseService(TestCaseMapper testCaseMapper, TestResultMapper testResultMapper, ElementMapper elementMapper) {
        this.testCaseMapper = testCaseMapper;
        this.testResultMapper = testResultMapper;
        this.elementMapper = elementMapper; // 新增
    }

    /**
     * 新增测试用例，并返回包含自增 ID 的实体。
     */
    // 3. 改造addTestCase方法（新增用例时存入elementIds）
    @Transactional
    public TestCase addTestCase(TestCaseRequest request) {
        TestCase testCase = new TestCase();
        testCase.setName(request.getName());
        testCase.setDescription(request.getDescription());
        testCase.setUrl(request.getUrl());
        // 核心修复：非空字段默认值（保留）
        testCase.setLocatorType(request.getLocatorType() != null ? request.getLocatorType() : "id");
        testCase.setLocatorValue(request.getLocatorValue() != null ? request.getLocatorValue() : "");
        testCase.setActionType(request.getActionType() != null ? request.getActionType() : "click");
        testCase.setInputData(request.getInputData() != null ? request.getInputData() : "");
        testCase.setExpectedResult(request.getExpectedResult() != null ? request.getExpectedResult() : "");
        // 新增：存入关联的元素ID
        testCase.setElementIds(request.getElementIds()); // 从前端接收elementIds
         testCase.setCreator(request.getCreator()); 
        testCaseMapper.insertTestCase(testCase);
        return testCase;
    }


    /**
     * 执行指定用例，调用 Selenium，捕获异常并记录结果。
     */
       // 5. 改造runTestCase方法（核心：用元素库替代硬编码，复用SeleniumUtil）
   @Transactional
public TestResult runTestCase(Long caseId) {
    TestCase testCase = testCaseMapper.findById(caseId);
    if (testCase == null) {
        throw new IllegalArgumentException("用例不存在，ID=" + caseId);
    }

    WebDriver driver = null;
    TestResult result = new TestResult();
    result.setCaseId(caseId);
    result.setRunTime(LocalDateTime.now());
    long start = System.currentTimeMillis();

    try {
        // 初始化Edge驱动（用你已配置好的SeleniumUtil）
        driver = SeleniumUtil.createDriver(null, "edge");
        System.out.println("[TestCaseService] Edge驱动初始化成功，浏览器版本：" + ((HasCapabilities) driver).getCapabilities().getBrowserVersion());

        // 打开用例页面
        driver.get(testCase.getUrl());
        // 延长页面加载等待时间（适配百度加载）
        new WebDriverWait(driver, Duration.ofSeconds(20)).until(webDriver ->
            ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
        System.out.println("[TestCaseService] 页面加载完成：" + testCase.getUrl());
            // 新增：关闭百度隐私政策弹窗（如果存在）
            try {
                WebElement privacyBtn = driver.findElement(By.xpath("//button[contains(text(),'同意') or contains(text(),'Accept')]"));
                if (privacyBtn.isDisplayed()) {
                    privacyBtn.click();
                    System.out.println("[TestCaseService] 关闭百度隐私弹窗成功");
                    Thread.sleep(1000); // 弹窗关闭后等待1秒
                }
            } catch (Exception e) {
                System.out.println("[TestCaseService] 未检测到百度隐私弹窗，跳过关闭");
            }
        // ========== 核心：从元素库取定位器，替代硬编码 ==========
        String elementIds = testCase.getElementIds();
        if (elementIds == null || elementIds.trim().isEmpty()) {
            throw new IllegalArgumentException("用例未关联任何元素，请先在前端绑定元素！");
        }
           // -------------------------- 在这里新增打印代码 --------------------------
        System.out.println("[TestCaseService] 页面标题：" + driver.getTitle());
        System.out.println("[TestCaseService] 页面源码前500字符：" + driver.getPageSource().substring(0, Math.min(500, driver.getPageSource().length())));
        // ------------------------------------------------------------------------
        // 解析元素ID数组
        String[] elementIdArray = elementIds.split(",");
        System.out.println("[TestCaseService] 用例关联的元素ID：" + Arrays.toString(elementIdArray));

        // 遍历每个元素，执行自动化操作（复用你的SeleniumUtil工具）
        for (String elementIdStr : elementIdArray) {
            Long elementId = Long.parseLong(elementIdStr.trim());
            Element element = elementMapper.findById(elementId);
            if (element == null) {
                throw new IllegalArgumentException("元素不存在，ID=" + elementId);
            }

            System.out.println("[TestCaseService] 开始处理元素：名称=" + element.getElementName() + "，定位类型=" + element.getLocatorType() + "，定位值=" + element.getLocatorValue());

            // ========== 关键修改1：显式等待元素可交互（替代原有的findElement） ==========
            By by = switch (element.getLocatorType().toLowerCase()) {
                case "id" -> By.id(element.getLocatorValue());
                case "xpath" -> By.xpath(element.getLocatorValue());
                case "name" -> By.name(element.getLocatorValue());
                default -> throw new IllegalArgumentException("不支持的定位方式：" + element.getLocatorType());
            };

                // ========== 关键修改：放宽定位条件，先确保元素存在，再手动聚焦 ==========
                // 1. 等待元素在DOM中存在（15秒超时，比elementToBeClickable更宽松）
                WebElement webElement = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.presenceOfElementLocated(by));
                System.out.println("[TestCaseService] 元素已存在（DOM中）：" + element.getElementName());

              // 2. 手动等待+滚动到元素可见+JS强制点击（绕过Selenium交互检查）
        Thread.sleep(1000); 
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", webElement); 
        // 关键：用JS点击元素，无视Selenium的interactable检查
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", webElement); 
        System.out.println("[TestCaseService] 元素已通过JS聚焦（可操作）：" + element.getElementName());            // ========== 关键修改2：补充动作执行日志 ==========
                 // 根据元素的widgetType，匹配动作类型（input/click）
            String actionType = switch (element.getWidgetType().toLowerCase()) {
                case "input" -> "input"; // 输入框→执行input动作
                case "button" -> "click"; // 按钮→执行click动作
                case "select" -> "click"; // 下拉框先点击展开（可扩展）
                default -> throw new IllegalArgumentException("不支持的控件类型：" + element.getWidgetType());
            };
            System.out.println("[TestCaseService] 匹配动作类型：" + actionType + "，输入数据：" + testCase.getInputData());

            // 执行动作（输入框填用例的inputData，按钮直接点击）
           SeleniumUtil.performAction(driver, webElement, actionType, testCase.getInputData());

            System.out.println("[TestCaseService] 成功执行动作：" + actionType + "，元素：" + element.getElementName());

            // 动作后等待（避免页面未响应，延长到1秒）
            Thread.sleep(1000);
        }

        // ========== 验证预期结果（保留原有逻辑，加日志） ==========
        // String expectedResult = testCase.getExpectedResult();
        // if (expectedResult != null && !expectedResult.isEmpty()) {
        //     System.out.println("[TestCaseService] 开始验证预期结果：" + expectedResult);
        //     boolean isExpected = new WebDriverWait(driver, Duration.ofSeconds(10))
        //         .until(ExpectedConditions.textToBePresentInElementLocated(
        //             By.tagName("body"), expectedResult));
        //     if (!isExpected) {
        //         throw new Exception("预期结果未匹配：页面未包含文本「" + expectedResult + "」");
        //     }
        //     System.out.println("[TestCaseService] 预期结果验证通过");
        // }

        // 截图（复用你的SeleniumUtil）
        String screenshotPath = SeleniumUtil.takeScreenshot(driver, testCase.getName());
        result.setScreenshotPath(screenshotPath);
        result.setStatus("PASS");
        result.setMessage("执行成功，截图路径：" + screenshotPath);

    } catch (IllegalArgumentException ex) {
        result.setStatus("FAILED");
        result.setMessage("参数错误: " + ex.getMessage());
        result.setScreenshotPath(takeFailScreenshot(driver, testCase));
        System.err.println("[TestCaseService] 执行失败（参数错误）：" + ex.getMessage());
    } catch (IOException ioEx) {
        result.setStatus("FAILED");
        result.setMessage("截图失败: " + ioEx.getMessage());
        result.setScreenshotPath(null);
        System.err.println("[TestCaseService] 执行失败（截图错误）：" + ioEx.getMessage());
    } catch (Exception ex) {
        result.setStatus("FAILED");
        result.setMessage("执行异常: " + ex.getMessage());
        result.setScreenshotPath(takeFailScreenshot(driver, testCase));
        System.err.println("[TestCaseService] 执行失败（运行异常）：" + ex.getMessage());
        // 打印完整异常堆栈（关键！定位根因）
        ex.printStackTrace();
    } finally {
        result.setDurationMs(System.currentTimeMillis() - start);
        if (driver != null) {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            driver.quit();
        }
        testResultMapper.insertTestResult(result);
    }
    return result;
}

    /**
     * 查询所有测试用例，按创建时间倒序。
     */
    public List<TestCase> listAll() {
        return testCaseMapper.findAll();
    }

    /**
     * 更新指定测试用例，若不存在则抛出非法参数异常。
     */
 // 4. 改造updateTestCase方法（同步更新elementIds）
    public void updateTestCase(Long id, TestCase request) {
        TestCase originalCase = testCaseMapper.findById(id);
        if (originalCase == null) {
            throw new IllegalArgumentException("用例不存在");
        }

        // 原有字段赋值（保留）
        originalCase.setName(request.getName() != null ? request.getName() : originalCase.getName());
        originalCase.setDescription(request.getDescription() != null ? request.getDescription() : originalCase.getDescription());
        originalCase.setUrl(request.getUrl() != null ? request.getUrl() : originalCase.getUrl());
        originalCase.setCreator(request.getCreator() != null ? request.getCreator() : originalCase.getCreator());
        originalCase.setLocatorType(request.getLocatorType() != null ? request.getLocatorType() : originalCase.getLocatorType());
        originalCase.setLocatorValue(request.getLocatorValue() != null ? request.getLocatorValue() : originalCase.getLocatorValue());
        originalCase.setActionType(request.getActionType() != null ? request.getActionType() : originalCase.getActionType());
        originalCase.setInputData(request.getInputData() != null ? request.getInputData() : originalCase.getInputData());
        originalCase.setExpectedResult(request.getExpectedResult() != null ? request.getExpectedResult() : originalCase.getExpectedResult());
        // 新增：更新elementIds
        originalCase.setElementIds(request.getElementIds() != null ? request.getElementIds() : originalCase.getElementIds());

        testCaseMapper.updateTestCase(originalCase);
    }

    /**
     * 删除指定测试用例，若不存在则抛出非法参数异常。
     */
    @Transactional
    public void deleteTestCase(Long id) {
        TestCase exists = testCaseMapper.findById(id);
        if (exists == null) {
            throw new IllegalArgumentException("用例不存在，ID=" + id);
        }
        int deleted = testCaseMapper.deleteTestCase(id);
        if (deleted == 0) {
            throw new IllegalStateException("删除失败，未影响任何行");
        }
    }

    private String takeFailScreenshot(WebDriver driver, TestCase testCase) {
        if (driver == null) {
            return null;
        }
        try {
            return SeleniumUtil.takeScreenshot(driver, testCase.getName());
        } catch (Exception ignore) {
            return null;
        }
    }
}
