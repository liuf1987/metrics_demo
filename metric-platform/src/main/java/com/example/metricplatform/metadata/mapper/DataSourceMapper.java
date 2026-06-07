package com.example.metricplatform.metadata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.metricplatform.metadata.entity.DataSource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源配置 Mapper 接口
 */
@Mapper
public interface DataSourceMapper extends BaseMapper<DataSource> {
}
