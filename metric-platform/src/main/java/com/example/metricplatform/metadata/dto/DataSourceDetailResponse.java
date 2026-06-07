package com.example.metricplatform.metadata.dto;

import com.example.metricplatform.metadata.entity.FieldMetadata;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据源详情响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "数据源详情响应")
public class DataSourceDetailResponse {

    @Schema(description = "数据源ID")
    private Long id;

    @Schema(description = "数据源编码")
    private String sourceCode;

    @Schema(description = "数据源名称")
    private String sourceName;

    @Schema(description = "数据源类型")
    private String sourceType;

    @Schema(description = "数据源表达式")
    private String sourceExpr;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "启用状态")
    private Boolean enabled;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "字段列表")
    private List<FieldMetadata> fields;
}
