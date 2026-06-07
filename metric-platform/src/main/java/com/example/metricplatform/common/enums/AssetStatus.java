package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 素材审核状态枚举
 */
@Getter
public enum AssetStatus {

    APPROVED("approved", "已通过"),
    REJECTED("rejected", "已拒绝"),
    PENDING("pending", "待审核");

    @EnumValue
    @JsonValue
    private final String code;
    private final String desc;

    AssetStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
