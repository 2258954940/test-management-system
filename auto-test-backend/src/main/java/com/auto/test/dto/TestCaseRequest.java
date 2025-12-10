package com.auto.test.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 新增测试用例请求体，兼容前端 JSON 传参。
 */
public class TestCaseRequest {

    @NotBlank(message = "用例名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "访问地址不能为空")
    private String url;

    @NotBlank(message = "定位方式不能为空")
    private String locatorType;

    @NotBlank(message = "定位值不能为空")
    private String locatorValue;

    @NotBlank(message = "动作类型不能为空")
    private String actionType;

    private String inputData;

    @NotNull(message = "期望结果不能为空")
    private String expectedResult;

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
