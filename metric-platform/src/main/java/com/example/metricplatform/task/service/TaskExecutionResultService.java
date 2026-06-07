package com.example.metricplatform.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.metricplatform.task.entity.Task;
import com.example.metricplatform.task.entity.TaskExecutionResult;
import com.example.metricplatform.common.enums.TaskStatus;

import java.util.List;

/**
 * 任务执行结果服务接口
 */
public interface TaskExecutionResultService extends IService<TaskExecutionResult> {

    /**
     * 创建任务执行结果记录（开始执行时调用）
     */
    TaskExecutionResult createExecutionStart(Task task, String executionId);

    /**
     * 更新任务执行结果为成功
     */
    void updateToSuccess(Long executionResultId, String resultData);

    /**
     * 更新任务执行结果为失败
     */
    void updateToFailed(Long executionResultId, String errorMessage);

    /**
     * 查询任务的所有执行历史
     */
    List<TaskExecutionResult> getByTaskId(Long taskId);

    /**
     * 查询任务的所有执行历史（分页）
     */
    Page<TaskExecutionResult> pageByTaskId(Long taskId, Integer pageNum, Integer pageSize);

    /**
     * 根据任务编码查询执行历史
     */
    List<TaskExecutionResult> getByTaskCode(String taskCode);

    /**
     * 分页查询执行历史（支持按任务编码和状态筛选）
     */
    Page<TaskExecutionResult> page(Integer pageNum, Integer pageSize, String taskCode, TaskStatus status);

    /**
     * 获取最新的执行结果
     */
    TaskExecutionResult getLatestByTaskId(Long taskId);
}
