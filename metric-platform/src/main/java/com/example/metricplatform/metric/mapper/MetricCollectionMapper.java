package com.example.metricplatform.metric.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.metric.entity.MetricCollection;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标集合 Mapper 接口
 */
@Mapper
public interface MetricCollectionMapper extends BaseMapper<MetricCollection> {
}
