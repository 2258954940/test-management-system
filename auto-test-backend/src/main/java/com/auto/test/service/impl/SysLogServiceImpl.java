package com.auto.test.service.impl;

import com.auto.test.entity.SysLog;
import com.auto.test.mapper.SysLogMapper;
import com.auto.test.service.SysLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    public void saveLog(String username, String operationType, String operationContent) {
        SysLog log = new SysLog();
        log.setUsername(username);
        log.setOperationType(operationType);
        log.setOperationContent(operationContent);
        log.setCreateTime(LocalDateTime.now()); // 自动填充当前时间
        this.save(log);
    }

    @Override
    public List<SysLog> listLogs(String username, String operationType, LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        // 用户名模糊查询
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysLog::getUsername, username);
        }
        // 操作类型精确匹配
        if (operationType != null && !operationType.isEmpty() && !"请选择".equals(operationType)) {
            wrapper.eq(SysLog::getOperationType, operationType);
        }
        // 时间范围查询
        if (start != null) {
            wrapper.ge(SysLog::getCreateTime, start);
        }
        if (end != null) {
            wrapper.le(SysLog::getCreateTime, end);
        }
        // 按操作时间倒序
        wrapper.orderByDesc(SysLog::getCreateTime);
        return this.list(wrapper);
    }
}