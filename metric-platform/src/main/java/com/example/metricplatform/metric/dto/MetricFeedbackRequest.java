package com.example.metricplatform.metric.dto;

import com.example.metricplatform.common.enums.Satisfaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 指标评价请求
 */
@Data
@Schema(description = "指标评价请求")
public class MetricFeedbackRequest {

    @Schema(description = "聊天记录ID", required = true)
    @NotNull(message = "聊天记录ID不能为空")
    private Long chatHistoryId;

    @Schema(description = "满意度", required = true)
    @NotNull(message = "满意度不能为空")
    private Satisfaction satisfaction;
}
