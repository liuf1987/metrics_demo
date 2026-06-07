package com.example.metricplatform.metric.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 筛选条件组
 * 支持嵌套条件结构
 */
@Data
@Schema(description = "筛选条件组")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterGroup {
    
    @Schema(description = "条件列表，可包含 FilterCondition 或嵌套的 FilterGroup",
            oneOf = {FilterCondition.class, FilterGroup.class})
    private List<Object> conditions;
    
    @Schema(description = "逻辑运算符", example = "AND", allowableValues = {"AND", "OR"})
    private String logic = "AND";
}
