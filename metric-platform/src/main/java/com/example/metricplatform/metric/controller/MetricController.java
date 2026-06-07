package com.example.metricplatform.metric.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.metricplatform.common.Result;
import com.example.metricplatform.metric.dto.MetricCreateRequest;
import com.example.metricplatform.metric.dto.MetricExecuteRequest;
import com.example.metricplatform.metric.dto.MetricUpdateRequest;
import com.example.metricplatform.metric.entity.MetricConfig;
import com.example.metricplatform.metric.service.MetricConfigService;
import com.example.metricplatform.metric.service.QueryEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 指标配置控制器
 */
@Tag(name = "指标管理", description = "指标配置相关接口")
@RestController
@RequestMapping("/metrics")
@RequiredArgsConstructor
public class MetricController {

    private final MetricConfigService metricConfigService;
    private final QueryEngineService queryEngineService;

    @Operation(summary = "创建指标", description = "创建新的指标配置")
    @ApiResponses({
        @ApiResponse(responseCode = "3001", description = "指标编码已存在"),
        @ApiResponse(responseCode = "3002", description = "源表名不能为空"),
        @ApiResponse(responseCode = "3003", description = "数据源不存在"),
        @ApiResponse(responseCode = "3004", description = "数据源已禁用"),
        @ApiResponse(responseCode = "3005", description = "字段不存在"),
        @ApiResponse(responseCode = "3006", description = "字段已禁用"),
        @ApiResponse(responseCode = "3007", description = "统计字段不存在"),
        @ApiResponse(responseCode = "3008", description = "统计字段已禁用"),
        @ApiResponse(responseCode = "3011", description = "字段类型不支持聚合函数"),
        @ApiResponse(responseCode = "1001", description = "参数校验失败")
    })
    @PostMapping
    public Result<MetricConfig> create(@Valid @RequestBody MetricCreateRequest request) {
        MetricConfig metric = metricConfigService.create(request);
        return Result.success(metric);
    }

    @Operation(summary = "更新指标", description = "更新指定指标配置")
    @ApiResponses({
        @ApiResponse(responseCode = "3001", description = "指标编码已存在"),
        @ApiResponse(responseCode = "3002", description = "源表名不能为空"),
        @ApiResponse(responseCode = "3003", description = "数据源不存在"),
        @ApiResponse(responseCode = "3004", description = "数据源已禁用"),
        @ApiResponse(responseCode = "3005", description = "字段不存在"),
        @ApiResponse(responseCode = "3006", description = "字段已禁用"),
        @ApiResponse(responseCode = "3007", description = "统计字段不存在"),
        @ApiResponse(responseCode = "3008", description = "统计字段已禁用"),
        @ApiResponse(responseCode = "3011", description = "字段类型不支持聚合函数"),
        @ApiResponse(responseCode = "1001", description = "参数校验失败")
    })
    @PutMapping("/{id}")
    public Result<MetricConfig> update(
            @Parameter(description = "指标ID") @PathVariable Long id,
            @RequestBody MetricUpdateRequest request) {
        MetricConfig metric = metricConfigService.update(id, request);
        return Result.success(metric);
    }

    @Operation(summary = "删除指标", description = "删除指定指标")
    @ApiResponses({
        @ApiResponse(responseCode = "3001", description = "指标编码已存在")
    })
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "指标ID") @PathVariable Long id) {
        metricConfigService.delete(id);
        return Result.success();
    }

    @Operation(summary = "查询指标详情", description = "根据指标编码查询详情")
    @ApiResponses({
        @ApiResponse(responseCode = "3001", description = "指标编码已存在")
    })
    @GetMapping("/{code}")
    public Result<MetricConfig> getByCode(@Parameter(description = "指标编码") @PathVariable String code) {
        MetricConfig metric = metricConfigService.getByCode(code);
        return Result.success(metric);
    }

    @Operation(summary = "分页查询指标列表", description = "分页查询指标配置列表")
    @GetMapping
    public Result<Page<MetricConfig>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类编码") @RequestParam(required = false) String categoryCode,
            @Parameter(description = "启用状态") @RequestParam(required = false) Boolean enabled,
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword) {
        Page<MetricConfig> page = metricConfigService.page(pageNum, pageSize, categoryCode, enabled, keyword);
        return Result.success(page);
    }

    @Operation(summary = "启用/停用指标", description = "切换指标的启用状态")
    @ApiResponses({
        @ApiResponse(responseCode = "3001", description = "指标编码已存在")
    })
    @PostMapping("/{id}/toggle")
    public Result<MetricConfig> toggle(
            @Parameter(description = "指标ID") @PathVariable Long id,
            @Parameter(description = "是否启用") @RequestParam Boolean enabled) {
        MetricConfig metric = metricConfigService.toggle(id, enabled);
        return Result.success(metric);
    }

    @Operation(summary = "执行指标查询", description = "根据指标配置动态生成SQL并执行查询")
    @ApiResponses({
        @ApiResponse(responseCode = "3001", description = "指标编码已存在"),
        @ApiResponse(responseCode = "3010", description = "指标已停用"),
        @ApiResponse(responseCode = "3002", description = "源表名不能为空"),
        @ApiResponse(responseCode = "3003", description = "数据源不存在"),
        @ApiResponse(responseCode = "3004", description = "数据源已禁用"),
        @ApiResponse(responseCode = "3005", description = "字段不存在"),
        @ApiResponse(responseCode = "3006", description = "字段已禁用"),
        @ApiResponse(responseCode = "4006", description = "分组字段不存在"),
        @ApiResponse(responseCode = "4007", description = "分组字段已禁用"),
        @ApiResponse(responseCode = "4008", description = "字段分组功能已被禁用"),
        @ApiResponse(responseCode = "4009", description = "过滤字段不存在"),
        @ApiResponse(responseCode = "4010", description = "过滤字段已禁用"),
        @ApiResponse(responseCode = "4011", description = "字段过滤功能已被禁用"),
        @ApiResponse(responseCode = "4012", description = "排序字段已禁用"),
        @ApiResponse(responseCode = "4013", description = "字段排序功能已被禁用"),
        @ApiResponse(responseCode = "4014", description = "窗口函数字段不存在"),
        @ApiResponse(responseCode = "4015", description = "窗口函数字段已禁用"),
        @ApiResponse(responseCode = "4901", description = "查询执行失败")
    })
    @PostMapping("/{code}/execute")
    public Result<List<Map<String, Object>>> execute(
            @Parameter(description = "指标编码") @PathVariable String code,
            @RequestBody(required = false) MetricExecuteRequest request) {
        List<Map<String, Object>> result = queryEngineService.execute(code, request);
        return Result.success(result);
    }
}
