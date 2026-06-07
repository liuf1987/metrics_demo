package com.example.metricplatform.ai.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RecommendationResult {
    private Boolean hasMatch;
    private List<RecommendedMetric> recommendedMetrics = new ArrayList<>();
    private String message;
}
