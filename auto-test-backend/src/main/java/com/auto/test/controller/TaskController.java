// src/main/java/com/auto/test/controller/TaskController.java
package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.Task;
import com.auto.test.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

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
     * 创建任务
     */
    @PostMapping("/create")
    public ApiResponse<String> createTask(@RequestBody Map<String, Object> data) {
        Task task = new Task();
        task.setTaskName((String) data.get("name"));
        task.setCaseId((String) data.get("caseIds")); // 前端传的是逗号分隔的字符串
        task.setExecType((String) data.get("mode"));
        
        // 处理定时任务的Cron表达式（简单示例：把date+time转成Cron）
        if ("timed".equals(task.getExecType())) {
            Date date = (Date) data.get("date");
            Date time = (Date) data.get("time");
            SimpleDateFormat sdfDate = new SimpleDateFormat("dd MM HH mm");
            String[] dt = sdfDate.format(new Date(date.getTime() + time.getTime())).split(" ");
            task.setCronExpression(dt[3] + " " + dt[2] + " " + dt[1] + " " + dt[0] + " * ?");
        }

        boolean success = taskService.createTask(task);
        return success ? ApiResponse.success("任务创建成功") : ApiResponse.error("任务创建失败");
    }

    /**
     * 运行任务
     */
    @PostMapping("/run/{id}")
    public ApiResponse<String> runTask(@PathVariable Integer id) {
        boolean success = taskService.runTask(id);
        return success ? ApiResponse.success("任务开始执行") : ApiResponse.error("任务执行失败");
    }

    /**
     * 停止任务
     */
    @PostMapping("/stop/{id}")
    public ApiResponse<String> stopTask(@PathVariable Integer id) {
        boolean success = taskService.stopTask(id);
        return success ? ApiResponse.success("任务已终止") : ApiResponse.error("任务终止失败");
    }

    /**
     * 获取任务日志
     */
    @GetMapping("/log/{id}")
    public ApiResponse<Map<String, List<String>>> getTaskLog(@PathVariable Integer id) {
        List<String> logs = taskService.getTaskLog(id);
        Map<String, List<String>> result = new HashMap<>();
        result.put("logs", logs);
        return ApiResponse.success(result);
    }
}