package com.example.metricplatform.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.ai.entity.AIChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI聊天历史 Mapper 接口
 */
@Mapper
public interface AIChatHistoryMapper extends BaseMapper<AIChatHistory> {
}
