package com.example.metricplatform.metric.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 单个筛选条件
 */
@Data
@Schema(description = "单个筛选条件")
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterCondition {
    
    @Schema(description = "字段名", example = "status")
    private String field;
    
    @Schema(description = "操作符", example = "=", allowableValues = {"=", "!=", ">", "<", ">=", "<=", "LIKE", "IN", "NOT IN"})
    private String operator;
    
    @Schema(description = "筛选值", example = "approved")
    private Object value;
    
    @Schema(description = "动态参数名（可选），用于从请求 params 中替换值", example = "status_param")
    private String paramName;
    
    @JsonProperty("param_name")
    public void setParamNameOld(String paramName) {
        this.paramName = paramName;
    }
    
    @JsonProperty("param_name")
    public String getParamNameOld() {
        return this.paramName;
    }
}
