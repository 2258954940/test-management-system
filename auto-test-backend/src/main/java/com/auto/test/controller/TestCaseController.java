package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.dto.RunCaseRequest;
import com.auto.test.dto.TestCaseRequest;
import com.auto.test.entity.SiteTestConfigDO;
import com.auto.test.entity.TestCase;
import com.auto.test.entity.TestResult;
import com.auto.test.service.SiteTestConfigService;
import com.auto.test.service.TestCaseService;
import jakarta.validation.Valid;
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

    // 构造器注入（Spring推荐方式）
    public TestCaseController(TestCaseService testCaseService, SiteTestConfigService siteTestConfigService) {
        this.testCaseService = testCaseService;
        this.siteTestConfigService = siteTestConfigService;
    }

    /**
     * 新增测试用例
     */
    @PostMapping("/addCase")
    public ApiResponse<TestCase> addCase(@Valid @RequestBody TestCaseRequest request) {
        try {
            TestCase saved = testCaseService.addTestCase(request);
            return ApiResponse.success("新增用例成功", saved);
        } catch (Exception ex) {
            return ApiResponse.error("新增用例失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 扩展版：执行用例（支持纯UI/登录+API两种模式）
     */
  @PostMapping("/runCase")
public ApiResponse<TestResult> runCase(@Valid @RequestBody RunCaseRequest request) {
    try {
        // 1. 先查询用例，获取登录相关配置
        TestCase testCase = testCaseService.findById(request.getCaseId());
        if (testCase == null) {
            return ApiResponse.error("用例不存在", null);
        }

        // 2. 校验：需要登录时，账号密码不能为空
        if (testCase.getNeedLogin()) {
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ApiResponse.error("需要登录时，测试账号不能为空", null);
            }
            if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
                return ApiResponse.error("需要登录时，测试密码不能为空", null);
            }
            if (testCase.getSiteCode() == null || testCase.getSiteCode().trim().isEmpty()) {
                return ApiResponse.error("用例配置了需要登录，但未关联测试网站", null);
            }
        }

        // 3. 查询网站配置（需要登录时）
        SiteTestConfigDO siteConfig = null;
        if (testCase.getNeedLogin()) {
            siteConfig = siteTestConfigService.getBySiteCode(testCase.getSiteCode());
            if (siteConfig == null) {
                return ApiResponse.error("未找到[" + testCase.getSiteCode() + "]的网站配置", null);
            }
        }

        // 4. 执行用例（预期昵称复用用例的断言预期值）
        TestResult result = testCaseService.runTestCase(
                request.getCaseId(),
                siteConfig,
                testCase.getNeedLogin(),
                request.getUsername(),
                request.getPassword(),
                testCase.getAssertExpectedValue() // 断言预期值作为预期昵称
        );

        String msg = "执行完毕，状态: " + result.getStatus();
        return ApiResponse.success(msg, result);
    } catch (Exception ex) {
        String errMsg = ex.getMessage() == null ? "用例执行异常" : ex.getMessage();
        return ApiResponse.error("执行失败: " + errMsg, null);
    }
}

    /**
     * 查询所有测试用例
     */
    @GetMapping("")
    public ApiResponse<List<TestCase>> listCases() {
        try {
            List<TestCase> caseList = testCaseService.listAll();
            return ApiResponse.success("查询用例列表成功", caseList);
        } catch (Exception ex) {
            return ApiResponse.error("查询用例列表失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 更新指定测试用例
     */
    @PutMapping("/case/{id}")
    public ApiResponse<String> updateCase(@PathVariable Long id, @RequestBody TestCase request) {
        try {
            testCaseService.updateTestCase(id, request);
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
    public ApiResponse<String> deleteCase(@PathVariable Long id) {
        try {
            testCaseService.deleteTestCase(id);
            return ApiResponse.success("删除用例成功", "ok");
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error("删除失败: " + ex.getMessage(), null);
        } catch (Exception ex) {
            return ApiResponse.error("删除失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 查询所有网站配置（供前端下拉选择）
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
}