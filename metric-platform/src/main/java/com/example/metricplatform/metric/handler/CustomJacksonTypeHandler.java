package com.example.metricplatform.metric.handler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 自定义JacksonTypeHandler，配置为忽略未知属性
 */
public class CustomJacksonTypeHandler extends AbstractJsonTypeHandler<Object> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    private final Class<?> type;
    
    public CustomJacksonTypeHandler(Class<?> type) {
        this.type = type;
    }
    
    @Override
    protected Object parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
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
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
}