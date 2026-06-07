package com.example.metricplatform.metric.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指标分页响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricPageResponse {

    /**
     * 分页数据
     */
    private Page<MetricDetailResponse> page;
}
