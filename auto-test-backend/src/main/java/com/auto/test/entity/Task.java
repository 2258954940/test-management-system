package com.auto.test.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Task {
    private Long id; // 对应数据库id
    private String taskName; // 对应task_name
    private String caseId; // 关键：把caseIds改成caseId（匹配数据库case_id）
    private String execType; // 对应exec_type
    private String cronExpression; // 对应cron_expression
    private String status; // 对应status
    private String createBy; // 对应create_by
    private Date createTime; // 对应create_time
}