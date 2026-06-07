package com.example.metricplatform.metric.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 指标查询配置
 * 用于任务中配置单个指标的查询参数
 */
@Data
@Schema(description = "指标查询配置")
public class MetricQueryConfig {

    @Schema(description = "指标编码", required = true)
    @NotBlank(message = "指标编码不能为空")
    private String metricCode;

    @Schema(description = "指标名称（可选）")
    private String metricName;

    @Schema(description = "动态参数覆盖，格式为\"param_name\": \"param_value\"，其中param_name为指标配置中的参数名")
    private Map<String, Object> params;

    @Schema(description = "覆盖的LIMIT值")
    private Integer limitValue;

    /**
     * 转换为 MetricExecuteRequest
     */
    public MetricExecuteRequest toExecuteRequest() {
        MetricExecuteRequest request = new MetricExecuteRequest();
        request.setParams(this.params);
        request.setLimitValue(this.limitValue);
        return request;
    }
}