package com.auto.test.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
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
    private String creator;
    private String elementIds;
    // 断言相关字段
    private String assertType;
    private String assertLocatorType;
    private String assertLocatorValue;
    private String assertExpectedValue;
    // ========== 登录相关属性 ==========
    private Boolean needLogin; // 是否需要登录（true/false）
    private String siteCode;   // 关联的网站编码（如SAUCE_DEMO）
    // 新增：测试账号密码（毕设直接明文存储）
    private String testAccount;  // 默认测试账号
    private String testPassword; // 默认测试密码
    // 时间字段
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}