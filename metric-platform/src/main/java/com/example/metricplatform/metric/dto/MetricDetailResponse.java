package com.example.metricplatform.metric.dto;

import com.example.metricplatform.metric.entity.MetricConfig;
import com.example.metricplatform.metric.entity.MetricStatItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 指标详情响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricDetailResponse {

    /**
     * 指标配置
     */
    private MetricConfig metricConfig;

    /**
     * 统计项列表
     */
    private List<MetricStatItem> statItems;
}
