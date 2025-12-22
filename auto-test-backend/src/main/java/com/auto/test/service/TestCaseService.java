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
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
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

/**
 * 用例业务服务：新增、查询、更新、删除、执行（扩展UI+API组合验证+动态断言）。
 */
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

    /**
     * 新增测试用例（包含断言配置字段）
     */
    @Transactional
    public TestCase addTestCase(TestCaseRequest request) {
        TestCase testCase = new TestCase();
        // 原有字段赋值
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
        // 新增用例时
        testCase.setNeedLogin(request.getNeedLogin() != null ? request.getNeedLogin() : false);
        testCase.setSiteCode(request.getSiteCode());
        // 新增：断言配置字段赋值
        testCase.setAssertType(request.getAssertType() != null ? request.getAssertType() : "TEXT");
        testCase.setAssertLocatorType(request.getAssertLocatorType() != null ? request.getAssertLocatorType() : "id");
        testCase.setAssertLocatorValue(request.getAssertLocatorValue() != null ? request.getAssertLocatorValue() : "");
        testCase.setAssertExpectedValue(request.getAssertExpectedValue() != null ? request.getAssertExpectedValue() : "");
        
        testCaseMapper.insertTestCase(testCase);
        return testCase;
    }

    // 新增：根据ID查询用例的方法
    public TestCase findById(Long id) {
        TestCase testCase = testCaseMapper.findById(id);
        if (testCase == null) {
            throw new IllegalArgumentException("用例不存在，ID=" + id);
        }
        return testCase;
    }

    /**
     * 兼容原有接口：仅执行UI操作（无API验证）
     */
    @Transactional
    public TestResult runTestCase(Long caseId) {
        SiteTestConfigDO defaultConfig = siteTestConfigService.getBySiteCode("BAIDU");
        return runTestCase(caseId, defaultConfig, false, "", "", "");
    }

    /**
     * 扩展版：执行用例（支持UI+API组合验证 + 动态断言）
     * 核心优化：先登录→再访问目标页→执行操作→断言
     */
    @Transactional
    public TestResult runTestCase(Long caseId, SiteTestConfigDO siteConfig,
                                  boolean needLogin, // 新增：是否需要登录
                                  String username, String password, String expectedNickname) {
        TestCase testCase = testCaseMapper.findById(caseId);
        if (testCase == null) {
            throw new IllegalArgumentException("用例不存在，ID=" + caseId);
        }

        WebDriver driver = null;
        TestResult result = new TestResult();
        result.setCaseId(caseId);
        result.setRunTime(LocalDateTime.now());
        long start = System.currentTimeMillis();
        Map<String, String> verifyDetail = new HashMap<>();
        boolean isAllPass = true;

        try {
            log.info("【用例执行开始】caseId={}, 用例名称={}, 测试URL={}, 目标网站={}, 是否登录={}",
                     caseId, testCase.getName(), testCase.getUrl(),
                     siteConfig != null ? siteConfig.getSiteName() : "默认", needLogin);

            // 初始化驱动
            driver = SeleniumUtil.createDriver(null, "edge");
            log.info("[驱动初始化成功] 浏览器版本：{}",
                     ((HasCapabilities) driver).getCapabilities().getBrowserVersion());

            // ========== 核心优化：第一步：先执行登录流程（如果需要） ==========
            if (needLogin && siteConfig != null && username != null && !username.isEmpty()) {
                log.info("[登录流程开始] 目标网站：{}，账号：{}", siteConfig.getSiteName(), username);
                Map<String, String> loginVerifyDetail = validateWithLoginUI(driver, siteConfig, username, password, expectedNickname);
                verifyDetail.putAll(loginVerifyDetail);
                // 检查登录结果
                for (String key : loginVerifyDetail.keySet()) {
                    if (loginVerifyDetail.get(key).contains("失败")) {
                        isAllPass = false;
                        // 登录失败直接终止，无需后续操作
                        throw new RuntimeException("登录失败：" + loginVerifyDetail.get(key));
                    }
                }
                log.info("[登录流程完成] caseId={} 登录成功", caseId);
            }

            // ========== 第二步：访问目标页（登录成功后） ==========
            if (testCase.getUrl() != null && !testCase.getUrl().isEmpty()) {
                driver.get(testCase.getUrl());
                // 等待页面加载完成（通用等待，适配所有网站）
                new WebDriverWait(driver, Duration.ofSeconds(20)).until(webDriver ->
                    ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
                log.info("[页面加载完成] URL={}, 标题={}", testCase.getUrl(), driver.getTitle());

                // 关闭隐私弹窗（适配百度/Swag Labs，只处理百度的弹窗）
                if (testCase.getUrl().contains("baidu.com")) {
                    closePrivacyPopup(driver);
                }

                // 执行关联元素操作（原逻辑保留，适配搜索类用例）
                String elementIds = testCase.getElementIds();
                if (elementIds != null && !elementIds.trim().isEmpty()) {
                    String[] elementIdArray = elementIds.split(",");
                    log.info("[元素解析] 共{}个元素待处理：{}", elementIdArray.length, Arrays.toString(elementIdArray));

                    for (String elementIdStr : elementIdArray) {
                        Long elementId = Long.parseLong(elementIdStr.trim());
                        Element element = elementMapper.findById(elementId);
                        if (element == null) {
                            throw new IllegalArgumentException("元素不存在，ID=" + elementId);
                        }

                        log.info("[元素处理] ID={}, 名称={}, 定位类型={}, 定位值={}",
                                 elementId, element.getElementName(), element.getLocatorType(), element.getLocatorValue());

                        By by = switch (element.getLocatorType().toLowerCase()) {
                            case "id" -> By.id(element.getLocatorValue());
                            case "xpath" -> By.xpath(element.getLocatorValue());
                            case "name" -> By.name(element.getLocatorValue());
                            default -> throw new IllegalArgumentException("不支持的定位方式：" + element.getLocatorType());
                        };

                        WebElement webElement = new WebDriverWait(driver, Duration.ofSeconds(15))
                            .until(ExpectedConditions.presenceOfElementLocated(by));

                        // JS操作确保元素可交互
                        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", webElement);
                        Thread.sleep(1000);

                        // 执行动作
                        String actionType = switch (element.getWidgetType().toLowerCase()) {
                            case "input" -> "input";
                            case "button" -> "click";
                            case "select" -> "click";
                            default -> testCase.getActionType();
                        };
                        SeleniumUtil.performAction(driver, webElement, actionType, testCase.getInputData());
                        log.info("[动作执行完成] {} - 输入内容：{}", element.getElementName(), testCase.getInputData());
                        Thread.sleep(1000);
                    }
                } else {
                    log.warn("[元素关联] 用例未绑定任何元素，仅加载页面");
                }
            }

            // ========== 第三步：动态UI断言（核心验证） ==========
            if (testCase.getAssertType() != null && !testCase.getAssertType().isEmpty()
                    && testCase.getAssertLocatorValue() != null && !testCase.getAssertLocatorValue().isEmpty()) {
                log.info("[动态断言开始] caseId={}, 断言类型={}, 定位类型={}, 预期值={}",
                         caseId, testCase.getAssertType(), testCase.getAssertLocatorType(), testCase.getAssertExpectedValue());
                try {
                    // 调用AssertUtil执行配置化断言
                    AssertUtil.assertByConfig(
                        driver,
                        testCase.getAssertType(),
                        testCase.getAssertLocatorType(),
                        testCase.getAssertLocatorValue(),
                        testCase.getAssertExpectedValue()
                    );
                    verifyDetail.put("动态UI断言", "通过：" + testCase.getAssertExpectedValue());
                    log.info("[动态断言成功] caseId={} 验证通过", caseId);
                } catch (Exception e) {
                    isAllPass = false;
                    verifyDetail.put("动态UI断言", "失败：" + e.getMessage());
                    log.error("[动态断言失败] caseId={}", caseId, e);
                }
            } else {
                log.warn("[动态断言跳过] caseId={} 未配置断言规则", caseId);
            }

            // ========== 结果处理 ==========
            String screenshotPath = SeleniumUtil.takeScreenshot(driver, testCase.getName());
            result.setScreenshotPath(screenshotPath);
            result.setStatus(isAllPass ? "PASS" : "FAILED");

            // 拼接验证详情
            StringBuilder msg = new StringBuilder();
            msg.append(isAllPass ? "执行成功" : "执行失败");
            msg.append("，截图路径：").append(screenshotPath);
            if (!verifyDetail.isEmpty()) {
                msg.append(" | 验证详情：").append(verifyDetail);
            }
            result.setMessage(msg.toString());
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                    // 将Map序列化为标准JSON字符串
                    String verifyDetailJson = objectMapper.writeValueAsString(verifyDetail);
                    result.setVerifyDetail(verifyDetailJson);
            } catch (JsonProcessingException e) {
                    log.error("验证详情转JSON失败", e);
                    result.setVerifyDetail("{}"); // 失败时给空JSON
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

                if (driver != null) {
                    try {
                        Thread.sleep(3000);
                        driver.quit();
                        log.info("[驱动关闭] caseId={} 成功", caseId);
                    } catch (Exception ignored) {
                        log.warn("[驱动关闭] caseId={} 失败", caseId, ignored);
                    }
                }

                testResultMapper.insertTestResult(result);
                log.info("[结果保存] caseId={} 执行结果已存入数据库", caseId);
            } catch (Exception e) {
                log.error("【finally块异常】caseId={} 收尾操作失败", caseId, e);
                result.setMessage(result.getMessage() + " | 收尾异常：" + e.getMessage());
            }
        }
        return result;
    }

    /**
     * 纯UI登录验证（适配Swag Labs/百度等网站，去掉无用的API验证）
     */
    private Map<String, String> validateWithLoginUI(WebDriver driver, SiteTestConfigDO siteConfig,
                                                String username, String password, String expectedNickname) {
        Map<String, String> verifyDetail = new HashMap<>();
        try {
            // 1. 打开登录页（强制等待登录页加载）
            driver.get(siteConfig.getLoginPageUrl());
            log.info("[登录流程] 打开登录页：{}", siteConfig.getLoginPageUrl());
            new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlToBe(siteConfig.getLoginPageUrl()));

            // 2. 等待并输入用户名（增强显式等待，确保元素可交互）
            WebElement usernameInput = new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.elementToBeClickable(By.id(siteConfig.getUsernameElementId())));
            usernameInput.clear();
            usernameInput.sendKeys(username);
            log.info("[登录流程] 输入用户名：{}", username);
            Thread.sleep(500); // 短等待，避免输入过快

            // 3. 等待并输入密码
            WebElement pwdInput = new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.elementToBeClickable(By.id(siteConfig.getPasswordElementId())));
            pwdInput.clear();
            pwdInput.sendKeys(password);
            log.info("[登录流程] 输入密码：******");
            Thread.sleep(500);

            // 4. 等待并点击登录按钮（JS点击+显式等待）
            WebElement loginBtn = new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.elementToBeClickable(By.id(siteConfig.getLoginBtnElementId())));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", loginBtn);
            log.info("[登录流程] 点击登录按钮");

            // 5. 等待登录跳转（验证是否跳转到首页）
            new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.urlContains(siteConfig.getHomeUrl()));
            verifyDetail.put("登录操作", "成功：跳转到目标页 " + siteConfig.getHomeUrl());

            // 6. 验证断言元素（预期昵称/商品标题）
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

    /**
     * 关闭百度隐私政策弹窗（仅适配百度，其他网站跳过）
     */
    private void closePrivacyPopup(WebDriver driver) {
        try {
            WebElement privacyBtn = new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(text(),'同意') or contains(text(),'Accept')]")
                ));
            if (privacyBtn.isDisplayed()) {
                privacyBtn.click();
                log.info("[百度隐私弹窗] 关闭成功");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.warn("[百度隐私弹窗] 未检测到/关闭失败，跳过", e);
        }
    }

    /**
     * 查询所有测试用例
     */
    public List<TestCase> listAll() {
        return testCaseMapper.findAll();
    }

    /**
     * 更新指定测试用例（包含断言配置字段）
     */
    public void updateTestCase(Long id, TestCase request) {
        TestCase originalCase = testCaseMapper.findById(id);
        if (originalCase == null) {
            throw new IllegalArgumentException("用例不存在");
        }

        // 原有字段更新
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
        // 更新用例时
        originalCase.setNeedLogin(request.getNeedLogin() != null ? request.getNeedLogin() : originalCase.getNeedLogin());
        originalCase.setSiteCode(request.getSiteCode() != null ? request.getSiteCode() : originalCase.getSiteCode());
        // 新增：断言配置字段更新
        originalCase.setAssertType(request.getAssertType() != null ? request.getAssertType() : originalCase.getAssertType());
        originalCase.setAssertLocatorType(request.getAssertLocatorType() != null ? request.getAssertLocatorType() : originalCase.getAssertLocatorType());
        originalCase.setAssertLocatorValue(request.getAssertLocatorValue() != null ? request.getAssertLocatorValue() : originalCase.getAssertLocatorValue());
        originalCase.setAssertExpectedValue(request.getAssertExpectedValue() != null ? request.getAssertExpectedValue() : originalCase.getAssertExpectedValue());

        testCaseMapper.updateTestCase(originalCase);
    }

    /**
     * 删除指定测试用例
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

    /**
     * 失败时截图
     */
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