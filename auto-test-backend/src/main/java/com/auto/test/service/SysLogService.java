package com.auto.test.service;

import com.auto.test.entity.SysLog;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;
import java.time.LocalDateTime;

public interface SysLogService extends IService<SysLog> {
    // 保存操作日志
    void saveLog(String username, String operationType, String operationContent);
    
    // 按条件查询日志（用户名、操作类型、时间范围）
    List<SysLog> listLogs(String username, String operationType, LocalDateTime start, LocalDateTime end);
}