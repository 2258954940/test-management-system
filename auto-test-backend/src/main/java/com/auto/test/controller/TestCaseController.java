package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.dto.RunCaseRequest;
import com.auto.test.dto.TestCaseRequest;
import com.auto.test.entity.SiteTestConfigDO;
import com.auto.test.entity.TestCase;
import com.auto.test.entity.TestResult;
import com.auto.test.entity.User;
import com.auto.test.service.SiteTestConfigService;
import com.auto.test.service.TestCaseService;
import com.auto.test.service.SysLogService;
import com.auto.test.service.UserService;
import com.auto.test.utils.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用例接口层，对接前端 Vue，请求体为 JSON。
 */
@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "http://localhost:8080") // 允许前端跨域请求
public class TestCaseController {

    private final TestCaseService testCaseService;
    private final SiteTestConfigService siteTestConfigService;
    private final SysLogService sysLogService;
    private final UserService userService;

    public TestCaseController(TestCaseService testCaseService,
                              SiteTestConfigService siteTestConfigService,
                              SysLogService sysLogService,
                              UserService userService) {
        this.testCaseService = testCaseService;
        this.siteTestConfigService = siteTestConfigService;
        this.sysLogService = sysLogService;
        this.userService = userService;
    }

    // ========== 新增：根据ID查询单个用例详情（核心修复） ==========
    @GetMapping("/case/{id}")
    public ApiResponse<TestCase> getCaseById(@PathVariable Long id) {
        try {
            TestCase testCase = testCaseService.findById(id);
            if (testCase == null) {
                return ApiResponse.error("用例不存在", null);
            }
            // 确保返回的用例包含testAccount/testPassword字段
            return ApiResponse.success("查询成功", testCase);
        } catch (Exception ex) {
            return ApiResponse.error("查询用例详情失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 新增测试用例
     */
    @PostMapping("/addCase")
    public ApiResponse<TestCase> addCase(@Valid @RequestBody TestCaseRequest request,
                                         @RequestHeader("Authorization") String token) {
        try {
            String currentUser = getRealUsernameFromTokenAndSetContext(token);
            TestCase saved = testCaseService.addTestCase(request);
            String caseName = saved.getName() != null ? saved.getName() : "未命名用例-" + saved.getId();
            sysLogService.saveLog(
                    currentUser,
                    "新增测试用例",
                    "新增用例-" + caseName + "（ID：" + saved.getId() + "）"
            );
            return ApiResponse.success("新增用例成功", saved);
        } catch (Exception ex) {
            return ApiResponse.error("新增用例失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 执行用例
     */
    @PostMapping({"/runCase", "/run/{caseId}"})
    public ApiResponse<TestResult> runCase(@PathVariable(name = "caseId", required = false) Long pathCaseId,
                                           @Valid @RequestBody RunCaseRequest request,
                                           @RequestParam(name = "browserType", required = false, defaultValue = "edge") String browserType,
                                           @RequestHeader("Authorization") String token) {
        try {
            String currentUser = getRealUsernameFromTokenAndSetContext(token);
            Long targetCaseId = request.getCaseId() != null ? request.getCaseId() : pathCaseId;
            if (targetCaseId == null) {
                return ApiResponse.error("用例ID不能为空", null);
            }
            TestCase testCase = testCaseService.findById(targetCaseId);
            if (testCase == null) {
                return ApiResponse.error("用例不存在", null);
            }

            String finalUsername = null;
            String finalPassword = null;
            if (testCase.getNeedLogin()) {
                if (StringUtils.hasText(testCase.getTestAccount())) {
                    finalUsername = testCase.getTestAccount();
                }
                if (StringUtils.hasText(testCase.getTestPassword())) {
                    finalPassword = testCase.getTestPassword();
                }
                if (StringUtils.hasText(request.getUsername())) {
                    finalUsername = request.getUsername();
                }
                if (StringUtils.hasText(request.getPassword())) {
                    finalPassword = request.getPassword();
                }
                if (!StringUtils.hasText(finalUsername)) {
                    return ApiResponse.error("需要登录时，测试账号不能为空", null);
                }
                if (!StringUtils.hasText(finalPassword)) {
                    return ApiResponse.error("需要登录时，测试密码不能为空", null);
                }
                if (testCase.getSiteCode() == null || testCase.getSiteCode().trim().isEmpty()) {
                    return ApiResponse.error("用例配置了需要登录，但未关联测试网站", null);
                }
            }

            SiteTestConfigDO siteConfig = null;
            if (testCase.getNeedLogin()) {
                siteConfig = siteTestConfigService.getBySiteCode(testCase.getSiteCode());
                if (siteConfig == null) {
                    return ApiResponse.error("未找到[" + testCase.getSiteCode() + "]的网站配置", null);
                }
            }

            TestResult result = testCaseService.runTestCase(
                    request.getCaseId(),
                    siteConfig,
                    testCase.getNeedLogin(),
                    finalUsername,
                    finalPassword,
                    testCase.getAssertExpectedValue(),
                    browserType,
                    null
            );

            String caseName = testCase.getName() != null ? testCase.getName() : "未命名用例-" + testCase.getId();
            sysLogService.saveLog(
                    currentUser,
                    "执行测试用例",
                    "执行用例-" + caseName + "（ID：" + testCase.getId() + "），浏览器：" + browserType + "，结果：" + result.getStatus()
            );

            String msg = "执行完毕，状态: " + result.getStatus();
            return ApiResponse.success(msg, result);
        } catch (Exception ex) {
            String errMsg = ex.getMessage() == null ? "用例执行异常" : ex.getMessage();
            return ApiResponse.error("执行失败: " + errMsg, null);
        }
    }

    /**
     * 查询所有测试用例（修复：确保返回testAccount/testPassword字段）
     */
    @GetMapping("")
    public ApiResponse<List<TestCase>> listCases() {
        try {
            List<TestCase> caseList = testCaseService.listAll();
            // 列表查询也返回完整字段（包含账号密码）
            return ApiResponse.success("查询用例列表成功", caseList);
        } catch (Exception ex) {
            return ApiResponse.error("查询用例列表失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 更新指定测试用例
     */
    @PutMapping("/case/{id}")
    public ApiResponse<String> updateCase(@PathVariable Long id,
                                          @RequestBody TestCase request,
                                          @RequestHeader("Authorization") String token) {
        try {
            String currentUser = getRealUsernameFromTokenAndSetContext(token);
            TestCase oldCase = testCaseService.findById(id);
            if (oldCase == null) {
                return ApiResponse.error("用例不存在", null);
            }
            testCaseService.updateTestCase(id, request);
            String oldCaseName = oldCase.getName() != null ? oldCase.getName() : "未命名用例-" + oldCase.getId();
            sysLogService.saveLog(
                    currentUser,
                    "编辑测试用例",
                    "编辑用例-" + oldCaseName + "（ID：" + id + "）"
            );
            return ApiResponse.success("更新用例成功", "ok");
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error("更新失败: " + ex.getMessage(), null);
        } catch (Exception ex) {
            return ApiResponse.error("更新失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 删除指定测试用例
     */
    @DeleteMapping("/case/{id}")
    public ApiResponse<String> deleteCase(@PathVariable Long id,
                                          @RequestHeader("Authorization") String token) {
        try {
            String currentUser = getRealUsernameFromTokenAndSetContext(token);
            TestCase oldCase = testCaseService.findById(id);
            if (oldCase == null) {
                return ApiResponse.error("用例不存在", null);
            }
            testCaseService.deleteTestCase(id);
            String oldCaseName = oldCase.getName() != null ? oldCase.getName() : "未命名用例-" + oldCase.getId();
            sysLogService.saveLog(
                    currentUser,
                    "删除测试用例",
                    "删除用例-" + oldCaseName + "（ID：" + id + "）"
            );
            return ApiResponse.success("删除用例成功", "ok");
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error("删除失败: " + ex.getMessage(), null);
        } catch (Exception ex) {
            return ApiResponse.error("删除失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 查询所有网站配置
     */
    @GetMapping("/siteConfigs")
    public ApiResponse<List<SiteTestConfigDO>> listSiteConfigs() {
        try {
            List<SiteTestConfigDO> configs = siteTestConfigService.listAll();
            return ApiResponse.success("查询网站配置成功", configs);
        } catch (Exception ex) {
            return ApiResponse.error("查询网站配置失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 从Token解析真实用户名并设置到SecurityContext
     */
    private String getRealUsernameFromTokenAndSetContext(String token) {
        try {
            token = java.net.URLDecoder.decode(token, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Token解码失败，请重新登录");
        }

        if (!StringUtils.hasText(token) || !token.startsWith("auto-test-token-")) {
            throw new RuntimeException("Token无效，请重新登录");
        }

        String[] tokenParts = token.split("-");
        if (tokenParts.length < 5) {
            throw new RuntimeException("Token格式错误，请重新登录");
        }

        String realUsername = tokenParts[3];
        System.out.println("TestCaseController解析出的用户名：" + realUsername);

        User user = userService.getByUsername(realUsername);
        if (user == null) {
            throw new RuntimeException("操作人不存在，请重新登录：" + realUsername);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                realUsername,
                null,
                java.util.Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return realUsername;
    }
}