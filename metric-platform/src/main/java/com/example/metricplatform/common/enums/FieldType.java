package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.util.Set;

/**
 * 字段类型枚举
 */
@Getter
public enum FieldType {

    STRING("string", "字符串", Set.of("count", "max", "min")),
    NUMBER("number", "数字", Set.of("count", "sum", "avg", "max", "min")),
    DATETIME("datetime", "日期时间", Set.of("count", "max", "min")),
    JSON("json", "JSON", Set.of("count")),
    ARRAY("array", "数组", Set.of("count"));

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;
    private final Set<String> supportedAggregations;

    FieldType(String code, String desc, Set<String> supportedAggregations) {
        this.code = code;
        this.desc = desc;
        this.supportedAggregations = supportedAggregations;
    }

    /**
     * 检查字段类型是否支持指定的聚合函数
     */
    public boolean supportsAggregation(String aggregation) {
        if (aggregation == null) {
            return false;
        }
        return supportedAggregations.contains(aggregation.toLowerCase());
    }

    /**
     * 获取支持的聚合函数列表（用于错误提示）
     */
    public String getSupportedAggregationsString() {
        return String.join(", ", supportedAggregations);
    }

    public String toUpperCase() {
        return code.toUpperCase();
    }

}
