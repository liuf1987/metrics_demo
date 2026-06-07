package com.example.metricplatform.task.service.impl;

import com.example.metricplatform.metric.dto.MetricExecuteRequest;
import com.example.metricplatform.metric.dto.MetricQueryConfig;
import com.example.metricplatform.task.entity.Task;
import com.example.metricplatform.task.entity.TaskExecutionResult;
import com.example.metricplatform.common.enums.TaskType;
import com.example.metricplatform.metric.service.QueryEngineService;
import com.example.metricplatform.task.service.TaskExecutionResultService;
import com.example.metricplatform.task.service.TaskMetricItemService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 任务执行器
 * 负责执行具体类型的任务
 */
@Slf4j
@Component("metricTaskExecutor")
@RequiredArgsConstructor
public class TaskExecutor {

    private final QueryEngineService queryEngineService;
    private final ObjectMapper objectMapper;
    private final TaskMetricItemService taskMetricItemService;
    private final TaskExecutionResultService taskExecutionResultService;

    /**
     * 执行任务
     * @param task 任务对象
     * @return 执行结果
     */
    public String execute(Task task) {
        TaskType taskType = task.getTaskType();
        log.info("开始执行任务: taskId={}, taskType={}", task.getId(), taskType);

        // 生成唯一执行ID
        String executionId = UUID.randomUUID().toString();
        
        // 创建执行记录
        TaskExecutionResult executionResult = taskExecutionResultService.createExecutionStart(task, executionId);
        Long executionResultId = executionResult.getId();

        try {
            String result;
            switch (taskType) {
                case QUERY:
                    result = executeQueryTask(task);
                    break;
                case EXPORT:
                    result = executeExportTask(task);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的任务类型: " + taskType);
            }
            
            // 更新执行结果为成功
            taskExecutionResultService.updateToSuccess(executionResultId, result);
            log.info("任务执行成功: taskId={}", task.getId());
            return result;
        } catch (Exception e) {
            log.error("任务执行失败: taskId={}, error={}", task.getId(), e.getMessage(), e);
            // 更新执行结果为失败
            taskExecutionResultService.updateToFailed(executionResultId, e.getMessage());
            throw e;
        }
    }

    /**
     * 执行查询任务
     * 支持执行多个指标查询，并返回所有指标的查询结果
     */
    private String executeQueryTask(Task task) {
        List<MetricQueryConfig> metricConfigs = task.getMetricConfigs();
        
        log.info("执行查询任务: taskId={}, metricCount={}", task.getId(), 
                metricConfigs != null ? metricConfigs.size() : 0);

        // 存储所有指标的查询结果
        List<Map<String, Object>> allResults = new ArrayList<>();

        if (metricConfigs != null) {
            for (MetricQueryConfig config : metricConfigs) {
                String metricCode = config.getMetricCode();
                MetricExecuteRequest request = config.toExecuteRequest();

                log.info("执行指标查询: taskId={}, metricCode={}", task.getId(), metricCode);
                
                // 更新子指标状态为执行中
                taskMetricItemService.updateToRunning(task.getId(), metricCode);
                
                try {
                    // 执行单个指标查询
                    List<Map<String, Object>> metricResult = queryEngineService.execute(metricCode, request);

                    // 包装结果，包含指标信息
                    Map<String, Object> wrappedResult = new HashMap<>();
                    wrappedResult.put("metricCode", metricCode);
                    wrappedResult.put("metricName", config.getMetricName());
                    wrappedResult.put("params", config.getParams());
                    wrappedResult.put("data", metricResult);
                    wrappedResult.put("count", metricResult != null ? metricResult.size() : 0);

                    allResults.add(wrappedResult);

                    // 更新子指标状态为成功
                    String resultJson = objectMapper.writeValueAsString(metricResult);
                    taskMetricItemService.updateToSuccess(task.getId(), metricCode, resultJson, null);

                } catch (Exception e) {
                    log.error("指标查询失败: taskId={}, metricCode={}, error={}", 
                            task.getId(), metricCode, e.getMessage());
                    // 更新子指标状态为失败
                    taskMetricItemService.updateToFailed(task.getId(), metricCode, e.getMessage());
                    // 将受检异常包装为运行时异常
                    throw new RuntimeException("指标查询失败: " + metricCode, e);
                }
            }
        }

        try {
            return objectMapper.writeValueAsString(allResults);
        } catch (Exception e) {
            log.error("序列化查询结果失败: {}", e.getMessage());
            throw new RuntimeException("序列化查询结果失败", e);
        }
    }

    /**
     * 执行导出任务
     * 支持导出多个指标的数据
     */
    private String executeExportTask(Task task) {
        List<MetricQueryConfig> metricConfigs = task.getMetricConfigs();
        
        log.info("执行导出任务: taskId={}, metricCount={}", task.getId(), 
                metricConfigs != null ? metricConfigs.size() : 0);

        // 存储所有指标的查询结果
        List<Map<String, Object>> allResults = new ArrayList<>();

        if (metricConfigs != null) {
            for (MetricQueryConfig config : metricConfigs) {
                String metricCode = config.getMetricCode();
                MetricExecuteRequest request = config.toExecuteRequest();

                log.info("执行指标导出: taskId={}, metricCode={}", task.getId(), metricCode);
                
                // 更新子指标状态为执行中
                taskMetricItemService.updateToRunning(task.getId(), metricCode);
                
                try {
                    // 执行单个指标查询
                    List<Map<String, Object>> metricResult = queryEngineService.execute(metricCode, request);

                    // 包装结果，包含指标信息
                    Map<String, Object> wrappedResult = new HashMap<>();
                    wrappedResult.put("metricCode", metricCode);
                    wrappedResult.put("metricName", config.getMetricName());
                    wrappedResult.put("params", config.getParams());
                    wrappedResult.put("data", metricResult);
                    wrappedResult.put("count", metricResult != null ? metricResult.size() : 0);

                    allResults.add(wrappedResult);

                    // 更新子指标状态为成功
                    String resultJson = objectMapper.writeValueAsString(metricResult);
                    taskMetricItemService.updateToSuccess(task.getId(), metricCode, resultJson, null);

                } catch (Exception e) {
                    log.error("指标导出失败: taskId={}, metricCode={}, error={}", 
                            task.getId(), metricCode, e.getMessage());
                    // 更新子指标状态为失败
                    taskMetricItemService.updateToFailed(task.getId(), metricCode, e.getMessage());
                    // 将受检异常包装为运行时异常
                    throw new RuntimeException("指标导出失败: " + metricCode, e);
                }
            }
        }

        // 导出任务返回导出文件路径或标识
        // 这里简化处理，返回结果数据
        // 实际项目中可以生成 CSV/Excel 文件并返回文件路径
        try {
            String jsonData = objectMapper.writeValueAsString(allResults);
            // 模拟导出文件路径
            String exportPath = "/exports/" + task.getTaskCode() + ".json";
            log.info("导出文件生成成功: taskId={}, path={}", task.getId(), exportPath);
            return jsonData;
        } catch (Exception e) {
            log.error("导出任务执行失败: {}", e.getMessage());
            throw new RuntimeException("导出任务执行失败", e);
        }
    }
}