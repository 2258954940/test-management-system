package com.auto.test.entity;

import java.time.LocalDateTime;

/**
 * 测试用例实体，对应表 test_case。
 */
public class TestCase {

    private Long id;
    private String name;
    private String description;
    private String url;
    private String locatorType;
    private String locatorValue;
    private String actionType;
    private String inputData;
    private String expectedResult;
    private LocalDateTime createTime;
    private String creator; // 新增
    private LocalDateTime updateTime;
    private String elementIds; // 关联的元素ID，逗号分隔
        public LocalDateTime getUpdateTime() {
            return updateTime;
        }
        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }
        // 新增getter/setter
        public String getCreator() {
            return creator;
        }
        public void setCreator(String creator) {
            this.creator = creator;
        }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
            // 新增getter/setter
        public String getElementIds() {
            return elementIds;
        }
        public void setElementIds(String elementIds) {
            this.elementIds = elementIds;
        }
}

