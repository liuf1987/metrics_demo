package com.example.metricplatform.metric.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 窗口函数配置包装类
 */
@Data
@Schema(description = "窗口函数配置")
@JsonIgnoreProperties(ignoreUnknown = true)
public class WindowFunctions {
    
    @Schema(description = "窗口函数列表")
    private List<WindowFunction> functions;
}
