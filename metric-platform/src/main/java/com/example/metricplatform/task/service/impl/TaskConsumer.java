package com.example.metricplatform.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.metricplatform.task.entity.Task;
import com.example.metricplatform.common.enums.TaskStatus;
import com.example.metricplatform.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 任务消费者
 * 定时从 Redis 队列获取任务并执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskConsumer {

    private static final int MAX_RETRY_COUNT = 3;
    private static final long POLL_INTERVAL_MS = 1000; // 1秒

    private final TaskQueueService taskQueueService;
    private final TaskExecutor taskExecutor;
    private final TaskMapper taskMapper;

    /**
     * 定时消费任务
     * 每秒执行一次，从队列获取任务并处理
     */
    @Scheduled(fixedRate = POLL_INTERVAL_MS)
    public void consumeTask() {
        // 从队列获取任务ID
        Long taskId = taskQueueService.dequeue();
        if (taskId == null) {
            return;
        }

        try {
            processTask(taskId);
        } catch (Exception e) {
            log.error("任务处理异常: taskId={}, error={}", taskId, e.getMessage(), e);
        }
    }

    /**
     * 处理单个任务
     */
    private void processTask(Long taskId) {
        // 查询任务详情
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            log.warn("任务不存在: taskId={}", taskId);
            return;
        }

        // 检查任务状态，只处理 pending 状态的任务
        if (task.getStatus() != TaskStatus.PENDING) {
            log.info("任务状态不是待执行，跳过: taskId={}, status={}", taskId, task.getStatus());
            return;
        }

        // 更新任务状态为运行中
        updateTaskToRunning(task);

        try {
            // 执行任务
            String result = taskExecutor.execute(task);

            // 更新任务状态为成功
            updateTaskToSuccess(task, result);

        } catch (Exception e) {
            log.error("任务执行失败: taskId={}, error={}", taskId, e.getMessage(), e);

            // 处理失败情况
            handleTaskFailure(task, e.getMessage());
        }
    }

    /**
     * 更新任务状态为运行中
     */
    private void updateTaskToRunning(Task task) {
        LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Task::getId, task.getId())
                .set(Task::getStatus, TaskStatus.RUNNING)
                .set(Task::getStartedAt, LocalDateTime.now());

        taskMapper.update(null, updateWrapper);
        log.info("任务状态更新为运行中: taskId={}", task.getId());
    }

    /**
     * 更新任务状态为成功
     */
    private void updateTaskToSuccess(Task task, String result) {
        LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Task::getId, task.getId())
                .set(Task::getStatus, TaskStatus.SUCCESS)
                .set(Task::getResultData, truncateResult(result))
                .set(Task::getFinishedAt, LocalDateTime.now());

        taskMapper.update(null, updateWrapper);
        log.info("任务执行成功: taskId={}", task.getId());
    }

    /**
     * 处理任务失败
     */
    private void handleTaskFailure(Task task, String errorMessage) {
        int currentRetryCount = task.getRetryCount() != null ? task.getRetryCount() : 0;
        int newRetryCount = currentRetryCount + 1;

        if (newRetryCount < MAX_RETRY_COUNT) {
            // 重试次数未达到上限，重新加入队列
            log.info("任务将重试: taskId={}, retryCount={}/{}", task.getId(), newRetryCount, MAX_RETRY_COUNT);

            // 更新重试次数
            LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Task::getId, task.getId())
                    .set(Task::getStatus, TaskStatus.PENDING)
                    .set(Task::getRetryCount, newRetryCount)
                    .set(Task::getErrorMessage, truncateMessage(errorMessage))
                    .set(Task::getStartedAt, null);

            taskMapper.update(null, updateWrapper);

            // 重新加入队列
            taskQueueService.enqueue(task);
        } else {
            // 重试次数已达上限，标记为失败
            log.info("任务重试次数已达上限，标记为失败: taskId={}, retryCount={}", task.getId(), newRetryCount);

            LambdaUpdateWrapper<Task> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(Task::getId, task.getId())
                    .set(Task::getStatus, TaskStatus.FAILED)
                    .set(Task::getRetryCount, newRetryCount)
                    .set(Task::getErrorMessage, truncateMessage(errorMessage))
                    .set(Task::getFinishedAt, LocalDateTime.now());

            taskMapper.update(null, updateWrapper);
        }
    }

    /**
     * 截断结果数据，防止数据过长
     */
    private String truncateResult(String result) {
        if (result == null) {
            return null;
        }
        int maxLength = 10000;
        if (result.length() > maxLength) {
            return result.substring(0, maxLength) + "...[truncated]";
        }
        return result;
    }

    /**
     * 截断错误信息，防止信息过长
     */
    private String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        int maxLength = 500;
        if (message.length() > maxLength) {
            return message.substring(0, maxLength) + "...";
        }
        return message;
    }
}
