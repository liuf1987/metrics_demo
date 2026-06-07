package com.example.metricplatform.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class JsonResponseParser {

    private final ObjectMapper objectMapper;

    public <T> T parse(String response, Class<T> valueType) {
        String jsonContent = extractJson(response);
        try {
            log.debug("解析JSON内容: {}", jsonContent);
            return objectMapper.readValue(jsonContent, valueType);
        } catch (Exception e) {
            log.error("解析JSON失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析AI返回的JSON失败: " + e.getMessage());
        }
    }

    private String extractJson(String response) {
        if (response == null || response.trim().isEmpty()) {
            throw new RuntimeException("AI返回的内容为空");
        }

        String content = response.trim();
        
        // 情况1: 已经是纯JSON
        if (content.startsWith("{") || content.startsWith("[")) {
            return extractCompleteJson(content);
        }
        
        // 情况2: 包含Markdown代码块
        if (content.contains("```json")) {
            return extractFromMarkdownCodeBlock(content, "```json", "```");
        }
        if (content.contains("```")) {
            return extractFromMarkdownCodeBlock(content, "```", "```");
        }
        
        // 情况3: 尝试查找第一个{或[
        int startIndex = Math.min(
            content.indexOf('{') >= 0 ? content.indexOf('{') : Integer.MAX_VALUE,
            content.indexOf('[') >= 0 ? content.indexOf('[') : Integer.MAX_VALUE
        );
        
        if (startIndex != Integer.MAX_VALUE) {
            String remaining = content.substring(startIndex);
            return extractCompleteJson(remaining);
        }
        
        throw new RuntimeException("无法从AI返回中提取JSON");
    }

    private String extractFromMarkdownCodeBlock(String content, String startMarker, String endMarker) {
        int start = content.indexOf(startMarker);
        if (start == -1) {
            throw new RuntimeException("找不到Markdown代码块开始标记");
        }
        
        int contentStart = start + startMarker.length();
        int end = content.indexOf(endMarker, contentStart);
        if (end == -1) {
            // 尝试找最后一个结束标记
            end = content.lastIndexOf(endMarker);
        }
        
        if (end == -1 || end <= contentStart) {
            throw new RuntimeException("找不到Markdown代码块结束标记");
        }
        
        String json = content.substring(contentStart, end).trim();
        log.debug("从Markdown代码块中提取到JSON: {}", json);
        return json;
    }

    private String extractCompleteJson(String content) {
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
            String json = content.substring(startIndex, endIndex).trim();
            log.debug("提取到完整JSON: {}", json);
            return json;
        }
        
        throw new RuntimeException("无法提取完整的JSON结构");
    }
}
