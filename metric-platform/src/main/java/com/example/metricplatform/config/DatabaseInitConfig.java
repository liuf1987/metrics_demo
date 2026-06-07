package com.example.metricplatform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库初始化配置
 * 应用启动时自动创建数据库（如果不存在）
 */
@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseInitConfig {

    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @PostConstruct
    public void initDatabase() {
        log.info("开始检查并初始化数据库...");
        
        try {
            Class.forName(driverClassName);
            
            String dbName = extractDatabaseName(url);
            String serverUrl = extractServerUrl(url);
            
            log.info("数据库名称: {}", dbName);
            log.info("服务器地址: {}", serverUrl);
            
            Connection connection = DriverManager.getConnection(serverUrl, username, password);
            Statement statement = connection.createStatement();
            
            String createDbSql = "CREATE DATABASE IF NOT EXISTS `" + dbName + 
                    "` DEFAULT CHARACTER SET = `utf8mb4` COLLATE = `utf8mb4_unicode_ci`";
            
            log.info("执行SQL: {}", createDbSql);
            statement.executeUpdate(createDbSql);
            
            statement.close();
            connection.close();
            
            log.info("数据库初始化成功: {}", dbName);
            
        } catch (Exception e) {
            log.warn("数据库初始化失败（可能数据库已存在或连接失败）: {}", e.getMessage());
            log.debug("详细错误信息", e);
        }
    }

    /**
     * 从 URL 中提取数据库名称
     */
    private String extractDatabaseName(String jdbcUrl) {
        Pattern pattern = Pattern.compile("jdbc:mysql://[^/]+/([^?]+)");
        Matcher matcher = pattern.matcher(jdbcUrl);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        throw new IllegalArgumentException("无法从 URL 中提取数据库名称: " + jdbcUrl);
    }

    /**
     * 从 URL 中提取服务器地址（去掉数据库名称）
     */
    private String extractServerUrl(String jdbcUrl) {
        Pattern pattern = Pattern.compile("(jdbc:mysql://[^/]+)/");
        Matcher matcher = pattern.matcher(jdbcUrl);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        throw new IllegalArgumentException("无法从 URL 中提取服务器地址: " + jdbcUrl);
    }
}
