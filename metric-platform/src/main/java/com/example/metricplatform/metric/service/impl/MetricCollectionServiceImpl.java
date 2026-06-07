package com.example.metricplatform.metric.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.metricplatform.metric.entity.MetricCollection;
import com.example.metricplatform.metric.entity.MetricCollectionItem;
import com.example.metricplatform.metric.entity.MetricConfig;
import com.example.metricplatform.common.exception.ErrorCode;
import com.example.metricplatform.metric.mapper.MetricCollectionItemMapper;
import com.example.metricplatform.metric.mapper.MetricCollectionMapper;
import com.example.metricplatform.metric.mapper.MetricConfigMapper;
import com.example.metricplatform.metric.service.MetricCollectionService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标集合服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricCollectionServiceImpl extends ServiceImpl<MetricCollectionMapper, MetricCollection> implements MetricCollectionService {

    private final MetricCollectionItemMapper collectionItemMapper;
    private final MetricConfigMapper metricConfigMapper;

    @Override
    public List<MetricCollection> listAll() {
        return lambdaQuery()
                .eq(MetricCollection::getEnabled, true)
                .orderByAsc(MetricCollection::getCreateTime)
                .list();
    }

    @Override
    public Map<String, Object> getDetailByCode(String collectionCode) {
        MetricCollection collection = lambdaQuery()
                .eq(MetricCollection::getCollectionCode, collectionCode)
                .one();
        
        if (collection == null) {
            log.warn("指标集合不存在，collectionCode: {}", collectionCode);
            throw ErrorCode.COLLECTION_NOT_FOUND_BY_CODE.exception(collectionCode);
        }

        // 查询集合中的指标项
        LambdaQueryWrapper<MetricCollectionItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MetricCollectionItem::getCollectionId, collection.getId())
                .orderByAsc(MetricCollectionItem::getSortOrder);
        List<MetricCollectionItem> items = collectionItemMapper.selectList(itemWrapper);

        // 查询指标详情
        List<MetricConfig> metrics = new ArrayList<>();
        for (MetricCollectionItem item : items) {
            MetricConfig metric = metricConfigMapper.selectById(item.getMetricId());
            if (metric != null) {
                metrics.add(metric);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("collection", collection);
        result.put("metrics", metrics);
        
        return result;
    }

    @Override
    public List<MetricCollection> listByRole(String targetRole) {
        return lambdaQuery()
                .eq(MetricCollection::getEnabled, true)
                .eq(MetricCollection::getTargetRole, targetRole)
                .orderByAsc(MetricCollection::getCreateTime)
                .list();
    }
}
