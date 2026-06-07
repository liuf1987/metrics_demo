package com.example.metricplatform.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.metricplatform.metric.dto.MetricQueryConfig;
import com.example.metricplatform.task.dto.TaskCreateRequest;
import com.example.metricplatform.task.entity.Task;
import com.example.metricplatform.common.enums.ExecuteMode;
import com.example.metricplatform.common.enums.TaskStatus;
import com.example.metricplatform.common.exception.BusinessException;
import com.example.metricplatform.common.exception.ErrorCode;
import com.example.metricplatform.task.mapper.TaskMapper;
import com.example.metricplatform.metric.service.MetricConfigService;
import com.example.metricplatform.task.service.TaskMetricItemService;
import com.example.metricplatform.task.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 任务服务实现
 */
@Slf4j
@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final TaskQueueService taskQueueService;
    private final TaskSchedulerService taskSchedulerService;
    private final MetricConfigService metricConfigService;
    private final TaskMetricItemService taskMetricItemService;

    public TaskServiceImpl(TaskQueueService taskQueueService,
                           @Lazy TaskSchedulerService taskSchedulerService,
                           MetricConfigService metricConfigService,
                           TaskMetricItemService taskMetricItemService) {
        this.taskQueueService = taskQueueService;
        this.taskSchedulerService = taskSchedulerService;
        this.metricConfigService = metricConfigService;
        this.taskMetricItemService = taskMetricItemService;
    }

    @Override
    @Transactional
    public Task create(TaskCreateRequest request) {
        // 验证参数
        validateRequest(request);

        // 创建任务对象
        Task task = new Task();
        task.setTaskCode(generateTaskCode());
        task.setTaskType(request.getTaskType());
        task.setExecuteMode(request.getExecuteMode());
        task.setCronExpression(request.getCronExpression());
        task.setTaskName(request.getTaskName());
        task.setTaskDesc(request.getTaskDesc());
        task.setMetricConfigs(request.getMetricConfigs());
        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(0);

        // 先入库
        save(task);

        // 创建子任务指标
        if (request.getMetricConfigs() != null && !request.getMetricConfigs().isEmpty()) {
            List<String> metricCodes = request.getMetricConfigs().stream()
                    .map(MetricQueryConfig::getMetricCode)
                    .toList();
            List<String> metricNames = request.getMetricConfigs().stream()
                    .map(MetricQueryConfig::getMetricName)
                    .toList();
            taskMetricItemService.createForTask(task.getId(), metricCodes, metricNames);
        }

        log.info("任务已入库: taskCode={}, executeMode={}", task.getTaskCode(), task.getExecuteMode());

        // 根据执行模式处理
        if (ExecuteMode.INSTANT.equals(request.getExecuteMode())) {
            // 即时执行：加入队列
            taskQueueService.enqueue(task);
            log.info("即时任务已加入队列: taskCode={}", task.getTaskCode());
        } else if (ExecuteMode.SCHEDULED.equals(request.getExecuteMode())) {
            // 定时执行：注册到调度器
            taskSchedulerService.scheduleTask(task);
            log.info("定时任务已注册到调度器: taskCode={}, cron={}", task.getTaskCode(), task.getCronExpression());
        }

        return task;
    }

    /**
     * 验证请求参数
     */
    private void validateRequest(TaskCreateRequest request) {
        // 验证所有指标是否存在且可用
        for (MetricQueryConfig config : request.getMetricConfigs()) {
            try {
                metricConfigService.getByCode(config.getMetricCode());
            } catch (BusinessException e) {
                throw e;
            }
        }

        // 如果是定时任务，必须配置cron表达式
        if (ExecuteMode.SCHEDULED.equals(request.getExecuteMode()) 
                && !StringUtils.hasText(request.getCronExpression())) {
            log.warn("定时任务缺少cron表达式");
            throw ErrorCode.TASK_CRON_REQUIRED.exception();
        }
    }

    @Override
    public Task getByCode(String taskCode) {
        Task task = lambdaQuery()
                .eq(Task::getTaskCode, taskCode)
                .one();
        if (task == null) {
            log.warn("任务不存在，taskCode: {}", taskCode);
            throw ErrorCode.TASK_NOT_FOUND_BY_CODE.exception(taskCode);
        }
        return task;
    }

    @Override
    public Task updateStatus(Long id, TaskStatus status, String resultData, String errorMessage) {
        Task task = getById(id);
        if (task == null) {
            log.warn("任务不存在，id: {}", id);
            throw ErrorCode.TASK_NOT_FOUND_BY_ID.exception(id);
        }

        task.setStatus(status);
        
        if (status == TaskStatus.RUNNING && task.getStartedAt() == null) {
            task.setStartedAt(LocalDateTime.now());
        }
        
        if (status == TaskStatus.SUCCESS || status == TaskStatus.FAILED) {
            task.setFinishedAt(LocalDateTime.now());
        }
        
        if (resultData != null) {
            task.setResultData(resultData);
        }
        
        if (errorMessage != null) {
            task.setErrorMessage(errorMessage);
        }

        updateById(task);
        return task;
    }

    @Override
    public Page<Task> page(Integer pageNum, Integer pageSize, String metricCode, TaskStatus status) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(metricCode)) {
            wrapper.apply("JSON_CONTAINS(metric_configs, CONCAT('{\"metricCode\":\"', {0}, '\"}'))", metricCode);
        }
        if (status != null) {
            wrapper.eq(Task::getStatus, status);
        }
        
        wrapper.orderByDesc(Task::getCreateTime);
        
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    @Transactional
    public void delete(String taskCode) {
        Task task = getByCode(taskCode);
        if (task == null) {
            log.warn("任务不存在，taskCode: {}", taskCode);
            throw ErrorCode.TASK_NOT_FOUND_BY_CODE.exception(taskCode);
        }

        // 如果是定时任务，先取消调度
        if (ExecuteMode.SCHEDULED.equals(task.getExecuteMode())) {
            taskSchedulerService.cancelTaskByCode(taskCode);
        }

        removeById(task.getId());
        log.info("任务已删除: taskCode={}", taskCode);
    }

    @Override
    public List<Task> loadScheduledTasks() {
        return lambdaQuery()
                .eq(Task::getExecuteMode, ExecuteMode.SCHEDULED)
                .isNotNull(Task::getCronExpression)
                .list();
    }

    /**
     * 生成任务编码
     */
    private String generateTaskCode() {
        return "TASK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}