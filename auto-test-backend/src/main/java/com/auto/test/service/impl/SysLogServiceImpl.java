package com.auto.test.service.impl;

import com.auto.test.entity.SysLog;
import com.auto.test.mapper.SysLogMapper;
import com.auto.test.service.SysLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // 替换为Spring的StringUtils，判空更通用
import java.time.LocalDateTime;

@Service
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    @Override
    public void saveLog(String username, String operationType, String operationContent) {
        String actualUsername = username;
        
        // 1. 从SecurityContext获取真实操作人（接口层已确保有值）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() != null
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            actualUsername = authentication.getName();
        }

        // 2. 严格校验：操作人不能为空（接口层已保证，此处仅二次校验）
        if (actualUsername == null || actualUsername.trim().isEmpty()) {
            throw new RuntimeException("无法识别操作人，日志记录失败");
        }

        // 3. 构建日志对象（确保username为真实操作人）
        SysLog log = new SysLog();
        log.setUsername(actualUsername);       // 真实操作人
        log.setOperationType(operationType);   // 操作类型
        log.setOperationContent(operationContent); // 操作内容
        log.setCreateTime(LocalDateTime.now());// 操作时间

        // 4. 插入日志（此时username必有值，数据库不会报错）
        this.save(log);
    }

    @Override
    public Page<SysLog> listLogs(String username, String operationType, LocalDateTime start, LocalDateTime end,
                                 long pageNum, long pageSize) {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();
        // 用户名模糊查询（优化判空逻辑，使用Spring的StringUtils更通用）
        if (StringUtils.hasText(username)) {
            wrapper.like(SysLog::getUsername, username);
        }
        // 操作类型精确匹配（优化判空，排除空字符串）
        if (StringUtils.hasText(operationType) && !"请选择".equals(operationType)) {
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
        
        // 核心：创建分页对象，执行分页查询（MyBatis-Plus分页插件会拦截此方法）
        Page<SysLog> page = new Page<>(pageNum, pageSize);
        return this.page(page, wrapper);
    }
}