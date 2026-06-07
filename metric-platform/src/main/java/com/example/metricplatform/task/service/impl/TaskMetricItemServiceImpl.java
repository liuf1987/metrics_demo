package com.example.metricplatform.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.metricplatform.task.entity.TaskMetricItem;
import com.example.metricplatform.common.enums.TaskMetricItemStatus;
import com.example.metricplatform.task.mapper.TaskMetricItemMapper;
import com.example.metricplatform.task.service.TaskMetricItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务子指标服务实现
 */
@Slf4j
@Service
public class TaskMetricItemServiceImpl extends ServiceImpl<TaskMetricItemMapper, TaskMetricItem> implements TaskMetricItemService {

    @Override
    public void createForTask(Long taskId, List<String> metricCodes, List<String> metricNames) {
        if (metricCodes == null || metricCodes.isEmpty()) {
            return;
        }

        List<TaskMetricItem> items = new ArrayList<>();
        for (int i = 0; i < metricCodes.size(); i++) {
            TaskMetricItem item = new TaskMetricItem();
            item.setTaskId(taskId);
            
            // 确保metricCode不为null
            String metricCode = metricCodes.get(i);
            if (metricCode == null || metricCode.trim().isEmpty()) {
                metricCode = "unknown_metric_" + i;
            }
            item.setMetricCode(metricCode);
            
            // 如果metricNames为空或对应位置的值为null，则使用metricCode作为默认值
            String metricName = metricCode; // 默认使用metricCode
            if (metricNames != null && i < metricNames.size()) {
                String name = metricNames.get(i);
                if (name != null && !name.trim().isEmpty()) {
                    metricName = name;
                }
            }
            item.setMetricName(metricName);
            
            item.setSortOrder(i);
            item.setStatus(TaskMetricItemStatus.PENDING);
            items.add(item);
            
            log.debug("创建子指标: taskId={}, metricCode={}, metricName={}", taskId, metricCode, metricName);
        }

        saveBatch(items);
        log.info("为任务创建子指标完成: taskId={}, count={}", taskId, items.size());
    }

    @Override
    public List<TaskMetricItem> getByTaskId(Long taskId) {
        return lambdaQuery()
                .eq(TaskMetricItem::getTaskId, taskId)
                .orderByAsc(TaskMetricItem::getSortOrder)
                .list();
    }

    @Override
    public void updateToRunning(Long taskId, String metricCode) {
        TaskMetricItem item = getByTaskIdAndMetricCode(taskId, metricCode);
        if (item == null) {
            log.warn("未找到子指标: taskId={}, metricCode={}", taskId, metricCode);
            return;
        }

        item.setStatus(TaskMetricItemStatus.RUNNING);
        item.setStartedAt(LocalDateTime.now());
        updateById(item);

        log.info("子指标开始执行: taskId={}, metricCode={}", taskId, metricCode);
    }

    @Override
    public void updateToSuccess(Long taskId, String metricCode, String resultData, Integer durationMs) {
        TaskMetricItem item = getByTaskIdAndMetricCode(taskId, metricCode);
        if (item == null) {
            log.warn("未找到子指标: taskId={}, metricCode={}", taskId, metricCode);
            return;
        }

        item.setStatus(TaskMetricItemStatus.SUCCESS);
        item.setResultData(resultData);
        item.setFinishedAt(LocalDateTime.now());
        
        // 计算执行时长
        if (item.getStartedAt() != null) {
            Duration duration = Duration.between(item.getStartedAt(), item.getFinishedAt());
            item.setExecuteDuration((int) duration.toMillis());
        } else if (durationMs != null) {
            item.setExecuteDuration(durationMs);
        }

        updateById(item);
        log.info("子指标执行成功: taskId={}, metricCode={}", taskId, metricCode);
    }

    @Override
    public void updateToFailed(Long taskId, String metricCode, String errorMessage) {
        TaskMetricItem item = getByTaskIdAndMetricCode(taskId, metricCode);
        if (item == null) {
            log.warn("未找到子指标: taskId={}, metricCode={}", taskId, metricCode);
            return;
        }

        item.setStatus(TaskMetricItemStatus.FAILED);
        item.setErrorMessage(errorMessage);
        item.setFinishedAt(LocalDateTime.now());
        
        // 计算执行时长
        if (item.getStartedAt() != null) {
            Duration duration = Duration.between(item.getStartedAt(), item.getFinishedAt());
            item.setExecuteDuration((int) duration.toMillis());
        }

        updateById(item);
        log.warn("子指标执行失败: taskId={}, metricCode={}, error={}", taskId, metricCode, errorMessage);
    }

    @Override
    public boolean isAllCompleted(Long taskId) {
        long totalCount = lambdaQuery()
                .eq(TaskMetricItem::getTaskId, taskId)
                .count();

        if (totalCount == 0) {
            return true;
        }

        long completedCount = lambdaQuery()
                .eq(TaskMetricItem::getTaskId, taskId)
                .in(TaskMetricItem::getStatus, TaskMetricItemStatus.SUCCESS, TaskMetricItemStatus.FAILED)
                .count();

        return totalCount == completedCount;
    }

    @Override
    public boolean hasFailed(Long taskId) {
        return lambdaQuery()
                .eq(TaskMetricItem::getTaskId, taskId)
                .eq(TaskMetricItem::getStatus, TaskMetricItemStatus.FAILED)
                .count() > 0;
    }

    /**
     * 根据任务ID和指标编码查询子指标
     */
    private TaskMetricItem getByTaskIdAndMetricCode(Long taskId, String metricCode) {
        return lambdaQuery()
                .eq(TaskMetricItem::getTaskId, taskId)
                .eq(TaskMetricItem::getMetricCode, metricCode)
                .one();
    }
}
