package com.example.metricplatform.metadata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "元数据加载请求")
public class MetadataLoadRequest {

    @Schema(description = "表名模式（如 asset%），用于匹配业务表")
    private String tableNamePattern;
}
