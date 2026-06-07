package com.example.metricplatform.metric.dto;

import com.example.metricplatform.common.enums.AggregationType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 指标统计项请求 DTO
 */
@Data
@Schema(description = "指标统计项")
public class MetricStatItemRequest {

    @Schema(description = "统计项编码", required = true, example = "asset_count")
    @NotBlank(message = "统计项编码不能为空")
    private String itemCode;

    @Schema(description = "统计项名称", required = true, example = "素材数量")
    @NotBlank(message = "统计项名称不能为空")
    private String itemName;

    @Schema(description = "统计字段", required = true, example = "asset_id")
    @NotBlank(message = "统计字段不能为空")
    private String fieldName;

    @Schema(description = "聚合方式", required = true, example = "count", allowableValues = {"count", "sum", "avg", "max", "min"})
    @NotNull(message = "聚合方式不能为空")
    private AggregationType aggregation;

    @Schema(description = "显示顺序", example = "1")
    private Integer sortOrder = 0;
}
