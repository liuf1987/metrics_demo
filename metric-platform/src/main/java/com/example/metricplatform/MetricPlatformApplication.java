package com.example.metricplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据指标平台主启动类
 */
@SpringBootApplication
@MapperScan("com.example.metricplatform.*.mapper")
@EnableScheduling
public class MetricPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricPlatformApplication.class, args);
    }

}
