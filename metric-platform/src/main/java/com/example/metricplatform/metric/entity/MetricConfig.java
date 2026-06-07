package com.example.metricplatform.metric.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.example.metricplatform.metric.dto.FilterGroup;
import com.example.metricplatform.metric.dto.SortRule;
import com.example.metricplatform.metric.dto.WindowFunctions;
import com.example.metricplatform.common.entity.BaseEntity;
import com.example.metricplatform.metric.handler.CustomJacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 指标配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "metric_config", autoResultMap = true)
public class MetricConfig extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 指标编码
     */
    @TableField("metric_code")
    private String metricCode;

    /**
     * 指标名称
     */
    @TableField("metric_name")
    private String metricName;

    /**
     * 指标描述
     */
    @TableField("metric_desc")
    private String metricDesc;

    /**
     * 来源表名
     */
    @TableField("source_table")
    private String sourceTable;

    /**
     * 业务分类编码
     */
    @TableField("category_code")
    private String categoryCode;

    /**
     * 业务分类名称
     */
    @TableField("category_name")
    private String categoryName;

    /**
     * 分组维度数组
     */
    @TableField(value = "group_by_fields", typeHandler = JacksonTypeHandler.class)
    private List<String> groupByFields;

    /**
     * 筛选条件
     */
    @TableField(value = "filter_conditions", typeHandler = JacksonTypeHandler.class)
    private FilterGroup filterConditions;

    /**
     * HAVING条件
     */
    @TableField(value = "having_conditions", typeHandler = JacksonTypeHandler.class)
    private FilterGroup havingConditions;

    /**
     * 排序规则
     */
    @TableField(value = "sort_rules", typeHandler = JacksonTypeHandler.class)
    private List<SortRule> sortRules;

    /**
     * LIMIT值
     */
    @TableField("limit_value")
    private Integer limitValue;

    /**
     * 窗口函数定义
     */
    @TableField(value = "window_functions", typeHandler = CustomJacksonTypeHandler.class)
    private WindowFunctions windowFunctions;

    /**
     * 查询次数统计
     */
    @TableField("query_count")
    private Integer queryCount;

    /**
     * 启用/停用
     */
    @TableField("enabled")
    private Boolean enabled;

    /**
     * 禁用原因
     */
    @TableField("disabled_reason")
    private String disabledReason;

    /**
     * 统计项列表（不映射到数据库）
     */
    @TableField(exist = false)
    private List<MetricStatItem> statItems;
}
