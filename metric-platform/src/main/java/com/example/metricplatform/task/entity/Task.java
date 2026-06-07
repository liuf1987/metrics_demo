package com.example.metricplatform.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.metric.dto.MetricQueryConfig;
import com.example.metricplatform.common.entity.BaseEntity;
import com.example.metricplatform.metric.handler.MetricQueryConfigListTypeHandler;
import com.example.metricplatform.common.enums.ExecuteMode;
import com.example.metricplatform.common.enums.TaskStatus;
import com.example.metricplatform.common.enums.TaskType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "task", autoResultMap = true)
public class Task extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 任务编码
     */
    @TableField("task_code")
    private String taskCode;

    /**
     * 任务名称
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 任务描述
     */
    @TableField("task_desc")
    private String taskDesc;

    /**
     * 任务类型
     */
    @TableField("task_type")
    private TaskType taskType;

    /**
     * 执行模式：即时执行/定时执行
     */
    @TableField("execute_mode")
    private ExecuteMode executeMode;

    /**
     * Cron表达式（定时任务使用）
     */
    @TableField("cron_expression")
    private String cronExpression;

    /**
     * 关联的指标查询配置列表（支持多个指标）
     */
    @TableField(value = "metric_configs", typeHandler = MetricQueryConfigListTypeHandler.class)
    private List<MetricQueryConfig> metricConfigs;

    /**
     * 任务状态
     */
    @TableField("status")
    private TaskStatus status;

    /**
     * 查询结果（JSON格式，包含所有指标的查询结果）
     */
    @TableField("result_data")
    private String resultData;

    /**
     * 失败原因
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;

    /**
     * 开始时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /**
     * 下次执行时间（定时任务使用）
     */
    @TableField("next_run_time")
    private LocalDateTime nextRunTime;

    /**
     * 定时任务调度ID（用于取消定时任务）
     */
    @TableField("scheduler_id")
    private String schedulerId;
}