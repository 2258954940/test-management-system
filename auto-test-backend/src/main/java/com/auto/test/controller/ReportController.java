package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.service.TestReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 报表导出接口。
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final TestReportService testReportService;

    public ReportController(TestReportService testReportService) {
        this.testReportService = testReportService;
    }

    @GetMapping("/exportExcel")
    public void exportExcel(HttpServletResponse response) {
        try {
            testReportService.exportTestReportExcel(response);
        } catch (Exception e) {
            try {
                response.reset();
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                String body = String.format("{\"code\":500,\"msg\":\"导出失败: %s\",\"data\":null}", e.getMessage());
                response.getWriter().write(body);
            } catch (Exception ignored) {
                // ignore secondary errors
            }
        }
    }
}
