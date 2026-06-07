package com.example.metricplatform.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.example.metricplatform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * AI推荐记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "ai_recommendation", autoResultMap = true)
public class AIRecommendation extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 用户查询语句
     */
    @TableField("user_query")
    private String userQuery;

    /**
     * 推荐的指标编码列表
     */
    @TableField(value = "recommended_metrics", typeHandler = JacksonTypeHandler.class)
    private Object recommendedMetrics;

    /**
     * 新指标生成建议
     */
    @TableField("new_metric_suggest")
    private String newMetricSuggest;

    /**
     * 生成的指标配置
     */
    @TableField(value = "generated_metric_config", typeHandler = JacksonTypeHandler.class)
    private Object generatedMetricConfig;

    /**
     * 验证结果
     */
    @TableField("validation_result")
    private String validationResult;

    /**
     * 状态
     */
    @TableField("status")
    private String status;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 推荐置信度
     */
    @TableField("confidence")
    private BigDecimal confidence;
}
