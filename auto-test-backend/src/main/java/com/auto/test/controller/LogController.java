package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.SysLog;
import com.auto.test.service.SysLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/system/log")
public class LogController {

    private final SysLogService sysLogService;

    public LogController(SysLogService sysLogService) {
        this.sysLogService = sysLogService;
    }

    /**
     * 查询操作日志列表
     */
    @GetMapping("/list")
    public ApiResponse<List<SysLog>> listLogs(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end
    ) {
        List<SysLog> logs = sysLogService.listLogs(username, operationType, start, end);
        return ApiResponse.success(logs);
    }
}