package com.example.metricplatform.ai.dto;

import com.example.metricplatform.metric.entity.MetricConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "AI聊天响应")
public class AIChatResponse {

    @Schema(description = "用户查询语句")
    private String query;

    @Schema(description = "找到的现有指标列表")
    private List<MetricConfig> existingMetrics;

    @Schema(description = "生成的新指标（如果有）")
    private MetricConfig generatedMetric;

    @Schema(description = "建议信息")
    private String suggestion;

    @Schema(description = "聊天记录ID")
    private Long chatHistoryId;

    @Schema(description = "是否无法处理此需求")
    private Boolean isCannotProceed;
}
