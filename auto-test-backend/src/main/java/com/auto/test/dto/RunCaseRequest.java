package com.auto.test.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RunCaseRequest {
    @NotNull(message = "用例ID不能为空")
    private Long caseId;
    // 仅保留账号密码（敏感信息，执行时输入）
    private String username; 
    private String password;
}