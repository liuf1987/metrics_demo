package com.example.metricplatform.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.metricplatform.task.entity.TaskMetricItem;

import java.util.List;

/**
 * 任务子指标服务接口
 */
public interface TaskMetricItemService extends IService<TaskMetricItem> {

    /**
     * 为任务创建子指标列表
     *
     * @param taskId 任务ID
     * @param metricCodes 指标编码列表
     * @param metricNames 指标名称列表（与编码列表对应）
     */
    void createForTask(Long taskId, List<String> metricCodes, List<String> metricNames);

    /**
     * 查询任务的所有子指标
     *
     * @param taskId 任务ID
     * @return 子指标列表
     */
    List<TaskMetricItem> getByTaskId(Long taskId);

    /**
     * 更新子指标状态为执行中
     *
     * @param taskId 任务ID
     * @param metricCode 指标编码
     */
    void updateToRunning(Long taskId, String metricCode);

    /**
     * 更新子指标状态为成功
     *
     * @param taskId 任务ID
     * @param metricCode 指标编码
     * @param resultData 执行结果
     * @param durationMs 执行耗时(毫秒)
     */
    void updateToSuccess(Long taskId, String metricCode, String resultData, Integer durationMs);

    /**
     * 更新子指标状态为失败
     *
     * @param taskId 任务ID
     * @param metricCode 指标编码
     * @param errorMessage 错误信息
     */
    void updateToFailed(Long taskId, String metricCode, String errorMessage);

    /**
     * 查询任务的子指标是否全部完成
     *
     * @param taskId 任务ID
     * @return 是否全部完成
     */
    boolean isAllCompleted(Long taskId);

    /**
     * 查询任务是否有失败的子指标
     *
     * @param taskId 任务ID
     * @return 是否有失败
     */
    boolean hasFailed(Long taskId);
}
