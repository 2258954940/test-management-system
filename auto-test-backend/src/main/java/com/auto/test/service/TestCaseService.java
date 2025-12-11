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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用例业务服务：新增、查询、更新、删除、执行。
 */
@Service
public class TestCaseService {

    private final TestCaseMapper testCaseMapper;
    private final TestResultMapper testResultMapper;

    public TestCaseService(TestCaseMapper testCaseMapper, TestResultMapper testResultMapper) {
        this.testCaseMapper = testCaseMapper;
        this.testResultMapper = testResultMapper;
    }

    /**
     * 新增测试用例，并返回包含自增 ID 的实体。
     */
    @Transactional
    public TestCase addTestCase(TestCaseRequest request) {
        TestCase testCase = new TestCase();
        testCase.setName(request.getName());
        testCase.setDescription(request.getDescription());
        testCase.setUrl(request.getUrl());
    // 核心修复：给数据库非空字段加默认值，避开NOT NULL约束
    testCase.setLocatorType(request.getLocatorType() != null ? request.getLocatorType() : "id"); // 默认值
    testCase.setLocatorValue(request.getLocatorValue() != null ? request.getLocatorValue() : "");
    testCase.setActionType(request.getActionType() != null ? request.getActionType() : "click"); // 默认值
    testCase.setInputData(request.getInputData() != null ? request.getInputData() : "");
    testCase.setExpectedResult(request.getExpectedResult() != null ? request.getExpectedResult() : "");
        testCaseMapper.insertTestCase(testCase);
        return testCase;
    }

    /**
     * 执行指定用例，调用 Selenium，捕获异常并记录结果。
     */
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
            driver = SeleniumUtil.createDriver(null, "edge");
            System.out.println("Selenium版本：" + new BuildInfo().getReleaseLabel());
            System.out.println("Edge驱动路径：" + System.getProperty("webdriver.edge.driver"));
            System.out.println("Edge驱动版本：" + ((HasCapabilities) driver).getCapabilities().getCapability("msedgedriverVersion"));

            driver.get(testCase.getUrl());

            new WebDriverWait(driver, Duration.ofSeconds(10)).until(webDriver ->
                ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));

            Object ids = ((JavascriptExecutor) driver).executeScript(
                "return Array.from(document.querySelectorAll('[id]')).map(e => e.id);");
            System.out.println("[Debug] Page IDs: " + ids);

            By by = By.id("kw");
            new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(by));

            Boolean isKwExist = (Boolean) ((JavascriptExecutor) driver)
                .executeScript("return document.getElementById('kw') !== null;");
            System.out.println("kw元素是否存在：" + isKwExist);

            ((JavascriptExecutor) driver).executeScript(
                "document.getElementById('kw').value = arguments[0];",
                testCase.getInputData());
            ((JavascriptExecutor) driver).executeScript("document.getElementById('su').click();");

            String actualInput = (String) ((JavascriptExecutor) driver)
                .executeScript("return document.getElementById('kw').value;");
            System.out.println("搜索框实际输入内容：" + actualInput);

            new WebDriverWait(driver, Duration.ofSeconds(8))
                .until(ExpectedConditions.presenceOfElementLocated(By.id("content_left")));

            String screenshotPath = SeleniumUtil.takeScreenshot(driver, testCase.getName());
            result.setScreenshotPath(screenshotPath);
            result.setStatus("PASS");
            result.setMessage("执行成功，截图已保存");
        } catch (IllegalArgumentException ex) {
            result.setStatus("FAILED");
            result.setMessage("参数错误: " + ex.getMessage());
            result.setScreenshotPath(takeFailScreenshot(driver, testCase));
        } catch (IOException ioEx) {
            result.setStatus("FAILED");
            result.setMessage("截图失败: " + ioEx.getMessage());
            result.setScreenshotPath(null);
        } catch (Exception ex) {
            result.setStatus("FAILED");
            result.setMessage("执行异常: " + ex.getMessage());
            result.setScreenshotPath(takeFailScreenshot(driver, testCase));
        } finally {
            result.setDurationMs(System.currentTimeMillis() - start);
            if (driver != null) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
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
    @Transactional
    public void updateTestCase(Long id, TestCase payload) {
        TestCase exists = testCaseMapper.findById(id);
        if (exists == null) {
            throw new IllegalArgumentException("用例不存在，ID=" + id);
        }
        payload.setId(id);
        int updated = testCaseMapper.updateTestCase(payload);
        if (updated == 0) {
            throw new IllegalStateException("更新失败，未影响任何行");
        }
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
