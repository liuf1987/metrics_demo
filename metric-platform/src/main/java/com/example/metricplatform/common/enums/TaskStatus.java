package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务状态枚举
 */
@Getter
public enum TaskStatus {

    PENDING("pending", "待执行"),
    RUNNING("running", "执行中"),
    PAUSED("paused", "已暂停"),
    SUCCESS("success", "成功"),
    FAILED("failed", "失败"),
    CANCELLED("cancelled", "已取消");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    TaskStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}