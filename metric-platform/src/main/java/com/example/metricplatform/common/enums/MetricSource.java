package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 指标来源枚举
 */
@Getter
public enum MetricSource {

    EXISTING("EXISTING", "现有指标"),
    GENERATED("GENERATED", "生成指标");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    MetricSource(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
