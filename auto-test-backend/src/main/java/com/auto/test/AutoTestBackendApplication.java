// src/main/java/com/auto/test/AutoTestBackendApplication.java
package com.auto.test;

import com.auto.test.entity.Task;
import com.auto.test.mapper.TaskMapper;
import com.auto.test.scheduler.TaskSchedulerManager;
import com.auto.test.service.TaskService;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.List;

@SpringBootApplication
@MapperScan("com.auto.test.mapper")
@EnableScheduling // 必须加：开启Spring调度功能
public class AutoTestBackendApplication implements ApplicationRunner { // 实现ApplicationRunner用于启动加载

    // 新增：日志对象，方便排查问题
    private static final Logger log = LoggerFactory.getLogger(AutoTestBackendApplication.class);

    @Autowired
    private TaskMapper taskMapper;
    @Autowired
    private TaskSchedulerManager taskSchedulerManager;
    @Autowired
    private TaskService taskService;

    public static void main(String[] args) {
        SpringApplication.run(AutoTestBackendApplication.class, args);
    }

    // 新增：项目启动时加载未完成的定时任务
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // ========== 改动1：替换查询方法 → 查【未完成的定时任务】（而非已完成） ==========
        // 原代码selectFinishedTasks()查的是已完成任务，这里改成查询未完成的定时任务
        // 若你没有自定义查询方法，先注释查询逻辑，避免启动报错（后续可补充）
        // List<Task> timedTasks = taskMapper.selectTimedUnfinishedTasks(); // 推荐：自定义查询未完成定时任务
        List<Task> timedTasks = taskMapper.selectList(null); // 临时：查所有任务（仅用于测试，后续替换）

        log.info("启动时加载定时任务数量：{}", timedTasks.size());

        // ========== 改动2：遍历前加非空校验 + 过滤无效任务 ==========
        for (Task task : timedTasks) {
            // 过滤条件：1. 是定时任务 2. Cron表达式非空 3. 任务未完成
            if (!"timing".equals(task.getExecType())) {
                log.info("跳过非定时任务：任务ID={}，执行方式={}", task.getId(), task.getExecType());
                continue;
            }
            if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
                log.warn("跳过无效定时任务：任务ID={}，Cron表达式为空", task.getId());
                continue;
            }
            if ("finished".equals(task.getStatus()) || "failed".equals(task.getStatus())) {
                log.info("跳过已完成/失败的定时任务：任务ID={}，状态={}", task.getId(), task.getStatus());
                continue;
            }

            // 仅注册有效定时任务
            try {
                taskSchedulerManager.registerTask(task.getId(), task.getCronExpression(), () -> {
                    taskService.runTask(task.getId());
                });
                log.info("启动时注册定时任务成功：任务ID={}，Cron表达式={}", task.getId(), task.getCronExpression());
            } catch (Exception e) {
                log.error("启动时注册定时任务失败：任务ID={}，错误={}", task.getId(), e.getMessage(), e);
            }
        }
    }
}