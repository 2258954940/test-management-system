// src/main/java/com/auto/test/controller/TaskController.java
package com.auto.test.controller;

import com.auto.test.common.ApiResponse;
import com.auto.test.entity.Task;
import com.auto.test.scheduler.TaskSchedulerManager;
import com.auto.test.service.TaskService;
import org.slf4j.Logger; // 新增：导入日志接口
import org.slf4j.LoggerFactory; // 新增：导入日志工厂
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {
    // 新增：声明日志对象
    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private TaskService taskService;
    
    @Autowired
    private TaskSchedulerManager taskSchedulerManager;

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
     * 创建任务（核心修改：返回任务ID，支持前端自动触发立即执行）
     */
    @PostMapping("/create")
    public ApiResponse<Map<String, Object>> createTask(@RequestBody Map<String, Object> data) {
        Task task = new Task();
        // 字段映射逻辑不变
        task.setTaskName((String) data.get("taskName"));
        task.setCaseId((String) data.get("caseIds"));
        String executeType = (String) data.get("executeType");
        task.setExecType(executeType);
        
        // 接收前端的Cron表达式
        String cronExpression = (String) data.get("cronExpression");
        task.setCronExpression(cronExpression);

        // 任务基础信息
        task.setStatus("pending");
        task.setCreateBy("admin");
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

        // 构建返回结果：包含提示信息 + 新任务ID（核心修改）
        Map<String, Object> result = new HashMap<>();
        result.put("message", "任务创建成功");
        result.put("id", task.getId()); // 前端可通过createRes.data.id获取ID
        return ApiResponse.success(result);
    }

    /**
     * 运行任务（原有逻辑不变）
     */
    @PostMapping("/run/{id}")
    public ApiResponse<String> runTask(@PathVariable Integer id) {
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
            return ApiResponse.success("定时任务已注册，将按规则执行");
        }

        // 立即执行：调用原有逻辑
        boolean success = taskService.runTask(id);
        return success ? ApiResponse.success("任务开始执行") : ApiResponse.error("任务执行失败");
    }

    /**
     * 停止任务（原有逻辑不变）
     */
    @PostMapping("/stop/{id}")
    public ApiResponse<String> stopTask(@PathVariable Integer id) {
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
            return ApiResponse.success("任务已终止");
        } else {
            return ApiResponse.error("任务终止失败");
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
     * 新增：删除任务接口（适配前端POST请求）
     */
    @PostMapping("/delete/{id}")
    public ApiResponse<String> deleteTask(@PathVariable Integer id) {
        try {
            boolean success = taskService.deleteTask(id);
            if (success) {
                return ApiResponse.success("任务删除成功");
            } else {
                return ApiResponse.error("任务不存在，删除失败");
            }
        } catch (Exception e) {
            log.error("删除任务异常", e); // 现在log对象可正常使用
            return ApiResponse.error("删除任务失败：" + e.getMessage());
        }
    }
}