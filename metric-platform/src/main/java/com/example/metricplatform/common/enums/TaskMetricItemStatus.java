package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 任务子指标执行状态枚举
 */
@Getter
public enum TaskMetricItemStatus {
    /**
     * 待执行
     */
    PENDING("pending", "待执行"),
    
    /**
     * 执行中
     */
    RUNNING("running", "执行中"),
    
    /**
     * 执行成功
     */
    SUCCESS("success", "执行成功"),
    
    /**
     * 执行失败
     */
    FAILED("failed", "执行失败");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    TaskMetricItemStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TaskMetricItemStatus fromCode(String code) {
        for (TaskMetricItemStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}
