package com.example.metricplatform.task.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.metricplatform.common.Result;
import com.example.metricplatform.task.dto.TaskCreateRequest;
import com.example.metricplatform.task.entity.Task;
import com.example.metricplatform.task.entity.TaskExecutionResult;
import com.example.metricplatform.task.entity.TaskMetricItem;
import com.example.metricplatform.common.enums.TaskStatus;
import com.example.metricplatform.task.service.TaskExecutionResultService;
import com.example.metricplatform.task.service.TaskMetricItemService;
import com.example.metricplatform.task.service.impl.TaskQueueService;
import com.example.metricplatform.task.service.impl.TaskSchedulerService;
import com.example.metricplatform.task.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 任务控制器
 */
@Tag(name = "任务管理", description = "任务相关接口")
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final TaskSchedulerService taskSchedulerService;
    private final TaskMetricItemService taskMetricItemService;
    private final TaskExecutionResultService taskExecutionResultService;
    private final TaskQueueService taskQueueService;

    @Operation(summary = "创建任务", description = "创建新的查询或导出任务，支持即时执行和定时执行")
    @ApiResponses({
        @ApiResponse(responseCode = "1001", description = "参数验证失败"),
        @ApiResponse(responseCode = "3102", description = "指标不存在"),
        @ApiResponse(responseCode = "3010", description = "指标已停用"),
        @ApiResponse(responseCode = "5003", description = "定时任务必须配置Cron表达式")
    })
    @PostMapping
    public Result<Task> create(@Valid @RequestBody TaskCreateRequest request) {
        Task task = taskService.create(request);
        return Result.success(task);
    }

    @Operation(summary = "查询任务状态", description = "根据任务编码查询任务状态")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @GetMapping("/{code}")
    public Result<Task> getByCode(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        return Result.success(task);
    }

    @Operation(summary = "分页查询任务列表", description = "分页查询任务列表")
    @GetMapping
    public Result<Page<Task>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "指标编码") @RequestParam(required = false) String metricCode,
            @Parameter(description = "任务状态") @RequestParam(required = false) TaskStatus status) {
        Page<Task> page = taskService.page(pageNum, pageSize, metricCode, status);
        return Result.success(page);
    }

    @Operation(summary = "删除任务", description = "删除指定任务")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @DeleteMapping("/{code}")
    public Result<Void> delete(@Parameter(description = "任务编码") @PathVariable String code) {
        taskService.delete(code);
        return Result.success();
    }

    @Operation(summary = "暂停定时任务", description = "暂停一个正在运行的定时任务（仅对定时任务有效）")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @PostMapping("/{code}/pause")
    public Result<Map<String, Object>> pauseTask(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        boolean success = taskSchedulerService.pauseTask(code);
        Map<String, Object> result = new HashMap<>();
        result.put("taskCode", code);
        result.put("success", success);
        result.put("message", success ? "定时任务已暂停" : "任务暂停失败（不是定时任务）");
        return Result.success(result);
    }

    @Operation(summary = "继续定时任务", description = "继续一个已暂停的定时任务（仅对定时任务有效）")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @PostMapping("/{code}/resume")
    public Result<Map<String, Object>> resumeTask(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        boolean success = taskSchedulerService.resumeTask(code);
        Map<String, Object> result = new HashMap<>();
        result.put("taskCode", code);
        result.put("success", success);
        result.put("message", success ? "定时任务已继续" : "任务继续失败（不是暂停状态）");
        return Result.success(result);
    }

    @Operation(summary = "停止/取消定时任务", description = "停止/取消一个定时任务（仅对定时任务有效）")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @PostMapping("/{code}/cancel")
    public Result<Map<String, Object>> cancelTask(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        boolean success = taskSchedulerService.cancelTaskByCode(code);
        Map<String, Object> result = new HashMap<>();
        result.put("taskCode", code);
        result.put("success", success);
        result.put("message", success ? "定时任务已取消" : "任务取消失败");
        return Result.success(result);
    }

    @Operation(summary = "手动触发任务", description = "立即触发执行一次任务（适用于即时任务和定时任务）")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @PostMapping("/{code}/trigger")
    public Result<Map<String, Object>> triggerTask(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        
        // 更新任务状态为待执行
        task.setStatus(TaskStatus.PENDING);
        task.setRetryCount(0);
        task.setErrorMessage(null);
        taskService.updateById(task);
        
        // 加入队列立即执行
        taskQueueService.enqueue(task);
        
        Map<String, Object> result = new HashMap<>();
        result.put("taskCode", code);
        result.put("message", "任务已加入执行队列");
        return Result.success(result);
    }

    @Operation(summary = "查询任务调度状态", description = "查询任务是否已调度或暂停（仅对定时任务有效）")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @GetMapping("/{code}/status-detail")
    public Result<Map<String, Object>> getTaskStatusDetail(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("taskCode", code);
        result.put("taskStatus", task.getStatus());
        result.put("isScheduled", taskSchedulerService.isScheduled(code));
        result.put("isPaused", taskSchedulerService.isPaused(code));
        result.put("nextRunTime", task.getNextRunTime());
        
        return Result.success(result);
    }

    @Operation(summary = "查询任务子指标执行状态", description = "查询任务中每个指标的执行状态")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @GetMapping("/{code}/metrics")
    public Result<List<TaskMetricItem>> getTaskMetrics(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        
        List<TaskMetricItem> metricItems = taskMetricItemService.getByTaskId(task.getId());
        return Result.success(metricItems);
    }

    @Operation(summary = "查询任务执行历史", description = "查询任务的所有执行历史记录")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @GetMapping("/{code}/executions")
    public Result<List<TaskExecutionResult>> getTaskExecutions(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        
        List<TaskExecutionResult> results = taskExecutionResultService.getByTaskId(task.getId());
        return Result.success(results);
    }

    @Operation(summary = "分页查询任务执行历史", description = "分页查询任务的执行历史记录")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @GetMapping("/{code}/executions/page")
    public Result<Page<TaskExecutionResult>> getTaskExecutionsPage(
            @Parameter(description = "任务编码") @PathVariable String code,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        
        Page<TaskExecutionResult> page = taskExecutionResultService.pageByTaskId(task.getId(), pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取任务最新执行结果", description = "获取任务的最新执行结果")
    @ApiResponses({
        @ApiResponse(responseCode = "5103", description = "任务不存在")
    })
    @GetMapping("/{code}/executions/latest")
    public Result<TaskExecutionResult> getLatestExecution(@Parameter(description = "任务编码") @PathVariable String code) {
        Task task = taskService.getByCode(code);
        if (task == null) {
            return Result.error(5103, "任务不存在: " + code);
        }
        
        TaskExecutionResult result = taskExecutionResultService.getLatestByTaskId(task.getId());
        return Result.success(result);
    }

    @Operation(summary = "分页查询所有任务执行历史", description = "分页查询所有任务的执行历史，支持按任务编码和状态筛选")
    @GetMapping("/executions/page")
    public Result<Page<TaskExecutionResult>> getAllExecutionsPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "任务编码(可选)") @RequestParam(required = false) String taskCode,
            @Parameter(description = "执行状态(可选)") @RequestParam(required = false) TaskStatus status) {
        Page<TaskExecutionResult> page = taskExecutionResultService.page(pageNum, pageSize, taskCode, status);
        return Result.success(page);
    }
}