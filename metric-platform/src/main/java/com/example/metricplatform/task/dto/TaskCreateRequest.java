package com.example.metricplatform.task.dto;

import com.example.metricplatform.common.enums.ExecuteMode;
import com.example.metricplatform.common.enums.TaskType;
import com.example.metricplatform.metric.dto.MetricQueryConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

/**
 * 创建任务请求
 */
@Data
@Schema(description = "创建任务请求")
public class TaskCreateRequest {

    @Schema(description = "任务类型", required = true)
    @NotNull(message = "任务类型不能为空")
    private TaskType taskType;

    @Schema(description = "执行模式：即时执行(instant) / 定时执行(scheduled)", required = true)
    @NotNull(message = "执行模式不能为空")
    private ExecuteMode executeMode;

    @Schema(description = "Cron表达式（定时任务必填），格式如：0 0 2 * * ?（每天凌晨2点）")
    @Pattern(regexp = "^$|^\\s*([0-9\\*\\-/]+)\\s+([0-9\\*\\-/]+)\\s+([0-9\\*\\-/]+)\\s+([0-9\\*\\-/\\?LW]+)\\s+([0-9A-Z\\*\\-/]+)\\s+([0-9A-Z\\*\\-\\/\\?#]+)(\\s+([0-9A-Z\\*\\-/]+))?$", 
             message = "Cron表达式格式不正确")
    private String cronExpression;

    @Schema(description = "任务名称")
    private String taskName;

    @Schema(description = "任务描述")
    private String taskDesc;

    @Schema(description = "关联的指标查询配置列表，一个任务可以包含多个指标查询", required = true)
    @NotEmpty(message = "指标查询配置列表不能为空")
    @Valid
    private List<MetricQueryConfig> metricConfigs;
}