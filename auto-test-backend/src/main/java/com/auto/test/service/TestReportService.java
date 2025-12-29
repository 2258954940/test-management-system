package com.auto.test.service;

import com.auto.test.dto.TestReportExcelDTO;
import com.auto.test.mapper.SiteTestConfigMapper;
import com.auto.test.mapper.TestCaseMapper;
import com.auto.test.mapper.TestResultMapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import com.alibaba.excel.EasyExcel;

/**
 * 测试报告导出服务。
 */
@Service
public class TestReportService {
    private static final Logger log = LoggerFactory.getLogger(TestReportService.class);

    private final TestResultMapper testResultMapper;
    private final TestCaseMapper testCaseMapper;
    private final SiteTestConfigMapper siteTestConfigMapper;

    public TestReportService(TestResultMapper testResultMapper,
                             TestCaseMapper testCaseMapper,
                             SiteTestConfigMapper siteTestConfigMapper) {
        this.testResultMapper = testResultMapper;
        this.testCaseMapper = testCaseMapper;
        this.siteTestConfigMapper = siteTestConfigMapper;
    }

    /**
     * 导出测试报告为 Excel。
     * 备注：毕设演示用途，直接查询并导出，不做复杂权限控制。
     */
    public void exportTestReportExcel(HttpServletResponse response) {
        try {
            // 临时验证：跳过数据库查询，直接写占位行，确保Excel非0KB
            // List<TestReportExcelDTO> rows = Collections.singletonList(buildPlaceholderRow());
        // 1. 查询真实数据库数据
        List<TestReportExcelDTO> rows = testResultMapper.selectReportRows();
        
        // 2. 空数据时用占位行兜底（保留，避免文件空白）
        if (CollectionUtils.isEmpty(rows)) {
            rows = Collections.singletonList(buildPlaceholderRow());
        }

            String fileName = URLEncoder.encode("测试报告.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

            try (var out = response.getOutputStream()) {
                EasyExcel.write(out, TestReportExcelDTO.class)
                        .sheet("测试报告")
                        .doWrite(rows);
                out.flush();
            }
        } catch (Exception ex) {
            log.error("导出测试报告Excel失败", ex);
            // 返回明确的 JSON 错误，避免空响应
            try {
                response.reset();
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setContentType("application/json;charset=UTF-8");
                response.setHeader("Content-Disposition", "inline; filename=\"export-error.json\"");
                try (PrintWriter writer = response.getWriter()) {
                    writer.write("{\"success\":false,\"message\":\"导出失败: " + sanitize(ex.getMessage()) + "\"}");
                    writer.flush();
                }
            } catch (Exception ignored) {
                // 二次失败只能放弃写回
            }
        }
    }

    private TestReportExcelDTO buildPlaceholderRow() {
        TestReportExcelDTO placeholder = new TestReportExcelDTO();
        placeholder.setCaseId(0L);
        placeholder.setCaseName("暂无测试结果");
        placeholder.setSiteName("-");
        placeholder.setExecuteStatus("-");
        placeholder.setExecuteTime(0L);
        placeholder.setCreateTime(null);
        placeholder.setFailReason("-");
        placeholder.setScreenshotPath("-");
        return placeholder;
    }

    private String sanitize(String message) {
        if (message == null) {
            return "未知错误";
        }
        return message.replace('"', ' ');
    }
}
