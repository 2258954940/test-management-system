package com.auto.test.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName; // 必须导入这个包
import lombok.Data;
import java.util.Date;

@Data
@TableName("test_task") // 关键：指定实体类对应的数据库表名是test_task
public class Task {
    private Integer id; // 任务ID
    private String taskName; // 任务名称
    private String caseId; // 关联用例ID（逗号分隔）
    private String execType; // 执行方式：immediate/timed
    private String cronExpression; // 定时Cron表达式
    private String status; // 执行状态：pending/running/finished/failed
    private String createBy; // 创建人
    @TableField(exist = false)
    private Integer totalCase; // 总用例数
    @TableField(exist = false)
    private Integer successCase; // 成功用例数
    @TableField(exist = false)
    private Integer failCase; // 失败用例数
    @TableField(exist = false)
    private String successRate; // 成功率（保留2位小数，如100.00%）
    private Date createTime; // 创建时间
}