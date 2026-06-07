package com.example.metricplatform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI生成失败响应")
public class AIGenerationFailure {

    @Schema(description = "是否无法继续", required = true)
    private Boolean cannotProceed;

    @Schema(description = "失败原因")
    private String reason;
}
