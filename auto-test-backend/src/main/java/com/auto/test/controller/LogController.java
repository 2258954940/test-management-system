package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.SysLog;
import com.auto.test.entity.User;
import com.auto.test.service.SysLogService;
import com.auto.test.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/system/log")
public class LogController {

    private final SysLogService sysLogService;
    private final UserService userService; // 注入UserService用于解析Token

    // 构造器注入
    public LogController(SysLogService sysLogService, UserService userService) {
        this.sysLogService = sysLogService;
        this.userService = userService;
    }

    /**
     * 查询操作日志列表（修复时间解析+Token校验）
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> listLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String start, // 改为String接收前端时间字符串
            @RequestParam(required = false) String end,   // 改为String接收前端时间字符串
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestHeader("Authorization") String token  // 接收Token用于校验
    ) {
        try {
            // 1. 解析Token（校验操作人权限，非必须但保证接口安全）
            String realUsername = getRealUsernameFromToken(token);

            // 2. 格式化时间参数（前端传的是 "YYYY-MM-DD HH:mm:ss" 字符串）
            LocalDateTime startTime = null;
            LocalDateTime endTime = null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            if (StringUtils.hasText(start)) {
                startTime = LocalDateTime.parse(start, formatter);
            }
            if (StringUtils.hasText(end)) {
                endTime = LocalDateTime.parse(end, formatter);
            }

            // 3. 分页查询日志
            Page<SysLog> page = sysLogService.listLogs(
                    username, operationType, startTime, endTime, pageNum, pageSize
            );

            // 4. 组装返回数据（必须包含total/list/pageNum/pageSize）
            Map<String, Object> data = new HashMap<>();
            data.put("total", page.getTotal());    // 总条数（前端分页核心）
            data.put("list", page.getRecords());  // 当前页数据
            data.put("pageNum", page.getCurrent());// 当前页码
            data.put("pageSize", page.getSize()); // 每页条数
            return ApiResponse.success(data);
        } catch (Exception ex) {
            return ApiResponse.error("查询日志失败：" + ex.getMessage(), null);
        }
    }

    /**
     * 从Token解析真实用户名（复用UserController的逻辑）
     */
    private String getRealUsernameFromToken(String token) {
        try {
            // 先解码前端传的URL编码Token
            token = URLDecoder.decode(token, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new RuntimeException("Token解码失败，请重新登录");
        }

        if (!StringUtils.hasText(token) || !token.startsWith("auto-test-token-")) {
            throw new RuntimeException("Token无效，请重新登录");
        }

        // 拆分Token：auto-test-token-admin123-1771481838993 → 数组长度5
        String[] tokenParts = token.split("-");
        if (tokenParts.length < 5) {
            throw new RuntimeException("Token格式错误，请重新登录");
        }

        // 正确截取用户名（索引3）
        String realUsername = tokenParts[3];
        User user = userService.getByUsername(realUsername);
        if (user == null) {
            throw new RuntimeException("操作人不存在，请重新登录：" + realUsername);
        }
        return realUsername;
    }
}