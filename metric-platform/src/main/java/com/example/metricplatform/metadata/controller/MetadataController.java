package com.example.metricplatform.metadata.controller;

import com.example.metricplatform.common.Result;
import com.example.metricplatform.metadata.dto.BatchFieldPropertiesUpdateRequest;
import com.example.metricplatform.metadata.dto.DataSourceDetailResponse;
import com.example.metricplatform.metadata.dto.FieldPropertiesUpdateRequest;
import com.example.metricplatform.metadata.dto.MetadataLoadRequest;
import com.example.metricplatform.metric.dto.StatusUpdateRequest;
import com.example.metricplatform.metadata.entity.DataSource;
import com.example.metricplatform.metadata.entity.FieldMetadata;
import com.example.metricplatform.metadata.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "元数据管理", description = "元数据加载和查询相关接口")
@RestController
@RequestMapping("/metadata")
@RequiredArgsConstructor
public class MetadataController {

    private final MetadataService metadataService;

    @Operation(summary = "加载元数据", description = "从数据库 information_schema 中加载表和字段元数据")
    @PostMapping("/load")
    public Result<Void> loadMetadata(@RequestBody(required = false) MetadataLoadRequest request) {
        String pattern = request != null ? request.getTableNamePattern() : null;
        metadataService.loadMetadata(pattern);
        return Result.success();
    }

    @Operation(summary = "查询所有数据源", description = "获取所有已加载的数据源列表")
    @GetMapping("/datasources")
    public Result<List<DataSource>> getAllDataSources() {
        List<DataSource> dataSources = metadataService.getAllDataSources();
        return Result.success(dataSources);
    }

    @Operation(summary = "查询数据源详情", description = "获取指定数据源的详细信息，包含字段列表")
    @ApiResponses({
        @ApiResponse(responseCode = "2101", description = "数据源不存在")
    })
    @GetMapping("/datasources/{sourceId}")
    public Result<DataSourceDetailResponse> getDataSourceDetail(
            @Parameter(description = "数据源ID") @PathVariable Long sourceId) {
        List<FieldMetadata> fields = metadataService.getFieldsBySourceId(sourceId);
        DataSource dataSource = metadataService.getDataSourceById(sourceId);
        
        DataSourceDetailResponse response = DataSourceDetailResponse.builder()
                .id(dataSource.getId())
                .sourceCode(dataSource.getSourceCode())
                .sourceName(dataSource.getSourceName())
                .sourceType(dataSource.getSourceType().name())
                .sourceExpr(dataSource.getSourceExpr())
                .description(dataSource.getDescription())
                .enabled(dataSource.getEnabled())
                .createTime(dataSource.getCreateTime())
                .updateTime(dataSource.getUpdateTime())
                .fields(fields)
                .build();
        
        return Result.success(response);
    }

    @Operation(summary = "查询数据源字段", description = "根据数据源ID获取该数据源的所有字段")
    @ApiResponses({
        @ApiResponse(responseCode = "2101", description = "数据源不存在")
    })
    @GetMapping("/datasources/{sourceId}/fields")
    public Result<List<FieldMetadata>> getFieldsBySourceId(
            @Parameter(description = "数据源ID") @PathVariable Long sourceId) {
        List<FieldMetadata> fields = metadataService.getFieldsBySourceId(sourceId);
        return Result.success(fields);
    }

    @Operation(summary = "更新数据源状态", description = "启用或禁用指定的数据源，禁用时会同步禁用相关指标")
    @ApiResponses({
        @ApiResponse(responseCode = "2101", description = "数据源不存在")
    })
    @PostMapping("/datasources/{sourceId}/status")
    public Result<Void> updateDataSourceStatus(
            @Parameter(description = "数据源ID") @PathVariable Long sourceId,
            @Valid @RequestBody StatusUpdateRequest request) {
        metadataService.enableDataSource(sourceId, request.getEnabled());
        return Result.success();
    }

    @Operation(summary = "更新字段状态", description = "启用或禁用指定的字段，禁用时会同步禁用相关指标")
    @ApiResponses({
        @ApiResponse(responseCode = "3005", description = "字段不存在")
    })
    @PostMapping("/fields/{fieldId}/status")
    public Result<Void> updateFieldStatus(
            @Parameter(description = "字段ID") @PathVariable Long fieldId,
            @Valid @RequestBody StatusUpdateRequest request) {
        metadataService.enableField(fieldId, request.getEnabled());
        return Result.success();
    }

    @Operation(summary = "更新字段属性", description = "设置字段的可筛选、可分组、可聚合、可排序等属性")
    @ApiResponses({
        @ApiResponse(responseCode = "3005", description = "字段不存在")
    })
    @PutMapping("/fields/{fieldId}/properties")
    public Result<Void> updateFieldProperties(
            @Parameter(description = "字段ID") @PathVariable Long fieldId,
            @RequestBody FieldPropertiesUpdateRequest request) {
        metadataService.updateFieldProperties(
                fieldId,
                request.getFilterable(),
                request.getGroupable(),
                request.getAggregatable(),
                request.getSortable(),
                request.getFieldLabel()
        );
        return Result.success();
    }

    @Operation(summary = "批量更新字段属性", description = "批量设置多个字段的可筛选、可分组、可聚合、可排序属性")
    @ApiResponses({
        @ApiResponse(responseCode = "3005", description = "字段不存在"),
        @ApiResponse(responseCode = "1002", description = "必填参数为空")
    })
    @PutMapping("/fields/batch/properties")
    public Result<Void> batchUpdateFieldProperties(@RequestBody BatchFieldPropertiesUpdateRequest request) {
        metadataService.batchUpdateFieldProperties(
                request.getFieldIds(),
                request.getFilterable(),
                request.getGroupable(),
                request.getAggregatable(),
                request.getSortable()
        );
        return Result.success();
    }
}
