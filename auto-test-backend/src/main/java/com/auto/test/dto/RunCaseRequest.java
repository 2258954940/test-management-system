package com.auto.test.dto;

import lombok.Data;

@Data
public class RunCaseRequest {
    private Long caseId;
    // 仅保留账号密码（敏感信息，执行时输入）
    private String username; 
    private String password;
    // 新增：浏览器类型（chrome/firefox/edge），默认edge
    private String browserType;
}