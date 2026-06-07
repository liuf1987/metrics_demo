package com.example.metricplatform.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.ai.entity.AIRecommendation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI推荐记录 Mapper 接口
 */
@Mapper
public interface AIRecommendationMapper extends BaseMapper<AIRecommendation> {
}
