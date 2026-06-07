package com.example.metricplatform.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.metricplatform.ai.dto.*;
import com.example.metricplatform.ai.entity.AIChatHistory;
import com.example.metricplatform.metric.dto.MetricCreateRequest;
import com.example.metricplatform.metric.dto.MetricFeedbackRequest;
import com.example.metricplatform.metric.dto.MetricStatItemRequest;
import com.example.metricplatform.metadata.entity.DataSource;
import com.example.metricplatform.metadata.entity.FieldMetadata;
import com.example.metricplatform.metric.entity.MetricConfig;
import com.example.metricplatform.common.enums.MetricSource;
import com.example.metricplatform.common.exception.BusinessException;
import com.example.metricplatform.ai.mapper.AIChatHistoryMapper;
import com.example.metricplatform.metric.entity.MetricStatItem;
import com.example.metricplatform.metadata.mapper.DataSourceMapper;
import com.example.metricplatform.metadata.mapper.FieldMetadataMapper;
import com.example.metricplatform.metric.mapper.MetricConfigMapper;
import com.example.metricplatform.ai.service.AIService;
import com.example.metricplatform.ai.service.AIConfigurationChecker;
import com.example.metricplatform.ai.service.JsonResponseParser;
import com.example.metricplatform.metric.service.MetricConfigService;
import com.example.metricplatform.ai.service.PromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于阿里云百炼大模型的AI服务实现
 * 
 * 本服务类通过阿里云百炼大模型提供以下功能：
 * 1. 指标查询：根据用户需求查询现有的指标配置
 * 2. 指标生成：根据用户需求和数据元数据生成新的指标配置
 * 3. 聊天历史管理：保存和查询AI交互历史
 * 4. 反馈收集：收集用户对AI服务的满意度反馈
 * 
 * 通过 OpenAI 兼容模式与阿里云百炼进行交互，
 * 利用百炼的智能分析能力帮助用户快速找到或生成合适的业务指标。
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private final MetricConfigMapper metricConfigMapper;
    private final AIChatHistoryMapper aiChatHistoryMapper;
    private final DataSourceMapper dataSourceMapper;
    private final FieldMetadataMapper fieldMetadataMapper;
    private final MetricConfigService metricConfigService;
    private final ChatModel chatModel;
    private final AIConfigurationChecker aiConfigurationChecker;
    private final PromptTemplateService promptTemplateService;
    private final JsonResponseParser jsonResponseParser;

    @Value("${metadata.exclude-tables:}")
    private List<String> excludeTables;

    /**
     * 构造函数
     * 
     * @param metricConfigMapper 指标配置Mapper
     * @param aiChatHistoryMapper AI聊天历史Mapper
     * @param dataSourceMapper 数据源Mapper
     * @param fieldMetadataMapper 字段元数据Mapper
     * @param metricConfigService 指标配置服务
     * @param chatModel 聊天模型
     * @param aiConfigurationChecker AI配置检查器
     * @param promptTemplateService 提示词模板服务
     * @param jsonResponseParser JSON响应解析器
     */
    public AIServiceImpl(
            MetricConfigMapper metricConfigMapper,
            AIChatHistoryMapper aiChatHistoryMapper,
            DataSourceMapper dataSourceMapper,
            FieldMetadataMapper fieldMetadataMapper,
            MetricConfigService metricConfigService,
            ChatModel chatModel,
            AIConfigurationChecker aiConfigurationChecker,
            PromptTemplateService promptTemplateService,
            JsonResponseParser jsonResponseParser) {
        this.metricConfigMapper = metricConfigMapper;
        this.aiChatHistoryMapper = aiChatHistoryMapper;
        this.dataSourceMapper = dataSourceMapper;
        this.fieldMetadataMapper = fieldMetadataMapper;
        this.metricConfigService = metricConfigService;
        this.chatModel = chatModel;
        this.aiConfigurationChecker = aiConfigurationChecker;
        this.promptTemplateService = promptTemplateService;
        this.jsonResponseParser = jsonResponseParser;
    }

    /**
     * 使用阿里云百炼大模型查询现有指标
     * 
     * 根据用户输入的查询需求，结合已有的指标配置和数据元数据，
     * 通过阿里云百炼大模型智能匹配最合适的现有指标。
     * 
     * @param request 包含用户查询内容的请求对象
     * @return 包含匹配的指标列表和AI响应的结果对象
     */
    @Override
    public AIChatResponse queryMetrics(AIChatRequest request) {
        String query = request.getQuery().trim();
        log.info("开始查询现有指标，用户需求: {}", query);

        String prompt = null;
        String aiResponse = null;
        ChatResponse chatResponse = null;
        long startTime = System.currentTimeMillis();
        Long promptTokens = null;
        Long completionTokens = null;

        try {
            // 1. 获取所有现有指标
            List<MetricConfig> existingMetrics = getAllActiveMetrics();
            log.debug("获取到 {} 个现有指标", existingMetrics.size());

            // 2. 获取元数据
            List<Map<String, Object>> metadataTables = getMetadataTables();
            Map<String, List<FieldMetadata>> tableFields = getTableFields(metadataTables);

            // 3. 构建提示词
            prompt = promptTemplateService.buildRecommendationPrompt(
                    query, existingMetrics, metadataTables, tableFields);
            log.debug("发送给AI的推荐提示词: {}", prompt);

            // 4. 调用AI - 使用完整的Prompt和ChatResponse获取token信息
            log.info("开始调用AI服务...");
            List<Message> messages = List.of(new UserMessage(prompt));
            chatResponse = chatModel.call(new Prompt(messages));
            aiResponse = chatResponse.getResult().getOutput().getContent();
            log.info("AI推荐响应: {}", aiResponse);

            // 计算请求时长和获取真实的token数量
            long requestDuration = System.currentTimeMillis() - startTime;
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                promptTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
                completionTokens = chatResponse.getMetadata().getUsage().getGenerationTokens();
            } else {
                // 回退到估算方式
                promptTokens = (long) estimateTokens(prompt);
                completionTokens = (long) estimateTokens(aiResponse);
            }

            // 5. 解析AI返回的结果
            RecommendationResult recommendationResult = jsonResponseParser.parse(
                    aiResponse, RecommendationResult.class);

            // 6. 根据AI推荐结果获取详细指标信息
            List<MetricConfig> recommendedMetrics = new ArrayList<>();
            if (recommendationResult.getHasMatch() && recommendationResult.getRecommendedMetrics() != null) {
                for (RecommendedMetric rm : recommendationResult.getRecommendedMetrics()) {
                    MetricConfig metric = findMetricByCode(rm.getMetricCode());
                    if (metric != null) {
                        recommendedMetrics.add(metric);
                    }
                }
            }

            // 7. 保存聊天历史
            AIChatHistory chatHistory = new AIChatHistory();
            chatHistory.setUserQuery(request.getQuery());
            chatHistory.setSourceInterface("/ai/query");
            chatHistory.setMetricSource(MetricSource.EXISTING);
            chatHistory.setAiPrompt(prompt);
            chatHistory.setAiResponse(aiResponse);
            chatHistory.setPromptTokens(promptTokens);
            chatHistory.setCompletionTokens(completionTokens);
            chatHistory.setRequestDurationMs(requestDuration);
            aiChatHistoryMapper.insert(chatHistory);

            String message = recommendationResult.getMessage() != null ? 
                    recommendationResult.getMessage() : 
                    (recommendedMetrics.isEmpty() ? "未找到合适的现有指标" : "已为您推荐相关指标");

            return new AIChatResponse(
                    request.getQuery(),
                    recommendedMetrics,
                    null,
                    message,
                    chatHistory.getId(),
                    false);

        } catch (Exception e) {
            log.error("查询现有指标失败", e);
            // 保存聊天历史（即使失败也保存）
            if (prompt != null) {
                try {
                    long requestDuration = System.currentTimeMillis() - startTime;
                    AIChatHistory chatHistory = new AIChatHistory();
                    chatHistory.setUserQuery(request.getQuery());
                    chatHistory.setSourceInterface("/ai/query");
                    chatHistory.setMetricSource(MetricSource.EXISTING);
                    chatHistory.setAiPrompt(prompt);
                    chatHistory.setAiResponse(aiResponse != null ? aiResponse : "ERROR: " + e.getMessage());
                    chatHistory.setPromptTokens(promptTokens);
                    chatHistory.setCompletionTokens(completionTokens);
                    chatHistory.setRequestDurationMs(requestDuration);
                    aiChatHistoryMapper.insert(chatHistory);
                } catch (Exception ex) {
                    log.error("保存失败历史也失败", ex);
                }
            }
            throw new BusinessException("查询现有指标失败: " + e.getMessage());
        }
    }

    /**
     * 使用阿里云百炼大模型生成新指标
     * 
     * 根据用户输入的查询需求和数据元数据，通过阿里云百炼大模型
     * 智能生成新的指标配置，并保存到数据库中。
     * 
     * @param request 包含用户查询内容的请求对象
     * @return 包含生成的指标配置和AI响应的结果对象
     */
    @Override
    @Transactional
    public AIChatResponse generateMetric(AIChatRequest request) {
        aiConfigurationChecker.checkConfiguration();
        
        String query = request.getQuery().trim();
        log.info("开始生成新指标，用户需求: {}", query);

        String prompt = null;
        String aiResponse = null;
        ChatResponse chatResponse = null;
        long startTime = System.currentTimeMillis();
        Long promptTokens = null;
        Long completionTokens = null;

        try {
            // 1. 获取元数据
            List<Map<String, Object>> metadataTables = getMetadataTables();
            Map<String, List<FieldMetadata>> tableFields = getTableFields(metadataTables);

            // 2. 构建提示词
            prompt = promptTemplateService.buildGenerationPrompt(
                    query, metadataTables, tableFields);
            log.debug("发送给AI的生成提示词: {}", prompt);

            // 3. 调用AI - 使用完整的Prompt和ChatResponse获取token信息
            log.info("开始调用AI服务...");
            List<Message> messages = List.of(new UserMessage(prompt));
            chatResponse = chatModel.call(new Prompt(messages));
            aiResponse = chatResponse.getResult().getOutput().getContent();
            log.info("AI生成响应: {}", aiResponse);

            // 计算请求时长和获取真实的token数量
            long requestDuration = System.currentTimeMillis() - startTime;
            if (chatResponse.getMetadata() != null && chatResponse.getMetadata().getUsage() != null) {
                promptTokens = chatResponse.getMetadata().getUsage().getPromptTokens();
                completionTokens = chatResponse.getMetadata().getUsage().getGenerationTokens();
            } else {
                // 回退到估算方式
                promptTokens = (long) estimateTokens(prompt);
                completionTokens = (long) estimateTokens(aiResponse);
            }

            // 4. 先检查是否是无法继续的失败响应（优先使用更直接的JSON检查）
            boolean isCannotProceed = checkCannotProceed(aiResponse);
            if (isCannotProceed) {
                AIGenerationFailure failure = null;
                String reason = "当前功能不支持此需求";
                try {
                    failure = jsonResponseParser.parse(aiResponse, AIGenerationFailure.class);
                    if (failure != null && failure.getReason() != null) {
                        reason = failure.getReason();
                    }
                } catch (Exception e) {
                    log.warn("解析失败响应失败，使用默认原因", e);
                }
                
                log.warn("AI无法处理此需求: {}", reason);
                // 保存聊天历史
                AIChatHistory chatHistory = new AIChatHistory();
                chatHistory.setUserQuery(request.getQuery());
                chatHistory.setSourceInterface("/ai/generate");
                chatHistory.setMetricSource(MetricSource.GENERATED);
                chatHistory.setAiPrompt(prompt);
                chatHistory.setAiResponse(aiResponse);
                chatHistory.setPromptTokens(promptTokens);
                chatHistory.setCompletionTokens(completionTokens);
                chatHistory.setRequestDurationMs(requestDuration);
                aiChatHistoryMapper.insert(chatHistory);
                
                // 返回正常的200响应，标记为无法处理
                return new AIChatResponse(
                    request.getQuery(),
                    null,
                    null,
                    reason,
                    chatHistory.getId(),
                    true
                );
            }
            
            // 6. 解析为指标配置
            MetricConfig generatedConfig = jsonResponseParser.parse(
                    aiResponse, MetricConfig.class);

            // 7. 确保一些必填字段有值
            if (generatedConfig.getEnabled() == null) {
                generatedConfig.setEnabled(true);
            }

            // 8. 检查并处理 metricCode 和 metricName 冲突
            generatedConfig = ensureUniqueMetricCodeAndName(generatedConfig);

            // 9. 尝试保存新指标，如果失败则当做cannotProceed处理
            MetricConfig savedMetric = null;
            String saveErrorMessage = null;
            try {
                savedMetric = metricConfigService.create(
                        convertToCreateRequest(generatedConfig));
            } catch (Exception saveException) {
                log.warn("保存指标失败，当做无法处理处理: {}", saveException.getMessage());
                saveErrorMessage = saveException.getMessage();
            }

            // 10. 保存聊天历史
            AIChatHistory chatHistory = new AIChatHistory();
            chatHistory.setUserQuery(request.getQuery());
            chatHistory.setSourceInterface("/ai/generate");
            if (savedMetric != null) {
                chatHistory.setMetricId(savedMetric.getId());
            }
            chatHistory.setMetricSource(MetricSource.GENERATED);
            chatHistory.setAiPrompt(prompt);
            chatHistory.setAiResponse(aiResponse);
            chatHistory.setPromptTokens(promptTokens);
            chatHistory.setCompletionTokens(completionTokens);
            chatHistory.setRequestDurationMs(requestDuration);
            aiChatHistoryMapper.insert(chatHistory);

            // 11. 根据保存结果返回响应
            if (savedMetric != null) {
                // 保存成功，返回正常响应
                return new AIChatResponse(
                        request.getQuery(),
                        null,
                        savedMetric,
                        "已成功生成新指标",
                        chatHistory.getId(),
                        false);
            } else {
                // 保存失败，当做无法处理
                String reason = "当前功能不支持此需求，请联系管理员提升执行引擎能力";
                log.warn("AI无法处理此需求（保存指标失败）: {}", saveErrorMessage);
                return new AIChatResponse(
                        request.getQuery(),
                        null,
                        null,
                        reason,
                        chatHistory.getId(),
                        true);
            }

        } catch (Exception e) {
            log.error("生成新指标失败", e);
            // 保存聊天历史（即使失败也保存）
            if (prompt != null) {
                try {
                    long requestDuration = System.currentTimeMillis() - startTime;
                    AIChatHistory chatHistory = new AIChatHistory();
                    chatHistory.setUserQuery(request.getQuery());
                    chatHistory.setSourceInterface("/ai/generate");
                    chatHistory.setMetricSource(MetricSource.GENERATED);
                    chatHistory.setAiPrompt(prompt);
                    chatHistory.setAiResponse(aiResponse != null ? aiResponse : "ERROR: " + e.getMessage());
                    chatHistory.setPromptTokens(promptTokens);
                    chatHistory.setCompletionTokens(completionTokens);
                    chatHistory.setRequestDurationMs(requestDuration);
                    aiChatHistoryMapper.insert(chatHistory);
                    
                    // 其他异常也当做无法处理返回
                    String reason = "当前功能不支持此需求，请联系管理员提升执行引擎能力";
                    log.warn("AI无法处理此需求: {}", reason);
                    return new AIChatResponse(
                            request.getQuery(),
                            null,
                            null,
                            reason,
                            chatHistory.getId(),
                            true);
                } catch (Exception ex) {
                    log.error("保存失败历史也失败", ex);
                    // 如果连聊天历史都保存失败，还是抛异常
                    throw new BusinessException("生成新指标失败: " + e.getMessage());
                }
            }
            // 如果连prompt都没设置，抛异常
            throw new BusinessException("生成新指标失败: " + e.getMessage());
        }
    }

    /**
     * 提交用户满意度反馈
     * 
     * 用于收集用户对阿里云百炼大模型服务的满意度反馈。
     * 
     * @param request 包含聊天历史ID和满意度评分的请求对象
     */
    @Override
    public void submitFeedback(MetricFeedbackRequest request) {
        AIChatHistory chatHistory = aiChatHistoryMapper.selectById(request.getChatHistoryId());
        if (chatHistory == null) {
            throw new BusinessException(3001, "聊天记录不存在");
        }

        chatHistory.setSatisfaction(request.getSatisfaction());
        aiChatHistoryMapper.updateById(chatHistory);
    }

    /**
     * 获取AI聊天历史记录
     * 
     * 分页查询用户与阿里云百炼大模型的交互历史记录。
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 包含聊天历史的分页结果
     */
    @Override
    public Page<AIChatHistory> getChatHistory(Integer pageNum, Integer pageSize) {
        Page<AIChatHistory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AIChatHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(AIChatHistory::getCreatedAt);
        return aiChatHistoryMapper.selectPage(page, wrapper);
    }

    /**
     * 查找相似指标
     * 
     * （当前为简化实现，返回所有启用的指标）
     * 
     * @param query 查询关键词
     * @param topN 返回的最多数量
     * @return 指标列表
     */
    @Override
    public List<MetricConfig> findSimilarMetrics(String query, Integer topN) {
        return getAllActiveMetrics();
    }

    // ==================== 私有方法 ====================

    /**
     * 检查AI响应是否包含 cannotProceed=true 标志
     * 这个方法使用更直接的方式，避免复杂的解析可能导致的问题
     * 
     * @param aiResponse AI返回的原始响应
     * @return 是否是不能继续的失败响应
     */
    private boolean checkCannotProceed(String aiResponse) {
        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            return false;
        }
        
        try {
            // 提取JSON内容
            String jsonContent = extractJsonFromResponse(aiResponse);
            
            // 使用简单的字符串检查，避免复杂的类解析失败
            // 检查是否包含 "cannotProceed" : true 或 "cannotProceed": true
            if (jsonContent.contains("\"cannotProceed\"") && 
                (jsonContent.contains(": true") || jsonContent.contains(":true"))) {
                log.info("检测到 cannotProceed=true 标志，将中断指标生成流程");
                return true;
            }
            
            // 也尝试用 ObjectMapper 直接解析，作为备选方案
            try {
                com.fasterxml.jackson.databind.JsonNode rootNode = 
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonContent);
                if (rootNode.has("cannotProceed")) {
                    return rootNode.get("cannotProceed").asBoolean(false);
                }
            } catch (Exception e) {
                log.debug("使用JsonNode检查失败，已通过字符串检查: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.debug("检查cannotProceed标志时出现异常: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 从AI响应中提取JSON内容（与JsonResponseParser类似的逻辑，但不抛异常）
     */
    private String extractJsonFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        String content = response.trim();
        
        // 情况1: 已经是纯JSON
        if (content.startsWith("{") || content.startsWith("[")) {
            return extractCompleteJsonSimple(content);
        }
        
        // 情况2: 包含Markdown代码块
        if (content.contains("```json")) {
            return extractFromMarkdownSimple(content, "```json", "```");
        }
        if (content.contains("```")) {
            return extractFromMarkdownSimple(content, "```", "```");
        }
        
        // 情况3: 尝试查找第一个{或[
        int startIndex = Math.min(
            content.indexOf('{') >= 0 ? content.indexOf('{') : Integer.MAX_VALUE,
            content.indexOf('[') >= 0 ? content.indexOf('[') : Integer.MAX_VALUE
        );
        
        if (startIndex != Integer.MAX_VALUE) {
            String remaining = content.substring(startIndex);
            return extractCompleteJsonSimple(remaining);
        }
        
        return content;
    }
    
    private String extractFromMarkdownSimple(String content, String startMarker, String endMarker) {
        int start = content.indexOf(startMarker);
        if (start == -1) return content;
        
        int contentStart = start + startMarker.length();
        int end = content.indexOf(endMarker, contentStart);
        if (end == -1) {
            end = content.lastIndexOf(endMarker);
        }
        
        if (end == -1 || end <= contentStart) {
            return content;
        }
        
        return content.substring(contentStart, end).trim();
    }
    
    private String extractCompleteJsonSimple(String content) {
        int balance = 0;
        int startIndex = -1;
        int endIndex = -1;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{' || c == '[') {
                if (balance == 0) {
                    startIndex = i;
                }
                balance++;
            } else if (c == '}' || c == ']') {
                balance--;
                if (balance == 0 && startIndex != -1) {
                    endIndex = i + 1;
                    break;
                }
            }
        }
        
        if (startIndex != -1 && endIndex != -1) {
            return content.substring(startIndex, endIndex).trim();
        }
        
        return content;
    }

    /**
     * 确保 metricCode 和 metricName 唯一，如果已存在则添加时间戳后缀
     * 
     * @param config 原始指标配置
     * @return 更新后的指标配置
     */
    private MetricConfig ensureUniqueMetricCodeAndName(MetricConfig config) {
        String originalCode = config.getMetricCode();
        String originalName = config.getMetricName();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);

        // 检查 metricCode 是否存在
        if (checkMetricCodeExists(originalCode)) {
            String newCode = originalCode + "_" + timestamp;
            log.info("metricCode '{}' 已存在，使用新编码: {}", originalCode, newCode);
            config.setMetricCode(newCode);
        }

        // 检查 metricName 是否存在
        if (checkMetricNameExists(originalName)) {
            String newName = originalName + " (" + timestamp + ")";
            log.info("metricName '{}' 已存在，使用新名称: {}", originalName, newName);
            config.setMetricName(newName);
        }

        return config;
    }

    /**
     * 检查 metricCode 是否已存在
     * 
     * @param metricCode 指标编码
     * @return 是否存在
     */
    private boolean checkMetricCodeExists(String metricCode) {
        if (metricCode == null || metricCode.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<MetricConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricConfig::getMetricCode, metricCode);
        return metricConfigMapper.selectCount(wrapper) > 0;
    }

    /**
     * 检查 metricName 是否已存在
     * 
     * @param metricName 指标名称
     * @return 是否存在
     */
    private boolean checkMetricNameExists(String metricName) {
        if (metricName == null || metricName.trim().isEmpty()) {
            return false;
        }
        LambdaQueryWrapper<MetricConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricConfig::getMetricName, metricName);
        return metricConfigMapper.selectCount(wrapper) > 0;
    }

    private List<MetricConfig> getAllActiveMetrics() {
        LambdaQueryWrapper<MetricConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricConfig::getEnabled, true);
        return metricConfigMapper.selectList(wrapper);
    }

    private MetricConfig findMetricByCode(String metricCode) {
        if (metricCode == null || metricCode.trim().isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<MetricConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MetricConfig::getMetricCode, metricCode);
        return metricConfigMapper.selectOne(wrapper);
    }

    private List<Map<String, Object>> getMetadataTables() {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getEnabled, true);
        if (excludeTables != null && !excludeTables.isEmpty()) {
            wrapper.notIn(DataSource::getSourceCode, excludeTables);
        }
        
        List<DataSource> dataSources = dataSourceMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        
        for (DataSource ds : dataSources) {
            Map<String, Object> tableInfo = new HashMap<>();
            tableInfo.put("tableName", ds.getSourceCode());
            tableInfo.put("tableDesc", ds.getDescription());
            tableInfo.put("sourceId", ds.getId());
            result.add(tableInfo);
        }
        
        return result;
    }

    private Map<String, List<FieldMetadata>> getTableFields(List<Map<String, Object>> tables) {
        Map<String, List<FieldMetadata>> result = new HashMap<>();
        
        for (Map<String, Object> table : tables) {
            String tableName = (String) table.get("tableName");
            Long sourceId = (Long) table.get("sourceId");
            
            LambdaQueryWrapper<FieldMetadata> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FieldMetadata::getSourceId, sourceId);
            List<FieldMetadata> fields = fieldMetadataMapper.selectList(wrapper);
            result.put(tableName, fields);
        }
        
        return result;
    }

    private MetricCreateRequest convertToCreateRequest(MetricConfig metric) {
        MetricCreateRequest request = new MetricCreateRequest();
        request.setMetricCode(metric.getMetricCode());
        request.setMetricName(metric.getMetricName());
        request.setMetricDesc(metric.getMetricDesc());
        request.setSourceTable(metric.getSourceTable());
        request.setCategoryCode(metric.getCategoryCode());
        request.setCategoryName(metric.getCategoryName());
        request.setLimitValue(metric.getLimitValue());
        request.setGroupByFields(metric.getGroupByFields());
        request.setFilterConditions(metric.getFilterConditions());
        request.setHavingConditions(metric.getHavingConditions());
        request.setSortRules(metric.getSortRules());
        request.setWindowFunctions(metric.getWindowFunctions());
        request.setStatItems(convertStatItemsToRequest(metric.getStatItems()));
        
        return request;
    }
    
    /**
     * 将 MetricStatItem 实体列表转换为 MetricStatItemRequest 列表
     */
    private List<MetricStatItemRequest> convertStatItemsToRequest(
            List<MetricStatItem> statItems) {
        if (statItems == null || statItems.isEmpty()) {
            return null;
        }
        
        List<MetricStatItemRequest> requests = new ArrayList<>();
        for (MetricStatItem item : statItems) {
            MetricStatItemRequest request =
                new MetricStatItemRequest();
            request.setItemCode(item.getItemCode());
            request.setItemName(item.getItemName());
            request.setFieldName(item.getFieldName());
            request.setAggregation(item.getAggregation());
            request.setSortOrder(item.getSortOrder());
            requests.add(request);
        }
        return requests;
    }

    /**
     * 估算文本的token数量
     * 简化估算：中文按每个字1个token，英文按每个单词1个token
     * 
     * @param text 输入文本
     * @return 估算的token数量
     */
    private Integer estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        int tokenCount = 0;
        boolean inEnglish = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            
            // 检查是否为中文字符（Unicode范围）
            if (c >= 0x4E00 && c <= 0x9FFF || c >= 0x3400 && c <= 0x4DBF) {
                tokenCount++;
                inEnglish = false;
            }
            // 检查是否为英文字母或数字
            else if (Character.isLetterOrDigit(c)) {
                if (!inEnglish) {
                    tokenCount++;
                    inEnglish = true;
                }
            }
            // 其他字符（标点符号等）
            else {
                tokenCount++;
                inEnglish = false;
            }
        }
        
        return tokenCount;
    }
}
