package com.example.metricplatform.metric.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.metricplatform.metric.dto.MetricCreateRequest;
import com.example.metricplatform.metric.dto.MetricUpdateRequest;
import com.example.metricplatform.metric.entity.MetricConfig;

/**
 * 指标配置服务接口
 */
public interface MetricConfigService {

    /**
     * 创建指标
     */
    MetricConfig create(MetricCreateRequest request);

    /**
     * 更新指标
     */
    MetricConfig update(Long id, MetricUpdateRequest request);

    /**
     * 删除指标
     */
    void delete(Long id);

    /**
     * 根据编码查询指标详情
     */
    MetricConfig getByCode(String metricCode);

    /**
     * 根据ID查询指标详情
     */
    MetricConfig getById(Long id);

    /**
     * 分页查询指标列表
     */
    Page<MetricConfig> page(Integer pageNum, Integer pageSize, String categoryCode, Boolean enabled, String keyword);

    /**
     * 启用/停用指标
     */
    MetricConfig toggle(Long id, Boolean enabled);

    /**
     * 增加查询次数
     */
    void incrementQueryCount(String metricCode);

    /**
     * 禁用使用指定源表的所有指标
     * @param sourceTable 源表名
     * @param reason 禁用原因
     */
    void disableBySourceTable(String sourceTable, String reason);

    /**
     * 禁用使用指定字段的所有指标
     * @param fieldId 字段ID
     * @param reason 禁用原因
     */
    void disableByField(Long fieldId, String reason);
}
