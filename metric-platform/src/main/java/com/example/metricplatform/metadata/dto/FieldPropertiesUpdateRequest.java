package com.example.metricplatform.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字段属性更新请求 DTO
 */
@Data
@Schema(description = "字段属性更新请求")
public class FieldPropertiesUpdateRequest {

    @Schema(description = "是否可筛选", example = "true")
    private Boolean filterable;

    @Schema(description = "是否可分组", example = "true")
    private Boolean groupable;

    @Schema(description = "是否可聚合", example = "true")
    private Boolean aggregatable;

    @Schema(description = "是否可排序", example = "true")
    private Boolean sortable;

    @Schema(description = "字段显示名称", example = "素材数量")
    private String fieldLabel;
}
