package com.example.metricplatform.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量字段属性更新请求 DTO
 */
@Data
@Schema(description = "批量字段属性更新请求")
public class BatchFieldPropertiesUpdateRequest {

    @NotEmpty(message = "字段ID列表不能为空")
    @Schema(description = "字段ID列表", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> fieldIds;

    @Schema(description = "是否可筛选", example = "true")
    private Boolean filterable;

    @Schema(description = "是否可分组", example = "true")
    private Boolean groupable;

    @Schema(description = "是否可聚合", example = "true")
    private Boolean aggregatable;

    @Schema(description = "是否可排序", example = "true")
    private Boolean sortable;
}
