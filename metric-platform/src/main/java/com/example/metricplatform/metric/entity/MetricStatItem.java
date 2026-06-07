package com.example.metricplatform.metric.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.common.entity.BaseEntity;
import com.example.metricplatform.common.enums.AggregationType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标统计项实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("metric_stat_item")
public class MetricStatItem extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 关联指标配置ID
     */
    @TableField("metric_id")
    private Long metricId;

    /**
     * 统计项编码
     */
    @TableField("item_code")
    private String itemCode;

    /**
     * 统计项名称
     */
    @TableField("item_name")
    private String itemName;

    /**
     * 统计字段
     */
    @TableField("field_name")
    private String fieldName;

    /**
     * 聚合方式
     */
    @TableField("aggregation")
    private AggregationType aggregation;

    /**
     * 显示顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
