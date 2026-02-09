// src/main/java/com/auto/test/scheduler/TaskSchedulerManager.java
package com.auto.test.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
// 关键修改：替换 javax.annotation.PostConstruct → jakarta.annotation.PostConstruct
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Component
public class TaskSchedulerManager {
    // 日志对象
    private static final Logger log = LoggerFactory.getLogger(TaskSchedulerManager.class);
    // 线程池调度器
    private final ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    // 存储已注册的定时任务（key=taskId，你的ID是Integer类型！）
    private final Map<Integer, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 初始化线程池
        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("task-scheduler-");
        taskScheduler.initialize();
    }

    /**
     * 注册定时任务（适配你的Integer类型ID）
     * @param taskId 任务ID（Integer）
     * @param cron Cron表达式
     * @param runnable 任务执行逻辑
     */
    public void registerTask(Integer taskId, String cron, Runnable runnable) {
        // 新增：Cron表达式非空校验
        if (cron == null || cron.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron表达式不能为空！");
        }
        // 先取消旧任务，避免重复
        cancelTask(taskId);
        // 注册新任务（指定中国时区，避免时间偏移）
        ScheduledFuture<?> future = taskScheduler.schedule(
                runnable,
                new CronTrigger(cron, TimeZone.getTimeZone("Asia/Shanghai"))
        );
        taskMap.put(taskId, future);
        log.info("定时任务注册成功，任务ID={}，Cron表达式={}", taskId, cron);
    }

    /**
     * 取消定时任务
     * @param taskId 任务ID
     */
    public void cancelTask(Integer taskId) {
        if (taskMap.containsKey(taskId)) {
            taskMap.get(taskId).cancel(false);
            taskMap.remove(taskId);
            log.info("定时任务已取消，任务ID={}", taskId);
        }
    }
}