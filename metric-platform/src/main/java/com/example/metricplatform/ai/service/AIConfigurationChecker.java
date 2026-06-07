package com.example.metricplatform.ai.service;

/**
 * 阿里云百炼配置检查服务
 * 
 * 该服务用于检查阿里云百炼大模型的配置是否正确和可用。
 * 包括API密钥、API端点和模型名称等配置项的验证。
 */
public interface AIConfigurationChecker {
    
    /**
     * 检查阿里云百炼配置是否可用
     * 
     * 验证API密钥、API端点和模型名称等配置项是否正确，
     * 并检查ChatModel是否能够正常初始化。
     * 
     * @throws com.example.metricplatform.common.exception.BusinessException 如果配置不可用
     */
    void checkConfiguration();
    
    /**
     * 检查是否配置了阿里云百炼的API Key
     * 
     * @return true 如果配置了API Key
     */
    boolean hasApiKey();
    
    /**
     * 获取配置的阿里云百炼模型名称
     * 
     * @return 模型名称（如 qwen-turbo, qwen-plus, qwen-max 等）
     */
    String getModelName();
}
