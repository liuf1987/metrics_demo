package com.example.metricplatform.metric.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.metricplatform.metric.converter.MetricConfigConverter;
import com.example.metricplatform.metric.dto.MetricCreateRequest;
import com.example.metricplatform.metric.dto.MetricUpdateRequest;
import com.example.metricplatform.metadata.entity.DataSource;
import com.example.metricplatform.metadata.entity.FieldMetadata;
import com.example.metricplatform.metric.entity.MetricConfig;
import com.example.metricplatform.metric.entity.MetricStatItem;
import com.example.metricplatform.common.enums.FieldType;
import com.example.metricplatform.common.exception.BusinessException;
import com.example.metricplatform.common.exception.ErrorCode;
import com.example.metricplatform.metric.mapper.MetricConfigMapper;
import com.example.metricplatform.metric.mapper.MetricStatItemMapper;
import com.example.metricplatform.metric.service.MetricConfigService;
import com.example.metricplatform.metadata.service.MetadataService;
import com.example.metricplatform.metric.service.QueryEngineService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标配置服务实现
 */
@Slf4j
@Service
public class MetricConfigServiceImpl extends ServiceImpl<MetricConfigMapper, MetricConfig> implements MetricConfigService {

    private final MetadataService metadataService;
    private final ObjectMapper objectMapper;
    private final MetricConfigConverter metricConfigConverter;
    private final MetricStatItemMapper metricStatItemMapper;
    private final QueryEngineService queryEngineService;

    public MetricConfigServiceImpl(MetadataService metadataService,
                                   ObjectMapper objectMapper,
                                   MetricConfigConverter metricConfigConverter,
                                   MetricStatItemMapper metricStatItemMapper,
                                   @Lazy QueryEngineService queryEngineService) {
        this.metadataService = metadataService;
        this.objectMapper = objectMapper;
        this.metricConfigConverter = metricConfigConverter;
        this.metricStatItemMapper = metricStatItemMapper;
        this.queryEngineService = queryEngineService;
    }

    @Override
    //@Transactional
    public MetricConfig create(MetricCreateRequest request) {
        // 检查编码是否已存在
        MetricConfig existing = findByCode(request.getMetricCode());
        if (existing != null) {
            log.warn("指标编码已存在，metricCode: {}", request.getMetricCode());
            throw ErrorCode.PARAM_CODE_EXISTS.exception(request.getMetricCode());
        }
        
        log.info("create: request.getStatItems() = {}", 
                request.getStatItems() != null ? "size=" + request.getStatItems().size() : "null");
        if (request.getStatItems() != null) {
            for (int i = 0; i < request.getStatItems().size(); i++) {
                var item = request.getStatItems().get(i);
                log.info("  request statItems[{}]: itemCode={}, itemName={}, fieldName={}, aggregation={}", 
                        i, item.getItemCode(), item.getItemName(), item.getFieldName(), item.getAggregation());
            }
        }

        MetricConfig metric = metricConfigConverter.toEntity(request);
        
        log.info("create: after converter, metric.getStatItems() = {}", 
                metric.getStatItems() != null ? "size=" + metric.getStatItems().size() : "null");
        if (metric.getStatItems() != null) {
            for (int i = 0; i < metric.getStatItems().size(); i++) {
                var item = metric.getStatItems().get(i);
                log.info("  metric statItems[{}]: itemCode={}, itemName={}, fieldName={}, aggregation={}", 
                        i, item.getItemCode(), item.getItemName(), item.getFieldName(), item.getAggregation());
            }
        }

        // 验证指标配置的完整性和正确性
        validateMetricConfig(metric);
        
        validateMetadata(metric);
        save(metric);
        
        saveStatItems(metric);
        
        // 验证指标查询是否正常
        validateMetricQuery(metric.getMetricCode());
        
        return metric;
    }

    @Override
    //@Transactional
    public MetricConfig update(Long id, MetricUpdateRequest request) {
        MetricConfig metric = getById(id);
        if (metric == null) {
            log.warn("指标不存在，id: {}", id);
            throw ErrorCode.PARAM_CODE_EXISTS.exception(id.toString());
        }

        // 保存原有的 statItems，如果 request 里没有 statItems，我们要保留
        List<MetricStatItem> originalStatItems = metric.getStatItems();
        
        // 保存原有的 enabled 值（因为 MapStruct 的 IGNORE 策略可能导致 Boolean 类型无法正确更新）
        Boolean originalEnabled = metric.getEnabled();

        metricConfigConverter.updateFromRequest(request, metric);
        
        // 如果 request 中明确指定了 enabled，则使用 request 的值；否则保持原值
        if (request.getEnabled() != null) {
            metric.setEnabled(request.getEnabled());
        } else {
            metric.setEnabled(originalEnabled);
        }

        // 如果 request 里没有 statItems，我们恢复原来的 statItems，不覆盖
        if (request.getStatItems() == null) {
            metric.setStatItems(originalStatItems);
        }

        // 验证指标配置的完整性和正确性
        validateMetricConfig(metric);
        
        validateMetadata(metric);
        updateById(metric);

        // 只有在 request 明确有 statItems 时才更新 statItems
        if (request.getStatItems() != null) {
            saveStatItems(metric);
        }
        
        // 验证指标查询是否正常
        validateMetricQuery(metric.getMetricCode());
        
        return metric;
    }

