package com.example.metricplatform.ai.service.impl;

import com.example.metricplatform.common.exception.BusinessException;
import com.example.metricplatform.ai.service.AIConfigurationChecker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 阿里云百炼配置检查服务实现
 *
 * 检查阿里云百炼服务的配置是否完整和有效。
 */
@Slf4j
@Service
public class AIConfigurationCheckerImpl implements AIConfigurationChecker {

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:}")
    private String model;

    private final Optional<ChatModel> chatModel;

    public AIConfigurationCheckerImpl(Optional<ChatModel> chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public void checkConfiguration() {
        log.debug("检查阿里云百炼配置: apiKey={}, model={}", 
            apiKey != null && !apiKey.isEmpty() ? "***" : "EMPTY", 
            model);

        if (!hasApiKey()) {
            throw new BusinessException("阿里云百炼服务未配置，请先在 application.yml 中配置 spring.ai.openai.api-key");
        }

        // 检查ChatModel是否可用
        if (chatModel.isEmpty()) {
            throw new BusinessException("阿里云百炼服务初始化失败，请检查配置是否正确");
        }

        log.info("阿里云百炼配置检查通过: model={}", model);
    }

    @Override
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    @Override
    public String getModelName() {
        return model;
    }
}
