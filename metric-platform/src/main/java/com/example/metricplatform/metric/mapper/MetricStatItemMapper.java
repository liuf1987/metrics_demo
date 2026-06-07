package com.example.metricplatform.metric.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.metric.entity.MetricStatItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标统计项 Mapper 接口
 */
@Mapper
public interface MetricStatItemMapper extends BaseMapper<MetricStatItem> {
}
