package com.example.metricplatform.metric.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标集合实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("metric_collection")
public class MetricCollection extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 集合编码
     */
    @TableField("collection_code")
    private String collectionCode;

    /**
     * 集合名称
     */
    @TableField("collection_name")
    private String collectionName;

    /**
     * 集合描述
     */
    @TableField("description")
    private String description;

    /**
     * 目标角色/部门
     */
    @TableField("target_role")
    private String targetRole;

    /**
     * 启用状态
     */
    @TableField("enabled")
    private Boolean enabled;
}
