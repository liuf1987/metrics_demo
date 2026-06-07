package com.example.metricplatform.metric.service;

import com.example.metricplatform.metric.entity.MetricCollection;

import java.util.List;
import java.util.Map;

/**
 * 指标集合服务接口
 */
public interface MetricCollectionService {

    /**
     * 查询所有指标集合列表
     */
    List<MetricCollection> listAll();

    /**
     * 根据编码查询集合详情（包含指标列表）
     */
    Map<String, Object> getDetailByCode(String collectionCode);

    /**
     * 根据角色查询指标集合列表
     */
    List<MetricCollection> listByRole(String targetRole);
}
