package com.auto.test.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志实体（对应表sys_log）
 */
@Data
@TableName("sys_log")
public class SysLog {
    @TableId(type = IdType.AUTO)
    private Long id;          // 日志ID
    private String username;  // 操作人用户名
    private String operationType; // 操作类型（如：新增用户、执行任务）
    private String operationContent; // 操作内容（如：新增用户-张三）
    private LocalDateTime createTime; // 操作时间
}