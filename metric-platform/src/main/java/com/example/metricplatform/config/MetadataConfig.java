package com.example.metricplatform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 元数据配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "metadata")
public class MetadataConfig {

    /**
     * 需要排除的表名称清单（不参与指标查询的后台业务表）
     */
    private List<String> excludeTables = new ArrayList<>();

    /**
     * 获取排除表的集合（转换为小写用于不区分大小写比较）
     */
    public Set<String> getExcludeTablesSet() {
        return excludeTables.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
     * 检查指定表是否应该被排除
     */
    public boolean shouldExclude(String tableName) {
        if (tableName == null) {
            return true;
        }
        return getExcludeTablesSet().contains(tableName.toLowerCase());
    }
}