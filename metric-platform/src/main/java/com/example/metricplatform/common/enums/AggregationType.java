package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 聚合方式枚举
 */
@Getter
public enum AggregationType {

    COUNT("count", "计数"),
    SUM("sum", "求和"),
    AVG("avg", "平均值"),
    MAX("max", "最大值"),
    MIN("min", "最小值");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    AggregationType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
