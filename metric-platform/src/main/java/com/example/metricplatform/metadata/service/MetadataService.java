package com.example.metricplatform.metadata.service;

import com.example.metricplatform.metadata.entity.DataSource;
import com.example.metricplatform.metadata.entity.FieldMetadata;

import java.util.List;

/**
 * 元数据服务接口
 */
public interface MetadataService {

    /**
     * 加载元数据
     * @param tableNamePattern 表名模式，支持通配符
     */
    void loadMetadata(String tableNamePattern);

    /**
     * 获取所有数据源
     * @return 数据源列表
     */
    List<DataSource> getAllDataSources();

    /**
     * 根据数据源ID获取字段列表
     * @param sourceId 数据源ID
     * @return 字段列表
     */
    List<FieldMetadata> getFieldsBySourceId(Long sourceId);

    /**
     * 启用/禁用数据源
     * @param sourceId 数据源ID
     * @param enabled 是否启用
     */
    void enableDataSource(Long sourceId, Boolean enabled);

    /**
     * 启用/禁用字段
     * @param fieldId 字段ID
     * @param enabled 是否启用
     */
    void enableField(Long fieldId, Boolean enabled);

    /**
     * 根据编码获取数据源
     * @param sourceCode 数据源编码
     * @return 数据源
     */
    DataSource getDataSourceByCode(String sourceCode);

    /**
     * 根据ID获取数据源
     * @param sourceId 数据源ID
     * @return 数据源
     */
    DataSource getDataSourceById(Long sourceId);

    /**
     * 根据ID获取字段
     * @param fieldId 字段ID
     * @return 字段
     */
    FieldMetadata getFieldById(Long fieldId);

    /**
     * 更新字段属性
     * @param fieldId 字段ID
     * @param filterable 是否可筛选
     * @param groupable 是否可分组
     * @param aggregatable 是否可聚合
     * @param sortable 是否可排序
     * @param fieldLabel 字段显示名称
     */
    void updateFieldProperties(Long fieldId, Boolean filterable, Boolean groupable, 
                                Boolean aggregatable, Boolean sortable, 
                                String fieldLabel);

    /**
     * 批量更新字段属性
     * @param fieldIds 字段ID列表
     * @param filterable 是否可筛选
     * @param groupable 是否可分组
     * @param aggregatable 是否可聚合
     * @param sortable 是否可排序
     */
    void batchUpdateFieldProperties(List<Long> fieldIds, Boolean filterable, 
                                    Boolean groupable, Boolean aggregatable, Boolean sortable);
}
