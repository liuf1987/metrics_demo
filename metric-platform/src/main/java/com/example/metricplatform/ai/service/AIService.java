package com.example.metricplatform.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.metricplatform.ai.dto.AIChatRequest;
import com.example.metricplatform.ai.dto.AIChatResponse;
import com.example.metricplatform.metric.dto.MetricFeedbackRequest;
import com.example.metricplatform.ai.entity.AIChatHistory;
import com.example.metricplatform.metric.entity.MetricConfig;

import java.util.List;

/**
 * 基于阿里云百炼大模型的AI服务接口
 * 
 * 该服务接口提供与AI指标生成相关的功能，所有AI功能均通过阿里云百炼大模型实现。
 * 包括指标查询、指标生成、反馈收集和聊天历史管理等功能。
 */
public interface AIService {

    /**
     * 使用阿里云百炼大模型查询现有指标
     * 
     * 根据用户查询语句，结合现有指标配置和数据元数据，
     * 通过阿里云百炼大模型智能匹配最合适的现有指标。
     * 
     * @param request 包含用户查询内容的请求对象
     * @return 包含匹配的指标列表和AI响应的结果对象
     */
    AIChatResponse queryMetrics(AIChatRequest request);

    /**
     * 使用阿里云百炼大模型生成新指标
     * 
     * 根据用户查询语句和数据元数据，通过阿里云百炼大模型
     * 智能生成新的指标配置，并保存到数据库中。
     * 
     * @param request 包含用户查询内容的请求对象
     * @return 包含生成的指标配置和AI响应的结果对象
     */
    AIChatResponse generateMetric(AIChatRequest request);

    /**
     * 提交指标评价
     * 
     * 对查询或生成的指标进行满意度评价，用于持续改进AI服务质量。
     * 
     * @param request 包含聊天历史ID和满意度评分的请求对象
     */
    void submitFeedback(MetricFeedbackRequest request);

    /**
     * 获取聊天历史
     * 
     * 分页查询用户与阿里云百炼大模型的交互历史记录。
     * 
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 包含聊天历史的分页结果
     */
    Page<AIChatHistory> getChatHistory(Integer pageNum, Integer pageSize);

    /**
     * 查找相似指标
     * 
     * 查找与查询关键词相似的现有指标。
     * 
     * @param query 查询关键词
     * @param topN 返回的最多数量
     * @return 指标列表
     */
    List<MetricConfig> findSimilarMetrics(String query, Integer topN);
}
