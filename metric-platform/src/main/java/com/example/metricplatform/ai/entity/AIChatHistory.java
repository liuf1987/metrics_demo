package com.example.metricplatform.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.metricplatform.common.enums.MetricSource;
import com.example.metricplatform.common.enums.Satisfaction;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI聊天历史实体类
 */
@Data
@TableName("ai_chat_history")
public class AIChatHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户查询
     */
    private String userQuery;

    /**
     * 来源接口
     */
    private String sourceInterface;

    /**
     * 关联的指标ID
     */
    private Long metricId;

    /**
     * 指标来源
     */
    private MetricSource metricSource;

    /**
     * 满意度
     */
    private Satisfaction satisfaction;

    /**
     * AI提示词
     */
    private String aiPrompt;

    /**
     * AI响应
     */
    private String aiResponse;

    /**
     * 输入提示词的token数量
     */
    private Long promptTokens;

    /**
     * 输出响应的token数量
     */
    private Long completionTokens;

    /**
     * 请求处理时长（毫秒）
     */
    private Long requestDurationMs;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
