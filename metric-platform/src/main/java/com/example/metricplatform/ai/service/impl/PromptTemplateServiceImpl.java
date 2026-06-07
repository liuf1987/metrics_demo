package com.example.metricplatform.ai.service.impl;

import com.example.metricplatform.metadata.entity.FieldMetadata;
import com.example.metricplatform.metric.entity.MetricConfig;
import com.example.metricplatform.ai.service.PromptTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final ObjectMapper objectMapper;

    @Override
    public String buildRecommendationPrompt(
            String userQuery,
            List<MetricConfig> existingMetrics,
            List<Map<String, Object>> metadataTables,
            Map<String, List<FieldMetadata>> tableFields
    ) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("你是一个专业的数据分析师助手。请基于以下信息为用户推荐合适的现有指标。\n\n");
        sb.append("## 用户需求\n").append(userQuery).append("\n\n");
        
        sb.append("## 现有的指标配置\n");
        for (int i = 0; i < existingMetrics.size(); i++) {
            MetricConfig metric = existingMetrics.get(i);
            sb.append("### 指标").append(i + 1).append("\n");
            sb.append("- 指标编码: ").append(metric.getMetricCode()).append("\n");
            sb.append("- 指标名称: ").append(metric.getMetricName()).append("\n");
            sb.append("- 指标描述: ").append(metric.getMetricDesc() != null ? metric.getMetricDesc() : "无").append("\n");
            sb.append("- 来源表: ").append(metric.getSourceTable() != null ? metric.getSourceTable() : "未指定").append("\n");
            sb.append("\n");
        }
        
        sb.append("## 可用的表和字段元数据\n");
        for (Map<String, Object> table : metadataTables) {
            String tableName = (String) table.get("tableName");
            sb.append("### 表: ").append(tableName).append("\n");
            
            List<FieldMetadata> fields = tableFields.get(tableName);
            if (fields != null) {
                sb.append("字段列表:\n");
                for (FieldMetadata field : fields) {
                    sb.append("- ").append(field.getFieldName())
                      .append(" (类型: ").append(field.getFieldType()).append(")");
                    if (field.getFieldLabel() != null && !field.getFieldLabel().isEmpty()) {
                        sb.append(" - ").append(field.getFieldLabel());
                    }
                    
                    // 显示支持的聚合函数
                    sb.append(" | 支持的聚合函数: ");
                    try {
                        com.example.metricplatform.common.enums.FieldType fieldType =
                            com.example.metricplatform.common.enums.FieldType.valueOf(
                                field.getFieldType().toUpperCase());
                        sb.append(fieldType.getSupportedAggregationsString());
                    } catch (Exception e) {
                        sb.append("count");
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        
        sb.append("## 任务\n");
        sb.append("请分析用户需求，并从现有指标中选择最匹配的指标。\n\n");
        sb.append("## 要求\n");
        sb.append("1. 仔细理解用户的需求\n");
        sb.append("2. 对比现有指标的描述和来源表，选择最相关的\n");
        sb.append("3. 为每个推荐的指标给出评分（0-100分，越高越匹配）和推荐理由\n");
        sb.append("4. 如果没有任何合适的现有指标，请设置 hasMatch 为 false，推荐指标列表为空，并在 message 中说明原因\n");
        sb.append("5. 返回严格的JSON格式，不要有任何其他文字说明\n\n");
        
        sb.append("## 返回JSON格式\n");
        sb.append("```json\n");
        sb.append("{\n");
        sb.append("  \"hasMatch\": true/false,\n");
        sb.append("  \"recommendedMetrics\": [\n");
        sb.append("    {\n");
        sb.append("      \"metricCode\": \"指标编码\",\n");
        sb.append("      \"metricName\": \"指标名称\",\n");
        sb.append("      \"score\": 95,\n");
        sb.append("      \"reason\": \"推荐理由\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"message\": \"说明文字\"\n");
        sb.append("}\n");
        sb.append("```\n");
        
        return sb.toString();
    }

    @Override
    public String buildGenerationPrompt(
            String userQuery,
            List<Map<String, Object>> metadataTables,
            Map<String, List<FieldMetadata>> tableFields
    ) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("你是一个专业的数据分析师和SQL专家。请基于以下信息为用户生成新的指标配置。\n\n");
        sb.append("## 用户需求\n").append(userQuery).append("\n\n");
        
        sb.append("## 可用的表和字段元数据\n");
        for (Map<String, Object> table : metadataTables) {
            String tableName = (String) table.get("tableName");
            sb.append("### 表: ").append(tableName).append("\n");
            
            List<FieldMetadata> fields = tableFields.get(tableName);
            if (fields != null) {
                sb.append("字段列表:\n");
                for (FieldMetadata field : fields) {
                    sb.append("- ").append(field.getFieldName())
                      .append(" (类型: ").append(field.getFieldType()).append(")");
                    if (field.getFieldLabel() != null && !field.getFieldLabel().isEmpty()) {
                        sb.append(" - ").append(field.getFieldLabel());
                    }
                    
                    // 显示支持的聚合函数
                    sb.append(" | 支持的聚合函数: ");
                    try {
                        com.example.metricplatform.common.enums.FieldType fieldType =
                            com.example.metricplatform.common.enums.FieldType.valueOf(
                                field.getFieldType().toUpperCase());
                        sb.append(fieldType.getSupportedAggregationsString());
                    } catch (Exception e) {
                        sb.append("count");
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        
        sb.append("## 指标配置JSON规范（完全体）\n");
        sb.append("请严格按照以下JSON结构生成指标配置：\n\n");
        sb.append("```json\n");
        sb.append(getCompleteMetricSpecExample());
        sb.append("\n```\n\n");
        
        sb.append("## 字段说明\n");
        sb.append("### 基础字段\n");
        sb.append("- metricCode: 指标编码（唯一，建议用下划线分隔的英文）\n");
        sb.append("- metricName: 指标名称（中文描述性名称）\n");
        sb.append("- metricDesc: 指标详细描述\n");
        sb.append("- sourceTable: 主数据源表名（从上面的元数据中选择）\n");
        sb.append("- categoryCode: 业务分类编码（可选，例如 content_operation）\n");
        sb.append("- categoryName: 业务分类名称（可选，例如 内容运营）\n");
        sb.append("- limitValue: 限制返回行数（可选，例如 100）\n");
        sb.append("- enabled: 是否启用，默认为 true\n");
        
        sb.append("\n### 分组字段（groupByFields数组）\n");
        sb.append("- 字符串数组，每个元素是分组的字段名\n");
        sb.append("- 例如: [\"status\", \"platform\"]\n");
        
        sb.append("\n### 筛选条件（filterConditions对象）\n");
        sb.append("- conditions: 条件数组，每个元素是单个筛选条件\n");
        sb.append("  - field: 字段名（必须是数据库表中存在的字段，不能使用计算表达式如 \"complete_play_count / play_count\"）\n");
        sb.append("  - operator: 操作符（=, !=, >, <, >=, <=, LIKE, IN, NOT IN）\n");
        sb.append("  - value: 筛选值\n");
        sb.append("  - paramName: 动态参数名（可选，用于从请求参数中替换值）\n");
        sb.append("- logic: 逻辑运算符，AND 或 OR，默认为 AND\n");
        sb.append("\n⚠️ 重要：filterConditions 中的 field 必须是数据库表中存在的简单字段名，不能使用计算表达式（如 \"a/b\", \"a+b\", \"(a+b)/c\" 等）。如果需要基于计算结果筛选，有两个方案：\n");
        sb.append("  方案1：把计算逻辑定义为一个聚合统计项（statItems），然后在 havingConditions 中使用该统计项的 itemCode 作为字段名\n");
        sb.append("  方案2：如果是针对原始数据的计算，可以考虑把计算写在 filterConditions 的 field 中（虽然不推荐，但执行引擎已支持这种写法）\n");
        
        sb.append("\n### Having条件（havingConditions对象）\n");
        sb.append("- 结构与 filterConditions 相同\n");
        sb.append("- field 应该是统计项的 itemCode（例如 total_count）\n");
        
        sb.append("\n### 排序规则（sortRules数组）\n");
        sb.append("- field: 排序字段名，可以是分组字段、统计项的 itemCode，或者数据库表字段\n");
        sb.append("- order: 排序方向，asc 或 desc\n");
        sb.append("- paramName: 动态参数名（可选，用于从请求参数中替换排序方向）\n");
        sb.append("\n⚠️ 非常重要：\n");
        sb.append("1. 如果 sortRules 中引用了统计项的 itemCode（例如 avg_size），则必须在 statItems 数组中定义对应的统计项！\n");
        sb.append("2. statItems 中的 itemCode 就是排序时引用的字段名！\n");
        sb.append("3. 例如：如果 sortRules 中有 {\"field\": \"avg_size\", \"order\": \"desc\"}，那么 statItems 中必须有一个 itemCode 为 \"avg_size\" 的统计项！\n");
        sb.append("4. sortRules 中的 field 通常应该是简单字段名或统计项别名，但执行引擎也允许使用计算表达式（如 \"complete_play_count / play_count\"），不过建议优先使用统计项别名的方式\n");
        
        sb.append("\n### 统计项列表（statItems数组）\n");
        sb.append("- itemCode: 统计项编码（唯一标识，例如 total_count），这个编码会在 sortRules 和 havingConditions 中被引用！\n");
        sb.append("- itemName: 统计项名称（例如 总数）\n");
        sb.append("- fieldName: 统计的字段名（从元数据中选择）\n");
        sb.append("- aggregation: 聚合方式（只能是以下五种：count, sum, avg, max, min）\n");
        sb.append("- sortOrder: 显示顺序，默认为 0\n");
        sb.append("\n⚠️ 🔴 非常重要 - 必须遵守：\n");
        sb.append("1. statItems 中的 itemCode 是排序和筛选的关键！\n");
        sb.append("2. 如果 sortRules 或 havingConditions 中引用了某个字段名，必须确保 statItems 中有对应的 itemCode！\n");
        sb.append("3. 🔴 聚合方式只能是：count, sum, avg, max, min！绝对不允许使用其他聚合函数！\n");
        sb.append("4. 🔴 如果用户要求使用 STDDEV、VARIANCE、STD、VAR、STDEV、VAR_SAMP、STDDEV_POP、PERCENTILE、MEDIAN 等不支持的聚合函数，必须返回 cannotProceed=true！\n");
        sb.append("5. 🔴 检查元数据中每个字段支持的聚合函数，只能使用该字段明确支持的聚合函数！\n");
        sb.append("6. 🔴 如果任何一个字段不支持用户要求的聚合函数，必须返回 cannotProceed=true！\n");
        
        sb.append("\n### 窗口函数（windowFunctions对象，可选）\n");
        sb.append("- functions: 窗口函数数组\n");
        sb.append("  - name: 窗口函数名（row_number, rank, dense_rank, sum, avg, max, min, ntile, lag, lead等）\n");
        sb.append("  - n: NTILE等函数的数字参数（例如 4）\n");
        sb.append("  - arguments: LAG/LEAD等函数的参数数组（例如 [\"play_count\", 1, 0]）\n");
        sb.append("  - partition_by: 分区字段数组（可以是表字段或分组字段）\n");
        sb.append("  - order_by: 排序规则数组（可以是表字段、分组字段或统计项别名）\n");
        sb.append("  - frame: 窗口框架定义（可选，例如 \"ROWS BETWEEN 2 PRECEDING AND CURRENT ROW\"）\n");
        sb.append("  - item_name: 统计项名称（作为别名使用）\n");
        sb.append("  - alias: 结果别名（可选，优先使用 item_name）\n");
        sb.append("\n### ⚠️ 窗口函数与 GROUP BY 同时使用的限制\n");
        sb.append("- 如果同时使用 GROUP BY 和窗口函数，窗口函数的 partition_by、order_by、arguments 只能使用 GROUP BY 中出现的字段或聚合函数的别名（例如 total_plays），不能直接引用原始表字段\n");
        sb.append("- 建议：如果需要窗口函数对原始数据进行处理，不要使用 GROUP BY，直接使用原始数据 + 窗口函数\n");
        sb.append("- 例如：LAG(\"play_count\", 1, 0) 这种引用原始字段的写法，在 GROUP BY 后会失败，应该改为 LAG(\"total_plays\", 1, 0)（total_plays 是聚合后的别名）\n");
        
        sb.append("\n## 任务\n");
        sb.append("1. 🔴 首先进行严格检查，判断是否能够满足用户需求！如果不能，直接返回 cannotProceed=true！\n");
        sb.append("2. 仔细理解用户的需求\n");
        sb.append("3. 基于提供的元数据信息，选择合适的表和字段\n");
        sb.append("4. 按照上述的规范JSON结构生成完整的指标配置\n");
        sb.append("5. 确保使用的字段都在提供的元数据中\n");
        sb.append("6. 只能使用元数据中提供的表和字段\n");
        sb.append("7. 只能使用示例模版中明确列出的属性，不要添加任何额外的字段（例如不要添加 customExpression 等）\n");
        sb.append("\n## 🔴 必须返回 cannotProceed=true 的情况（检查顺序判断，只要满足其中一个就返回）：\n");
        sb.append("1. 用户要求使用 STDDEV、VARIANCE、STD、VAR、STDEV、VAR_SAMP、STDDEV_POP、PERCENTILE、MEDIAN 等聚合函数\n");
        sb.append("2. 用户要求使用除 count、sum、avg、max、min 以外的任何聚合函数\n");
        sb.append("3. 用户要求使用的聚合函数不在相关字段支持的聚合函数列表中\n");
        sb.append("4. 用户需求需要使用自定义的 SQL 表达式或函数（如 CONCAT、SUBSTRING、DATE_FORMAT 等）\n");
        sb.append("5. 用户需求需要使用 JOIN、UNION、子查询等复杂 SQL 功能\n");
        sb.append("6. 用户需求需要使用 CASE WHEN、IF 等条件表达式\n");
        sb.append("7. 用户需求需要对字段进行数学计算（如 a/b、a+b 等），除非这种计算是简单的统计聚合\n");
        sb.append("8. 用户需求需要使用窗口函数以外的其他高级 SQL 功能\n");
        sb.append("9. 无法使用模版中的现有字段实现用户需求\n");
        sb.append("\n## 🔴 如果满足上述任何条件，直接返回：\n");
        sb.append("{\"cannotProceed\": true, \"reason\": \"当前功能不支持此需求，请联系管理员提升执行引擎能力\"}\n");
        sb.append("\n## 只有在确认可以完全满足以下所有条件时，才生成指标配置：\n");
        sb.append("1. 所有聚合函数都是 count、sum、avg、max、min 之一\n");
        sb.append("2. 所有使用的字段都在元数据中，且聚合函数在该字段支持的列表中\n");
        sb.append("3. 不需要使用任何高级 SQL 功能\n");
        sb.append("4. 所有功能都可以通过提供的字段和聚合函数实现\n");
        sb.append("\n## 返回要求\n");
        sb.append("1. 返回严格的JSON格式，不要有任何其他文字说明\n\n");
        
        sb.append("## 返回JSON格式\n");
        sb.append("直接返回上述格式的完整指标配置JSON，不要包装在其他对象中。\n");
        
        return sb.toString();
    }
    
    private String getCompleteMetricSpecExample() {
        return "{\n" +
               "  \"metricCode\": \"asset_count_by_status_platform\",\n" +
               "  \"metricName\": \"完整指标测试\",\n" +
               "  \"metricDesc\": \"包含所有指标规则条件的测试指标\",\n" +
               "  \"sourceTable\": \"asset\",\n" +
               "  \"categoryCode\": \"test_category\",\n" +
               "  \"categoryName\": \"测试分类\",\n" +
               "  \"groupByFields\": [\"status\", \"platform\"],\n" +
               "  \"filterConditions\": {\n" +
               "    \"conditions\": [\n" +
               "      {\n" +
               "        \"field\": \"uploaded_at\",\n" +
               "        \"operator\": \">=\",\n" +
               "        \"value\": \"2026-05-01\",\n" +
               "        \"paramName\": \"filter_start_date\"\n" +
               "      },\n" +
               "      {\n" +
               "        \"field\": \"status\",\n" +
               "        \"operator\": \"IN\",\n" +
               "        \"value\": [\"approved\", \"pending\"],\n" +
               "        \"paramName\": \"filter_status\"\n" +
               "      }\n" +
               "    ],\n" +
               "    \"logic\": \"AND\"\n" +
               "  },\n" +
               "  \"havingConditions\": {\n" +
               "    \"conditions\": [\n" +
               "      {\n" +
               "        \"field\": \"total_count\",\n" +
               "        \"operator\": \">\",\n" +
               "        \"value\": 1,\n" +
               "        \"paramName\": \"having_min_count\"\n" +
               "      }\n" +
               "    ],\n" +
               "    \"logic\": \"AND\"\n" +
               "  },\n" +
               "  \"sortRules\": [\n" +
               "    {\n" +
               "      \"field\": \"total_count\",\n" +
               "      \"order\": \"desc\",\n" +
               "      \"paramName\": \"sort_by_count\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"field\": \"status\",\n" +
               "      \"order\": \"asc\",\n" +
               "      \"paramName\": \"sort_by_status\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"limitValue\": 50,\n" +
               "  \"windowFunctions\": {\n" +
               "    \"functions\": [\n" +
               "      {\n" +
               "        \"name\": \"row_number\",\n" +
               "        \"partition_by\": [\"platform\"],\n" +
               "        \"order_by\": [\n" +
               "          {\n" +
               "            \"field\": \"uploaded_at\",\n" +
               "            \"direction\": \"DESC\",\n" +
               "            \"paramName\": \"window_sort_date\"\n" +
               "          }\n" +
               "        ],\n" +
               "        \"item_name\": \"row_num\"\n" +
               "      },\n" +
               "      {\n" +
               "        \"name\": \"avg\",\n" +
               "        \"arguments\": [\"play_count\"],\n" +
               "        \"partition_by\": [\"platform\"],\n" +
               "        \"order_by\": [\n" +
               "          {\n" +
               "            \"field\": \"uploaded_at\",\n" +
               "            \"direction\": \"ASC\",\n" +
               "            \"paramName\": \"window_avg_order\"\n" +
               "          }\n" +
               "        ],\n" +
               "        \"frame\": \"ROWS BETWEEN 2 PRECEDING AND CURRENT ROW\",\n" +
               "        \"item_name\": \"moving_avg_3\"\n" +
               "      }\n" +
               "    ]\n" +
               "  },\n" +
               "  \"statItems\": [\n" +
               "    {\n" +
               "      \"fieldName\": \"asset_id\",\n" +
               "      \"aggregation\": \"count\",\n" +
               "      \"itemCode\": \"total_count\",\n" +
               "      \"itemName\": \"总数\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"fieldName\": \"file_size_bytes\",\n" +
               "      \"aggregation\": \"sum\",\n" +
               "      \"itemCode\": \"total_size\",\n" +
               "      \"itemName\": \"总文件大小\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"fieldName\": \"impression_count\",\n" +
               "      \"aggregation\": \"avg\",\n" +
               "      \"itemCode\": \"avg_impressions\",\n" +
               "      \"itemName\": \"平均曝光次数\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"enabled\": true\n" +
               "}";
    }
}
