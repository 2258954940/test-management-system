package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.dto.RunCaseRequest;
import com.auto.test.dto.TestCaseRequest;
import com.auto.test.entity.SiteTestConfigDO;
import com.auto.test.entity.TestCase;
import com.auto.test.entity.TestResult;
import com.auto.test.service.SiteTestConfigService;
import com.auto.test.service.TestCaseService;
import com.auto.test.service.SysLogService; // 新增：导入日志服务
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
    private final SysLogService sysLogService; // 新增：日志服务

    // 构造器注入（Spring推荐方式，添加SysLogService）
    public TestCaseController(TestCaseService testCaseService, 
                              SiteTestConfigService siteTestConfigService,
                              SysLogService sysLogService) { // 新增：日志服务注入
        this.testCaseService = testCaseService;
        this.siteTestConfigService = siteTestConfigService;
        this.sysLogService = sysLogService; // 新增：赋值
    }

    /**
     * 新增测试用例（添加日志）
     */
    @PostMapping("/addCase")
    public ApiResponse<TestCase> addCase(@Valid @RequestBody TestCaseRequest request) {
        try {
            TestCase saved = testCaseService.addTestCase(request);
            
            // ========== 修复点1：替换getCaseName() ==========
            // 请根据你的TestCase实体类字段名修改：
            // 若字段是name → saved.getName()
            // 若字段是testCaseName → saved.getTestCaseName()
            String caseName = saved.getName() != null ? saved.getName() : "未命名用例-" + saved.getId();
            
            // 新增：记录操作日志
            sysLogService.saveLog(
                "admin",
                "新增测试用例",
                "新增用例-" + caseName + "（ID：" + saved.getId() + "）"
            );
            
            return ApiResponse.success("新增用例成功", saved);
        } catch (Exception ex) {
            return ApiResponse.error("新增用例失败: " + ex.getMessage(), null);
        }
    }

    /**
     * 扩展版：执行用例（支持纯UI/登录+API两种模式 + 添加日志）
     */
    // 修改：执行用例接口，支持浏览器类型（默认edge）。路径保持兼容 /api/cases/runCase。
    @PostMapping({"/runCase", "/run/{caseId}"})
    public ApiResponse<TestResult> runCase(@PathVariable(name = "caseId", required = false) Long pathCaseId,
                                           @Valid @RequestBody RunCaseRequest request,
                                           @RequestParam(name = "browserType", required = false, defaultValue = "edge") String browserType) {
        try {
            Long targetCaseId = request.getCaseId() != null ? request.getCaseId() : pathCaseId;
            if (targetCaseId == null) {
                return ApiResponse.error("用例ID不能为空", null);
            }
            // 1. 先查询用例，获取登录相关配置
            TestCase testCase = testCaseService.findById(targetCaseId);
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
                    testCase.getAssertExpectedValue(),
                    browserType, // 新增：传入浏览器类型
                    null // 单次执行无任务关联
            );

            // ========== 修复点2：替换getCaseName() ==========
            // 请根据你的TestCase实体类字段名修改：
            String caseName = testCase.getName() != null ? testCase.getName() : "未命名用例-" + testCase.getId();
            
            // 新增：记录操作日志
            sysLogService.saveLog(
                "admin",
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
     * 查询所有测试用例（无需记录日志）
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
     * 更新指定测试用例（添加日志）
     */
    @PutMapping("/case/{id}")
    public ApiResponse<String> updateCase(@PathVariable Long id, @RequestBody TestCase request) {
        try {
            // 先查询原用例信息（用于日志）
            TestCase oldCase = testCaseService.findById(id);
            if (oldCase == null) {
                return ApiResponse.error("用例不存在", null);
            }
            
            testCaseService.updateTestCase(id, request);
            
            // ========== 修复点3：替换getCaseName() ==========
            String oldCaseName = oldCase.getName() != null ? oldCase.getName() : "未命名用例-" + oldCase.getId();
            
            // 新增：记录操作日志
            sysLogService.saveLog(
                "admin",
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
     * 删除指定测试用例（添加日志）
     */
    @DeleteMapping("/case/{id}")
    public ApiResponse<String> deleteCase(@PathVariable Long id) {
        try {
            // 先查询原用例信息（用于日志）
            TestCase oldCase = testCaseService.findById(id);
            if (oldCase == null) {
                return ApiResponse.error("用例不存在", null);
            }
            
            testCaseService.deleteTestCase(id);
            
            // ========== 修复点4：替换getCaseName() ==========
            String oldCaseName = oldCase.getName() != null ? oldCase.getName() : "未命名用例-" + oldCase.getId();
            
            // 新增：记录操作日志
            sysLogService.saveLog(
                "admin",
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
     * 查询所有网站配置（供前端下拉选择）（无需记录日志）
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