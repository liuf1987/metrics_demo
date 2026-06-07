package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务执行模式枚举
 */
@Getter
public enum ExecuteMode {

    INSTANT("instant", "即时执行"),
    SCHEDULED("scheduled", "定时执行");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    ExecuteMode(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}