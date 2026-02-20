package com.auto.test.service;

import com.auto.test.entity.SysLog;
import com.baomidou.mybatisplus.extension.service.IService;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface SysLogService extends IService<SysLog> {
    // 保存操作日志
    void saveLog(String username, String operationType, String operationContent);
    
    // 按条件查询日志（用户名、操作类型、时间范围）分页
    Page<SysLog> listLogs(String username, String operationType, LocalDateTime start, LocalDateTime end,
                          long pageNum, long pageSize);
}