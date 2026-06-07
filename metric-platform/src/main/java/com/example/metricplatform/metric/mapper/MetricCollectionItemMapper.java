package com.example.metricplatform.metric.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.metric.entity.MetricCollectionItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标集合关联 Mapper 接口
 */
@Mapper
public interface MetricCollectionItemMapper extends BaseMapper<MetricCollectionItem> {
}
