package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务类型枚举
 */
@Getter
public enum TaskType {

    QUERY("query", "查询"),
    EXPORT("export", "导出");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    TaskType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}