// src/main/java/com/auto/test/controller/TaskController.java
package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.Task;
import com.auto.test.entity.User;
import com.auto.test.scheduler.TaskSchedulerManager;
import com.auto.test.service.TaskService;
import com.auto.test.service.SysLogService;
import com.auto.test.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {
    // 声明日志对象
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskSchedulerManager taskSchedulerManager;
    
    @Autowired
    private SysLogService sysLogService;
    
    @Autowired
    private UserService userService; // 新增：注入UserService用于解析Token

    /**
     * 分页获取任务列表（适配前端分页）
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "5") Integer pageSize
    ) {
        List<Task> allTasks = taskService.list();
        // 构建前端需要的分页数据
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("records", allTasks);
        pageData.put("total", allTasks.size());
        pageData.put("pageNum", pageNum);
        pageData.put("pageSize", pageSize);
        return ApiResponse.success(pageData);
    }

    /**
     * 获取已完成任务列表
     */
    @GetMapping("/finished-list")
    public ApiResponse<List<Task>> getFinishedTasks() {
        List<Task> finishedTasks = taskService.listFinishedTasks();
        return ApiResponse.success(finishedTasks);
    }

    /**
     * 创建任务（修复：获取真实登录用户）
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createTask(@RequestBody Map<String, Object> data,
                                                       @RequestHeader("Authorization") String token) { // 新增Token参数
        try {
            // 核心：从Token解析真实用户名并设置到SecurityContext
            String currentUser = getRealUsernameFromTokenAndSetContext(token);
            
            Task task = new Task();
            // 字段映射逻辑不变
            task.setTaskName((String) data.get("taskName"));
            task.setCaseId((String) data.get("caseIds"));
            String executeType = (String) data.get("executeType");
            task.setExecType(executeType);
            
            // 接收前端的Cron表达式
            String cronExpression = (String) data.get("cronExpression");
            task.setCronExpression(cronExpression);

            // 任务基础信息：创建人改为当前真实用户
            task.setStatus("pending");
            task.setCreateBy(currentUser); // 原："admin"
            task.setCreateTime(new Date());

            // 调用service创建任务（确保service的createTask方法会回显主键ID）
            boolean success = taskService.createTask(task);
            if (!success) {
                return ApiResponse.error("任务创建失败");
            }

            // 打印日志确认ID已回显
            System.out.println("创建任务返回ID=" + task.getId());

            if (task.getId() == null) {
                return ApiResponse.error("任务创建失败，ID未回显");
            }

            // 定时任务注册逻辑不变
            if ("timing".equals(executeType) && cronExpression != null && !cronExpression.isEmpty()) {
                taskSchedulerManager.registerTask(task.getId(), cronExpression, () -> {
                    taskService.runTask(task.getId());
                });
            }

            // 记录操作日志（使用真实用户名）
            sysLogService.saveLog(
                    currentUser,
                    "新增调度任务",
                    "新增任务-" + task.getTaskName() + "（ID：" + task.getId() + "）"
            );

            // 构建返回结果：包含提示信息 + 新任务ID
            Map<String, Object> result = new HashMap<>();
            result.put("message", "任务创建成功");
            result.put("id", task.getId());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return ApiResponse.error("创建任务失败：" + e.getMessage());
        }
    }

    /**
     * 运行任务（修复：获取真实登录用户）
     */
    @PostMapping("/run/{id}")
    public ApiResponse<String> runTask(@PathVariable Integer id,
                                       @RequestHeader("Authorization") String token) { // 新增Token参数
        try {
            // 核心：从Token解析真实用户名并设置到SecurityContext
            String currentUser = getRealUsernameFromTokenAndSetContext(token);
            
            Task task = taskService.getById(id);
            if (task == null) {
                return ApiResponse.error("任务不存在");
            }

            // 定时任务：注册到调度器
            if ("timing".equals(task.getExecType()) && task.getCronExpression() != null && !task.getCronExpression().isEmpty()) {
                taskSchedulerManager.registerTask(id, task.getCronExpression(), () -> {
                    taskService.runTask(id);
                });
                task.setStatus("running");
                taskService.updateById(task);
                
                // 记录操作日志（使用真实用户名）
                sysLogService.saveLog(
                        currentUser,
                        "注册定时任务",
                        "注册任务-" + task.getTaskName() + "（ID：" + id + "），Cron：" + task.getCronExpression()
                    );
                
                return ApiResponse.success("定时任务已注册，将按规则执行");
            }

            // 立即执行：调用原有逻辑
            boolean success = taskService.runTask(id);
            
            // 记录操作日志（使用真实用户名）
            if (success) {
                sysLogService.saveLog(
                        currentUser,
                        "执行调度任务",
                        "执行任务-" + task.getTaskName() + "（ID：" + id + "）"
                    );
            }
            
            return success ? ApiResponse.success("任务开始执行") : ApiResponse.error("任务执行失败");
        } catch (Exception e) {
            log.error("运行任务失败", e);
            return ApiResponse.error("运行任务失败：" + e.getMessage());
        }
    }

    /**
     * 停止任务（修复：获取真实登录用户）
     */
    @PostMapping("/stop/{id}")
    public ApiResponse<String> stopTask(@PathVariable Integer id,
                                        @RequestHeader("Authorization") String token) { // 新增Token参数
        try {
            // 核心：从Token解析真实用户名并设置到SecurityContext
            String currentUser = getRealUsernameFromTokenAndSetContext(token);
            
            // 取消定时任务
            taskSchedulerManager.cancelTask(id);
            
            // 调用原有终止逻辑
            boolean success = taskService.stopTask(id);
            if (success) {
                // 补充：更新任务状态为已完成
                Task task = new Task();
                task.setId(id);
                task.setStatus("finished");
                taskService.updateById(task);
                
                // 记录操作日志（使用真实用户名）
                Task oldTask = taskService.getById(id);
                sysLogService.saveLog(
                        currentUser,
                        "停止调度任务",
                        "停止任务-" + (oldTask != null ? oldTask.getTaskName() : "ID:" + id)
                    );
                
                return ApiResponse.success("任务已终止");
            } else {
                return ApiResponse.error("任务终止失败");
            }
        } catch (Exception e) {
            log.error("停止任务失败", e);
            return ApiResponse.error("停止任务失败：" + e.getMessage());
        }
    }

    /**
     * 获取任务日志（原有逻辑不变）
     */
    @GetMapping("/log/{id}")
    public ApiResponse<Map<String, List<String>>> getTaskLog(@PathVariable Integer id) {
        List<String> logs = taskService.getTaskLog(id);
        Map<String, List<String>> result = new HashMap<>();
        result.put("logs", logs);
        return ApiResponse.success(result);
    }

    /**
     * 删除任务接口（修复：获取真实登录用户）
     */
    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteTask(@PathVariable Integer id,
                                          @RequestHeader("Authorization") String token) { // 新增Token参数
        try {
            // 核心：从Token解析真实用户名并设置到SecurityContext
            String currentUser = getRealUsernameFromTokenAndSetContext(token);
            
            // 先查询任务信息（用于日志）
            Task oldTask = taskService.getById(id);
            boolean success = taskService.deleteTask(id);
            if (success) {
                // 记录操作日志（使用真实用户名）
                sysLogService.saveLog(
                        currentUser,
                        "删除调度任务",
                        "删除任务-" + (oldTask != null ? oldTask.getTaskName() : "ID:" + id)
                    );
                
                return ApiResponse.success("任务删除成功");
            } else {
                return ApiResponse.error("任务不存在，删除失败");
            }
        } catch (Exception e) {
            log.error("删除任务异常", e);
            return ApiResponse.error("删除任务失败：" + e.getMessage());
        }
    }

    // ===================== 核心工具方法 =====================
    /**
     * 从Token解析真实用户名并设置到SecurityContext
     */
    private String getRealUsernameFromTokenAndSetContext(String token) {
        try {
            // 1. 解码URL编码的Token
            token = java.net.URLDecoder.decode(token, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Token解码失败，请重新登录");
        }

        // 2. 校验Token格式
        if (!StringUtils.hasText(token) || !token.startsWith("auto-test-token-")) {
            throw new RuntimeException("Token无效，请重新登录");
        }

        // 3. 拆分Token（格式：auto-test-token-张三-1771481838993）
        String[] tokenParts = token.split("-");
        if (tokenParts.length < 5) {
            throw new RuntimeException("Token格式错误，请重新登录");
        }

        // 4. 提取用户名（索引3）
        String realUsername = tokenParts[3];
        // System.out.println("TaskController解析出的用户名：" + realUsername); // 调试用

        // 5. 校验用户是否存在
        User user = userService.getByUsername(realUsername);
        if (user == null) {
            throw new RuntimeException("操作人不存在，请重新登录：" + realUsername);
        }

        // 6. 设置到SecurityContext
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                realUsername,
                null,
                java.util.Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return realUsername;
    }
}