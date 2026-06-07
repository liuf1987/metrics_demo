package com.example.metricplatform.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.metricplatform.task.entity.Task;
import com.example.metricplatform.common.enums.ExecuteMode;
import com.example.metricplatform.common.enums.TaskStatus;
import com.example.metricplatform.task.mapper.TaskMapper;
import com.example.metricplatform.task.service.TaskService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 定时任务调度服务
 * 负责管理定时任务的注册、执行、暂停、继续和取消
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskSchedulerService {

    private final TaskScheduler taskScheduler;
    private final TaskQueueService taskQueueService;
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    /**
     * 存储已注册的定时任务 key: schedulerId, value: ScheduledFuture
     */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    /**
     * 存储暂停的任务 key: taskCode, value: Task
     */
    private final Map<String, Task> pausedTasks = new ConcurrentHashMap<>();

    /**
     * 启动时恢复定时任务
     */
    @PostConstruct
    public void init() {
        log.info("初始化定时任务调度服务，恢复已存在的定时任务");
        // 从数据库加载所有定时任务并注册（排除已暂停和已取消的）
        taskService.loadScheduledTasks().forEach(task -> {
            if (TaskStatus.PAUSED.equals(task.getStatus())) {
                pausedTasks.put(task.getTaskCode(), task);
                log.info("任务已暂停，暂不注册调度: taskCode={}", task.getTaskCode());
            } else if (!TaskStatus.CANCELLED.equals(task.getStatus())) {
                scheduleTask(task);
            }
        });
    }

    /**
     * 销毁时取消所有定时任务
     */
    @PreDestroy
    public void destroy() {
        log.info("销毁定时任务调度服务，取消所有定时任务");
        scheduledTasks.forEach((schedulerId, future) -> {
            try {
                future.cancel(true);
            } catch (Exception e) {
                log.warn("取消定时任务失败: schedulerId={}", schedulerId);
            }
        });
        scheduledTasks.clear();
        pausedTasks.clear();
    }

    /**
     * 注册定时任务
     * @param task 任务对象
     */
    public void scheduleTask(Task task) {
        if (task.getExecuteMode() != ExecuteMode.SCHEDULED) {
            log.warn("任务不是定时任务，跳过注册: taskCode={}", task.getTaskCode());
            return;
        }

        if (task.getCronExpression() == null || task.getCronExpression().trim().isEmpty()) {
            log.warn("定时任务没有配置Cron表达式: taskCode={}", task.getTaskCode());
            return;
        }

        // 生成调度ID
        String schedulerId = generateSchedulerId(task.getTaskCode());
        
        // 如果已存在该任务的调度，先取消
        cancelTask(schedulerId);

        // 创建定时任务
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> executeScheduledTask(task),
                new CronTrigger(task.getCronExpression())
        );

        scheduledTasks.put(schedulerId, future);
        
        // 更新任务的调度ID和下次执行时间
        task.setSchedulerId(schedulerId);
        task.setNextRunTime(calculateNextRunTime(task.getCronExpression()));
        taskService.updateById(task);

        log.info("定时任务注册成功: taskCode={}, schedulerId={}, cron={}", 
                task.getTaskCode(), schedulerId, task.getCronExpression());
    }

    /**
     * 暂停定时任务
     * @param taskCode 任务编码
     * @return 操作结果
     */
    public boolean pauseTask(String taskCode) {
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>().eq(Task::getTaskCode, taskCode)
        );
        if (task == null) {
            log.warn("任务不存在，无法暂停: taskCode={}", taskCode);
            return false;
        }

        if (task.getExecuteMode() != ExecuteMode.SCHEDULED) {
            log.warn("不是定时任务，无法暂停: taskCode={}", taskCode);
            return false;
        }

        if (TaskStatus.PAUSED.equals(task.getStatus())) {
            log.warn("任务已经是暂停状态: taskCode={}", taskCode);
            return false;
        }

        // 取消调度
        String schedulerId = generateSchedulerId(taskCode);
        cancelTask(schedulerId);

        // 更新任务状态为暂停
        task.setStatus(TaskStatus.PAUSED);
        task.setSchedulerId(null);
        taskService.updateById(task);

        // 保存到暂停任务列表
        pausedTasks.put(taskCode, task);

        log.info("定时任务已暂停: taskCode={}", taskCode);
        return true;
    }

    /**
     * 继续定时任务
     * @param taskCode 任务编码
     * @return 操作结果
     */
    public boolean resumeTask(String taskCode) {
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>().eq(Task::getTaskCode, taskCode)
        );
        if (task == null) {
            log.warn("任务不存在，无法继续: taskCode={}", taskCode);
            return false;
        }

        if (task.getExecuteMode() != ExecuteMode.SCHEDULED) {
            log.warn("不是定时任务，无法继续: taskCode={}", taskCode);
            return false;
        }

        if (!TaskStatus.PAUSED.equals(task.getStatus())) {
            log.warn("任务不是暂停状态，无法继续: taskCode={}", taskCode);
            return false;
        }

        // 从暂停列表移除
        pausedTasks.remove(taskCode);

        // 重新注册调度
        task.setStatus(TaskStatus.PENDING);
        scheduleTask(task);

        log.info("定时任务已继续: taskCode={}", taskCode);
        return true;
    }

    /**
     * 取消/停止定时任务
     * @param taskCode 任务编码
     * @return 操作结果
     */
    public boolean cancelTaskByCode(String taskCode) {
        Task task = taskMapper.selectOne(
            new LambdaQueryWrapper<Task>().eq(Task::getTaskCode, taskCode)
        );
        if (task == null) {
            log.warn("任务不存在，无法取消: taskCode={}", taskCode);
            return false;
        }

        // 取消调度
        String schedulerId = generateSchedulerId(taskCode);
        cancelTask(schedulerId);

        // 从暂停列表移除
        pausedTasks.remove(taskCode);

        // 更新任务状态为已取消
        task.setStatus(TaskStatus.CANCELLED);
        task.setSchedulerId(null);
        task.setNextRunTime(null);
        taskService.updateById(task);

        log.info("定时任务已取消: taskCode={}", taskCode);
        return true;
    }

    /**
     * 执行定时任务
     */
    private void executeScheduledTask(Task task) {
        // 检查任务是否被暂停或取消
        if (TaskStatus.PAUSED.equals(task.getStatus()) || TaskStatus.CANCELLED.equals(task.getStatus())) {
            log.info("任务已暂停或取消，跳过执行: taskCode={}", task.getTaskCode());
            return;
        }

        log.info("定时任务触发执行: taskCode={}, cron={}", task.getTaskCode(), task.getCronExpression());
        
        try {
            // 将任务加入队列执行
            taskQueueService.enqueue(task);
            
            // 更新下次执行时间
            task.setNextRunTime(calculateNextRunTime(task.getCronExpression()));
            taskService.updateById(task);
            
            log.info("定时任务已加入队列: taskCode={}", task.getTaskCode());
        } catch (Exception e) {
            log.error("定时任务执行失败: taskCode={}, error={}", task.getTaskCode(), e.getMessage(), e);
        }
    }

    /**
     * 取消定时任务
     * @param schedulerId 调度ID
     */
    public void cancelTask(String schedulerId) {
        ScheduledFuture<?> future = scheduledTasks.remove(schedulerId);
        if (future != null) {
            future.cancel(true);
            log.info("定时任务已取消: schedulerId={}", schedulerId);
        }
    }

    /**
     * 检查任务是否已调度
     * @param taskCode 任务编码
     * @return true表示已调度
     */
    public boolean isScheduled(String taskCode) {
        String schedulerId = generateSchedulerId(taskCode);
        return scheduledTasks.containsKey(schedulerId);
    }

    /**
     * 检查任务是否已暂停
     * @param taskCode 任务编码
     * @return true表示已暂停
     */
    public boolean isPaused(String taskCode) {
        return pausedTasks.containsKey(taskCode);
    }

    /**
     * 生成调度ID
     */
    private String generateSchedulerId(String taskCode) {
        return "SCHED_" + taskCode;
    }

    /**
     * 计算下次执行时间
     */
    private LocalDateTime calculateNextRunTime(String cronExpression) {
        try {
            CronExpression cron = CronExpression.parse(cronExpression);
            LocalDateTime now = LocalDateTime.now();
            return cron.next(now);
        } catch (Exception e) {
            log.error("计算下次执行时间失败: cron={}, error={}", cronExpression, e.getMessage());
            return null;
        }
    }
}
