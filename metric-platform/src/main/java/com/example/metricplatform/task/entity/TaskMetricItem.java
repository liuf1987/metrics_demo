package com.example.metricplatform.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.common.entity.BaseEntity;
import com.example.metricplatform.common.enums.TaskMetricItemStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务子指标实体类
 * 用于单独维护每个任务中每个指标的执行状态
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_metric_item")
public class TaskMetricItem extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 关联的任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 指标编码
     */
    @TableField("metric_code")
    private String metricCode;

    /**
     * 指标名称
     */
    @TableField("metric_name")
    private String metricName;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 执行状态
     */
    @TableField("status")
    private TaskMetricItemStatus status;

    /**
     * 执行结果(JSON格式)
     */
    @TableField("result_data")
    private String resultData;

    /**
     * 失败原因
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 开始执行时间
     */
    @TableField("started_at")
    private LocalDateTime startedAt;

    /**
     * 执行结束时间
     */
    @TableField("finished_at")
    private LocalDateTime finishedAt;

    /**
     * 执行耗时(毫秒)
     */
    @TableField("execute_duration")
    private Integer executeDuration;
}
