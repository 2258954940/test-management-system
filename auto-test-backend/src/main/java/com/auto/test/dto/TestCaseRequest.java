package com.auto.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 新增测试用例请求体，兼容前端 JSON 传参。
 */
public class TestCaseRequest {

    @NotBlank(message = "用例名称不能为空")
    private String name;

    private String description; // 前端传了，非必填

    @NotBlank(message = "访问地址不能为空")
    private String url;

    // 暂时去掉必填校验（前端还没做这些字段的输入框）
    private String locatorType;

    private String locatorValue;

    private String actionType;

    private String inputData;

    // 暂时去掉必填校验（前端还没做）
    private String expectedResult;

    // 新增：接收前端传的creator字段
    @NotBlank(message = "创建人不能为空")
    private String creator;

    // ========== 补充creator的getter/setter ==========
    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    // ========== 原有字段的getter/setter保留 ==========
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocatorType() {
        return locatorType;
    }

    public void setLocatorType(String locatorType) {
        this.locatorType = locatorType;
    }

    public String getLocatorValue() {
        return locatorValue;
    }

    public void setLocatorValue(String locatorValue) {
        this.locatorValue = locatorValue;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getInputData() {
        return inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }
}