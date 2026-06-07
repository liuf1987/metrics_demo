package com.example.metricplatform.metric.service;

import com.example.metricplatform.metric.dto.MetricExecuteRequest;

import java.util.List;
import java.util.Map;

/**
 * 查询引擎服务接口
 */
public interface QueryEngineService {

    /**
     * 执行指标查询
     * @param metricCode 指标编码
     * @param request 执行请求参数
     * @return 查询结果
     */
    List<Map<String, Object>> execute(String metricCode, MetricExecuteRequest request);

    /**
     * 生成查询SQL
     * @param metricCode 指标编码
     * @param request 执行请求参数
     * @return 生成的SQL语句
     */
    String generateSql(String metricCode, MetricExecuteRequest request);
}
