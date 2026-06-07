package com.example.metricplatform.metric.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.metric.entity.MetricCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标业务分类 Mapper 接口
 */
@Mapper
public interface MetricCategoryMapper extends BaseMapper<MetricCategory> {
}
