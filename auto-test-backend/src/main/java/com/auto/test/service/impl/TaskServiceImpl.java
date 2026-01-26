package com.auto.test.service.impl;

import com.auto.test.entity.Task;
import com.auto.test.mapper.TaskMapper;
import com.auto.test.mapper.TestResultMapper;
import com.auto.test.service.TaskService;
import com.auto.test.service.TestCaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.text.DecimalFormat;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {
    // 日志对象（确保导入org.slf4j.Logger/LoggerFactory）
    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private TestCaseService testCaseService; // 关联批量执行用例的Service
    @Autowired
    private TestResultMapper testResultMapper; // 新增：统计任务结果

    @Override
    public boolean createTask(Task task) {
        task.setStatus("pending"); // 默认待执行
        task.setCreateBy("admin"); // 默认创建人
        boolean isSuccess = taskMapper.insertTask(task) > 0;
        if (isSuccess) {
            log.info("【任务创建成功】任务名称={}", task.getTaskName());
        } else {
            log.error("【任务创建失败】任务名称={}", task.getTaskName());
        }
        return isSuccess;
    }

    @Override
    public boolean runTask(Integer taskId) {
        // 1. 查询任务
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            // 修复：用占位符传taskId，符合SLF4J标准写法
            log.error("【执行任务失败】任务ID={}，任务不存在", taskId);
            return false;
        }
        
        // 2. 更新任务状态为“执行中”
        taskMapper.updateTaskStatus(taskId, "running");
        log.info("【执行任务开始】任务ID={}，任务名称={}，关联用例ID={}", 
                 taskId, task.getTaskName(), task.getCaseId());
        
        try {
            // 关键：把String类型的caseId（逗号分隔）转成Long列表，匹配TestCaseService的Long类型
            List<Long> caseIds = Arrays.stream(task.getCaseId().split(","))
                    .map(Long::parseLong) // 用Long.parseLong，匹配你现有用例ID的Long类型
                    .collect(Collectors.toList());
            
            // 调用批量执行方法，默认用edge浏览器（和你单条用例执行默认一致）
            testCaseService.batchRunTestCases(caseIds, "edge", taskId.longValue());
            
            // 4. 执行完成后更新状态为“已完成”
            taskMapper.updateTaskStatus(taskId, "finished");
            log.info("【执行任务成功】任务ID={}，所有关联用例执行完成", taskId);
            return true;
        } catch (Exception e) {
            // 修复：异常对象放最后一位，符合SLF4J标准写法
            log.error("【执行任务失败】任务ID={}，执行异常：{}", taskId, e.getMessage(), e);
            // 执行异常，更新状态为“失败”
            taskMapper.updateTaskStatus(taskId, "failed");
            return false;
        }
    }

    @Override
    public boolean stopTask(Integer taskId) {
        // 实际项目需终止Selenium进程，这里简化为更新状态为“失败”
        boolean isSuccess = taskMapper.updateTaskStatus(taskId, "failed") > 0;
        if (isSuccess) {
            log.info("【终止任务成功】任务ID={}", taskId);
        } else {
            log.error("【终止任务失败】任务ID={}，更新状态失败", taskId);
        }
        return isSuccess;
    }

    @Override
    public List<String> getTaskLog(Integer taskId) {
        // 实际项目需从日志文件/数据库读取，这里返回模拟日志（匹配前端日志展示）
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            log.error("【获取任务日志失败】任务ID={}，任务不存在", taskId);
            return Collections.singletonList("任务不存在，无法获取日志");
        }

        List<String> logs = new ArrayList<>();
        logs.add(String.format("[%s] 任务开始执行：%s", new Date(), task.getTaskName()));
        logs.add(String.format("关联用例ID：%s，执行方式：%s", task.getCaseId(), 
                "immediate".equals(task.getExecType()) ? "立即执行" : "定时执行"));
        // 拆分用例ID，模拟每个用例的执行日志
        Arrays.stream(task.getCaseId().split(","))
                .forEach(cid -> logs.add(String.format("执行用例ID：%s → 执行成功", cid)));
        logs.add(String.format("[%s] 任务执行完成，当前状态：%s", new Date(), task.getStatus()));
        return logs;
    }

   @Override
public List<Task> listFinishedTasks() {
    List<Task> finished = this.lambdaQuery().eq(Task::getStatus, "finished").list();
    if (finished == null || finished.isEmpty()) {
        return finished;
    }
    DecimalFormat df = new DecimalFormat("0.00");
    for (Task task : finished) {
        String caseIdStr = task.getCaseId();
        // 1. 配置的总用例数（保留，用于展示）
        List<String> idList = (caseIdStr == null || caseIdStr.trim().isEmpty())
                ? Collections.emptyList()
                : Arrays.stream(caseIdStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
        int configTotal = idList.size(); // 配置的用例数
        task.setTotalCase(configTotal);  // 展示配置的总用例数

        // 2. 关键修复：提前提取任务ID，严格空值处理，避免查全部
        Long currentTaskId = task.getId() != null ? task.getId().longValue() : null;
        Integer success = 0;
        Integer fail = 0;
        // 只有任务ID非空时，才查询该任务的实际执行结果
        if (currentTaskId != null) {
            success = Optional.ofNullable(testResultMapper.countSuccessByTaskId(currentTaskId)).orElse(0);
            fail = Optional.ofNullable(testResultMapper.countFailByTaskId(currentTaskId)).orElse(0);
        }
        task.setSuccessCase(success);
        task.setFailCase(fail);

        // 3. 核心修复：用【实际执行的用例数】做分母计算成功率，避免除0
        int actualExecuteTotal = success + fail; // 实际执行的总用例数
        if (actualExecuteTotal > 0) {
            double rate = (double) success * 100 / actualExecuteTotal; // 正确公式：成功数/实际执行总数*100%
            task.setSuccessRate(df.format(rate) + "%");
        } else {
            task.setSuccessRate("0.00%"); // 未实际执行任何用例，成功率0
        }
    }
    return finished;
}
}