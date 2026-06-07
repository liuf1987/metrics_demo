package com.example.metricplatform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 满意度枚举
 */
@Getter
public enum Satisfaction {

    SATISFIED("SATISFIED", "满意", 0),
    DISSATISFIED("DISSATISFIED", "不满意", 1);

    @EnumValue
    private final String code;
    private final String desc;
    private final Integer index;

    Satisfaction(String code, String desc, Integer index) {
        this.code = code;
        this.desc = desc;
        this.index = index;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static Satisfaction fromValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String strValue = ((String) value).toUpperCase();
            for (Satisfaction s : values()) {
                if (s.code.equals(strValue)) {
                    return s;
                }
            }
        }
        if (value instanceof Integer || value instanceof Number) {
            int intValue = ((Number) value).intValue();
            for (Satisfaction s : values()) {
                if (s.index.equals(intValue)) {
                    return s;
                }
            }
        }
        throw new IllegalArgumentException("无效的满意度值: " + value);
    }
}
