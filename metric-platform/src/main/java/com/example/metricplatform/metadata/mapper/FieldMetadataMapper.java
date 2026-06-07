package com.example.metricplatform.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.metadata.entity.FieldMetadata;
import org.apache.ibatis.annotations.Mapper;

/**
 * 字段元数据 Mapper 接口
 */
@Mapper
public interface FieldMetadataMapper extends BaseMapper<FieldMetadata> {
}
