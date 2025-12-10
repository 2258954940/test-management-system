package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.dto.RunCaseRequest;
import com.auto.test.dto.TestCaseRequest;
import com.auto.test.entity.TestCase;
import com.auto.test.entity.TestResult;
import com.auto.test.service.TestCaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用例接口层，对接前端 Vue，请求体为 JSON。
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:8080")
public class TestCaseController {

    private final TestCaseService testCaseService;

    public TestCaseController(TestCaseService testCaseService) {
        this.testCaseService = testCaseService;
    }

    @PostMapping("/addCase")
    public ApiResponse<TestCase> addCase(@Valid @RequestBody TestCaseRequest request) {
        try {
            TestCase saved = testCaseService.addTestCase(request);
            return ApiResponse.success("新增用例成功", saved);
        } catch (Exception ex) {
            return ApiResponse.error("新增用例失败: " + ex.getMessage(), null);
        }
    }

    @PostMapping("/runCase")
    public ApiResponse<TestResult> runCase(@Valid @RequestBody RunCaseRequest request) {
        try {
            TestResult result = testCaseService.runTestCase(request.getCaseId());
            String msg = "执行完毕，状态: " + result.getStatus();
            return ApiResponse.success(msg, result);
        } catch (Exception ex) {
            // 捕获用例不存在、驱动错误等异常，返回友好提示。
            return ApiResponse.error("执行失败: " + ex.getMessage(), null);
        }
    }

    @GetMapping("/cases")
    public ApiResponse<java.util.List<TestCase>> listCases() {
        try {
            return ApiResponse.success("查询成功", testCaseService.listAll());
        } catch (Exception ex) {
            return ApiResponse.error("查询失败: " + ex.getMessage(), null);
        }
    }

    @PutMapping("/case/{id}")
    public ApiResponse<String> updateCase(@PathVariable Long id, @RequestBody TestCase request) {
        try {
            testCaseService.updateTestCase(id, request);
            return ApiResponse.success("更新成功", "ok");
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error("更新失败: " + ex.getMessage(), null);
        } catch (Exception ex) {
            return ApiResponse.error("更新失败: " + ex.getMessage(), null);
        }
    }

    @DeleteMapping("/case/{id}")
    public ApiResponse<String> deleteCase(@PathVariable Long id) {
        try {
            testCaseService.deleteTestCase(id);
            return ApiResponse.success("删除成功", "ok");
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error("删除失败: " + ex.getMessage(), null);
        } catch (Exception ex) {
            return ApiResponse.error("删除失败: " + ex.getMessage(), null);
        }
    }
}
