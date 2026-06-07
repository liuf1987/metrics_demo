package com.example.metricplatform.metric.service.impl;

import com.example.metricplatform.metric.dto.SortRule;
import com.example.metricplatform.metric.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.metricplatform.metadata.entity.DataSource;
import com.example.metricplatform.metadata.entity.FieldMetadata;
import com.example.metricplatform.metric.entity.MetricConfig;
import com.example.metricplatform.metric.entity.MetricStatItem;
import com.example.metricplatform.common.exception.ErrorCode;
import com.example.metricplatform.metric.mapper.MetricStatItemMapper;
import com.example.metricplatform.metric.service.MetricConfigService;
import com.example.metricplatform.metadata.service.MetadataService;
import com.example.metricplatform.metric.service.QueryEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 查询引擎服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QueryEngineServiceImpl implements QueryEngineService {

    private final MetricConfigService metricConfigService;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final MetadataService metadataService;
    private final MetricStatItemMapper metricStatItemMapper;

    @Override
    public List<Map<String, Object>> execute(String metricCode, MetricExecuteRequest request) {
        log.info("开始执行指标查询，metricCode: {}", metricCode);
        
        // 获取指标配置
        MetricConfig metric = metricConfigService.getByCode(metricCode);
        if (!Boolean.TRUE.equals(metric.getEnabled())) {
            log.error("指标已停用，metricCode: {}", metricCode);
            throw ErrorCode.PARAM_DISABLED.exception(metricCode);
        }

        // 验证元数据
        validateMetadata(metric, request);

        // 生成SQL
        String sql = generateSql(metricCode, request);
        log.info("执行指标查询，metricCode: {}, SQL: {}", metricCode, sql);

        try {
            // 执行查询
            List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
            
            // 增加查询次数
            metricConfigService.incrementQueryCount(metricCode);
            
            log.info("指标查询执行成功，metricCode: {}, 结果数量: {}", metricCode, result.size());
            return result;
        } catch (Exception e) {
            log.error("执行查询失败，metricCode: {}, error: {}", metricCode, e.getMessage(), e);
            throw ErrorCode.QUERY_EXECUTE_ERROR.exception(e.getMessage());
        }
    }

    @Override
    public String generateSql(String metricCode, MetricExecuteRequest request) {
        log.info("开始生成SQL，metricCode: {}", metricCode);
        
        MetricConfig metric = metricConfigService.getByCode(metricCode);
        if (metric == null) {
            log.error("指标不存在，metricCode: {}", metricCode);
            throw ErrorCode.METRIC_NOT_FOUND.exception(metricCode);
        }

        // 验证元数据
        validateMetadata(metric, request);

        // 获取聚合统计项配置（已自动填充）
        List<MetricStatItem> statItems = metric.getStatItems();

        StringBuilder sql = new StringBuilder();
        
        // 解析窗口函数（添加兜底机制）
        Map<String, Object> params = request != null ? request.getParams() : null;
        Object windowFuncObj = metric.getWindowFunctions();
        List<String> windowFunctionExpressions = parseWindowFunctions(windowFuncObj, params);
        
        // 兜底：如果直接解析失败，尝试将对象转换为字符串再解析
        if (windowFunctionExpressions == null || windowFunctionExpressions.isEmpty()) {
            try {
                String windowFuncStr = objectMapper.writeValueAsString(windowFuncObj);
                log.info("兜底解析窗口函数: {}", windowFuncStr);
                windowFunctionExpressions = parseWindowFunctions(windowFuncStr, params);
            } catch (Exception e) {
                log.warn("兜底解析窗口函数失败: {}", e.getMessage());
            }
        }
        
        boolean hasWindowFunctions = windowFunctionExpressions != null && !windowFunctionExpressions.isEmpty();
        
        // 生成 SELECT 子句
        sql.append("SELECT ");
        
        // 添加 GROUP BY 字段到 SELECT
        Set<String> groupByFields = new HashSet<>();
        if (metric.getGroupByFields() != null) {
            String groupBySql = parseGroupByFields(metric.getGroupByFields());
            if (groupBySql != null && !groupBySql.isEmpty()) {
                sql.append(groupBySql);
                extractFieldsFromObject(metric.getGroupByFields(), groupByFields);
            }
        }
        
        // 添加聚合字段到 SELECT
        // 注意：当有分组时，即使有窗口函数，我们也需要添加聚合字段
        // 因为 HAVING 和 ORDER BY 子句中可能引用这些聚合别名
        if (statItems != null && !statItems.isEmpty()) {
            for (MetricStatItem item : statItems) {
                // 如果前面有内容，需要加逗号
                if (!groupByFields.isEmpty() || sql.length() > "SELECT ".length()) {
                    sql.append(", ");
                }
                String aggregationSql = buildAggregationExpression(item);
                sql.append(aggregationSql).append(" AS ").append(item.getItemCode());
            }
        }
        
        // 添加窗口函数到 SELECT
        if (hasWindowFunctions) {
            // 如果有窗口函数但没有分组，需要先添加所有字段
            if (groupByFields.isEmpty() && (statItems == null || statItems.isEmpty())) {
                sql.append("*");
            }
            for (String windowExpr : windowFunctionExpressions) {
                // 如果前面有内容，需要加逗号
                if (sql.length() > "SELECT ".length()) {
                    sql.append(", ");
                }
                sql.append(windowExpr);
            }
        }
        
        // 如果没有分组，没有聚合，也没有窗口函数，则选择所有字段
        if (groupByFields.isEmpty() && (statItems == null || statItems.isEmpty()) && !hasWindowFunctions) {
            sql.append("*");
        }
        
        sql.append(" FROM ").append(metric.getSourceTable());
        
        // 添加WHERE条件
        if (metric.getFilterConditions() != null) {
            sql.append(" WHERE 1=1");
            
            // 处理指标配置中的筛选条件
            if (metric.getFilterConditions() != null) {
                String filterSql = parseFilterConditions(metric.getFilterConditions(), params);
                if (filterSql != null && !filterSql.isEmpty()) {
                    sql.append(" AND ").append(filterSql);
                }
            }
        }

        // 添加GROUP BY
        if (metric.getGroupByFields() != null) {
            String groupBySql = parseGroupByFields(metric.getGroupByFields());
            if (groupBySql != null && !groupBySql.isEmpty()) {
                sql.append(" GROUP BY ").append(groupBySql);
            }
        }

        // 添加HAVING条件
        if (metric.getHavingConditions() != null) {
            String havingSql = parseFilterConditions(metric.getHavingConditions(), params);
            if (havingSql != null && !havingSql.isEmpty()) {
                sql.append(" HAVING ").append(havingSql);
            }
        }

        // 添加ORDER BY
        if (metric.getSortRules() != null) {
            String orderBySql = parseSortRules(metric.getSortRules(), params);
            if (orderBySql != null && !orderBySql.isEmpty()) {
                sql.append(" ORDER BY ").append(orderBySql);
            }
        }

        // 添加LIMIT
        Integer limit = metric.getLimitValue();
        if (request != null && request.getLimitValue() != null) {
            limit = request.getLimitValue();
        }
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ").append(limit);
        }

        String resultSql = sql.toString();
        
        // 替换动态参数占位符 {{param_name}}
        if (request != null && request.getParams() != null) {
            resultSql = replaceDynamicParams(resultSql, request.getParams());
        }
        
        log.info("SQL生成完成，metricCode: {}, SQL: {}", metricCode, resultSql);
        return resultSql;
    }
    
    /**
     * 替换动态参数占位符 {{param_name}}
     */
    private String replaceDynamicParams(String sql, Map<String, Object> params) {
        String result = sql;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String paramName = entry.getKey();
            Object paramValue = entry.getValue();
            String placeholder = "{{" + paramName + "}}";
            
            if (result.contains(placeholder)) {
                String valueStr;
                if (paramValue instanceof String) {
                    valueStr = "'" + paramValue + "'";
                } else {
                    valueStr = String.valueOf(paramValue);
                }
                result = result.replace(placeholder, valueStr);
            }
        }
        return result;
    }
    
    /**
     * 构建聚合表达式
     */
    private String buildAggregationExpression(MetricStatItem item) {
        String fieldName = item.getFieldName();
        String aggregation = item.getAggregation() != null ? item.getAggregation().name().toLowerCase() : "count";
        
        return switch (aggregation) {
            case "count" -> "COUNT(" + fieldName + ")";
            case "sum" -> "SUM(" + fieldName + ")";
            case "avg" -> "AVG(" + fieldName + ")";
            case "max" -> "MAX(" + fieldName + ")";
            case "min" -> "MIN(" + fieldName + ")";
            default -> "COUNT(" + fieldName + ")";
        };
    }

    private void validateMetadata(MetricConfig metric, MetricExecuteRequest request) {
        String sourceTable = metric.getSourceTable();
        if (!StringUtils.hasText(sourceTable)) {
            log.error("源表名为空，metricCode: {}", metric.getMetricCode());
            throw ErrorCode.PARAM_SOURCE_TABLE_EMPTY.exception();
        }

        log.info("开始验证元数据，metricCode: {}, sourceTable: {}", metric.getMetricCode(), sourceTable);

        DataSource dataSource = metadataService.getDataSourceByCode(sourceTable);
        if (dataSource == null) {
            log.error("数据源不存在，metricCode: {}, sourceTable: {}", metric.getMetricCode(), sourceTable);
            throw ErrorCode.PARAM_DATA_SOURCE_NOT_FOUND.exception(sourceTable);
        }
        if (!Boolean.TRUE.equals(dataSource.getEnabled())) {
            log.error("数据源已禁用，metricCode: {}, sourceTable: {}", metric.getMetricCode(), sourceTable);
            throw ErrorCode.PARAM_DATA_SOURCE_DISABLED.exception(sourceTable);
        }

        // 构建字段映射表：字段名 -> 字段元数据
        List<FieldMetadata> fields = metadataService.getFieldsBySourceId(dataSource.getId());
        Map<String, FieldMetadata> fieldMap = new HashMap<>();
        Set<String> enabledFieldNames = new HashSet<>();
        for (FieldMetadata field : fields) {
            fieldMap.put(field.getFieldName().toLowerCase(), field);
            if (Boolean.TRUE.equals(field.getEnabled())) {
                enabledFieldNames.add(field.getFieldName().toLowerCase());
            }
        }

        // 验证分组字段
        Set<String> groupByFields = extractGroupByFields(metric);
        for (String fieldName : groupByFields) {
            String lowerFieldName = fieldName.toLowerCase();
            if (!fieldMap.containsKey(lowerFieldName)) {
                log.error("分组字段不存在，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                throw ErrorCode.QUERY_GROUP_FIELD_NOT_FOUND.exception(fieldName);
            }
            FieldMetadata field = fieldMap.get(lowerFieldName);
            if (!Boolean.TRUE.equals(field.getEnabled())) {
                log.error("分组字段已禁用，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                throw ErrorCode.QUERY_GROUP_FIELD_DISABLED.exception(fieldName);
            }
            if (!Boolean.TRUE.equals(field.getGroupable())) {
                log.error("分组功能已被禁用，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                throw ErrorCode.QUERY_GROUP_FIELD_NOT_ALLOWED.exception(fieldName);
            }
        }

        // 验证过滤字段
        Set<String> filterFields = extractFilterFields(metric, request);
        for (String fieldName : filterFields) {
            // 跳过看起来像是表达式的字段名（包含运算符）
            if (isExpression(fieldName)) {
                log.info("过滤字段看起来是表达式，跳过验证，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                continue;
            }
            
            String lowerFieldName = fieldName.toLowerCase();
            if (!fieldMap.containsKey(lowerFieldName)) {
                log.error("过滤字段不存在，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                throw ErrorCode.QUERY_FILTER_FIELD_NOT_FOUND.exception(fieldName);
            }
            FieldMetadata field = fieldMap.get(lowerFieldName);
            if (!Boolean.TRUE.equals(field.getEnabled())) {
                log.error("过滤字段已禁用，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                throw ErrorCode.QUERY_FILTER_FIELD_DISABLED.exception(fieldName);
            }
            if (!Boolean.TRUE.equals(field.getFilterable())) {
                log.error("过滤功能已被禁用，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                throw ErrorCode.QUERY_FILTER_FIELD_NOT_ALLOWED.exception(fieldName);
            }
        }

        // 验证排序字段（数据库字段，不是聚合别名）
        Set<String> sortFields = extractSortFields(metric, fieldMap);
        for (String fieldName : sortFields) {
            // 跳过看起来像是表达式的字段名（包含运算符）
            if (isExpression(fieldName)) {
                log.info("排序字段看起来是表达式，跳过验证，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                continue;
            }
            
            String lowerFieldName = fieldName.toLowerCase();
            FieldMetadata field = fieldMap.get(lowerFieldName);
            if (field != null) { // 只验证数据库字段，聚合别名不需要验证
                if (!Boolean.TRUE.equals(field.getEnabled())) {
                    log.error("排序字段已禁用，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                    throw ErrorCode.QUERY_SORT_FIELD_DISABLED.exception(fieldName);
                }
                if (!Boolean.TRUE.equals(field.getSortable())) {
                    log.error("排序功能已被禁用，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                    throw ErrorCode.QUERY_SORT_FIELD_NOT_ALLOWED.exception(fieldName);
                }
            }
        }

        // 验证窗口函数字段
        Set<String> windowFields = extractWindowFunctionFields(metric);
        Set<String> aggregateItemCodes = new HashSet<>();
        // 收集所有聚合统计项的 itemCode（别名）
        List<MetricStatItem> statItems = metric.getStatItems();
        if (statItems != null) {
            for (MetricStatItem item : statItems) {
                aggregateItemCodes.add(item.getItemCode().toLowerCase());
            }
        }
        for (String fieldName : windowFields) {
            String lowerFieldName = fieldName.toLowerCase();
            // 如果是聚合统计项的别名，跳过验证
            if (aggregateItemCodes.contains(lowerFieldName)) {
                continue;
            }
            if (!fieldMap.containsKey(lowerFieldName)) {
                log.error("窗口函数字段不存在，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                throw ErrorCode.QUERY_WINDOW_FIELD_NOT_FOUND.exception(fieldName);
            }
            FieldMetadata field = fieldMap.get(lowerFieldName);
            if (!Boolean.TRUE.equals(field.getEnabled())) {
                log.error("窗口函数字段已禁用，metricCode: {}, fieldName: {}", metric.getMetricCode(), fieldName);
                throw ErrorCode.QUERY_WINDOW_FIELD_DISABLED.exception(fieldName);
            }
            // 窗口函数的字段不需要检查 groupable/sortable，因为 PARTITION BY/ORDER BY 可以使用任何字段
        }

        log.info("元数据验证通过，metricCode: {}", metric.getMetricCode());
    }

    /**
     * 提取分组字段
     */
    private Set<String> extractGroupByFields(MetricConfig metric) {
        Set<String> fieldNames = new HashSet<>();
        extractFieldsFromObject(metric.getGroupByFields(), fieldNames);
        return fieldNames;
    }

    /**
     * 提取过滤字段（仅指标配置中的 WHERE 过滤）
     */
    private Set<String> extractFilterFields(MetricConfig metric, MetricExecuteRequest request) {
        Set<String> fieldNames = new HashSet<>();
        extractFieldsFromObject(metric.getFilterConditions(), fieldNames);
        return fieldNames;
    }

    /**
     * 提取排序字段（仅提取数据库字段，排除聚合别名）
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractSortFields(MetricConfig metric, Map<String, FieldMetadata> fieldMap) {
        Set<String> fieldNames = new HashSet<>();
        Object sortRules = metric.getSortRules();
        if (sortRules == null) {
            return fieldNames;
        }

        try {
            List<Map<String, Object>> rules;
            if (sortRules instanceof String) {
                String str = (String) sortRules;
                str = str.trim();
                log.info("排序字段配置字符串: {}", str);
                if (str.startsWith("[")) {
                    rules = objectMapper.readValue(str, 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                } else {
                    return fieldNames;
                }
            } else if (sortRules instanceof List) {
                rules = (List<Map<String, Object>>) sortRules;
            } else {
                return fieldNames;
            }

            for (Map<String, Object> rule : rules) {
                Object fieldObj = rule.get("field");
                if (fieldObj != null) {
                    String fieldName = String.valueOf(fieldObj);
                    // 只收集数据库字段，不收集聚合别名
                    if (fieldMap.containsKey(fieldName.toLowerCase())) {
                        fieldNames.add(fieldName);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析排序字段失败: {}", e.getMessage());
        }

        return fieldNames;
    }

    /**
     * 提取窗口函数字段（PARTITION BY 和 ORDER BY 中的字段）
     */
    @SuppressWarnings("unchecked")
    private Set<String> extractWindowFunctionFields(MetricConfig metric) {
        Set<String> fieldNames = new HashSet<>();
        Object windowFunctions = metric.getWindowFunctions();
        if (windowFunctions == null) {
            return fieldNames;
        }

        try {
            // 统一转换为JSON字符串解析，避免类型判断问题
            String jsonStr;
            if (windowFunctions instanceof String) {
                jsonStr = (String) windowFunctions;
            } else {
                jsonStr = objectMapper.writeValueAsString(windowFunctions);
            }
            log.info("窗口函数字段配置: {}", jsonStr);

            if (!StringUtils.hasText(jsonStr)) {
                return fieldNames;
            }

            List<Map<String, Object>> functions;
            if (jsonStr.startsWith("[")) {
                functions = objectMapper.readValue(jsonStr, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            } else if (jsonStr.startsWith("{")) {
                Map<String, Object> funcMap = objectMapper.readValue(jsonStr, Map.class);
                if (funcMap.containsKey("functions")) {
                    Object funcsObj = funcMap.get("functions");
                    if (funcsObj instanceof List) {
                        functions = (List<Map<String, Object>>) funcsObj;
                    } else {
                        return fieldNames;
                    }
                } else {
                    functions = Collections.singletonList(funcMap);
                }
            } else {
                return fieldNames;
            }

            for (Map<String, Object> func : functions) {
                // 检查字段配置格式
                if (func.containsKey("name") && func.containsKey("partition_by") && func.containsKey("order_by")) {
                    // 从 partition_by 提取字段
                    Object partitionByObj = func.get("partition_by");
                    if (partitionByObj instanceof List) {
                        List<Object> partitionBy = (List<Object>) partitionByObj;
                        for (Object part : partitionBy) {
                            if (part != null) {
                                fieldNames.add(String.valueOf(part));
                            }
                        }
                    }

                    // 从 order_by 提取字段
                    Object orderByObj = func.get("order_by");
                    if (orderByObj instanceof List) {
                        List<Object> orderBy = (List<Object>) orderByObj;
                        for (Object order : orderBy) {
                            if (order instanceof Map) {
                                Map<String, Object> orderMap = (Map<String, Object>) order;
                                Object fieldObj = orderMap.get("field");
                                if (fieldObj != null) {
                                    fieldNames.add(String.valueOf(fieldObj));
                                }
                            } else if (order != null) {
                                // 如果是字符串，直接取
                                String orderStr = String.valueOf(order);
                                String field = orderStr.split("\\s+")[0];
                                if (StringUtils.hasText(field)) {
                                    fieldNames.add(field);
                                }
                            }
                        }
                    }
                } else if (func.containsKey("expr")) {
                    // 从 expr 表达式中提取字段
                    Object exprObj = func.get("expr");
                    if (exprObj instanceof String) {
                        extractFieldsFromWindowExpression((String) exprObj, fieldNames);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析窗口函数字段失败: {}", e.getMessage());
        }

        return fieldNames;
    }

    private Set<String> extractFieldNames(MetricConfig metric, MetricExecuteRequest request) {
        Set<String> fieldNames = new HashSet<>();
        // 只验证实际参与数据库查询的字段
        // GROUP BY、WHERE 中的字段必须是数据库原有字段
        // HAVING 中的字段可以是聚合字段别名（如 asset_count），不应在此验证
        extractFieldsFromObject(metric.getGroupByFields(), fieldNames);
        extractFieldsFromObject(metric.getFilterConditions(), fieldNames);
        // extractFieldsFromObject(metric.getHavingConditions(), fieldNames); // HAVING 可以使用聚合别名
        // 注意：sortRules 中的字段可能是聚合别名（如 asset_count），不是数据库原有字段
        // 聚合别名是 SQL 执行后产生的别名，不应在数据库表字段验证阶段检查
        // extractFieldsFromObject(metric.getSortRules(), fieldNames);
        // 提取窗口函数中的数据库字段（排除窗口函数生成的别名）
        extractFieldsFromWindowFunctions(metric.getWindowFunctions(), fieldNames);
        return fieldNames;
    }
    
    /**
     * 从窗口函数配置中提取数据库字段
     */
    @SuppressWarnings("unchecked")
    private void extractFieldsFromWindowFunctions(Object windowFunctions, Set<String> fieldNames) {
        if (windowFunctions == null) {
            return;
        }
        
        try {
            List<Map<String, Object>> functions;
            if (windowFunctions instanceof String) {
                String str = (String) windowFunctions;
                if (str.startsWith("[")) {
                    functions = objectMapper.readValue(str, 
                            objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                } else {
                    return;
                }
            } else if (windowFunctions instanceof List) {
                functions = (List<Map<String, Object>>) windowFunctions;
            } else {
                return;
            }
            
            for (Map<String, Object> func : functions) {
                Object expr = func.get("expr");
                if (expr instanceof String) {
                    // 从窗口函数表达式中提取字段名
                    extractFieldsFromWindowExpression((String) expr, fieldNames);
                }
            }
        } catch (Exception e) {
            log.warn("解析窗口函数字段失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从窗口函数表达式中提取字段名
     */
    private void extractFieldsFromWindowExpression(String expr, Set<String> fieldNames) {
        // 简单提取 PARTITION BY 和 ORDER BY 后面的字段名
        // 例如: ROW_NUMBER() OVER (PARTITION BY platform ORDER BY play_count DESC) AS rank_in_platform
        // 需要提取: platform, play_count
        
        // 移除 AS 后面的别名部分
        int asIndex = expr.toUpperCase().indexOf(" AS ");
        if (asIndex > 0) {
            expr = expr.substring(0, asIndex);
        }
        
        // 提取 PARTITION BY 后面的字段
        int partitionIndex = expr.toUpperCase().indexOf("PARTITION BY ");
        if (partitionIndex > 0) {
            String partitionPart = expr.substring(partitionIndex + 13);
            int orderIndex = partitionPart.toUpperCase().indexOf(" ORDER BY ");
            if (orderIndex > 0) {
                partitionPart = partitionPart.substring(0, orderIndex);
            }
            extractFieldNamesFromCommaSeparated(partitionPart, fieldNames);
        }
        
        // 提取 ORDER BY 后面的字段
        int orderIndex = expr.toUpperCase().indexOf(" ORDER BY ");
        if (orderIndex > 0) {
            String orderPart = expr.substring(orderIndex + 10);
            extractFieldNamesFromOrderBy(orderPart, fieldNames);
        }
    }
    
    /**
     * 从逗号分隔的字符串中提取字段名
     */
    private void extractFieldNamesFromCommaSeparated(String str, Set<String> fieldNames) {
        String[] parts = str.split(",");
        for (String part : parts) {
            String field = part.trim().split("\\s+")[0]; // 取第一个词（去掉 DESC/ASC）
            if (StringUtils.hasText(field)) {
                fieldNames.add(field);
            }
        }
    }
    
    /**
     * 从 ORDER BY 子句中提取字段名
     */
    private void extractFieldNamesFromOrderBy(String str, Set<String> fieldNames) {
        String[] parts = str.split(",");
        for (String part : parts) {
            String[] tokens = part.trim().split("\\s+");
            if (tokens.length > 0 && StringUtils.hasText(tokens[0])) {
                fieldNames.add(tokens[0]);
            }
        }
    }

    private void extractFieldsFromObject(Object obj, Set<String> fieldNames) {
        if (obj == null) {
            return;
        }
        try {
            Object converted = objectMapper.convertValue(obj, Object.class);
            extractFieldsFromValue(converted, fieldNames);
        } catch (Exception e) {
            // 忽略转换异常，简化处理
        }
    }

    @SuppressWarnings("unchecked")
    private void extractFieldsFromValue(Object value, Set<String> fieldNames) {
        if (value == null) {
            return;
        }
        if (value instanceof String) {
            String str = (String) value;
            // 跳过占位符值 {{param_name}}，它们不是字段名
            if (StringUtils.hasText(str) && !(str.startsWith("{{") && str.endsWith("}}"))) {
                fieldNames.add(str);
            }
        } else if (value instanceof List) {
            List<Object> list = (List<Object>) value;
            for (Object item : list) {
                extractFieldsFromValue(item, fieldNames);
            }
        } else if (value instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) value;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                if ("field".equalsIgnoreCase(key) || "fieldName".equalsIgnoreCase(key) || "column".equalsIgnoreCase(key)) {
                    Object fieldValue = entry.getValue();
                    if (fieldValue instanceof String) {
                        fieldNames.add((String) fieldValue);
                    }
                } else if ("conditions".equalsIgnoreCase(key) || "filters".equalsIgnoreCase(key)) {
                    // 只对条件数组进行递归处理，其他值如 value、operator、logic 不需要递归
                    extractFieldsFromValue(entry.getValue(), fieldNames);
                }
                // 对于 value、operator、logic 等关键字的值，不进行递归处理，避免把值当作字段名
            }
        }
    }

    /**
     * 解析筛选条件（向后兼容版本，无params参数）
     */
    @SuppressWarnings("unchecked")
    private String parseFilterConditions(Object conditions) {
        return parseFilterConditions(conditions, null);
    }
    
    /**
     * 解析筛选条件（接受params参数版本）
     */
    @SuppressWarnings("unchecked")
    private String parseFilterConditions(Object conditions, Map<String, Object> params) {
        try {
            if (conditions instanceof String) {
                String str = (String) conditions;
                // 尝试解析为 JSON
                if (str.startsWith("{")) {
                    Map<String, Object> map = objectMapper.readValue(str, Map.class);
                    return parseFilterConditions(map, params);
                } else if (str.startsWith("[")) {
                    List<?> list = objectMapper.readValue(str, List.class);
                    return parseFilterConditions(list, params);
                }
                return str;
            }
            if (conditions instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) conditions;
                
                // 检查是否是嵌套条件结构
                if (map.containsKey("conditions")) {
                    Object conditionsList = map.get("conditions");
                    String logic = map.containsKey("logic") ? (String) map.get("logic") : "AND";
                    return parseConditionsList(conditionsList, logic, params);
                }
                
                // 检查是否是单个条件对象 {field, operator, value}
                if (map.containsKey("field") && map.containsKey("value")) {
                    return parseSingleCondition(map, params);
                }
                
                // 简单键值对格式
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (sb.length() > 0) {
                        sb.append(" AND ");
                    }
                    sb.append(entry.getKey()).append(" = '").append(entry.getValue()).append("'");
                }
                return sb.toString();
            }
            if (conditions instanceof List) {
                return parseConditionsList(conditions, "AND", params);
            }
            // 处理 FilterGroup 实体对象
            if (conditions instanceof FilterGroup) {
                FilterGroup filterGroup =
                    (FilterGroup) conditions;
                List<Object> conditionsList = filterGroup.getConditions();
                String logic = filterGroup.getLogic() != null ? filterGroup.getLogic() : "AND";
                return parseConditionsList(conditionsList, logic, params);
            }
            // 处理 FilterCondition 实体对象
            if (conditions instanceof FilterCondition) {
                FilterCondition filterCondition =
                    (FilterCondition) conditions;
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("field", filterCondition.getField());
                map.put("operator", filterCondition.getOperator());
                map.put("value", filterCondition.getValue());
                map.put("param_name", filterCondition.getParamName());
                return parseSingleCondition(map, params);
            }
            return objectMapper.writeValueAsString(conditions);
        } catch (Exception e) {
            log.warn("解析筛选条件失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析条件列表（向后兼容版本，无params参数）
     */
    @SuppressWarnings("unchecked")
    private String parseConditionsList(Object conditionsList, String logic) {
        return parseConditionsList(conditionsList, logic, null);
    }
    
    /**
     * 解析条件列表（接受params参数版本）
     */
    @SuppressWarnings("unchecked")
    private String parseConditionsList(Object conditionsList, String logic, Map<String, Object> params) {
        if (!(conditionsList instanceof List)) {
            return "";
        }
        List<?> list = (List<?>) conditionsList;
        StringBuilder sb = new StringBuilder();
        String separator = " " + logic + " ";
        
        for (Object item : list) {
            String condition = null;
            if (item instanceof Map) {
                condition = parseSingleCondition((Map<String, Object>) item, params);
            } else if (item instanceof FilterCondition) {
                // 处理 FilterCondition 实体对象
                FilterCondition filterCondition =
                    (FilterCondition) item;
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                map.put("field", filterCondition.getField());
                map.put("operator", filterCondition.getOperator());
                map.put("value", filterCondition.getValue());
                map.put("param_name", filterCondition.getParamName());
                condition = parseSingleCondition(map, params);
            } else if (item instanceof FilterGroup) {
                // 处理嵌套的 FilterGroup 实体对象
                condition = parseFilterConditions(item, params);
            }
            
            if (condition != null && !condition.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(separator);
                }
                sb.append(condition);
            }
        }
        return sb.toString();
    }
    
    /**
     * 解析单个条件对象 {field, operator, value}（向后兼容版本，无params参数）
     */
    private String parseSingleCondition(Map<String, Object> condition) {
        return parseSingleCondition(condition, null);
    }
    
    /**
     * 解析单个条件对象 {field, operator, value}（接受params参数版本）
     */
    private String parseSingleCondition(Map<String, Object> condition, Map<String, Object> params) {
        Object fieldObj = condition.get("field");
        Object operatorObj = condition.get("operator");
        Object valueObj = condition.get("value");
        Object paramNameObj = condition.get("param_name");
        
        if (fieldObj == null || valueObj == null) {
            return "";
        }
        
        String field = String.valueOf(fieldObj);
        String operator = operatorObj != null ? String.valueOf(operatorObj) : "=";
        Object actualValue = valueObj;
        
        // 检查是否有 param_name 字段，并从 params 中替换
        if (paramNameObj != null && params != null) {
            String paramName = String.valueOf(paramNameObj);
            if (params.containsKey(paramName)) {
                actualValue = params.get(paramName);
            }
        }
        
        String value = String.valueOf(actualValue);
        
        // 检查是否是占位符 {{param_name}}
        boolean isPlaceholder = value.startsWith("{{") && value.endsWith("}}");
        
        // 处理不同的操作符
        return switch (operator) {
            case "=", "==" -> field + " = " + formatValue(value, isPlaceholder);
            case "<>" -> field + " <> " + formatValue(value, isPlaceholder);
            case "!=" -> field + " != " + formatValue(value, isPlaceholder);
            case "<" -> field + " < " + formatValue(value, isPlaceholder);
            case "<=" -> field + " <= " + formatValue(value, isPlaceholder);
            case ">" -> field + " > " + formatValue(value, isPlaceholder);
            case ">=" -> field + " >= " + formatValue(value, isPlaceholder);
            case "like", "LIKE" -> field + " LIKE " + (isPlaceholder ? value : "'%" + value + "%'");
            case "in", "IN" -> field + " IN (" + formatInValues(actualValue) + ")";
            default -> field + " = " + formatValue(value, isPlaceholder);
        };
    }
    
    /**
     * 格式化值，如果是占位符则保留原样，否则根据类型加引号
     */
    private String formatValue(String value, boolean isPlaceholder) {
        if (isPlaceholder) {
            return value;
        }
        // 如果是数字，不加引号
        if (isNumber(value)) {
            return value;
        }
        return "'" + value + "'";
    }
    
    /**
     * 判断字符串是否是数字
     */
    private boolean isNumber(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * 判断字符串是否是表达式（包含运算符）
     */
    private boolean isExpression(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        // 如果包含这些运算符，就认为是表达式
        String operators = "+-*/()=";
        for (char c : operators.toCharArray()) {
            if (str.indexOf(c) >= 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 格式化 IN 条件的值
     */
    private String formatInValues(Object value) {
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .map(v -> "'" + v + "'")
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        }
        return "'" + value + "'";
    }

    /**
     * 解析分组字段
     */
    private String parseGroupByFields(Object fields) {
        try {
            if (fields instanceof String) {
                return (String) fields;
            }
            if (fields instanceof List) {
                List<?> list = (List<?>) fields;
                List<String> stringList = list.stream()
                        .map(Object::toString)
                        .toList();
                return String.join(", ", stringList);
            }
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            log.warn("解析分组字段失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析排序规则（向后兼容版本，无params参数）
     */
    @SuppressWarnings("unchecked")
    private String parseSortRules(Object sortRules) {
        return parseSortRules(sortRules, null);
    }
    
    /**
     * 解析排序规则（接受params参数版本）
     */
    @SuppressWarnings("unchecked")
    private String parseSortRules(Object sortRules, Map<String, Object> params) {
        try {
            if (sortRules instanceof String) {
                String str = (String) sortRules;
                // 尝试解析为 JSON
                if (str.startsWith("[")) {
                    List<?> list = objectMapper.readValue(str, List.class);
                    return parseSortRulesList(list, params);
                }
                return str;
            }
            if (sortRules instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) sortRules;
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(entry.getKey());
                    if ("desc".equalsIgnoreCase(String.valueOf(entry.getValue()))) {
                        sb.append(" DESC");
                    }
                }
                return sb.toString();
            }
            if (sortRules instanceof List) {
                return parseSortRulesList((List<?>) sortRules, params);
            }
            return objectMapper.writeValueAsString(sortRules);
        } catch (Exception e) {
            log.warn("解析排序规则失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析排序规则列表（向后兼容版本，无params参数）
     */
    private String parseSortRulesList(List<?> list) {
        return parseSortRulesList(list, null);
    }
    
    /**
     * 解析排序规则列表（接受params参数版本）
     */
    private String parseSortRulesList(List<?> list, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Object item : list) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            if (item instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) item;
                Object field = map.get("field");
                Object order = map.get("order");
                Object direction = map.get("direction");
                Object paramNameObj = map.get("param_name");
                
                if (field != null) {
                    sb.append(field);
                    
                    Object actualOrder = order != null ? order : direction;
                    // 检查是否有 param_name 字段，并从 params 中替换排序方向
                    if (paramNameObj != null && params != null) {
                        String paramName = String.valueOf(paramNameObj);
                        if (params.containsKey(paramName)) {
                            actualOrder = params.get(paramName);
                        }
                    }
                    
                    if ("desc".equalsIgnoreCase(String.valueOf(actualOrder))) {
                        sb.append(" DESC");
                    } else if ("asc".equalsIgnoreCase(String.valueOf(actualOrder))) {
                        sb.append(" ASC");
                    }
                }
            } else if (item instanceof com.example.metricplatform.metric.dto.SortRule) {
                // 处理 SortRule 实体对象
                SortRule sortRule =
                    (SortRule) item;
                String field = sortRule.getField();
                String order = sortRule.getOrder();
                String paramName = sortRule.getParamName();
                
                if (field != null) {
                    sb.append(field);
                    
                    String actualOrder = order;
                    // 检查是否有 param_name 字段，并从 params 中替换排序方向
                    if (paramName != null && params != null && params.containsKey(paramName)) {
                        actualOrder = String.valueOf(params.get(paramName));
                    }
                    
                    if ("desc".equalsIgnoreCase(actualOrder)) {
                        sb.append(" DESC");
                    } else if ("asc".equalsIgnoreCase(actualOrder)) {
                        sb.append(" ASC");
                    }
                }
            } else {
                sb.append(item);
            }
        }
        return sb.toString();
    }
    
    /**
     * 解析窗口函数配置
     */
    @SuppressWarnings("unchecked")
    private List<String> parseWindowFunctions(Object windowFunctions, Map<String, Object> params) {
        if (windowFunctions == null) {
            log.warn("窗口函数配置为空");
            return null;
        }
        
        try {
            // 处理 WindowFunctions 实体对象
            if (windowFunctions instanceof WindowFunctions) {
                WindowFunctions wf = 
                    (WindowFunctions) windowFunctions;
                List<WindowFunction> functions = wf.getFunctions();
                if (functions == null || functions.isEmpty()) {
                    return null;
                }
                
                List<String> result = new java.util.ArrayList<>();
                for (WindowFunction func : functions) {
                    // 构建窗口函数表达式
                    String expression = buildWindowFunctionExpressionFromEntity(func, params);
                    if (expression != null) {
                        result.add(expression);
                    }
                }
                return result;
            }
            
            // 统一转换为字符串再解析，避免类型问题
            String jsonStr;
            if (windowFunctions instanceof String) {
                jsonStr = (String) windowFunctions;
            } else {
                // 将对象转换为 JSON 字符串
                jsonStr = objectMapper.writeValueAsString(windowFunctions);
            }
            
            jsonStr = jsonStr.trim();
            log.info("窗口函数JSON字符串: '{}'", jsonStr);
            
            if (jsonStr.isEmpty() || "null".equals(jsonStr)) {
                log.warn("窗口函数JSON字符串为空");
                return null;
            }
            
            // 解析为 List<Map>
            List<Map<String, Object>> list;
            if (jsonStr.startsWith("[")) {
                list = objectMapper.readValue(jsonStr, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            } else if (jsonStr.startsWith("{")) {
                // 单个对象包装为列表
                Map<String, Object> single = objectMapper.readValue(jsonStr, Map.class);
                list = java.util.Collections.singletonList(single);
            } else {
                log.warn("窗口函数JSON格式不支持: {}", jsonStr);
                return null;
            }
            
            List<String> result = parseWindowFunctionsList(list, params);
            log.info("解析窗口函数成功，数量: {}", result != null ? result.size() : 0);
            return result;
        } catch (Exception e) {
            log.error("解析窗口函数失败: {}", e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * 解析窗口函数列表
     */
    private List<String> parseWindowFunctionsList(List<Map<String, Object>> functions, Map<String, Object> params) {
        return functions.stream()
                .map(func -> {
                    // 尝试直接获取 expr 字段
                    Object expr = func.get("expr");
                    if (expr instanceof String) {
                        return (String) expr;
                    }
                    
                    // 尝试获取嵌套的 functions 列表
                    Object functionsObj = func.get("functions");
                    if (functionsObj instanceof List) {
                        List<Map<String, Object>> nestedFunctions = (List<Map<String, Object>>) functionsObj;
                        return nestedFunctions.stream()
                                .map(f -> buildWindowFunctionExpression(f, params))
                                .filter(java.util.Objects::nonNull)
                                .findFirst()
                                .orElse(null);
                    }
                    
                    // 尝试直接构建窗口函数表达式
                    return buildWindowFunctionExpression(func, params);
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
    
    /**
     * 根据窗口函数配置构建表达式
     */
    private String buildWindowFunctionExpression(Map<String, Object> func, Map<String, Object> params) {
        Object nameObj = func.get("name");
        Object partitionByObj = func.get("partition_by");
        Object orderByObj = func.get("order_by");
        Object aliasObj = func.get("alias");
        Object itemNameObj = func.get("item_name");
        Object exprObj = func.get("expr");
        Object nObj = func.get("n");
        Object argumentsObj = func.get("arguments");
        Object frameObj = func.get("frame");
        
        // 如果已经有完整的 expr，直接返回
        if (exprObj instanceof String) {
            return (String) exprObj;
        }
        
        // 构建窗口函数表达式
        if (nameObj instanceof String) {
            String functionName = ((String) nameObj).toUpperCase();
            StringBuilder sb = new StringBuilder();
            
            // 函数名（带参数）
            sb.append(functionName).append("(");
            
            // 处理函数参数
            boolean hasArgs = false;
            
            // 判断是否是聚合函数类型的窗口函数
            boolean isAggregateWindowFunction = 
                "SUM".equals(functionName) || "AVG".equals(functionName) || 
                "MAX".equals(functionName) || "MIN".equals(functionName) || 
                "COUNT".equals(functionName);
            
            // 1. 如果是聚合函数，优先使用 arguments 数组的第一个元素作为字段
            if (isAggregateWindowFunction && argumentsObj instanceof List) {
                List<?> args = (List<?>) argumentsObj;
                if (!args.isEmpty()) {
                    Object arg = args.get(0);
                    if (arg instanceof String) {
                        String strArg = (String) arg;
                        if (strArg.startsWith("'") && strArg.endsWith("'")) {
                            sb.append(strArg);
                        } else {
                            sb.append(strArg);
                        }
                    } else {
                        sb.append(arg);
                    }
                    hasArgs = true;
                }
            }
            
            // 2. 处理 n 参数（用于 NTILE、PERCENTILE_CONT 等函数）
            else if (nObj != null) {
                sb.append(nObj);
                hasArgs = true;
            }
            
            // 3. 处理 arguments 数组（用于 LAG、LEAD、FIRST_VALUE 等函数）
            else if (argumentsObj instanceof List) {
                List<?> args = (List<?>) argumentsObj;
                if (!args.isEmpty()) {
                    for (int i = 0; i < args.size(); i++) {
                        if (i > 0) sb.append(", ");
                        Object arg = args.get(i);
                            if (arg instanceof String) {
                                String strArg = (String) arg;
                                if (strArg.startsWith("'") && strArg.endsWith("'")) {
                                    // 已经是带引号的字符串，直接使用
                                    sb.append(strArg);
                                } else if (isLikelyFieldName(strArg)) {
                                    // 看起来像是字段名，直接使用
                                    sb.append(strArg);
                                } else {
                                    // 是字符串值，加上单引号
                                    sb.append("'").append(strArg).append("'");
                                }
                            } else {
                                // 其他类型（数字等）
                                sb.append(arg);
                            }
                    }
                    hasArgs = true;
                }
            }
            
            sb.append(") OVER (");
            
            // PARTITION BY
            if (partitionByObj instanceof List) {
                List<?> partitionBy = (List<?>) partitionByObj;
                if (!partitionBy.isEmpty()) {
                    sb.append("PARTITION BY ").append(String.join(", ", partitionBy.stream().map(Object::toString).toList()));
                }
            }
            
            // ORDER BY
            if (orderByObj instanceof List) {
                List<?> orderBy = (List<?>) orderByObj;
                if (!orderBy.isEmpty()) {
                    if (sb.length() > functionName.length() + 10) { // "ROW_NUMBER() OVER ("
                        sb.append(" ");
                    }
                    sb.append("ORDER BY ");
                    
                    List<String> orderByParts = orderBy.stream()
                            .map(item -> {
                                if (item instanceof Map) {
                                    Map<?, ?> map = (Map<?, ?>) item;
                                    Object field = map.get("field");
                                    Object direction = map.get("direction");
                                    Object order = map.get("order");
                                    Object paramName = map.get("param_name");
                                    
                                    if (field != null) {
                                        String result = String.valueOf(field);
                                        Object effectiveDirectionObj = direction != null ? direction : order;
                                        String effectiveDirection = effectiveDirectionObj != null ? String.valueOf(effectiveDirectionObj) : "asc";
                                        
                                        // 如果有 param_name，并且请求参数中有该值，使用请求参数的排序方向
                                        if (params != null && paramName != null) {
                                            Object paramValue = params.get(String.valueOf(paramName));
                                            if (paramValue != null) {
                                                effectiveDirection = String.valueOf(paramValue);
                                            }
                                        }
                                        
                                        if ("desc".equalsIgnoreCase(effectiveDirection)) {
                                            result += " DESC";
                                        } else {
                                            result += " ASC";
                                        }
                                        return result;
                                    }
                                }
                                return String.valueOf(item);
                            })
                            .toList();
                    
                    sb.append(String.join(", ", orderByParts));
                }
            }
            
            // FRAME 配置
            if (frameObj instanceof String) {
                String frame = (String) frameObj;
                if (org.springframework.util.StringUtils.hasText(frame)) {
                    sb.append(" ").append(frame);
                }
            }
            
            sb.append(")");
            
            // AS alias (优先使用 item_name，如果没有则使用 alias)
            Object aliasToUse = null;
            if (itemNameObj instanceof String) {
                aliasToUse = itemNameObj;
            } else if (aliasObj instanceof String) {
                aliasToUse = aliasObj;
            }
            if (aliasToUse != null) {
                sb.append(" AS ").append(aliasToUse);
            }
            
            return sb.toString();
        }
        
        return null;
    }
    
    /**
     * 从 WindowFunction 实体对象构建窗口函数表达式
     */
    private String buildWindowFunctionExpressionFromEntity(WindowFunction func, Map<String, Object> params) {
        String name = func.getName();
        List<String> partitionBy = func.getPartitionBy();
        List<SortRule> orderBy = func.getOrderBy();
        String alias = func.getAlias();
        String itemName = func.getItemName();
        Integer n = func.getN();
        List<Object> arguments = func.getArguments();
        String frame = func.getFrame();
        
        // 构建窗口函数表达式
        if (name != null) {
            String functionName = name.toUpperCase();
            StringBuilder sb = new StringBuilder();
            
            // 函数名（带参数）
            sb.append(functionName).append("(");
            
            // 处理函数参数
            boolean hasArgs = false;
            
            // 判断是否是聚合函数类型的窗口函数
            boolean isAggregateWindowFunction = 
                "SUM".equals(functionName) || "AVG".equals(functionName) || 
                "MAX".equals(functionName) || "MIN".equals(functionName) || 
                "COUNT".equals(functionName);
            
            // 1. 如果是聚合函数，优先使用 arguments 数组的第一个元素作为字段
            if (isAggregateWindowFunction && arguments != null && !arguments.isEmpty()) {
                Object arg = arguments.get(0);
                if (arg instanceof String) {
                    String strArg = (String) arg;
                    if (strArg.startsWith("'") && strArg.endsWith("'")) {
                        sb.append(strArg);
                    } else {
                        sb.append(strArg);
                    }
                } else {
                    sb.append(arg);
                }
                hasArgs = true;
            }
            
            // 2. 处理 n 参数（用于 NTILE、PERCENTILE_CONT 等函数）
            else if (n != null) {
                sb.append(n);
                hasArgs = true;
            }
            
            // 3. 处理 arguments 数组（用于 LAG、LEAD、FIRST_VALUE 等函数）
            else if (arguments != null && !arguments.isEmpty()) {
                for (int i = 0; i < arguments.size(); i++) {
                    if (i > 0) sb.append(", ");
                    Object arg = arguments.get(i);
                    if (arg instanceof String) {
                        String strArg = (String) arg;
                        if (strArg.startsWith("'") && strArg.endsWith("'")) {
                            // 已经是带引号的字符串，直接使用
                            sb.append(strArg);
                        } else if (isLikelyFieldName(strArg)) {
                            // 看起来像是字段名，直接使用
                            sb.append(strArg);
                        } else {
                            // 是字符串值，加上单引号
                            sb.append("'").append(strArg).append("'");
                        }
                    } else {
                        // 其他类型（数字等）
                        sb.append(arg);
                    }
                }
                hasArgs = true;
            }
            
            sb.append(") OVER (");
            
            // PARTITION BY
            if (partitionBy != null && !partitionBy.isEmpty()) {
                sb.append("PARTITION BY ").append(String.join(", ", partitionBy));
            }
            
            // ORDER BY
            if (orderBy != null && !orderBy.isEmpty()) {
                if (sb.length() > functionName.length() + 10) { // "ROW_NUMBER() OVER ("
                    sb.append(" ");
                }
                sb.append("ORDER BY ");
                
                List<String> orderByParts = orderBy.stream()
                        .map(sortRule -> {
                            String field = sortRule.getField();
                            String direction = sortRule.getOrder();
                            String paramName = sortRule.getParamName();
                            
                            if (field != null) {
                                String result = field;
                                String effectiveDirection = direction != null ? direction : "asc";
                                
                                // 如果有 param_name，并且请求参数中有该值，使用请求参数的排序方向
                                if (params != null && paramName != null) {
                                    Object paramValue = params.get(paramName);
                                    if (paramValue != null) {
                                        effectiveDirection = String.valueOf(paramValue);
                                    }
                                }
                                
                                if ("desc".equalsIgnoreCase(effectiveDirection)) {
                                    result += " DESC";
                                } else {
                                    result += " ASC";
                                }
                                return result;
                            }
                            return null;
                        })
                        .filter(java.util.Objects::nonNull)
                        .toList();
                
                sb.append(String.join(", ", orderByParts));
            }
            
            // FRAME 配置
            if (org.springframework.util.StringUtils.hasText(frame)) {
                sb.append(" ").append(frame);
            }
            
            sb.append(")");
            
            // AS alias (优先使用 itemName，如果没有则使用 alias)
            String aliasToUse = func.getEffectiveAlias();
            if (aliasToUse != null) {
                sb.append(" AS ").append(aliasToUse);
            }
            
            return sb.toString();
        }
        
        return null;
    }
    
    /**
     * 判断字符串是否看起来像是字段名
     */
    private boolean isLikelyFieldName(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        
        // 字段名通常只包含字母、数字和下划线
        for (char c : str.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        
        // 如果看起来是纯数字，不是字段名
        try {
            Double.parseDouble(str);
            return false;
        } catch (NumberFormatException e) {
            // 不是数字，继续判断
        }
        
        // 常见的字段名特征
        return Character.isLetter(str.charAt(0));
    }
}
