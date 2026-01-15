package com.auto.test.controller;

import com.auto.test.entity.Task;
import com.auto.test.mapper.TaskMapper;
import com.auto.test.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {
    @Resource
    private TaskMapper taskMapper;

    /**
     * 分页获取任务列表（适配前端分页，无报错版本）
     */
    @GetMapping("/list")
    public ApiResponse<Map<String, Object>> getTaskList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        // 1. 查询所有任务（暂不做复杂分页，前端分页仅做前端展示）
        List<Task> allTasks = taskMapper.selectList(null);
        
        // 2. 构建前端需要的分页数据（用Map封装，避免匿名对象报错）
        Map<String, Object> pageData = new HashMap<>();
        pageData.put("records", allTasks); // 任务列表
        pageData.put("total", allTasks.size()); // 总条数
        pageData.put("pageNum", pageNum); // 当前页
        pageData.put("pageSize", pageSize); // 每页条数
        
        // 3. 返回统一响应
        return ApiResponse.success(pageData);
    }

    /**
     * 获取已完成任务列表（供测试报告下拉框使用）
     */
    @GetMapping("/finished-list")
    public ApiResponse<List<Task>> getFinishedTasks() {
        List<Task> finishedTasks = taskMapper.selectFinishedTasks();
        return ApiResponse.success(finishedTasks);
    }
}