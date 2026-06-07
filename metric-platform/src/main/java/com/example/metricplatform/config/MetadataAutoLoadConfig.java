package com.example.metricplatform.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.metricplatform.metadata.mapper.DataSourceMapper;
import com.example.metricplatform.metadata.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 元数据自动加载配置
 * 应用启动后自动检查 data_source 表，如果为空则加载元数据
 */
@Slf4j
@Component
public class MetadataAutoLoadConfig implements ApplicationRunner {

    private final DataSourceMapper dataSourceMapper;
    private final MetadataService metadataService;

    public MetadataAutoLoadConfig(DataSourceMapper dataSourceMapper, 
                                 MetadataService metadataService) {
        this.dataSourceMapper = dataSourceMapper;
        this.metadataService = metadataService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("检查数据源表是否为空...");
        
        try {
            // 检查 data_source 表中的数据数量
            long count = dataSourceMapper.selectCount(new LambdaQueryWrapper<>());
            
            if (count == 0) {
                log.info("数据源表为空，开始自动加载元数据...");
                metadataService.loadMetadata(null);
                log.info("元数据加载完成！");
            } else {
                log.info("数据源表已有数据（数量: {}），跳过自动加载", count);
            }
        } catch (Exception e) {
            log.error("自动加载元数据失败: {}", e.getMessage(), e);
        }
    }
}
