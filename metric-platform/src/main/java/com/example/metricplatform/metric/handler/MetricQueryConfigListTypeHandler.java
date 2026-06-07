package com.example.metricplatform.metric.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.example.metricplatform.metric.dto.MetricQueryConfig;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 专门处理 List&lt;MetricQueryConfig&gt; 的 TypeHandler
 */
public class MetricQueryConfigListTypeHandler extends AbstractJsonTypeHandler<Object> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    private static final TypeReference<List<MetricQueryConfig>> TYPE_REFERENCE = 
            new TypeReference<List<MetricQueryConfig>>() {};
    
    public MetricQueryConfigListTypeHandler(Class<?> type) {
        // type 参数暂时不需要使用
    }
    
    @Override
    protected Object parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TYPE_REFERENCE);
        } catch (Exception e) {
            throw new RuntimeException("JSON反序列化失败: " + json, e);
        }
    }
    
    @Override
    protected String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
}
