package com.example.metricplatform.common.exception;

/**
 * 全局异常码枚举
 * 
 * 异常码分类规则：
 * - 1000-1999: 通用错误
 * - 2000-2999: 元数据管理模块
 * - 3000-3999: 指标管理模块
 * - 4000-4999: 查询引擎模块
 * - 5000-5999: 任务管理模块
 * - 6000-6999: 指标集合模块
 * 
 * HTTP状态码对应：
 * - 400: 参数错误、业务校验失败（1000-1099, 2000-2099, 3000-3099, 4000-4099等）
 * - 404: 资源不存在（1100-1199, 2100-2199, 3100-3199等）
 * - 409: 资源冲突（1200-1299, 3200-3299等）
 * - 500: 服务器内部错误（1900-1999, 4900-4999等）
 */
public enum ErrorCode {
    
    // ==================== 通用错误 (1000-1999) ====================
    SUCCESS(200, "操作成功"),
    VALIDATION_ERROR(1001, "参数校验失败"),
    NULL_PARAMETER(1002, "必填参数为空"),
    INVALID_PARAMETER(1003, "参数格式不正确"),
    INVALID_JSON_FORMAT(1004, "JSON格式错误"),
    DATA_NOT_FOUND(1101, "数据不存在"),
    SYSTEM_ERROR(1901, "系统错误"),
    DATABASE_ERROR(1902, "数据库操作失败"),
    SQL_EXECUTE_ERROR(1903, "SQL执行失败"),

    PARAM_CODE_EXISTS(3001, "指标编码已存在"),
    PARAM_SOURCE_TABLE_EMPTY(3002, "源表名不能为空"),
    PARAM_DATA_SOURCE_NOT_FOUND(3003, "数据源不存在"),
    PARAM_DATA_SOURCE_DISABLED(3004, "数据源已禁用"),
    PARAM_FIELD_NOT_FOUND(3005, "字段不存在"),
    PARAM_FIELD_DISABLED(3006, "字段已禁用"),
    PARAM_STAT_FIELD_NOT_FOUND(3007, "统计字段不存在"),
    PARAM_STAT_FIELD_DISABLED(3008, "统计字段已禁用"),
    PARAM_FIELD_NOT_SUPPORT_AGGREGATION(3009, "字段不支持聚合操作"),
    PARAM_DISABLED(3010, "指标已停用"),
    PARAM_FIELD_TYPE_NOT_SUPPORT_AGGREGATION(3011, "字段类型不支持聚合函数"),
    PARAM_CONFIG_ERROR(3012, "指标配置错误: %s"),
    PARAM_CONFIG_MISSING_FIELD(3013, "配置缺少必要字段: %s"),
    PARAM_CONFIG_INVALID_FIELD(3014, "配置字段值无效: %s"),
    METRIC_NOT_FOUND(3102, "指标不存在: %s"),
    QUERY_GROUP_FIELD_NOT_FOUND(4006, "分组字段不存在"),
    QUERY_GROUP_FIELD_DISABLED(4007, "分组字段已禁用"),
    QUERY_GROUP_FIELD_NOT_ALLOWED(4008, "字段分组功能已被禁用"),
    QUERY_FILTER_FIELD_NOT_FOUND(4009, "过滤字段不存在"),
    QUERY_FILTER_FIELD_DISABLED(4010, "过滤字段已禁用"),
    QUERY_FILTER_FIELD_NOT_ALLOWED(4011, "字段过滤功能已被禁用"),
    QUERY_SORT_FIELD_DISABLED(4012, "排序字段已禁用"),
    QUERY_SORT_FIELD_NOT_ALLOWED(4013, "字段排序功能已被禁用"),
    QUERY_WINDOW_FIELD_NOT_FOUND(4014, "窗口函数字段不存在"),
    QUERY_WINDOW_FIELD_DISABLED(4015, "窗口函数字段已禁用"),
    QUERY_PARAMETER_NOT_FOUND(4016, "缺少必要的查询参数"),
    QUERY_PARAMETER_INVALID(4017, "查询参数格式不正确"),
    QUERY_EXECUTE_ERROR(4901, "查询执行失败"),
    QUERY_GENERATE_ERROR(4902, "SQL生成失败"),
    
    // ==================== 任务管理模块 (5000-5999) ====================
    TASK_NOT_FOUND(5101, "任务不存在"),
    TASK_NOT_FOUND_BY_ID(5102, "任务不存在: %d"),
    TASK_NOT_FOUND_BY_CODE(5103, "任务不存在: %s"),

    TASK_STATUS_INVALID(5001, "任务状态无效"),
    TASK_EXECUTE_FAILED(5002, "任务执行失败"),
    TASK_CRON_REQUIRED(5003, "定时任务必须配置Cron表达式"),
    
    // ==================== 指标集合模块 (6000-6999) ====================
    COLLECTION_NOT_FOUND(6101, "指标集合不存在"),
    COLLECTION_NOT_FOUND_BY_CODE(6102, "指标集合不存在: %s"),

    COLLECTION_CODE_EXISTS(6001, "集合编码已存在"),
    COLLECTION_METRIC_NOT_FOUND(6002, "集合中的指标不存在"),
    ;
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取带参数的错误消息
     * @param args 参数列表
     * @return 格式化后的错误消息
     */
    public String getMessage(Object... args) {
        if (args == null || args.length == 0) {
            return message;
        }
        return String.format(message, args);
    }
    
    /**
     * 创建业务异常
     * @return BusinessException实例
     */
    public BusinessException exception() {
        return new BusinessException(code, message);
    }
    
    /**
     * 创建带参数的业务异常
     * @param args 参数列表
     * @return BusinessException实例
     */
    public BusinessException exception(Object... args) {
        return new BusinessException(code, getMessage(args));
    }
}