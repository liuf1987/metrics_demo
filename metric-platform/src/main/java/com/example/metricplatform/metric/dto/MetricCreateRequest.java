package com.example.metricplatform.metric.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建指标请求
 */
@Data
@Schema(description = "创建指标请求")
public class MetricCreateRequest {

    @Schema(description = "指标编码", required = true)
    @NotBlank(message = "指标编码不能为空")
    private String metricCode;

    @Schema(description = "指标名称", required = true)
    @NotBlank(message = "指标名称不能为空")
    private String metricName;

    @Schema(description = "指标描述")
    private String metricDesc;

    @Schema(description = "来源表名", required = true)
    @NotBlank(message = "来源表名不能为空")
    private String sourceTable;

    @Schema(description = "业务分类编码")
    private String categoryCode;

    @Schema(description = "业务分类名称")
    private String categoryName;

    @Schema(description = "分组维度数组")
    private List<String> groupByFields;

    @Schema(description = "筛选条件")
    private FilterGroup filterConditions;

    @Schema(description = "HAVING条件")
    private FilterGroup havingConditions;

    @Schema(description = "排序规则")
    private List<SortRule> sortRules;

    @Schema(description = "LIMIT值")
    private Integer limitValue;

    @Schema(description = "窗口函数定义")
    private WindowFunctions windowFunctions;

    @Schema(description = "统计项列表")
    private List<MetricStatItemRequest> statItems;

    @Schema(description = "是否启用")
    private Boolean enabled = true;
}
