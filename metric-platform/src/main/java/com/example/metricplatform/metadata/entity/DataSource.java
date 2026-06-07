package com.example.metricplatform.metadata.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.common.entity.BaseEntity;
import com.example.metricplatform.common.enums.SourceType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源配置实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("data_source")
public class DataSource extends BaseEntity {

    /**
     * 主键ID
     */
    @TableId("id")
    private Long id;

    /**
     * 数据源编码
     */
    @TableField("source_code")
    private String sourceCode;

    /**
     * 数据源名称
     */
    @TableField("source_name")
    private String sourceName;

    /**
     * 数据源类型
     */
    @TableField("source_type")
    private SourceType sourceType;

    /**
     * 表名或SQL视图
     */
    @TableField("source_expr")
    private String sourceExpr;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 启用状态
     */
    @TableField("enabled")
    private Boolean enabled;
}
