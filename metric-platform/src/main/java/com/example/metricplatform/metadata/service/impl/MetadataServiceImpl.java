package com.example.metricplatform.metadata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.metricplatform.config.MetadataConfig;
import com.example.metricplatform.metadata.entity.DataSource;
import com.example.metricplatform.metadata.entity.FieldMetadata;
import com.example.metricplatform.common.enums.FieldType;
import com.example.metricplatform.common.enums.SourceType;
import com.example.metricplatform.common.exception.ErrorCode;
import com.example.metricplatform.metadata.mapper.DataSourceMapper;
import com.example.metricplatform.metadata.mapper.FieldMetadataMapper;
import com.example.metricplatform.metadata.service.MetadataService;
import com.example.metricplatform.metric.service.MetricConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSourceMapper dataSourceMapper;
    private final FieldMetadataMapper fieldMetadataMapper;
    private final MetricConfigService metricConfigService;
    private final MetadataConfig metadataConfig;

    public MetadataServiceImpl(JdbcTemplate jdbcTemplate, 
                               DataSourceMapper dataSourceMapper,
                               FieldMetadataMapper fieldMetadataMapper,
                               @Lazy MetricConfigService metricConfigService,
                               MetadataConfig metadataConfig) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSourceMapper = dataSourceMapper;
        this.fieldMetadataMapper = fieldMetadataMapper;
        this.metricConfigService = metricConfigService;
        this.metadataConfig = metadataConfig;
    }

    @Override
    @Transactional
    public void loadMetadata(String tableNamePattern) {
        String pattern = StringUtils.hasText(tableNamePattern) ? tableNamePattern : "%";

        List<Map<String, Object>> tables = queryTables(pattern);

        for (Map<String, Object> table : tables) {
            String tableName = (String) table.get("TABLE_NAME");
            
            // 检查是否需要排除该表
            if (metadataConfig.shouldExclude(tableName)) {
                log.info("跳过排除表: {}", tableName);
                continue;
            }
            
            String tableComment = (String) table.get("TABLE_COMMENT");

            DataSource dataSource = upsertDataSource(tableName, tableComment);

            List<Map<String, Object>> columns = queryColumns(tableName);

            for (Map<String, Object> column : columns) {
                String columnName = (String) column.get("COLUMN_NAME");
                String dataType = (String) column.get("DATA_TYPE");
                String columnComment = (String) column.get("COLUMN_COMMENT");
                String isNullable = (String) column.get("IS_NULLABLE");

                upsertFieldMetadata(dataSource.getId(), columnName, dataType, columnComment, "YES".equals(isNullable));
            }
        }
    }

    @Override
    public List<DataSource> getAllDataSources() {
        return dataSourceMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public List<FieldMetadata> getFieldsBySourceId(Long sourceId) {
        DataSource dataSource = dataSourceMapper.selectById(sourceId);
        if (dataSource == null) {
            log.warn("数据源不存在，sourceId: {}", sourceId);
            throw ErrorCode.PARAM_DATA_SOURCE_NOT_FOUND.exception(sourceId);
        }
        return fieldMetadataMapper.selectList(
                new LambdaQueryWrapper<FieldMetadata>()
                        .eq(FieldMetadata::getSourceId, sourceId)
        );
    }

    @Override
    @Transactional
    public void enableDataSource(Long sourceId, Boolean enabled) {
        DataSource dataSource = dataSourceMapper.selectById(sourceId);
        if (dataSource == null) {
            log.warn("数据源不存在，sourceId: {}", sourceId);
            throw ErrorCode.PARAM_DATA_SOURCE_NOT_FOUND.exception(sourceId);
        }
        
        log.info("{}数据源，sourceId: {}, sourceName: {}, sourceCode: {}", 
                enabled ? "启用" : "禁用", sourceId, dataSource.getSourceName(), dataSource.getSourceCode());
        
        dataSource.setEnabled(enabled);
        dataSourceMapper.updateById(dataSource);
        
        if (!enabled) {
            String reason = String.format("数据源[%s]已禁用", dataSource.getSourceName());
            metricConfigService.disableBySourceTable(dataSource.getSourceCode(), reason);
            log.info("完成禁用数据源{}及其相关指标", dataSource.getSourceCode());
        } else {
            log.info("完成启用数据源{}", dataSource.getSourceCode());
        }
    }

    @Override
    @Transactional
    public void enableField(Long fieldId, Boolean enabled) {
        FieldMetadata fieldMetadata = fieldMetadataMapper.selectById(fieldId);
        if (fieldMetadata == null) {
            log.warn("字段不存在，fieldId: {}", fieldId);
            throw ErrorCode.PARAM_FIELD_NOT_FOUND.exception(fieldId);
        }
        
        DataSource dataSource = dataSourceMapper.selectById(fieldMetadata.getSourceId());
        
        log.info("{}字段，fieldId: {}, fieldName: {}, sourceCode: {}", 
                enabled ? "启用" : "禁用", fieldId, fieldMetadata.getFieldName(), 
                dataSource != null ? dataSource.getSourceCode() : "unknown");
        
        fieldMetadata.setEnabled(enabled);
        fieldMetadataMapper.updateById(fieldMetadata);
        
        if (!enabled) {
            String sourceTable = dataSource != null ? dataSource.getSourceCode() : "unknown";
            String reason = String.format("字段[%s]已禁用", fieldMetadata.getFieldName());
            metricConfigService.disableByField(fieldId, reason);
            log.info("完成禁用字段{}及其相关指标", fieldMetadata.getFieldName());
        } else {
            log.info("完成启用字段{}", fieldMetadata.getFieldName());
        }
    }

    @Override
    public DataSource getDataSourceByCode(String sourceCode) {
        return dataSourceMapper.selectOne(
                new LambdaQueryWrapper<DataSource>()
                        .eq(DataSource::getSourceCode, sourceCode)
        );
    }

    @Override
    public DataSource getDataSourceById(Long sourceId) {
        return dataSourceMapper.selectById(sourceId);
    }

    @Override
    public FieldMetadata getFieldById(Long fieldId) {
        return fieldMetadataMapper.selectById(fieldId);
    }

    @Override
    @Transactional
    public void updateFieldProperties(Long fieldId, Boolean filterable, Boolean groupable,
                                      Boolean aggregatable, Boolean sortable,
                                      String fieldLabel) {
        FieldMetadata fieldMetadata = fieldMetadataMapper.selectById(fieldId);
        if (fieldMetadata == null) {
            log.warn("字段不存在，fieldId: {}", fieldId);
            throw ErrorCode.PARAM_FIELD_NOT_FOUND.exception(fieldId);
        }

        log.info("更新字段属性，fieldId: {}, fieldName: {}", fieldId, fieldMetadata.getFieldName());

        if (filterable != null) {
            fieldMetadata.setFilterable(filterable);
        }
        if (groupable != null) {
            fieldMetadata.setGroupable(groupable);
        }
        if (aggregatable != null) {
            fieldMetadata.setAggregatable(aggregatable);
        }
        if (sortable != null) {
            fieldMetadata.setSortable(sortable);
        }
        if (StringUtils.hasText(fieldLabel)) {
            fieldMetadata.setFieldLabel(fieldLabel);
        }

        fieldMetadataMapper.updateById(fieldMetadata);
        log.info("完成更新字段属性: {}", fieldMetadata.getFieldName());
    }

    @Override
    @Transactional
    public void batchUpdateFieldProperties(List<Long> fieldIds, Boolean filterable,
                                           Boolean groupable, Boolean aggregatable, Boolean sortable) {
        // 参数校验
        if (fieldIds == null || fieldIds.isEmpty()) {
            log.warn("参数校验失败：字段ID列表不能为空");
            throw ErrorCode.NULL_PARAMETER.exception("字段ID列表不能为空");
        }
        
        if (filterable == null && groupable == null && aggregatable == null && sortable == null) {
            log.warn("参数校验失败：至少需要指定一个要更新的属性");
            throw ErrorCode.NULL_PARAMETER.exception("至少需要指定一个要更新的属性");
        }
        
        log.info("批量更新字段属性，字段数量: {}", fieldIds.size());

        for (Long fieldId : fieldIds) {
            FieldMetadata fieldMetadata = fieldMetadataMapper.selectById(fieldId);
            if (fieldMetadata == null) {
                log.warn("字段不存在，fieldId: {}", fieldId);
                throw ErrorCode.PARAM_FIELD_NOT_FOUND.exception(fieldId);
            }
            
            if (filterable != null) {
                fieldMetadata.setFilterable(filterable);
            }
            if (groupable != null) {
                fieldMetadata.setGroupable(groupable);
            }
            if (aggregatable != null) {
                fieldMetadata.setAggregatable(aggregatable);
            }
            if (sortable != null) {
                fieldMetadata.setSortable(sortable);
            }
            fieldMetadataMapper.updateById(fieldMetadata);
        }

        log.info("完成批量更新字段属性");
    }

    private List<Map<String, Object>> queryTables(String tableNamePattern) {
        String sql = "SELECT TABLE_NAME, TABLE_COMMENT " +
                "FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME LIKE ?";
        return jdbcTemplate.queryForList(sql, tableNamePattern);
    }

    private List<Map<String, Object>> queryColumns(String tableName) {
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT, IS_NULLABLE " +
                "FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";
        return jdbcTemplate.queryForList(sql, tableName);
    }

    private DataSource upsertDataSource(String tableName, String tableComment) {
        DataSource existing = dataSourceMapper.selectOne(
                new LambdaQueryWrapper<DataSource>()
                        .eq(DataSource::getSourceCode, tableName)
        );

        DataSource dataSource;
        if (existing != null) {
            existing.setSourceName(StringUtils.hasText(tableComment) ? tableComment : tableName);
            existing.setDescription(tableComment);
            dataSourceMapper.updateById(existing);
            dataSource = existing;
        } else {
            dataSource = new DataSource();
            dataSource.setSourceCode(tableName);
            dataSource.setSourceName(StringUtils.hasText(tableComment) ? tableComment : tableName);
            dataSource.setSourceType(SourceType.TABLE);
            dataSource.setSourceExpr(tableName);
            dataSource.setDescription(tableComment);
            dataSource.setEnabled(true);
            dataSourceMapper.insert(dataSource);
        }

        return dataSource;
    }

    private void upsertFieldMetadata(Long sourceId, String columnName, String dataType, String columnComment, Boolean nullable) {
        FieldMetadata existing = fieldMetadataMapper.selectOne(
                new LambdaQueryWrapper<FieldMetadata>()
                        .eq(FieldMetadata::getSourceId, sourceId)
                        .eq(FieldMetadata::getFieldName, columnName)
        );

        FieldType fieldType = convertToFieldType(dataType);
        String fieldLabel = StringUtils.hasText(columnComment) ? columnComment : columnName;

        if (existing != null) {
            existing.setFieldType(fieldType);
            existing.setFieldLabel(fieldLabel);
            fieldMetadataMapper.updateById(existing);
        } else {
            FieldMetadata fieldMetadata = new FieldMetadata();
            fieldMetadata.setSourceId(sourceId);
            fieldMetadata.setFieldName(columnName);
            fieldMetadata.setFieldType(fieldType);
            fieldMetadata.setFieldLabel(fieldLabel);
            fieldMetadata.setFilterable(true);
            fieldMetadata.setGroupable(true);
            // 根据supportedAggregations判断是否可聚合：支持除count外的聚合函数才算可聚合
            fieldMetadata.setAggregatable(fieldType.getSupportedAggregations().size() > 1);
            fieldMetadata.setSortable(true);
            fieldMetadata.setEnabled(true);
            fieldMetadataMapper.insert(fieldMetadata);
        }
    }

    private FieldType convertToFieldType(String dataType) {
        String type = dataType.toLowerCase();

        if (type.contains("int") || type.contains("decimal") || type.contains("float") ||
                type.contains("double") || type.contains("numeric") || type.contains("bigint")) {
            return FieldType.NUMBER;
        } else if (type.contains("datetime") || type.contains("timestamp") ||
                type.contains("date") || type.contains("time")) {
            return FieldType.DATETIME;
        } else if (type.contains("json")) {
            return FieldType.JSON;
        } else {
            return FieldType.STRING;
        }
    }
}
