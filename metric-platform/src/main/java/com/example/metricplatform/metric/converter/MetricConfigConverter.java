
package com.example.metricplatform.metric.converter;

import com.example.metricplatform.metric.dto.MetricCreateRequest;
import com.example.metricplatform.metric.dto.MetricStatItemRequest;
import com.example.metricplatform.metric.dto.MetricUpdateRequest;
import com.example.metricplatform.metric.entity.MetricConfig;
import com.example.metricplatform.metric.entity.MetricStatItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * 指标配置转换器
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MetricConfigConverter {

    /**
     * 从创建请求映射到实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "queryCount", constant = "0")
    @Mapping(target = "disabledReason", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    MetricConfig toEntity(MetricCreateRequest request);

    /**
     * 统计项请求转实体
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "metricId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    MetricStatItem toStatItemEntity(MetricStatItemRequest request);

    /**
     * 统计项请求列表转实体列表
     */
    List<MetricStatItem> toStatItemEntityList(List<MetricStatItemRequest> requests);

    /**
     * 更新实体（仅更新非空字段）
     */
    void updateFromRequest(MetricUpdateRequest request, @MappingTarget MetricConfig entity);
}
