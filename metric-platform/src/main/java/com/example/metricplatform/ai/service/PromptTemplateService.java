package com.example.metricplatform.ai.service;

import com.example.metricplatform.metadata.entity.FieldMetadata;
import com.example.metricplatform.metric.entity.MetricConfig;

import java.util.List;
import java.util.Map;

public interface PromptTemplateService {
    
    String buildRecommendationPrompt(
        String userQuery,
        List<MetricConfig> existingMetrics,
        List<Map<String, Object>> metadataTables,
        Map<String, List<FieldMetadata>> tableFields
    );
    
    String buildGenerationPrompt(
        String userQuery,
        List<Map<String, Object>> metadataTables,
        Map<String, List<FieldMetadata>> tableFields
    );
}
