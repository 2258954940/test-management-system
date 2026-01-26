// src/main/java/com/auto/test/dto/TaskCreateDTO.java
package com.auto.test.dto;

import lombok.Data;
import java.util.List;

@Data
public class TaskCreateDTO {
    private String taskName; // 任务名称
    private List<Integer> caseIds; // 多选的用例ID
    private String executeType; // 执行方式：immediate/timing
    private String executeTime; // 定时执行时间
    private String browserType; // 执行浏览器
}