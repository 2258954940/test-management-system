package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.TestResult;
import com.auto.test.mapper.TestResultMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用例执行结果接口，提供查询列表与单条明细。
 */
@RestController
@RequestMapping("/api")
public class TestResultController {

    private final TestResultMapper testResultMapper;

    public TestResultController(TestResultMapper testResultMapper) {
        this.testResultMapper = testResultMapper;
    }

    @GetMapping("/results")
    public ApiResponse<List<TestResult>> listResults(@RequestParam(value = "caseId", required = false) Long caseId) {
        try {
            List<TestResult> results = (caseId == null)
                ? testResultMapper.findAll()
                : testResultMapper.findByCaseId(caseId);
            return ApiResponse.success("查询成功", results);
        } catch (Exception ex) {
            return ApiResponse.error("查询失败: " + ex.getMessage(), null);
        }
    }

    @GetMapping("/result/{id}")
    public ApiResponse<TestResult> getResult(@PathVariable Long id) {
        try {
            TestResult result = testResultMapper.findById(id);
            if (result == null) {
                return ApiResponse.error("执行结果不存在，ID=" + id, null);
            }
            return ApiResponse.success("查询成功", result);
        } catch (Exception ex) {
            return ApiResponse.error("查询失败: " + ex.getMessage(), null);
        }
    }
}
