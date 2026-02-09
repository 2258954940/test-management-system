// src/main/java/com/auto/test/dto/TaskCreateDTO.java
package com.auto.test.dto;

import lombok.Data;
import java.util.List;

@Data
public class TaskCreateDTO {
    private String taskName; // 任务名称（保留你原有字段名）
    private List<Integer> caseIds; // 多选的用例ID（保留）
    private String executeType; // 执行方式：immediate/timing（保留，注意你写的是timing，不是timed）
    private String executeTime; // 定时执行时间（保留，可兼容旧逻辑）
    private String browserType; // 执行浏览器（保留）
    private String cronExpression; // 新增：Cron表达式字段（前端传过来的）
}