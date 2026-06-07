package com.example.metricplatform.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.metricplatform.task.entity.Task;
import com.example.metricplatform.task.entity.TaskExecutionResult;
import com.example.metricplatform.common.enums.TaskStatus;
import com.example.metricplatform.task.mapper.TaskExecutionResultMapper;
import com.example.metricplatform.task.service.TaskExecutionResultService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务执行结果服务实现类
 */
@Slf4j
@Service
public class TaskExecutionResultServiceImpl extends ServiceImpl<TaskExecutionResultMapper, TaskExecutionResult> implements TaskExecutionResultService {

    @Override
    public TaskExecutionResult createExecutionStart(Task task, String executionId) {
        TaskExecutionResult result = new TaskExecutionResult();
        result.setTaskId(task.getId());
        result.setTaskCode(task.getTaskCode());
        result.setExecutionId(executionId);
        result.setTaskType(task.getTaskType());
        result.setExecuteMode(task.getExecuteMode() != null ? task.getExecuteMode().name() : "instant");
        result.setStatus(TaskStatus.RUNNING);
        result.setExecutionStart(LocalDateTime.now());
        result.setRetryCount(task.getRetryCount() != null ? task.getRetryCount() : 0);
        
        save(result);
        log.info("创建任务执行记录: taskId={}, executionId={}", task.getId(), executionId);
        return result;
    }

    @Override
    public void updateToSuccess(Long executionResultId, String resultData) {
        TaskExecutionResult result = getById(executionResultId);
        if (result == null) {
            log.warn("未找到执行记录: executionResultId={}", executionResultId);
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        result.setStatus(TaskStatus.SUCCESS);
        result.setResultData(resultData);
        result.setExecutionEnd(now);
        
        // 计算执行耗时
        if (result.getExecutionStart() != null) {
            Duration duration = Duration.between(result.getExecutionStart(), now);
            result.setExecutionDuration((int) duration.toMillis());
        }
        
        updateById(result);
        log.info("任务执行成功: executionResultId={}", executionResultId);
    }

    @Override
    public void updateToFailed(Long executionResultId, String errorMessage) {
        TaskExecutionResult result = getById(executionResultId);
        if (result == null) {
            log.warn("未找到执行记录: executionResultId={}", executionResultId);
            return;
        }
        
        LocalDateTime now = LocalDateTime.now();
        result.setStatus(TaskStatus.FAILED);
        result.setErrorMessage(errorMessage);
        result.setExecutionEnd(now);
        
        // 计算执行耗时
        if (result.getExecutionStart() != null) {
            Duration duration = Duration.between(result.getExecutionStart(), now);
            result.setExecutionDuration((int) duration.toMillis());
        }
        
        updateById(result);
        log.warn("任务执行失败: executionResultId={}, error={}", executionResultId, errorMessage);
    }

    @Override
    public List<TaskExecutionResult> getByTaskId(Long taskId) {
        return lambdaQuery()
                .eq(TaskExecutionResult::getTaskId, taskId)
                .orderByDesc(TaskExecutionResult::getCreateTime)
                .list();
    }

    @Override
    public Page<TaskExecutionResult> pageByTaskId(Long taskId, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<TaskExecutionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutionResult::getTaskId, taskId)
                .orderByDesc(TaskExecutionResult::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<TaskExecutionResult> getByTaskCode(String taskCode) {
        return lambdaQuery()
                .eq(TaskExecutionResult::getTaskCode, taskCode)
                .orderByDesc(TaskExecutionResult::getCreateTime)
                .list();
    }

    @Override
    public Page<TaskExecutionResult> page(Integer pageNum, Integer pageSize, String taskCode, TaskStatus status) {
        LambdaQueryWrapper<TaskExecutionResult> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(taskCode)) {
            wrapper.eq(TaskExecutionResult::getTaskCode, taskCode);
        }
        if (status != null) {
            wrapper.eq(TaskExecutionResult::getStatus, status);
        }
        
        wrapper.orderByDesc(TaskExecutionResult::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public TaskExecutionResult getLatestByTaskId(Long taskId) {
        return lambdaQuery()
                .eq(TaskExecutionResult::getTaskId, taskId)
                .orderByDesc(TaskExecutionResult::getCreateTime)
                .last("LIMIT 1")
                .one();
    }
}
