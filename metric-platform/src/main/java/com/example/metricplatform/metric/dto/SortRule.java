package com.example.metricplatform.metric.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 排序规则
 */
@Data
@Schema(description = "排序规则")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SortRule {
    
    @Schema(description = "排序字段", example = "play_count")
    private String field;
    
    @Schema(description = "排序方向", example = "desc", allowableValues = {"asc", "desc"})
    private String order;
    
    @JsonProperty("paramName")
    @JsonAlias("param_name")
    @Schema(description = "动态参数名（可选），用于从请求 params 中替换排序方向", example = "sort_direction")
    private String paramName;
    
    /**
     * 兼容旧数据格式的direction字段
     */
    @JsonProperty("direction")
    public void setDirection(String direction) {
        this.order = direction;
    }
    
    @JsonProperty("direction")
    public String getDirection() {
        return this.order;
    }
    
    /**
     * 兼容旧数据格式的param_name字段
     */
    @JsonProperty("param_name")
    public void setParamNameOld(String paramName) {
        this.paramName = paramName;
    }
    
    @JsonProperty("param_name")
    public String getParamNameOld() {
        return this.paramName;
    }
}
