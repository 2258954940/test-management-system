package com.auto.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增/编辑用例的请求DTO（必须包含断言字段）
 */
@Data
public class TestCaseRequest {
    // 原有字段（保持不变）
    @NotBlank(message = "用例名称不能为空")
    private String name;
    @NotBlank(message = "测试URL不能为空")
    private String url;
    @NotBlank(message = "步骤不能为空")
    private String description;
    @NotBlank(message = "输入数据不能为空")
    private String inputData;
    @NotBlank(message = "预期结果不能为空")
    private String expectedResult;
    private String creator;
    private String elementIds;
    private String locatorType;
    private String locatorValue;
    private String actionType;
    private Boolean needLogin; // 是否需要登录
    private String siteCode;   // 关联的网站编码
    // ========== 新增：断言字段（和前端提交的key完全对应） ==========
    @NotBlank(message = "断言类型不能为空")
    private String assertType;
    @NotBlank(message = "断言定位类型不能为空")
    private String assertLocatorType;
    @NotBlank(message = "断言定位值不能为空")
    private String assertLocatorValue;
    @NotBlank(message = "断言预期值不能为空（文本断言）")
    private String assertExpectedValue;
}