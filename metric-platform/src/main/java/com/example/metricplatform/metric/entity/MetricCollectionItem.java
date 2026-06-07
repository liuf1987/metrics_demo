package com.example.metricplatform.metric.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标集合关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("metric_collection_item")
public class MetricCollectionItem extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 关联集合ID
     */
    @TableField("collection_id")
    private Long collectionId;

    /**
     * 关联指标ID
     */
    @TableField("metric_id")
    private Long metricId;

    /**
     * 排序顺序
     */
    @TableField("sort_order")
    private Integer sortOrder;
}
