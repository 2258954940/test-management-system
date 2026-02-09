// src/main/java/com/auto/test/service/TaskService.java
package com.auto.test.service;

import com.auto.test.entity.Task;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface TaskService extends IService<Task> {
    // 创建任务
    boolean createTask(Task task);
    // 执行任务
    boolean runTask(Integer taskId);
    // 终止任务
    boolean stopTask(Integer taskId);
    // 获取任务日志
    List<String> getTaskLog(Integer taskId);
    // 查询所有已完成任务并携带统计字段
    List<Task> listFinishedTasks();

    Task getById(Integer id);
    boolean updateById(Task task);
    // 新增：删除任务接口
    boolean deleteTask(Integer taskId);
}