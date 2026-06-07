package com.example.metricplatform.metric.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 执行指标查询请求
 */
@Data
@Schema(description = "执行指标查询请求")
public class MetricExecuteRequest {

    @Schema(description = "动态参数覆盖, 格式为\"param_name\": \"param_value\",其中param_name为指标配置中的参数名，param_value为参数值")
    private Map<String, Object> params;

    @Schema(description = "覆盖的LIMIT值")
    private Integer limitValue;
}
