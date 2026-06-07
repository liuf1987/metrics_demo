package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 数据源类型枚举
 */
@Getter
public enum SourceType {

    TABLE("table", "表"),
    VIEW("view", "视图"),
    QUERY("query", "查询");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    SourceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
