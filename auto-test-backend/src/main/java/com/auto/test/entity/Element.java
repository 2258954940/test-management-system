// auto-test-backend/src/main/java/com/auto/test/entity/Element.java
package com.auto.test.entity;

import java.time.LocalDateTime;

public class Element {
    private Long id;
    private String elementName; // 元素名称
    private String pageUrl;     // 所属页面URL
    private String locatorType; // 定位器类型
    private String locatorValue;// 定位器值
    private String widgetType;  // 控件类型（可选）
    private String createBy;     // 创建人
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 生成所有字段的 getter/setter（和 TestCase 类的写法一致）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getElementName() { return elementName; }
    public void setElementName(String elementName) { this.elementName = elementName; }

    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }

    public String getLocatorType() { return locatorType; }
    public void setLocatorType(String locatorType) { this.locatorType = locatorType; }

    public String getLocatorValue() { return locatorValue; }
    public void setLocatorValue(String locatorValue) { this.locatorValue = locatorValue; }

    public String getWidgetType() { return widgetType; }
    public void setWidgetType(String widgetType) { this.widgetType = widgetType; }

    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}