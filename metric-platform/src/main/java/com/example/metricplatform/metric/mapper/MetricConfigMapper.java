
package com.example.metricplatform.metric.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.metric.entity.MetricConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * 指标配置 Mapper
 */
@Mapper
public interface MetricConfigMapper extends BaseMapper<MetricConfig> {
}
