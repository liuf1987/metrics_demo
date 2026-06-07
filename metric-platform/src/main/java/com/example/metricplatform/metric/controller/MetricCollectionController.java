package com.example.metricplatform.metric.controller;

import com.example.metricplatform.common.Result;
import com.example.metricplatform.metric.entity.MetricCollection;
import com.example.metricplatform.metric.service.MetricCollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 指标集合控制器
 */
@Tag(name = "指标集合", description = "指标集合相关接口")
@RestController
@RequestMapping("/collections")
@RequiredArgsConstructor
public class MetricCollectionController {

    private final MetricCollectionService metricCollectionService;

    @Operation(summary = "查询指标集合列表", description = "查询所有启用的指标集合")
    @GetMapping
    public Result<List<MetricCollection>> listAll() {
        List<MetricCollection> collections = metricCollectionService.listAll();
        return Result.success(collections);
    }

    @Operation(summary = "查询集合详情", description = "根据集合编码查询详情，包含集合中的指标列表")
    @ApiResponses({
        @ApiResponse(responseCode = "6102", description = "指标集合不存在")
    })
    @GetMapping("/{code}")
    public Result<Map<String, Object>> getDetailByCode(
            @Parameter(description = "集合编码") @PathVariable String code) {
        Map<String, Object> detail = metricCollectionService.getDetailByCode(code);
        return Result.success(detail);
    }

    @Operation(summary = "按角色查询集合", description = "根据目标角色查询指标集合列表")
    @GetMapping("/role/{role}")
    public Result<List<MetricCollection>> listByRole(
            @Parameter(description = "目标角色") @PathVariable String role) {
        List<MetricCollection> collections = metricCollectionService.listByRole(role);
        return Result.success(collections);
    }
}
