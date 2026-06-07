package com.example.metricplatform.metric.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 窗口函数定义
 */
@Data
@Schema(description = "窗口函数定义")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WindowFunction {
    
    @Schema(description = "窗口函数名称", example = "ROW_NUMBER", allowableValues = {"ROW_NUMBER", "RANK", "DENSE_RANK", "SUM", "AVG", "MAX", "MIN", "NTILE", "LAG", "LEAD"})
    private String name;
    
    @Schema(description = "分区字段列表", example = "[\"platform\"]")
    @JsonProperty("partition_by")
    private List<String> partitionBy;
    
    @Schema(description = "排序规则列表")
    @JsonProperty("order_by")
    private List<SortRule> orderBy;
    
    @Schema(description = "别名", example = "rank_in_platform")
    private String alias;
    
    @Schema(description = "统计项名称（作为别名使用）", example = "rank_in_platform")
    @JsonProperty("item_name")
    private String itemName;
    
    @Schema(description = "NTILE等函数的参数", example = "4")
    private Integer n;
    
    @Schema(description = "LAG/LEAD等函数的参数列表")
    private List<Object> arguments;
    
    @Schema(description = "窗口框架定义", example = "ROWS BETWEEN 2 PRECEDING AND CURRENT ROW")
    private String frame;
    
    /**
     * 获取实际使用的别名，优先使用 itemName，如果没有则使用 alias
     */
    public String getEffectiveAlias() {
        if (itemName != null && !itemName.isEmpty()) {
            return itemName;
        }
        return alias;
    }
}
