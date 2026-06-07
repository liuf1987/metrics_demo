package com.example.metricplatform.metric.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 状态更新请求 DTO
 */
@Data
@Schema(description = "状态更新请求")
public class StatusUpdateRequest {

    @NotNull(message = "启用状态不能为空")
    @Schema(description = "启用状态", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;
}
