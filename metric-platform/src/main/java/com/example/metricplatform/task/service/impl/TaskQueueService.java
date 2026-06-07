package com.example.metricplatform.task.service.impl;

import com.example.metricplatform.task.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 任务队列服务
 * 使用本地内存队列实现任务队列
 */
@Slf4j
@Service
public class TaskQueueService {

    private final LinkedBlockingQueue<Long> taskQueue = new LinkedBlockingQueue<>();

    /**
     * 将任务加入队列
     * @param task 任务对象
     */
    public void enqueue(Task task) {
        try {
            taskQueue.offer(task.getId());
            log.info("任务已加入队列: taskId={}, taskCode={}", task.getId(), task.getTaskCode());
        } catch (Exception e) {
            log.error("任务加入队列失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
            throw new RuntimeException("任务加入队列失败", e);
        }
    }

    /**
     * 从队列取出任务ID
     * @return 任务ID，如果队列为空则返回 null
     */
    public Long dequeue() {
        try {
            Long taskId = taskQueue.poll();
            if (taskId != null) {
                log.debug("从队列取出任务: taskId={}", taskId);
                return taskId;
            }
            return null;
        } catch (Exception e) {
            log.error("从队列取出任务失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 获取队列大小
     * @return 队列中待处理的任务数量
     */
    public Long getQueueSize() {
        try {
            return (long) taskQueue.size();
        } catch (Exception e) {
            log.error("获取队列大小失败: error={}", e.getMessage(), e);
            return 0L;
        }
    }
}
