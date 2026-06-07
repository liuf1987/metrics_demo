package com.example.metricplatform.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI聊天请求
 */
@Data
@Schema(description = "AI聊天请求")
public class AIChatRequest {

    @Schema(description = "用户查询语句", required = true)
    @NotBlank(message = "查询语句不能为空")
    private String query;

    @Schema(description = "返回推荐数量", defaultValue = "5")
    private Integer topN = 5;
}
