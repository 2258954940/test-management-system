// auto-test-backend/src/main/java/com/auto/test/dto/ElementParseRequest.java
package com.auto.test.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ElementParseRequest {
    @NotBlank(message = "解析URL不能为空")
    private String url; // 接收前端传的页面URL
}