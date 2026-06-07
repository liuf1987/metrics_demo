package com.example.metricplatform.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.metadata.entity.Asset;
import org.apache.ibatis.annotations.Mapper;

/**
 * 素材 Mapper 接口
 */
@Mapper
public interface AssetMapper extends BaseMapper<Asset> {
}
