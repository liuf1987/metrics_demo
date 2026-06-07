package com.example.metricplatform.ai.dto;

import lombok.Data;

@Data
public class RecommendedMetric {
    private String metricCode;
    private String metricName;
    private Integer score;
    private String reason;
}
