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
     * 导出测试报告为 Excel（按taskId筛选数据）
     */
    public void exportTestReportExcel(HttpServletResponse response, Long taskId) { // 接收taskId
        try {
            // 1. 按任务ID查询对应测试结果（核心修改）
            List<TestReportExcelDTO> rows = testResultMapper.selectReportRowsByTaskId(taskId);
            
            // 2. 空数据时用占位行兜底
            if (CollectionUtils.isEmpty(rows)) {
                rows = Collections.singletonList(buildPlaceholderRow());
            }

            // 3. 文件名加入taskId，区分不同任务的报告
            String fileName = URLEncoder.encode("测试报告-任务" + taskId + ".xlsx", 
                    StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

            try (var out = response.getOutputStream()) {
                EasyExcel.write(out, TestReportExcelDTO.class)
                        .sheet("测试报告-任务" + taskId) // 工作表名带taskId
                        .doWrite(rows);
                out.flush();
            }
        } catch (Exception ex) {
            log.error("导出测试报告Excel失败（任务ID：{}）", taskId, ex);
            // 返回明确的 JSON 错误
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