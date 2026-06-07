package com.example.metricplatform.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.common.entity.BaseEntity;
import com.example.metricplatform.common.enums.TaskStatus;
import com.example.metricplatform.common.enums.TaskType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 任务执行结果实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task_execution_result")
public class TaskExecutionResult extends BaseEntity {

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
     * 任务编码
     */
    @TableField("task_code")
    private String taskCode;

    /**
     * 本次执行ID，用于唯一标识一次执行
     */
    @TableField("execution_id")
    private String executionId;

    /**
     * 任务类型
     */
    @TableField("task_type")
    private TaskType taskType;

    /**
     * 执行模式：instant-即时执行，scheduled-定时执行
     */
    @TableField("execute_mode")
    private String executeMode;

    /**
     * 执行状态
     */
    @TableField("status")
    private TaskStatus status;

    /**
     * 执行结果数据(JSON格式)
     */
    @TableField("result_data")
    private String resultData;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 执行开始时间
     */
    @TableField("execution_start")
    private LocalDateTime executionStart;

    /**
     * 执行结束时间
     */
    @TableField("execution_end")
    private LocalDateTime executionEnd;

    /**
     * 执行耗时(毫秒)
     */
    @TableField("execution_duration")
    private Integer executionDuration;

    /**
     * 本次执行时的重试次数
     */
    @TableField("retry_count")
    private Integer retryCount;
}
