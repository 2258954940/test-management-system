package com.auto.test.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.util.Date;

/**
 * 测试报告Excel导出数据模型
 */
@Data
public class TestReportExcelDTO {
    // Excel列名 + 索引（索引对应列的顺序）
    @ExcelProperty(value = "用例ID", index = 0)
    @ColumnWidth(10) // 列宽
    private Long caseId;

    @ExcelProperty(value = "用例名称", index = 1)
    @ColumnWidth(25)
    private String caseName;

    @ExcelProperty(value = "关联测试站", index = 2)
    @ColumnWidth(20)
    private String siteName; // 测试站名称（从site_test_config表关联）

    @ExcelProperty(value = "执行状态", index = 3)
    @ColumnWidth(12)
    private String executeStatus; // PASS/FAILED

    @ExcelProperty(value = "执行耗时(ms)", index = 4)
    @ColumnWidth(15)
    private Long executeTime;

    @ExcelProperty(value = "执行时间", index = 5)
    @ColumnWidth(20)
    private Date createTime; // 执行时间（对应test_result的create_time）

    @ExcelProperty(value = "失败原因", index = 6)
    @ColumnWidth(30)
    private String failReason; // 失败原因（无失败则填"无"）

    @ExcelProperty(value = "截图路径", index = 7)
    @ColumnWidth(40)
    private String screenshotPath;
}