package com.auto.test.service;

import com.auto.test.dto.TestCaseRequest;
import com.auto.test.entity.Element;
import com.auto.test.entity.SiteTestConfigDO;
import com.auto.test.entity.TestCase;
import com.auto.test.entity.TestResult;
import com.auto.test.mapper.ElementMapper;
import com.auto.test.mapper.TestCaseMapper;
import com.auto.test.mapper.TestResultMapper;
import com.auto.test.utils.AssertUtil;
import com.auto.test.utils.SeleniumUtil;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestCaseService {
    private static final Logger log = LoggerFactory.getLogger(TestCaseService.class);
    private final TestCaseMapper testCaseMapper;
    private final TestResultMapper testResultMapper;
    private final ElementMapper elementMapper;
    private final SiteTestConfigService siteTestConfigService;

    @Autowired
    public TestCaseService(TestCaseMapper testCaseMapper, TestResultMapper testResultMapper,
                           ElementMapper elementMapper, SiteTestConfigService siteTestConfigService) {
        this.testCaseMapper = testCaseMapper;
        this.testResultMapper = testResultMapper;
        this.elementMapper = elementMapper;
        this.siteTestConfigService = siteTestConfigService;
    }

    @Transactional
    public TestCase addTestCase(TestCaseRequest request) {
        TestCase testCase = new TestCase();
        testCase.setName(request.getName());
        testCase.setDescription(request.getDescription());
        testCase.setUrl(request.getUrl());
        testCase.setLocatorType(request.getLocatorType() != null ? request.getLocatorType() : "id");
        testCase.setLocatorValue(request.getLocatorValue() != null ? request.getLocatorValue() : "");
        testCase.setActionType(request.getActionType() != null ? request.getActionType() : "click");
        testCase.setInputData(request.getInputData() != null ? request.getInputData() : "");
        testCase.setExpectedResult(request.getExpectedResult() != null ? request.getExpectedResult() : "");
        testCase.setElementIds(request.getElementIds());
        testCase.setCreator(request.getCreator());
        testCase.setNeedLogin(request.getNeedLogin() != null ? request.getNeedLogin() : false);
        testCase.setSiteCode(request.getSiteCode());
        // ========== 新增：保存测试账号密码 ==========
        testCase.setTestAccount(request.getTestAccount() != null ? request.getTestAccount() : "");
        testCase.setTestPassword(request.getTestPassword() != null ? request.getTestPassword() : "");
        // ========== 原有：断言字段 ==========
        testCase.setAssertType(request.getAssertType() != null ? request.getAssertType() : "TEXT");
        testCase.setAssertLocatorType(request.getAssertLocatorType() != null ? request.getAssertLocatorType() : "id");
        testCase.setAssertLocatorValue(request.getAssertLocatorValue() != null ? request.getAssertLocatorValue() : "");
        testCase.setAssertExpectedValue(request.getAssertExpectedValue() != null ? request.getAssertExpectedValue() : "");

        testCase.setBrowser(request.getBrowser() != null ? request.getBrowser() : "edge");
        testCaseMapper.insertTestCase(testCase);
        return testCase;
    }

    public TestCase findById(Long id) {
        TestCase testCase = testCaseMapper.findById(id);
        if (testCase == null) {
            throw new IllegalArgumentException("用例不存在，ID=" + id);
        }
        return testCase;
    }

    public TestResult runTestCase(Long caseId) {
        // 修复：先获取用例，再根据用例的site_code获取配置
        TestCase testCase = testCaseMapper.findById(caseId);
        if (testCase == null) {
            throw new IllegalArgumentException("用例不存在，ID=" + caseId);
        }
        // 优先使用用例的site_code；未配置则不强制回落默认站点
        SiteTestConfigDO defaultConfig = null;
        if (testCase.getSiteCode() != null && !testCase.getSiteCode().isEmpty()) {
            defaultConfig = siteTestConfigService.getBySiteCode(testCase.getSiteCode());
        }
        return runTestCase(caseId, defaultConfig, false, "", "", "", testCase.getBrowser(), null);   
     }

    public TestResult runTestCase(Long caseId, SiteTestConfigDO siteConfig,
                                  boolean needLogin,
                                  String username, String password, String expectedNickname,
                                  String browserType,
                                  Long taskId) {
        WebDriver driver = null;
        try {
            driver = SeleniumUtil.getWebDriver(browserType);
            return runTestCase(caseId, siteConfig, needLogin, username, password, expectedNickname, taskId, driver);
        } finally {
            if (driver != null) {
                try {
                    Thread.sleep(3000);
                    SeleniumUtil.quitDriver(driver);
                    log.info("[驱动关闭] caseId={} 成功", caseId);
                } catch (Exception ignored) {
                    log.warn("[驱动关闭] caseId={} 失败", caseId, ignored);
                }
            }
        }
    }

    public TestResult runTestCase(Long caseId, SiteTestConfigDO siteConfig,
                                  boolean needLogin, String username, String password, String expectedNickname,
                                  Long taskId, WebDriver driver) {
        TestCase testCase = testCaseMapper.findById(caseId);
        if (testCase == null) {
            throw new IllegalArgumentException("用例不存在，ID=" + caseId);
        }
        // if (needLogin) {
        //     if (username == null || username.isBlank()) {
        //         username = testCase.getTestAccount();
        //     }
        //     if (password == null || password.isBlank()) {
        //         password = testCase.getTestPassword();
        //     }

        //     if (username == null || username.isBlank() || password == null || password.isBlank()) {
        //         throw new RuntimeException("该用例需要登录，请配置测试账号和密码");
        //     }
        // }登录代码中已改为执行时输入账号密码，这里不再从用例读取默认账号密码，保持用例数据清洁。
        // 修复：如果传入的siteConfig为空，根据用例的site_code动态获取
        if (siteConfig == null) {
            String siteCode = testCase.getSiteCode();
            if (siteCode != null && !siteCode.isEmpty()) {
                siteConfig = siteTestConfigService.getBySiteCode(siteCode);
            }
        }

        TestResult result = new TestResult();
        result.setCaseId(caseId);
        result.setTaskId(taskId);
        result.setRunTime(LocalDateTime.now());
        long start = System.currentTimeMillis();
        Map<String, String> verifyDetail = new HashMap<>();
        boolean isAllPass = true;

        try {
                log.info("【用例执行开始】caseId={}, 用例名称={}, 测试URL={}, 是否登录={}",
                    caseId, testCase.getName(), testCase.getUrl(), needLogin);

            log.info("[驱动复用成功] 浏览器版本：{}",
                    ((HasCapabilities) driver).getCapabilities().getBrowserVersion());

            driver.manage().window().maximize();
            log.info("[浏览器已最大化] 确保所有元素可见");

            // if (needLogin && siteConfig != null && username != null && !username.isEmpty()) {
            //     log.info("[登录流程开始] 目标网站：{}，账号：{}", siteConfig.getSiteName(), username);
            //     Map<String, String> loginVerifyDetail = validateWithLoginUI(driver, siteConfig, username, password, expectedNickname);
            //     verifyDetail.putAll(loginVerifyDetail);
            //     for (String key : loginVerifyDetail.keySet()) {
            //         if (loginVerifyDetail.get(key).contains("失败")) {
            //             isAllPass = false;
            //             throw new RuntimeException("登录失败：" + loginVerifyDetail.get(key));
            //         }
            //     }
            //     log.info("[登录流程完成] caseId={} 登录成功", caseId);
            // }登录流程已改为单独接口，这里不再自动执行登录，保持用例执行的纯粹性和灵活性。

            if (testCase.getUrl() != null && !testCase.getUrl().isEmpty()) {
                driver.get(testCase.getUrl());

                new WebDriverWait(driver, Duration.ofSeconds(20)).until(webDriver ->
                        ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));

                String actualUrl = driver.getCurrentUrl();
                String actualTitle = driver.getTitle();
                log.info("[调试-页面信息] 实际访问URL：{}，预期URL：{}", actualUrl, testCase.getUrl());
                // log.info("[调试-页面信息] 实际页面标题：{}", actualTitle);

                Boolean isKwExist = (Boolean) ((JavascriptExecutor) driver).executeScript(
                        "return document.getElementById('kw') !== null;"
                );
                log.info("[调试-元素检查] id=kw 元素是否存在：{}", isKwExist);

                List<WebElement> allInputElements = driver.findElements(By.tagName("input"));
                log.info("[调试-元素列表] 页面中所有input元素数量：{}", allInputElements.size());
                for (int i = 0; i < allInputElements.size(); i++) {
                    WebElement input = allInputElements.get(i);
                    String inputId = input.getAttribute("id");
                    String inputName = input.getAttribute("name");
                    String inputType = input.getAttribute("type");
                    log.info("[调试-元素列表] 第{}个input：id={}, name={}, type={}", i + 1, inputId, inputName, inputType);
                }

                if (actualTitle == null || actualTitle.trim().isEmpty()) {
                    log.info("[页面加载完成] URL={}", testCase.getUrl());
                } else {
                    log.info("[页面加载完成] URL={}, 标题={}", testCase.getUrl(), actualTitle);
                }

                // 移除硬编码等待 head_wrapper
                new WebDriverWait(driver, Duration.ofSeconds(10)).until(webDriver ->
                        ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));

                String elementIds = testCase.getElementIds();
                if (elementIds != null && !elementIds.trim().isEmpty()) {
                    String[] elementIdArray = elementIds.split(",");
                    log.info("[元素解析] 共{}个元素待处理：{}", elementIdArray.length, Arrays.toString(elementIdArray));
                    int i = 0;
                    int dataIndex = 0;
                    for (String elementIdStr : elementIdArray) {
                        Long elementId = Long.parseLong(elementIdStr.trim());
                        Element element = elementMapper.findById(elementId);
                        if (element == null) {
                            String errMsg = "元素不存在，ID=" + elementId;
                            log.error(errMsg);
                            isAllPass = false;
                            verifyDetail.put("元素查询", "失败：" + errMsg);
                            throw new RuntimeException(errMsg);
                        }

                        log.info("[元素处理] ID={}, 名称={}, 定位类型={}, 定位值={}",
                                elementId, element.getElementName(), element.getLocatorType(), element.getLocatorValue());

                        By by = switch (element.getLocatorType().toLowerCase()) {
                            case "id" -> By.id(element.getLocatorValue());
                            case "xpath" -> By.xpath(element.getLocatorValue());
                            case "name" -> By.name(element.getLocatorValue());
                            default -> {
                                String errMsg = "不支持的定位方式：" + element.getLocatorType();
                                log.error(errMsg);
                                isAllPass = false;
                                verifyDetail.put("定位方式", "失败：" + errMsg);
                                throw new RuntimeException(errMsg);
                            }
                        };

                            WebElement webElement = null;
                            try {
                                log.info("[调试-定位开始] 等待元素{}存在（定位方式：{}={}）", element.getElementName(), element.getLocatorType(), element.getLocatorValue());
                                // 第一步：等待元素存在（presence）- 基础条件
                                WebElement tempElement = new WebDriverWait(driver, Duration.ofSeconds(15))
                                        .until(ExpectedConditions.presenceOfElementLocated(by));

                                try {
                                    // 第二步：优先等待元素可见（visibility）- 宽松条件，超时不抛异常
                                    webElement = new WebDriverWait(driver, Duration.ofSeconds(5))
                                            .until(ExpectedConditions.visibilityOfElementLocated(by));
                                    log.info("[元素定位成功-可见] ID={}, 名称={}", elementId, element.getElementName());
                                } catch (TimeoutException e) {
                                    // 兜底：元素存在但不可见时，直接使用存在的元素
                                    webElement = tempElement;
                                    log.warn("[元素定位-可见性超时] ID={}, 名称={}，使用存在但可能不可见的元素继续执行", elementId, element.getElementName());
                                    
                                    // 强制滚动到元素位置并尝试激活
                                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center', behavior: 'smooth'});", webElement);
                                    Thread.sleep(800);
                                    
                                    // 强制设置元素可见（JS兜底）
                                    ((JavascriptExecutor) driver).executeScript("arguments[0].style.display = 'block'; arguments[0].style.visibility = 'visible';", webElement);
                                }

                                // 补充元素状态日志
                                String elementTag = webElement.getTagName();
                                String elementIdAttr = webElement.getAttribute("id");
                                String elementNameAttr = webElement.getAttribute("name");
                                Boolean elementDisplayed = webElement.isDisplayed();
                                Boolean elementEnabled = webElement.isEnabled();
                                log.info("[调试-元素详情] 标签名：{}，id属性：{}，name属性：{}，是否可见：{}，是否可用：{}",
                                        elementTag, elementIdAttr, elementNameAttr, elementDisplayed, elementEnabled);
                        } catch (TimeoutException | NoSuchElementException ex) {
                            String errMsg = "元素定位超时/不存在：" + element.getLocatorType() + "=" + element.getLocatorValue();
                            log.error(errMsg, ex);
                            isAllPass = false;
                            verifyDetail.put("元素定位", "失败：" + errMsg);
                            throw new RuntimeException(errMsg, ex);
                        }

                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block: 'center'});", webElement);
                        Thread.sleep(400);

                        String actionType = switch (element.getWidgetType().toLowerCase()) {
                            case "input" -> "input";
                            case "button" -> "click";
                            case "select" -> "click";
                            default -> testCase.getActionType();
                        };
                        String tagName = webElement.getTagName();
                        String inputType = webElement.getAttribute("type");
                        if ("input".equalsIgnoreCase(tagName)
                                && inputType != null
                                && ("submit".equalsIgnoreCase(inputType) || "button".equalsIgnoreCase(inputType))) {
                            actionType = "click";
                        }

                        try {
                            String[] inputDataArray = testCase.getInputData() != null ? testCase.getInputData().split(",") : new String[0];
                            String currentInputData = "";
                            boolean isInputWidget = "input".equalsIgnoreCase(element.getWidgetType());
                            boolean shouldUseInputData = isInputWidget && "input".equalsIgnoreCase(actionType);

                            if (shouldUseInputData && dataIndex < inputDataArray.length) {
                                currentInputData = inputDataArray[dataIndex].trim();
                            }

                            // 执行动作时传入当前元素对应的输入数据（仅input消耗数据）
                            SeleniumUtil.performAction(driver, webElement, actionType, currentInputData);
                            log.info("[动作执行完成] {} - 执行{}操作，输入数据：{}", element.getElementName(), actionType, currentInputData);
                            verifyDetail.put("元素动作-" + element.getElementName(), "成功：执行" + actionType + "操作，输入：" + currentInputData);

                            if (shouldUseInputData) {
                                dataIndex++;
                            }
                        } catch (Exception e) {
                            String errMsg = "元素动作执行失败：" + element.getElementName() + "，动作类型=" + actionType;
                            log.error(errMsg, e);
                            isAllPass = false;
                            verifyDetail.put("元素动作-" + element.getElementName(), "失败：" + errMsg);
                            throw new RuntimeException(errMsg, e);
                        }

                        // 仅搜索类输入框才自动触发回车和窗口切换，普通表单输入不再触发
                        if ("input".equals(element.getWidgetType().toLowerCase())
                                && "SEARCH".equalsIgnoreCase(testCase.getAssertType())) {
                            try {
                                SeleniumUtil.performAction(driver, webElement, "enter", null);
                                log.info("[触发回车] {} - 输入后按回车键完成搜索", element.getElementName());
                                verifyDetail.put("回车触发-" + element.getElementName(), "成功");
                            } catch (Exception e) {
                                String errMsg = "回车触发失败：" + element.getElementName();
                                log.error(errMsg, e);
                                isAllPass = false;
                                verifyDetail.put("回车触发-" + element.getElementName(), "失败：" + errMsg);
                                throw new RuntimeException(errMsg, e);
                            }

                            try {
                                Thread.sleep(1000);
                                var windowHandles = driver.getWindowHandles();
                                log.info("[窗口切换] 当前总窗口数：{}", windowHandles.size());

                                if (windowHandles.size() > 1) {
                                    String newWindowHandle = (String) windowHandles.toArray()[windowHandles.size() - 1];
                                    driver.switchTo().window(newWindowHandle);
                                    log.info("[窗口切换] 已切换到新窗口，当前URL：{}", driver.getCurrentUrl());

                                    new WebDriverWait(driver, Duration.ofSeconds(15)).until(webDriver ->
                                            ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
                                    log.info("[页面加载] 新窗口页面加载完成，标题：{}", driver.getTitle());
                                    verifyDetail.put("窗口切换", "成功：切换到搜索结果页");
                                } else {
                                    log.warn("[窗口切换] 未检测到新窗口，跳过切换");
                                    verifyDetail.put("窗口切换", "警告：未检测到新窗口，仍在原页面");
                                }
                            } catch (Exception e) {
                                String errMsg = "窗口切换失败：" + e.getMessage();
                                log.error(errMsg, e);
                                isAllPass = false;
                                verifyDetail.put("窗口切换", "失败：" + errMsg);
                            }
                        }
                        Thread.sleep(400);
                        i++;
                    }
                } else {
                    log.warn("[元素关联] 用例未绑定任何元素，仅加载页面");
                }
            }

            if ("SEARCH".equalsIgnoreCase(testCase.getAssertType())) {
                log.info("[通用搜索断言开始] caseId={}, 验证关键词：{}，结果区域定位：{}={}",
                        caseId, testCase.getInputData().trim(),
                        testCase.getAssertLocatorType(), testCase.getAssertLocatorValue());
                try {
                    AssertUtil.assertSearchSuccess(
                            driver,
                            testCase.getInputData().trim(),
                            testCase.getAssertLocatorType(),
                            testCase.getAssertLocatorValue()
                    );
                    verifyDetail.put("通用搜索断言", "通过：URL+结果区域均包含关键词「" + testCase.getInputData().trim() + "」");
                    log.info("[通用搜索断言成功] caseId={} 验证通过", caseId);
                } catch (Exception e) {
                    isAllPass = false;
                    verifyDetail.put("通用搜索断言", "失败：" + e.getMessage());
                    log.error("[通用搜索断言失败] caseId={}", caseId, e);
                }
            } else if (testCase.getAssertType() != null && !testCase.getAssertType().isEmpty()
                    && testCase.getAssertLocatorValue() != null && !testCase.getAssertLocatorValue().isEmpty()) {
                log.info("[动态断言开始] caseId={}, 断言类型={}, 定位类型={}, 预期值={}",
                        caseId, testCase.getAssertType(), testCase.getAssertLocatorType(), testCase.getAssertExpectedValue().trim());
                try {
                    AssertUtil.assertByConfig(
                            driver,
                            testCase.getAssertType(),
                            testCase.getAssertLocatorType(),
                            testCase.getAssertLocatorValue(),
                            testCase.getAssertExpectedValue()
                    );
                    verifyDetail.put("动态UI断言", "通过：" + testCase.getAssertExpectedValue().trim());
                    log.info("[动态断言成功] caseId={} 验证通过", caseId);
                } catch (Exception e) {
                    isAllPass = false;
                    verifyDetail.put("动态UI断言", "失败：" + e.getMessage());
                    log.error("[动态断言失败] caseId={}", caseId, e);
                }
            } else {
                log.warn("[动态断言跳过] caseId={} 未配置断言规则", caseId);
            }

            String screenshotPath = SeleniumUtil.takeScreenshot(driver, testCase.getName());
            result.setScreenshotPath(screenshotPath);
            result.setStatus(isAllPass ? "PASS" : "FAILED");

            StringBuilder msg = new StringBuilder();
            msg.append(isAllPass ? "执行成功" : "执行失败");
            msg.append("，截图路径：").append(screenshotPath);
            if (!verifyDetail.isEmpty()) {
                msg.append(" | 验证详情：").append(verifyDetail);
            }
            result.setMessage(msg.toString());
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String verifyDetailJson = objectMapper.writeValueAsString(verifyDetail);
                result.setVerifyDetail(verifyDetailJson);
            } catch (JsonProcessingException e) {
                log.error("验证详情转JSON失败", e);
                result.setVerifyDetail("{}");
            }
            log.info("【用例执行成功】caseId={}, 耗时={}ms, 状态={}",
                    caseId, System.currentTimeMillis() - start, result.getStatus());

        } catch (IllegalArgumentException ex) {
            result.setStatus("FAILED");
            String errMsg = "参数错误: " + (ex.getMessage() != null ? ex.getMessage() : "无参数信息");
            result.setMessage(errMsg);
            result.setScreenshotPath(takeFailScreenshot(driver, testCase));
            log.error("【用例执行失败-参数错误】caseId={}, 原因={}", caseId, errMsg, ex);
        } catch (IOException ioEx) {
            result.setStatus("FAILED");
            String errMsg = "截图失败: " + (ioEx.getMessage() != null ? ioEx.getMessage() : "IO异常");
            result.setMessage(errMsg);
            result.setScreenshotPath(null);
            log.error("【用例执行失败-截图错误】caseId={}, 原因={}", caseId, errMsg, ioEx);
        } catch (Exception ex) {
            result.setStatus("FAILED");
            String errMsg = "执行异常: " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getName());
            result.setMessage(errMsg);
            result.setScreenshotPath(takeFailScreenshot(driver, testCase));
            log.error("【用例执行失败-运行异常】caseId={}, 原因={}", caseId, errMsg, ex);
        } finally {
            try {
                result.setDurationMs(System.currentTimeMillis() - start);
                log.info("[执行收尾] caseId={}, 总耗时={}ms, 状态={}",
                        caseId, result.getDurationMs(), result.getStatus());
                testResultMapper.insertTestResult(result);
                log.info("[结果保存] caseId={} 执行结果已存入数据库", caseId);
            } catch (Exception e) {
                log.error("【finally块异常】caseId={} 收尾操作失败", caseId, e);
                result.setMessage(result.getMessage() + " | 收尾异常：" + e.getMessage());
            }
        }
        return result;
    }

    public Map<String, Object> batchRunTestCases(List<Long> caseIds, String browserType, Long taskId) {
        int total = caseIds.size();
        int success = 0;
        int fail = 0;
        int retrySuccess = 0;
        int retryCount = 1;
        WebDriver driver = null;

        try {
            log.info("【批量执行开始】总用例数={}，执行浏览器={}，失败重试次数={}", total, browserType, retryCount);
            driver = SeleniumUtil.getWebDriver(browserType);
            log.info("[批量驱动初始化成功] 浏览器版本：{}", ((HasCapabilities) driver).getCapabilities().getBrowserVersion());

            for (Long caseId : caseIds) {
                boolean isExecuteSuccess = false;
                boolean isRetry = false;
                log.info("【开始执行用例】caseId={}，剩余未执行={}", caseId, total - (success + fail + retrySuccess));

                for (int i = 0; i <= retryCount; i++) {
                    try {
                        // 修复：根据每个用例的site_code获取对应配置
                        TestCase testCase = testCaseMapper.findById(caseId);
                        if (testCase == null) {
                            throw new IllegalArgumentException("用例不存在，ID=" + caseId);
                        }
                        String siteCode = testCase.getSiteCode() != null ? testCase.getSiteCode() : "BAIDU";
                        SiteTestConfigDO siteConfig = siteTestConfigService.getBySiteCode(siteCode);

                        TestResult testResult = this.runTestCase(
                                caseId, siteConfig, false, "", "", "", taskId, driver
                        );
                        if ("PASS".equals(testResult.getStatus())) {
                            isExecuteSuccess = true;
                            log.info("【用例执行成功】caseId={}（第{}次执行）", caseId, i + 1);
                            break;
                        } else {
                            throw new RuntimeException("用例执行结果为失败：" + testResult.getMessage());
                        }
                    } catch (Exception e) {
                        log.error("【用例执行失败】caseId={}（第{}次执行），原因：{}", caseId, i + 1, e.getMessage());
                        if (i == retryCount) {
                            isExecuteSuccess = false;
                            log.error("【用例重试失败】caseId={}，已重试{}次，放弃执行", caseId, retryCount);
                        } else {
                            isRetry = true;
                            log.info("【开始重试用例】caseId={}，第{}次重试", caseId, i + 2);
                            Thread.sleep(1000);
                        }
                    }
                }

                if (isExecuteSuccess) {
                    if (isRetry) {
                        retrySuccess++;
                    } else {
                        success++;
                    }
                } else {
                    fail++;
                }
                Thread.sleep(1500);
            }

            log.info("【批量执行中间统计】总={}，首次成功={}，重试成功={}，失败={}", total, success, retrySuccess, fail);
        } catch (Exception e) {
            log.error("【批量执行整体异常】执行中断，原因：{}", e.getMessage(), e);
        } finally {
            if (driver != null) {
                try {
                    Thread.sleep(3000);
                    SeleniumUtil.quitDriver(driver);
                    log.info("[批量驱动关闭成功] 所有用例执行完成，关闭浏览器");
                } catch (Exception ignored) {
                }
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total", total);
        resultMap.put("success", success);
        resultMap.put("fail", fail);
        resultMap.put("retrySuccess", retrySuccess);
        log.info("【批量执行完成】最终统计：总={}，首次成功={}，重试成功={}，失败={}", total, success, retrySuccess, fail);
        return resultMap;
    }

    private Map<String, String> validateWithLoginUI(WebDriver driver, SiteTestConfigDO siteConfig,
                                                   String username, String password, String expectedNickname) {
        Map<String, String> verifyDetail = new HashMap<>();
        try {
            driver.get(siteConfig.getLoginPageUrl());
            log.info("[登录流程] 打开登录页：{}", siteConfig.getLoginPageUrl());
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.urlToBe(siteConfig.getLoginPageUrl()));

            WebElement usernameInput = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(By.id(siteConfig.getUsernameElementId())));
            usernameInput.clear();
            usernameInput.sendKeys(username);
            log.info("[登录流程] 输入用户名：{}", username);
            Thread.sleep(500);

            WebElement pwdInput = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(By.id(siteConfig.getPasswordElementId())));
            pwdInput.clear();
            pwdInput.sendKeys(password);
            log.info("[登录流程] 输入密码：******");
            Thread.sleep(500);

            WebElement loginBtn = new WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(ExpectedConditions.elementToBeClickable(By.id(siteConfig.getLoginBtnElementId())));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginBtn);
            log.info("[登录流程] 点击登录按钮");

            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .until(ExpectedConditions.urlContains(siteConfig.getHomeUrl()));
            verifyDetail.put("登录操作", "成功：跳转到目标页 " + siteConfig.getHomeUrl());

            if (expectedNickname != null && !expectedNickname.isEmpty()
                    && siteConfig.getNicknameElementId() != null && !siteConfig.getNicknameElementId().isEmpty()) {
                try {
                    WebElement nicknameElement = new WebDriverWait(driver, Duration.ofSeconds(15))
                            .until(ExpectedConditions.visibilityOfElementLocated(By.id(siteConfig.getNicknameElementId())));
                    String actualNickname = nicknameElement.getText().trim();
                    if (actualNickname.equals(expectedNickname)) {
                        verifyDetail.put("登录后验证", "通过：预期[" + expectedNickname + "]，实际[" + actualNickname + "]");
                    } else {
                        verifyDetail.put("登录后验证", "失败：预期[" + expectedNickname + "]，实际[" + actualNickname + "]");
                    }
                } catch (Exception e) {
                    verifyDetail.put("登录后验证", "失败：元素定位异常 - " + e.getMessage());
                }
            }

        } catch (Exception e) {
            verifyDetail.put("登录操作", "失败：" + e.getMessage());
            log.error("[登录流程异常]", e);
        }
        return verifyDetail;
    }

    public List<TestCase> listAll() {
        return testCaseMapper.findAll();
    }

    public void updateTestCase(Long id, TestCase request) {
        TestCase originalCase = testCaseMapper.findById(id);
        if (originalCase == null) {
            throw new IllegalArgumentException("用例不存在");
        }

        originalCase.setName(request.getName() != null ? request.getName() : originalCase.getName());
        originalCase.setDescription(request.getDescription() != null ? request.getDescription() : originalCase.getDescription());
        originalCase.setUrl(request.getUrl() != null ? request.getUrl() : originalCase.getUrl());
        originalCase.setCreator(request.getCreator() != null ? request.getCreator() : originalCase.getCreator());
        originalCase.setLocatorType(request.getLocatorType() != null ? request.getLocatorType() : originalCase.getLocatorType());
        originalCase.setLocatorValue(request.getLocatorValue() != null ? request.getLocatorValue() : originalCase.getLocatorValue());
        originalCase.setActionType(request.getActionType() != null ? request.getActionType() : originalCase.getActionType());
        originalCase.setInputData(request.getInputData() != null ? request.getInputData() : originalCase.getInputData());
        originalCase.setExpectedResult(request.getExpectedResult() != null ? request.getExpectedResult() : originalCase.getExpectedResult());
        originalCase.setElementIds(request.getElementIds() != null ? request.getElementIds() : originalCase.getElementIds());
        originalCase.setNeedLogin(request.getNeedLogin() != null ? request.getNeedLogin() : originalCase.getNeedLogin());
        originalCase.setSiteCode(request.getSiteCode() != null ? request.getSiteCode() : originalCase.getSiteCode());
        // ========== 新增：更新测试账号密码 ==========
        originalCase.setTestAccount(request.getTestAccount() != null ? request.getTestAccount() : originalCase.getTestAccount());
        originalCase.setTestPassword(request.getTestPassword() != null ? request.getTestPassword() : originalCase.getTestPassword());
        // ========== 原有：断言字段 ==========
        originalCase.setAssertType(request.getAssertType() != null ? request.getAssertType() : originalCase.getAssertType());
        originalCase.setAssertLocatorType(request.getAssertLocatorType() != null ? request.getAssertLocatorType() : originalCase.getAssertLocatorType());
        originalCase.setAssertLocatorValue(request.getAssertLocatorValue() != null ? request.getAssertLocatorValue() : originalCase.getAssertLocatorValue());
        originalCase.setAssertExpectedValue(request.getAssertExpectedValue() != null ? request.getAssertExpectedValue() : originalCase.getAssertExpectedValue());

        originalCase.setBrowser(request.getBrowser() != null ? request.getBrowser() : originalCase.getBrowser());

        testCaseMapper.updateTestCase(originalCase);
    }

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
            return SeleniumUtil.takeScreenshot(driver, testCase.getName() + "_fail");
        } catch (Exception ignore) {
            return null;
        }
    }
}