package com.auto.test.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 执行用例请求体，仅需要传递用例主键 ID。
 */
public class RunCaseRequest {

    @NotNull(message = "用例ID不能为空")
    private Long caseId;

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }
}