    /**
     * 保存指标的统计项
     * 使用"先查再决定插入或更新"的策略，避免唯一键冲突
     */
    private void saveStatItems(MetricConfig metric) {
        List<MetricStatItem> statItems = metric.getStatItems();
        if (statItems == null || statItems.isEmpty()) {
            return;
        }

        Long metricId = metric.getId();
        
        // 对每个统计项，先查询是否存在，再决定插入或更新
        for (MetricStatItem statItem : statItems) {
            statItem.setMetricId(metricId);
            
            // 设置默认值
            if (statItem.getItemName() == null || statItem.getItemName().trim().isEmpty()) {
                if (statItem.getItemCode() != null) {
                    statItem.setItemName(statItem.getItemCode());
                } else {
                    statItem.setItemName(statItem.getFieldName());
                }
            }
            if (statItem.getSortOrder() == null) {
                statItem.setSortOrder(0);
            }
            
            // 查询是否存在相同的统计项（基于 metric_id + item_code）
            MetricStatItem existing = metricStatItemMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricStatItem>()
                    .eq(MetricStatItem::getMetricId, metricId)
                    .eq(MetricStatItem::getItemCode, statItem.getItemCode())
            );
            
            if (existing != null) {
                // 存在则更新
                statItem.setId(existing.getId());
                statItem.setCreateTime(existing.getCreateTime()); // 保留原创建时间
                statItem.setUpdateTime(null); // 让 MyBatis 自动更新
                statItem.setDeleted(existing.getDeleted()); // 保留原删除标记
                metricStatItemMapper.updateById(statItem);
            } else {
                // 不存在则插入
                statItem.setId(null);
                statItem.setCreateTime(null);
                statItem.setUpdateTime(null);
                statItem.setDeleted(null);
                metricStatItemMapper.insert(statItem);
            }
        }
        
        // 删除不在新列表中的旧统计项
        List<String> newItemCodes = statItems.stream()
            .map(MetricStatItem::getItemCode)
            .collect(java.util.stream.Collectors.toList());
        
        metricStatItemMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricStatItem>()
                .eq(MetricStatItem::getMetricId, metricId)
                .notIn(MetricStatItem::getItemCode, newItemCodes)
        );
    }

    private void validateMetadata(MetricConfig metric) {
        String sourceTable = metric.getSourceTable();
        if (!StringUtils.hasText(sourceTable)) {
            log.warn("源表不能为空");
            throw ErrorCode.PARAM_SOURCE_TABLE_EMPTY.exception();
        }

        DataSource dataSource = metadataService.getDataSourceByCode(sourceTable);
        if (dataSource == null) {
            log.warn("数据源不存在，sourceTable: {}", sourceTable);
            throw ErrorCode.PARAM_DATA_SOURCE_NOT_FOUND.exception(sourceTable);
        }
        if (!Boolean.TRUE.equals(dataSource.getEnabled())) {
            log.warn("数据源已禁用，sourceTable: {}", sourceTable);
            throw ErrorCode.PARAM_DATA_SOURCE_DISABLED.exception(sourceTable);
        }

        List<FieldMetadata> fields = metadataService.getFieldsBySourceId(dataSource.getId());
        Set<String> availableFieldNames = new HashSet<>();
        Set<String> enabledFieldNames = new HashSet<>();
        Map<String, FieldMetadata> fieldMap = new HashMap<>();
        
        for (FieldMetadata field : fields) {
            String lowerName = field.getFieldName().toLowerCase();
            availableFieldNames.add(lowerName);
            if (Boolean.TRUE.equals(field.getEnabled())) {
                enabledFieldNames.add(lowerName);
            }
            fieldMap.put(lowerName, field);
        }

        // 获取统计项的别名列表
        Set<String> statItemAliases = getStatItemAliases(metric);
        
        // 获取窗口函数的别名列表
        Set<String> windowFunctionAliases = getWindowFunctionAliases(metric);
        
        // 合并所有允许的别名
        Set<String> allValidAliases = new HashSet<>();
        allValidAliases.addAll(statItemAliases);
        allValidAliases.addAll(windowFunctionAliases);

        // 验证 HAVING 条件中的字段引用
        validateHavingConditions(metric.getHavingConditions(), allValidAliases, availableFieldNames);

        // 验证 ORDER BY 中的字段引用
        validateSortRules(metric.getSortRules(), allValidAliases, availableFieldNames);

        Set<String> usedFields = extractFieldNames(metric, allValidAliases);

        for (String fieldName : usedFields) {
            String lowerFieldName = fieldName.toLowerCase();
            if (!availableFieldNames.contains(lowerFieldName)) {
                log.warn("字段不存在，fieldName: {}", fieldName);
                throw ErrorCode.PARAM_FIELD_NOT_FOUND.exception(fieldName);
            }
            if (!enabledFieldNames.contains(lowerFieldName)) {
                log.warn("字段已禁用，fieldName: {}", fieldName);
                throw ErrorCode.PARAM_FIELD_DISABLED.exception(fieldName);
            }
        }

        validateAggregationRules(metric, fieldMap);
    }
    
    /**
     * 验证 HAVING 条件中的字段引用
     * HAVING 条件中的字段只能是：
     * 1. 表中存在的字段
     * 2. 统计项的别名（itemCode）
     */
    @SuppressWarnings("unchecked")
    private void validateHavingConditions(Object havingConditions, Set<String> statItemAliases, Set<String> availableFieldNames) {
        if (havingConditions == null) {
            return;
        }
        
        try {
            Map<String, Object> havingMap = objectMapper.convertValue(havingConditions, Map.class);
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) havingMap.get("conditions");
            
            if (conditions == null) {
                return;
            }
            
            for (Map<String, Object> condition : conditions) {
                String field = (String) condition.get("field");
                if (StringUtils.hasText(field)) {
                    // 跳过看起来像是表达式的字段名（包含运算符）
                    if (isExpression(field)) {
                        log.info("HAVING条件字段看起来是表达式，跳过验证，field = {}", field);
                        continue;
                    }
                    
                    String lowerField = field.toLowerCase();
                    // 检查字段是否是统计项别名或表字段
                    if (!statItemAliases.contains(lowerField) && !availableFieldNames.contains(lowerField)) {
                        log.warn("HAVING条件中引用的字段不存在，field: {}", field);
                        throw ErrorCode.PARAM_FIELD_NOT_FOUND.exception(
                            String.format("HAVING条件中引用的字段[%s]不是有效的统计项别名或表字段", field));
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析HAVING条件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 验证 ORDER BY 规则中的字段引用
     * ORDER BY 中的字段只能是：
     * 1. 表中存在的字段
     * 2. 统计项的别名（itemCode）
     */
    @SuppressWarnings("unchecked")
    private void validateSortRules(Object sortRules, Set<String> statItemAliases, Set<String> availableFieldNames) {
        if (sortRules == null) {
            return;
        }
        
        log.info("validateSortRules: statItemAliases = {}, availableFieldNames = {}", 
                statItemAliases, availableFieldNames);
        
        try {
            List<Map<String, Object>> rules = objectMapper.convertValue(sortRules, List.class);
            
            for (Map<String, Object> rule : rules) {
                String field = (String) rule.get("field");
                if (StringUtils.hasText(field)) {
                    // 跳过看起来像是表达式的字段名（包含运算符）
                    if (isExpression(field)) {
                        log.info("排序规则字段看起来是表达式，跳过验证，field = {}", field);
                        continue;
                    }
                    
                    String lowerField = field.toLowerCase();
                    log.info("validateSortRules: checking field = {}, lowerField = {}", field, lowerField);
                    log.info("  statItemAliases contains? {}", statItemAliases.contains(lowerField));
                    log.info("  availableFieldNames contains? {}", availableFieldNames.contains(lowerField));
                    
                    // 检查字段是否是统计项别名或表字段
                    if (!statItemAliases.contains(lowerField) && !availableFieldNames.contains(lowerField)) {
                        log.warn("排序规则中引用的字段不存在，field: {}", field);
                        throw ErrorCode.PARAM_FIELD_NOT_FOUND.exception(
                            String.format("排序规则中引用的字段[%s]不是有效的统计项别名或表字段", field));
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析排序规则失败: {}", e.getMessage());
        }
    }

    /**
     * 获取统计项的别名列表
     * 支持两种情况：
     * 1. 已有ID的指标：从数据库查询
     * 2. 新增的指标：从实体的statItems字段获取
     */
    private Set<String> getStatItemAliases(MetricConfig metric) {
        Set<String> aliases = new HashSet<>();
        
        log.info("getStatItemAliases: metric.getId() = {}, metric.getStatItems() = {}", 
                metric.getId(), 
                metric.getStatItems() != null ? "size=" + metric.getStatItems().size() : "null");
        
        // 情况1：已有ID，从数据库查询
        if (metric.getId() != null) {
            List<MetricStatItem> statItems = metricStatItemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricStatItem>()
                            .eq(MetricStatItem::getMetricId, metric.getId())
            );
            
            if (statItems != null) {
                for (MetricStatItem item : statItems) {
                    if (StringUtils.hasText(item.getItemCode())) {
                        aliases.add(item.getItemCode().toLowerCase());
                    }
                }
            }
            log.info("getStatItemAliases (from DB): aliases = {}", aliases);
        }
        // 情况2：没有ID，从实体的statItems字段获取（新增场景）
        else if (metric.getStatItems() != null && !metric.getStatItems().isEmpty()) {
            for (MetricStatItem item : metric.getStatItems()) {
                log.info("  stat item: itemCode = {}, itemName = {}, fieldName = {}", 
                        item.getItemCode(), item.getItemName(), item.getFieldName());
                if (StringUtils.hasText(item.getItemCode())) {
                    aliases.add(item.getItemCode().toLowerCase());
                }
            }
            log.info("getStatItemAliases (from entity): aliases = {}", aliases);
        }
        
        return aliases;
    }

    /**
     * 获取窗口函数的别名列表
     */
    @SuppressWarnings("unchecked")
    private Set<String> getWindowFunctionAliases(MetricConfig metric) {
        Set<String> aliases = new HashSet<>();
        
        Object windowFunctions = metric.getWindowFunctions();
        if (windowFunctions == null) {
            return aliases;
        }
        
        try {
            Map<String, Object> funcMap = objectMapper.convertValue(windowFunctions, Map.class);
            Object functionsObj = funcMap.get("functions");
            
            if (functionsObj instanceof List) {
                List<Object> functions = (List<Object>) functionsObj;
                for (Object funcObj : functions) {
                    if (funcObj instanceof Map) {
                        Map<String, Object> func = (Map<String, Object>) funcObj;
                        
                        // 优先使用 item_name
                        Object itemNameObj = func.get("item_name");
                        if (itemNameObj instanceof String && StringUtils.hasText((String) itemNameObj)) {
                            aliases.add(((String) itemNameObj).toLowerCase());
                        }
                        
                        // 其次使用 itemName
                        Object itemNameObj2 = func.get("itemName");
                        if (itemNameObj2 instanceof String && StringUtils.hasText((String) itemNameObj2)) {
                            aliases.add(((String) itemNameObj2).toLowerCase());
                        }
                        
                        // 最后使用 alias
                        Object aliasObj = func.get("alias");
                        if (aliasObj instanceof String && StringUtils.hasText((String) aliasObj)) {
                            aliases.add(((String) aliasObj).toLowerCase());
                        }
                    }
                }
            }
            log.info("getWindowFunctionAliases: aliases = {}", aliases);
        } catch (Exception e) {
            log.warn("解析窗口函数别名失败: {}", e.getMessage());
        }
        
        return aliases;
    }

    private void validateAggregationRules(MetricConfig metric, Map<String, FieldMetadata> fieldMap) {
        List<MetricStatItem> statItems = new ArrayList<>();
        
        // 支持两种情况：
        // 1. 新增场景：从实体的statItems字段获取
        // 2. 更新场景：从数据库查询
        if (metric.getStatItems() != null && !metric.getStatItems().isEmpty()) {
            statItems.addAll(metric.getStatItems());
        } else if (metric.getId() != null) {
            List<MetricStatItem> dbStatItems = metricStatItemMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MetricStatItem>()
                            .eq(MetricStatItem::getMetricId, metric.getId())
            );
            if (dbStatItems != null) {
                statItems.addAll(dbStatItems);
            }
        }

        if (statItems.isEmpty()) {
            return;
        }

        for (MetricStatItem statItem : statItems) {
            String fieldName = statItem.getFieldName();
            String aggregation = statItem.getAggregation() != null ? statItem.getAggregation().name() : null;

            if (!StringUtils.hasText(fieldName) || !StringUtils.hasText(aggregation)) {
                continue;
            }

            String lowerFieldName = fieldName.toLowerCase();
            FieldMetadata fieldMetadata = fieldMap.get(lowerFieldName);

            if (fieldMetadata == null) {
                log.warn("统计字段不存在，fieldName: {}", fieldName);
                throw ErrorCode.PARAM_STAT_FIELD_NOT_FOUND.exception(fieldName);
            }

            if (!Boolean.TRUE.equals(fieldMetadata.getEnabled())) {
                log.warn("统计字段已禁用，fieldName: {}", fieldName);
                throw ErrorCode.PARAM_STAT_FIELD_DISABLED.exception(fieldName);
            }

            // 根据FieldType的supportedAggregations检查聚合函数是否合法
            FieldType fieldType = fieldMetadata.getFieldType();
            if (!fieldType.supportsAggregation(aggregation)) {
                log.warn("字段不支持该聚合函数，fieldName: {}, aggregation: {}", fieldName, aggregation);
                throw ErrorCode.PARAM_FIELD_TYPE_NOT_SUPPORT_AGGREGATION.exception(
                    String.format("字段[%s]类型[%s]不支持聚合函数[%s]，支持的聚合函数为: %s", 
                        fieldName, fieldType.getDesc(), aggregation, fieldType.getSupportedAggregationsString()));
            }
        }
    }

    private Set<String> extractFieldNames(MetricConfig metric, Set<String> statItemAliases) {
        Set<String> fieldNames = new HashSet<>();
        
        // 提取分组字段
        extractFieldsFromArray(metric.getGroupByFields(), fieldNames, statItemAliases);
        
        // 提取过滤条件中的字段
        extractFieldsFromFilterConditions(metric.getFilterConditions(), fieldNames, statItemAliases);
        
        // 提取HAVING条件中的字段
        // extractFieldsFromFilterConditions(metric.getHavingConditions(), fieldNames, statItemAliases);
        
        // 提取排序规则中的字段
        extractFieldsFromSortRules(metric.getSortRules(), fieldNames, statItemAliases);
        
        // 提取窗口函数中的字段
        extractFieldsFromWindowFunctions(metric.getWindowFunctions(), fieldNames, statItemAliases);
        
        return fieldNames;
    }

    /**
     * 从数组中提取字段名（用于group_by_fields）
     */
    @SuppressWarnings("unchecked")
    private void extractFieldsFromArray(Object obj, Set<String> fieldNames, Set<String> statItemAliases) {
        if (obj == null) {
            return;
        }
        
        try {
            Object converted = objectMapper.convertValue(obj, Object.class);
            if (converted instanceof List) {
                List<Object> list = (List<Object>) converted;
                for (Object item : list) {
                    if (item instanceof String) {
                        String fieldName = (String) item;
                        if (StringUtils.hasText(fieldName)) {
                            String lowerFieldName = fieldName.toLowerCase();
                            // 排除统计项别名
                            if (!statItemAliases.contains(lowerFieldName)) {
                                fieldNames.add(lowerFieldName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略转换异常
        }
    }

    /**
     * 从过滤条件中提取字段名
     */
    @SuppressWarnings("unchecked")
    private void extractFieldsFromFilterConditions(Object obj, Set<String> fieldNames, Set<String> statItemAliases) {
        if (obj == null) {
            return;
        }
        
        try {
            Object converted = objectMapper.convertValue(obj, Object.class);
            if (converted instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) converted;
                Object conditions = map.get("conditions");
                
                if (conditions instanceof List) {
                    List<Object> conditionList = (List<Object>) conditions;
                    for (Object condition : conditionList) {
                        if (condition instanceof Map) {
                            Map<String, Object> condMap = (Map<String, Object>) condition;
                            Object field = condMap.get("field");
                            if (field instanceof String) {
                                String fieldName = (String) field;
                                if (StringUtils.hasText(fieldName)) {
                                    // 跳过看起来像是表达式的字段名
                                    if (isExpression(fieldName)) {
                                        log.info("过滤条件字段看起来是表达式，跳过提取，fieldName = {}", fieldName);
                                        continue;
                                    }
                                    String lowerFieldName = fieldName.toLowerCase();
                                    // 排除统计项别名
                                    if (!statItemAliases.contains(lowerFieldName)) {
                                        fieldNames.add(lowerFieldName);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略转换异常
        }
    }

    /**
     * 从排序规则中提取字段名
     */
    @SuppressWarnings("unchecked")
    private void extractFieldsFromSortRules(Object obj, Set<String> fieldNames, Set<String> statItemAliases) {
        if (obj == null) {
            return;
        }
        
        try {
            Object converted = objectMapper.convertValue(obj, Object.class);
            if (converted instanceof List) {
                List<Object> list = (List<Object>) converted;
                for (Object item : list) {
                    if (item instanceof Map) {
                        Map<String, Object> ruleMap = (Map<String, Object>) item;
                        Object field = ruleMap.get("field");
                        if (field instanceof String) {
                            String fieldName = (String) field;
                            if (StringUtils.hasText(fieldName)) {
                                // 跳过看起来像是表达式的字段名
                                if (isExpression(fieldName)) {
                                    log.info("排序规则字段看起来是表达式，跳过提取，fieldName = {}", fieldName);
                                    continue;
                                }
                                String lowerFieldName = fieldName.toLowerCase();
                                // 排除统计项别名
                                if (!statItemAliases.contains(lowerFieldName)) {
                                    fieldNames.add(lowerFieldName);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略转换异常
        }
    }

    /**
     * 从窗口函数中提取字段名
     */
    @SuppressWarnings("unchecked")
    private void extractFieldsFromWindowFunctions(Object obj, Set<String> fieldNames, Set<String> statItemAliases) {
        if (obj == null) {
            return;
        }
        
        try {
            Object converted = objectMapper.convertValue(obj, Object.class);
            if (converted instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) converted;
                Object functions = map.get("functions");
                
                if (functions instanceof List) {
                    List<Object> funcList = (List<Object>) functions;
                    for (Object func : funcList) {
                        if (func instanceof Map) {
                            Map<String, Object> funcMap = (Map<String, Object>) func;
                            
                            // 提取partition_by中的字段
                            Object partitionBy = funcMap.get("partitionBy");
                            if (partitionBy instanceof List) {
                                List<Object> partitionList = (List<Object>) partitionBy;
                                for (Object field : partitionList) {
                                    if (field instanceof String) {
                                        String fieldName = (String) field;
                                        if (StringUtils.hasText(fieldName)) {
                                            String lowerFieldName = fieldName.toLowerCase();
                                            // 排除统计项别名
                                            if (!statItemAliases.contains(lowerFieldName)) {
                                                fieldNames.add(lowerFieldName);
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // 提取order_by中的字段
                            Object orderBy = funcMap.get("orderBy");
                            if (orderBy instanceof List) {
                                List<Object> orderList = (List<Object>) orderBy;
                                for (Object order : orderList) {
                                    if (order instanceof Map) {
                                        Map<String, Object> orderMap = (Map<String, Object>) order;
                                        Object field = orderMap.get("field");
                                        if (field instanceof String) {
                                            String fieldName = (String) field;
                                            if (StringUtils.hasText(fieldName)) {
                                                String lowerFieldName = fieldName.toLowerCase();
                                                // 排除统计项别名
                                                if (!statItemAliases.contains(lowerFieldName)) {
                                                    fieldNames.add(lowerFieldName);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 忽略转换异常
        }
    }

    @Override
    public void delete(Long id) {
        MetricConfig metric = getById(id);
        if (metric == null) {
            log.warn("指标不存在，id: {}", id);
            throw ErrorCode.PARAM_CODE_EXISTS.exception(id.toString());
        }
        removeById(id);
    }

    @Override
    public MetricConfig getByCode(String metricCode) {
        if (!StringUtils.hasText(metricCode)) {
            log.warn("参数校验失败：指标编码不能为空");
            throw ErrorCode.NULL_PARAMETER.exception("指标编码不能为空");
        }
        
        MetricConfig metric = findByCode(metricCode);
        
        if (metric == null) {
            log.warn("指标不存在，metricCode: {}", metricCode);
            throw ErrorCode.METRIC_NOT_FOUND.exception(metricCode);
        }
        
        fillStatItems(metric);
        return metric;
    }

    /**
     * 根据编码查询指标（不抛异常，用于内部检查）
     */
    private MetricConfig findByCode(String metricCode) {
        if (!StringUtils.hasText(metricCode)) {
            return null;
        }
        
        return lambdaQuery()
                .eq(MetricConfig::getMetricCode, metricCode)
                .one();
    }

    @Override
    public MetricConfig getById(Long id) {
        MetricConfig metric = super.getById(id);
        if (metric != null) {
            fillStatItems(metric);
        }
        return metric;
    }

    @Override
    public Page<MetricConfig> page(Integer pageNum, Integer pageSize, String categoryCode, Boolean enabled, String keyword) {
        LambdaQueryWrapper<MetricConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(categoryCode)) {
            wrapper.eq(MetricConfig::getCategoryCode, categoryCode);
        }
        if (enabled != null) {
            wrapper.eq(MetricConfig::getEnabled, enabled);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(MetricConfig::getMetricCode, keyword)
                    .or()
                    .like(MetricConfig::getMetricName, keyword));
        }
        
        wrapper.orderByDesc(MetricConfig::getCreateTime);
        
        Page<MetricConfig> page = page(new Page<>(pageNum, pageSize), wrapper);
        
        // 批量填充统计项
        if (!page.getRecords().isEmpty()) {
            Set<Long> metricIds = page.getRecords().stream()
                    .map(MetricConfig::getId)
                    .collect(Collectors.toSet());
            
            List<MetricStatItem> allStatItems = metricStatItemMapper.selectList(
                    new LambdaQueryWrapper<MetricStatItem>()
                            .in(MetricStatItem::getMetricId, metricIds)
                            .orderByAsc(MetricStatItem::getSortOrder)
            );
            
            Map<Long, List<MetricStatItem>> statItemsMap = allStatItems.stream()
                    .collect(Collectors.groupingBy(MetricStatItem::getMetricId));
            
            for (MetricConfig metric : page.getRecords()) {
                metric.setStatItems(statItemsMap.getOrDefault(metric.getId(), new ArrayList<>()));
            }
        }
        
        return page;
    }

    /**
     * 填充单个指标的统计项
     */
    private void fillStatItems(MetricConfig metric) {
        List<MetricStatItem> statItems = metricStatItemMapper.selectList(
                new LambdaQueryWrapper<MetricStatItem>()
                        .eq(MetricStatItem::getMetricId, metric.getId())
                        .orderByAsc(MetricStatItem::getSortOrder)
        );
        metric.setStatItems(statItems);
    }

    @Override
    public MetricConfig toggle(Long id, Boolean enabled) {
        MetricConfig metric = getById(id);
        if (metric == null) {
            log.warn("指标不存在，id: {}", id);
            throw ErrorCode.PARAM_CODE_EXISTS.exception(id.toString());
        }
        
        metric.setEnabled(enabled);
        updateById(metric);
        return metric;
    }

    @Override
    public void incrementQueryCount(String metricCode) {
        lambdaUpdate()
                .eq(MetricConfig::getMetricCode, metricCode)
                .setSql("query_count = query_count + 1")
                .update();
    }

    @Override
    @Transactional
    public void disableBySourceTable(String sourceTable, String reason) {
        log.info("开始禁用使用源表的指标，sourceTable: {}, reason: {}", sourceTable, reason);
        
        List<MetricConfig> metrics = lambdaQuery()
                .eq(MetricConfig::getSourceTable, sourceTable)
                .eq(MetricConfig::getEnabled, true)
                .list();
        
        if (metrics.isEmpty()) {
            log.info("没有找到使用源表 {} 的启用指标", sourceTable);
            return;
        }
        
        for (MetricConfig metric : metrics) {
            metric.setEnabled(false);
            metric.setDisabledReason(reason);
            updateById(metric);
            log.info("已禁用指标，metricCode: {}, metricName: {}", metric.getMetricCode(), metric.getMetricName());
        }
        
        log.info("完成禁用使用源表 {} 的指标，共禁用 {} 个", sourceTable, metrics.size());
    }

    @Override
    @Transactional
    public void disableByField(Long fieldId, String reason) {
        log.info("开始禁用使用字段的指标，fieldId: {}, reason: {}", fieldId, reason);
        
        FieldMetadata fieldMetadata = metadataService.getFieldById(fieldId);
        
        if (fieldMetadata == null) {
            log.warn("字段不存在，fieldId: {}", fieldId);
            return;
        }
        
        DataSource dataSource = metadataService.getDataSourceById(fieldMetadata.getSourceId());
        
        if (dataSource == null) {
            log.warn("数据源不存在，sourceId: {}", fieldMetadata.getSourceId());
            return;
        }
        
        String sourceTable = dataSource.getSourceCode();
        String fieldName = fieldMetadata.getFieldName();
        
        List<MetricConfig> metrics = lambdaQuery()
                .eq(MetricConfig::getSourceTable, sourceTable)
                .eq(MetricConfig::getEnabled, true)
                .list();
        
        List<MetricConfig> metricsToDisable = new ArrayList<>();

        for (MetricConfig metric : metrics) {
            Set<String> statItemAliases = getStatItemAliases(metric);
            Set<String> usedFields = extractFieldNames(metric, statItemAliases);
            if (usedFields.stream().anyMatch(f -> f.equalsIgnoreCase(fieldName))) {
                metricsToDisable.add(metric);
            }
        }
        
        if (metricsToDisable.isEmpty()) {
            log.info("没有找到使用字段 {} 的启用指标", fieldName);
            return;
        }
        
        for (MetricConfig metric : metricsToDisable) {
            metric.setEnabled(false);
            metric.setDisabledReason(reason);
            updateById(metric);
            log.info("已禁用指标，metricCode: {}, metricName: {}", metric.getMetricCode(), metric.getMetricName());
        }
        
        log.info("完成禁用使用字段 {} 的指标，共禁用 {} 个", fieldName, metricsToDisable.size());
    }
    
    /**
     * 验证指标配置的完整性和正确性
     * 检查 filterConditions、havingConditions、sortRules、windowFunctions、statItems 的完整性
     * 
     * @param metric 指标配置
     */
    private void validateMetricConfig(MetricConfig metric) {
        // 验证过滤条件
        validateFilterConditions(metric.getFilterConditions(), "filterConditions");
        
        // 验证 HAVING 条件
        validateFilterConditions(metric.getHavingConditions(), "havingConditions");
        
        // 验证排序规则
        validateSortRulesComplete(metric.getSortRules());
        
        // 验证窗口函数
        validateWindowFunctionsComplete(metric.getWindowFunctions());
        
        // 验证统计项
        validateStatItemsComplete(metric.getStatItems());
    }
    
    /**
     * 验证过滤条件（包括 filterConditions 和 havingConditions）
     */
    @SuppressWarnings("unchecked")
    private void validateFilterConditions(Object filterGroupObj, String fieldName) {
        if (filterGroupObj == null) {
            return;
        }
        
        try {
            Map<String, Object> filterGroup = objectMapper.convertValue(filterGroupObj, Map.class);
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) filterGroup.get("conditions");
            
            if (conditions == null || conditions.isEmpty()) {
                return;
            }
            
            for (int i = 0; i < conditions.size(); i++) {
                Map<String, Object> condition = conditions.get(i);
                
                // 检查是否为嵌套的 FilterGroup
                if (condition.containsKey("conditions")) {
                    // 递归验证嵌套的 FilterGroup
                    validateFilterConditions(condition, fieldName + "[].conditions[" + i + "]");
                } else {
                    // 验证普通的 FilterCondition
                    validateFilterCondition(condition, fieldName + "[" + i + "]");
                }
            }
        } catch (Exception e) {
            log.error("验证过滤条件失败: {}", fieldName, e);
            throw ErrorCode.PARAM_CONFIG_ERROR.exception(
                String.format("验证%s失败: %s", fieldName, e.getMessage()));
        }
    }
    
    /**
     * 验证单个过滤条件
     */
    private void validateFilterCondition(Map<String, Object> condition, String path) {
        // 验证必需字段
        validateRequiredField(condition, "field", path);
        validateRequiredField(condition, "operator", path);
        
        // 检查值或动态参数
        boolean hasValue = condition.containsKey("value") && condition.get("value") != null;
        boolean hasParamName = condition.containsKey("paramName") || condition.containsKey("param_name");
        
        if (!hasValue && !hasParamName) {
            log.warn("过滤条件缺少必需字段，path: {}", path);
            throw ErrorCode.PARAM_CONFIG_MISSING_FIELD.exception(
                String.format("%s: 条件必须指定 value 或 paramName（动态参数）", path));
        }
        
        // 如果有动态参数，检查 paramName 的格式
        if (hasParamName) {
            String paramName = (String) condition.getOrDefault("paramName", condition.get("param_name"));
            if (StringUtils.hasText(paramName)) {
                validateParamNameFormat(paramName, path);
            }
        }
    }
    
    /**
     * 验证排序规则完整性
     */
    @SuppressWarnings("unchecked")
    private void validateSortRulesComplete(Object sortRulesObj) {
        if (sortRulesObj == null) {
            return;
        }
        
        try {
            List<Map<String, Object>> sortRules = objectMapper.convertValue(sortRulesObj, List.class);
            
            for (int i = 0; i < sortRules.size(); i++) {
                Map<String, Object> rule = sortRules.get(i);
                String path = "sortRules[" + i + "]";
                
                // 验证必需字段
                validateRequiredField(rule, "field", path);
                validateRequiredField(rule, "order", path);
                
                // 验证排序方向
                String order = (String) rule.get("order");
                if (StringUtils.hasText(order) && !"asc".equalsIgnoreCase(order) && !"desc".equalsIgnoreCase(order)) {
                    log.warn("排序方向无效，path: {}, order: {}", path, order);
                    throw ErrorCode.PARAM_CONFIG_INVALID_FIELD.exception(
                        String.format("%s.order: 排序方向必须是 'asc' 或 'desc'，实际值: %s", path, order));
                }
                
                // 如果有动态参数，检查 paramName 的格式
                String paramName = (String) rule.getOrDefault("paramName", rule.get("param_name"));
                if (StringUtils.hasText(paramName)) {
                    validateParamNameFormat(paramName, path);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证排序规则失败", e);
            throw ErrorCode.PARAM_CONFIG_ERROR.exception(
                String.format("验证排序规则失败: %s", e.getMessage()));
        }
    }
    
    /**
     * 验证窗口函数完整性
     */
    @SuppressWarnings("unchecked")
    private void validateWindowFunctionsComplete(Object windowFunctionsObj) {
        if (windowFunctionsObj == null) {
            return;
        }
        
        try {
            Map<String, Object> windowFunctions = objectMapper.convertValue(windowFunctionsObj, Map.class);
            List<Map<String, Object>> functions = (List<Map<String, Object>>) windowFunctions.get("functions");
            
            if (functions == null || functions.isEmpty()) {
                return;
            }
            
            for (int i = 0; i < functions.size(); i++) {
                Map<String, Object> func = functions.get(i);
                String path = "windowFunctions.functions[" + i + "]";
                
                // 验证必需字段
                validateRequiredField(func, "name", path);
                // 验证 alias 或 item_name 至少存在一个
                boolean hasAlias = func.containsKey("alias") && func.get("alias") != null && 
                    (func.get("alias") instanceof String && StringUtils.hasText((String) func.get("alias")));
                boolean hasItemName = func.containsKey("itemName") && func.get("itemName") != null && 
                    (func.get("itemName") instanceof String && StringUtils.hasText((String) func.get("itemName"))) ||
                    func.containsKey("item_name") && func.get("item_name") != null && 
                    (func.get("item_name") instanceof String && StringUtils.hasText((String) func.get("item_name")));
                if (!hasAlias && !hasItemName) {
                    log.warn("窗口函数缺少别名，path: {}", path);
                    throw ErrorCode.PARAM_CONFIG_MISSING_FIELD.exception(
                        String.format("%s: 窗口函数必须指定 alias 或 itemName（item_name）作为别名", path));
                }
                
                // 验证函数名称
                String funcName = (String) func.get("name");
                if (StringUtils.hasText(funcName)) {
                    funcName = funcName.toUpperCase();
                    if (!"ROW_NUMBER".equals(funcName) && !"RANK".equals(funcName) &&
                        !"DENSE_RANK".equals(funcName) && !"SUM".equals(funcName) &&
                        !"AVG".equals(funcName) && !"MAX".equals(funcName) && !"MIN".equals(funcName)) {
                        log.warn("不支持的窗口函数，path: {}, funcName: {}", path, funcName);
                        throw ErrorCode.PARAM_CONFIG_INVALID_FIELD.exception(
                            String.format("%s.name: 不支持的窗口函数: %s", path, funcName));
                    }
                }
                
                // 验证 partition_by
                List<Object> partitionBy = (List<Object>) func.get("partitionBy");
                if (partitionBy != null) {
                    for (int j = 0; j < partitionBy.size(); j++) {
                        if (!(partitionBy.get(j) instanceof String)) {
                            log.warn("分区字段类型无效，path: {}, index: {}", path, j);
                            throw ErrorCode.PARAM_CONFIG_INVALID_FIELD.exception(
                                String.format("%s.partition_by[%d]: 必须是字符串类型", path, j));
                        }
                        String fieldName = (String) partitionBy.get(j);
                        if (!StringUtils.hasText(fieldName)) {
                            log.warn("分区字段为空，path: {}, index: {}", path, j);
                            throw ErrorCode.PARAM_CONFIG_MISSING_FIELD.exception(
                                String.format("%s.partition_by[%d]: 分区字段不能为空", path, j));
                        }
                    }
                }
                
                // 验证 order_by
                List<Map<String, Object>> orderBy = (List<Map<String, Object>>) func.get("orderBy");
                if (orderBy != null) {
                    for (int j = 0; j < orderBy.size(); j++) {
                        Map<String, Object> orderRule = orderBy.get(j);
                        String orderPath = path + ".order_by[" + j + "]";
                        
                        validateRequiredField(orderRule, "field", orderPath);
                        validateRequiredField(orderRule, "order", orderPath);
                        
                        // 如果有动态参数，检查 paramName 的格式
                        String paramName = (String) orderRule.getOrDefault("paramName", orderRule.get("param_name"));
                        if (StringUtils.hasText(paramName)) {
                            validateParamNameFormat(paramName, orderPath);
                        }
                    }
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("验证窗口函数失败", e);
            throw ErrorCode.PARAM_CONFIG_ERROR.exception(
                String.format("验证窗口函数失败: %s", e.getMessage()));
        }
    }
    
    /**
     * 验证统计项完整性
     */
    private void validateStatItemsComplete(List<MetricStatItem> statItems) {
        if (statItems == null || statItems.isEmpty()) {
            return;
        }
        
        for (int i = 0; i < statItems.size(); i++) {
            MetricStatItem item = statItems.get(i);
            String path = "statItems[" + i + "]";
            
            // 验证必需字段
            if (!StringUtils.hasText(item.getItemCode())) {
                log.warn("统计项缺少编码，path: {}", path);
                throw ErrorCode.PARAM_CONFIG_MISSING_FIELD.exception(
                    String.format("%s.itemCode: 统计项编码不能为空", path));
            }
            if (!StringUtils.hasText(item.getItemName())) {
                log.warn("统计项缺少名称，path: {}", path);
                throw ErrorCode.PARAM_CONFIG_MISSING_FIELD.exception(
                    String.format("%s.itemName: 统计项名称不能为空", path));
            }
            if (!StringUtils.hasText(item.getFieldName())) {
                log.warn("统计项缺少字段，path: {}", path);
                throw ErrorCode.PARAM_CONFIG_MISSING_FIELD.exception(
                    String.format("%s.fieldName: 统计字段不能为空", path));
            }
            if (item.getAggregation() == null) {
                log.warn("统计项缺少聚合方式，path: {}", path);
                throw ErrorCode.PARAM_CONFIG_MISSING_FIELD.exception(
                    String.format("%s.aggregation: 聚合方式不能为空", path));
            }
        }
    }
    
    /**
     * 验证必需字段是否存在
     */
    private void validateRequiredField(Map<String, Object> obj, String fieldName, String path) {
        if (!obj.containsKey(fieldName) || obj.get(fieldName) == null || 
            (obj.get(fieldName) instanceof String && !StringUtils.hasText((String) obj.get(fieldName)))) {
            log.warn("缺少必需字段，path: {}, field: {}", path, fieldName);
            throw ErrorCode.PARAM_CONFIG_MISSING_FIELD.exception(
                String.format("%s.%s: 字段不能为空", path, fieldName));
        }
    }
    
    /**
     * 验证动态参数名格式
     * 参数名应该符合 Java 变量命名规范，且不能为空
     */
    private void validateParamNameFormat(String paramName, String path) {
        if (!StringUtils.hasText(paramName)) {
            log.warn("参数名不能为空，path: {}", path);
            throw ErrorCode.PARAM_CONFIG_INVALID_FIELD.exception(
                String.format("%s.paramName: 动态参数名不能为空", path));
        }
        
        // 参数名只能包含字母、数字、下划线，且不能以数字开头
        if (!paramName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$")) {
            log.warn("参数名格式不正确，path: {}, paramName: {}", path, paramName);
            throw ErrorCode.PARAM_CONFIG_INVALID_FIELD.exception(
                String.format("%s.paramName: 参数名格式不正确，应符合 Java 变量命名规范（字母、数字、下划线，不能以数字开头），实际值: %s", path, paramName));
        }
    }
    
    /**
     * 验证指标查询是否正常
     * 在创建和更新指标后调用，确保指标的JSON规则可以正确生成SQL并执行
     * 
     * @param metricCode 指标编码
     */
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

    private void validateMetricQuery(String metricCode) {
        if (!StringUtils.hasText(metricCode)) {
            return;
        }
        
        try {
            log.info("开始验证指标查询，metricCode: {}", metricCode);
            
            // 生成SQL（不执行，仅验证SQL语法）
            String sql = queryEngineService.generateSql(metricCode, null);
            log.info("指标SQL生成成功，metricCode: {}, sql: {}", metricCode, sql);
            
        } catch (Exception e) {
            log.error("指标查询验证失败，metricCode: {}", metricCode, e);
            throw ErrorCode.PARAM_CONFIG_ERROR.exception(
                String.format("指标配置验证失败，无法生成有效的查询SQL: %s", e.getMessage()));
        }
    }
}
