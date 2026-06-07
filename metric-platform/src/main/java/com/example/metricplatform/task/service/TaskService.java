package com.example.metricplatform.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.metricplatform.task.dto.TaskCreateRequest;
import com.example.metricplatform.task.entity.Task;
import com.example.metricplatform.common.enums.TaskStatus;

import java.util.List;

/**
 * 任务服务接口
 */
public interface TaskService extends IService<Task> {

    /**
     * 创建任务
     */
    Task create(TaskCreateRequest request);

    /**
     * 根据编码查询任务
     */
    Task getByCode(String taskCode);

    /**
     * 更新任务状态
     */
    Task updateStatus(Long id, TaskStatus status, String resultData, String errorMessage);

    /**
     * 分页查询任务列表
     */
    Page<Task> page(Integer pageNum, Integer pageSize, String metricCode, TaskStatus status);

    /**
     * 删除任务
     */
    void delete(String taskCode);

    /**
     * 加载所有定时任务
     */
    List<Task> loadScheduledTasks();
}