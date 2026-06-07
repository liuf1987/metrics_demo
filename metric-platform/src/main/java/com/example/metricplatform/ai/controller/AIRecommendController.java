package com.example.metricplatform.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.metricplatform.common.Result;
import com.example.metricplatform.ai.dto.AIChatRequest;
import com.example.metricplatform.ai.dto.AIChatResponse;
import com.example.metricplatform.metric.dto.MetricFeedbackRequest;
import com.example.metricplatform.ai.entity.AIChatHistory;
import com.example.metricplatform.ai.service.AIService;
import com.example.metricplatform.ai.service.AIConfigurationChecker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 基于阿里云百炼大模型的AI指标生成控制器
 * 
 * 本控制器提供与AI指标生成相关的接口，包括：
 * - 查询现有指标：根据用户查询语句查找相似的现有指标
 * - 生成新指标：根据用户查询语句生成新指标并保存
 * - 提交指标评价：对查询或生成的指标进行满意度评价
 * - 查询聊天历史：获取与AI的交互历史记录
 * - 检查AI配置：验证阿里云百炼服务配置是否正确
 * 
 * 所有AI功能均通过阿里云百炼大模型实现，利用其强大的语义理解和代码生成能力
 * 帮助用户快速找到或生成合适的业务指标。
 */
@Tag(name = "AI指标生成", description = "基于阿里云百炼大模型的AI指标生成相关接口")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@Validated
public class AIRecommendController {

    private final AIService aiService;
    private final AIConfigurationChecker aiConfigurationChecker;

    @Operation(summary = "查询现有指标", description = "根据用户查询语句查找相似的现有指标")
    @ApiResponses({
        @ApiResponse(responseCode = "400", description = "参数验证失败"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/query")
    public Result<AIChatResponse> query(@Valid @RequestBody AIChatRequest request) {
        // 查询现有指标不需要AI配置检查
        AIChatResponse response = aiService.queryMetrics(request);
        return Result.success(response);
    }

    @Operation(summary = "生成新指标", description = "根据用户查询语句生成新指标并保存")
    @ApiResponses({
        @ApiResponse(responseCode = "400", description = "参数验证失败"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/generate")
    public Result<AIChatResponse> generate(@Valid @RequestBody AIChatRequest request) {
        // 生成新指标需要AI配置检查
        aiConfigurationChecker.checkConfiguration();
        AIChatResponse response = aiService.generateMetric(request);
        return Result.success(response);
    }

    @Operation(summary = "提交指标评价", description = "对查询或生成的指标进行满意度评价")
    @ApiResponses({
        @ApiResponse(responseCode = "400", description = "参数验证失败"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody MetricFeedbackRequest request) {
        // 提交评价不需要AI配置检查
        aiService.submitFeedback(request);
        return Result.success();
    }

    @Operation(summary = "查询聊天历史", description = "获取AI聊天历史记录")
    @ApiResponses({
        @ApiResponse(responseCode = "400", description = "参数验证失败"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/history")
    public Result<Page<AIChatHistory>> getHistory(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须大于等于1") Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "每页大小必须大于等于1") Integer pageSize) {
        // 查询历史不需要AI配置检查
        Page<AIChatHistory> page = aiService.getChatHistory(pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "检查AI配置", description = "检查AI服务配置是否可用")
    @ApiResponses({
        @ApiResponse(responseCode = "400", description = "配置不正确"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/config/check")
    public Result<Void> checkConfig() {
        aiConfigurationChecker.checkConfiguration();
        return Result.success();
    }
}
