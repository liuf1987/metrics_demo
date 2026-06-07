package com.example.metricplatform.metadata.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.common.entity.BaseEntity;
import com.example.metricplatform.common.enums.FieldType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字段元数据实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "field_metadata", autoResultMap = true)
public class FieldMetadata extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 关联数据源ID
     */
    @TableField("source_id")
    private Long sourceId;

    /**
     * 字段名
     */
    @TableField("field_name")
    private String fieldName;

    /**
     * 字段类型
     */
    @TableField("field_type")
    private FieldType fieldType;

    /**
     * 显示名称
     */
    @TableField("field_label")
    private String fieldLabel;

    /**
     * 是否可筛选
     */
    @TableField("filterable")
    private Boolean filterable;

    /**
     * 是否可分组
     */
    @TableField("groupable")
    private Boolean groupable;

    /**
     * 是否可聚合
     */
    @TableField("aggregatable")
    private Boolean aggregatable;

    /**
     * 是否可排序
     */
    @TableField("sortable")
    private Boolean sortable;

    /**
     * 启用状态
     */
    @TableField("enabled")
    private Boolean enabled;
}
