-- =============================================
-- 指标配置后台服务 - 数据库初始化脚本
-- Flyway Migration V1
-- =============================================

-- =============================================
-- 1. 素材明细数据表
-- =============================================
CREATE TABLE IF NOT EXISTS asset (
    asset_id             VARCHAR(50) PRIMARY KEY COMMENT '素材ID',
    title                VARCHAR(255) NOT NULL COMMENT '素材标题',
    uploader             VARCHAR(100) NOT NULL COMMENT '上传人',
    uploaded_at          DATETIME NOT NULL COMMENT '上传时间',
    file_size_bytes      BIGINT NOT NULL COMMENT '文件大小(字节)',
    status               ENUM('approved','rejected','pending') NOT NULL COMMENT '审核状态',
    reviewer             VARCHAR(100) COMMENT '审核人',
    reviewed_at          DATETIME COMMENT '审核时间',
    tags                 JSON COMMENT '标签数组，如 ["活动","短视频"]',
    city                 VARCHAR(50) COMMENT '城市',
    platform             VARCHAR(20) COMMENT '投放平台：抖音/快手/小红书',
    duration_seconds     INT COMMENT '视频时长(秒)',
    impression_count     BIGINT DEFAULT 0 COMMENT '曝光次数',
    play_count           BIGINT DEFAULT 0 COMMENT '播放次数',
    complete_play_count  BIGINT DEFAULT 0 COMMENT '完播次数',

    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',

    INDEX idx_status (status),
    INDEX idx_uploader (uploader),
    INDEX idx_reviewer (reviewer),
    INDEX idx_city (city),
    INDEX idx_platform (platform),
    INDEX idx_uploaded_at (uploaded_at),
    INDEX idx_reviewed_at (reviewed_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='素材明细表';

-- =============================================
-- 2. 指标配置表
-- =============================================
CREATE TABLE IF NOT EXISTS metric_config (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_code          VARCHAR(100) UNIQUE NOT NULL COMMENT '指标编码，程序引用',
    metric_name          VARCHAR(200) NOT NULL COMMENT '指标名称',
    metric_desc          TEXT COMMENT '指标描述',
    source_table         VARCHAR(100) NOT NULL COMMENT '来源表名',
    category_code        VARCHAR(50) COMMENT '业务分类编码',
    category_name        VARCHAR(100) COMMENT '业务分类名称',
    group_by_fields      JSON COMMENT '分组维度数组',
    filter_conditions    JSON COMMENT '筛选条件，支持动态参数占位符',
    having_conditions    JSON COMMENT 'HAVING条件',
    sort_rules           JSON COMMENT '排序规则',
    limit_value          INT DEFAULT NULL COMMENT 'LIMIT值',
    window_functions     JSON COMMENT '窗口函数定义',
    query_count          INT DEFAULT 0 COMMENT '查询次数统计',
    enabled              BOOLEAN DEFAULT TRUE COMMENT '启用/停用',
    disabled_reason      VARCHAR(500) COMMENT '禁用原因',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    INDEX idx_enabled (enabled),
    INDEX idx_metric_code (metric_code),
    INDEX idx_category_code (category_code),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标配置表';

-- =============================================
-- 3. 查询/导出任务表
-- =============================================
CREATE TABLE IF NOT EXISTS task (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_code            VARCHAR(100) UNIQUE NOT NULL COMMENT '任务编码',
    task_name            VARCHAR(200) COMMENT '任务名称',
    task_desc            VARCHAR(500) COMMENT '任务描述',
    task_type            ENUM('query','export') NOT NULL COMMENT '任务类型',
    execute_mode         VARCHAR(20) DEFAULT 'instant' COMMENT '执行模式：instant-即时执行，scheduled-定时执行',
    cron_expression      VARCHAR(100) COMMENT 'Cron表达式（定时任务使用）',
    next_run_time        DATETIME COMMENT '下次执行时间（定时任务使用）',
    scheduler_id         VARCHAR(100) COMMENT '定时任务调度ID',
    metric_configs       JSON COMMENT '指标查询配置列表（支持多个指标）',
    status               ENUM('pending','running','paused','success','failed','cancelled') DEFAULT 'pending' COMMENT '任务状态',
    result_data          LONGTEXT COMMENT '查询结果(JSON或文件路径)',
    error_message        TEXT COMMENT '失败原因',
    retry_count          INT DEFAULT 0 COMMENT '重试次数',
    started_at           DATETIME COMMENT '开始时间',
    finished_at          DATETIME COMMENT '结束时间',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    INDEX idx_task_code (task_code),
    INDEX idx_status (status),
    INDEX idx_execute_mode (execute_mode),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务表';

-- =============================================
-- 4. 任务子指标表
-- =============================================
CREATE TABLE IF NOT EXISTS task_metric_item (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id              BIGINT NOT NULL COMMENT '关联的任务ID',
    metric_code          VARCHAR(100) NOT NULL COMMENT '指标编码',
    metric_name          VARCHAR(200) NOT NULL COMMENT '指标名称',
    sort_order           INT DEFAULT 0 COMMENT '排序顺序',
    
    -- 执行状态字段
    status               ENUM('pending','running','success','failed') DEFAULT 'pending' COMMENT '执行状态',
    result_data          LONGTEXT COMMENT '执行结果(JSON格式)',
    error_message        TEXT COMMENT '失败原因',
    started_at           DATETIME COMMENT '开始执行时间',
    finished_at          DATETIME COMMENT '执行结束时间',
    execute_duration     INT COMMENT '执行耗时(毫秒)',
    
    -- 元数据字段
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    -- 索引
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    INDEX idx_task_id (task_id),
    INDEX idx_metric_code (metric_code),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),
    UNIQUE KEY uk_task_metric (task_id, metric_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务子指标表';

-- =============================================
-- 5. 任务执行结果表
-- =============================================
CREATE TABLE IF NOT EXISTS task_execution_result (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id              BIGINT NOT NULL COMMENT '关联的任务ID',
    task_code            VARCHAR(100) NOT NULL COMMENT '任务编码',
    execution_id         VARCHAR(100) NOT NULL COMMENT '本次执行ID，用于唯一标识一次执行',
    task_type            ENUM('query','export') NOT NULL COMMENT '任务类型',
    execute_mode         VARCHAR(20) DEFAULT 'instant' COMMENT '执行模式：instant-即时执行，scheduled-定时执行',
    status               ENUM('pending','running','success','failed','cancelled') DEFAULT 'pending' COMMENT '执行状态',
    result_data          LONGTEXT COMMENT '执行结果数据(JSON格式)',
    error_message        TEXT COMMENT '错误信息',
    execution_start      DATETIME COMMENT '执行开始时间',
    execution_end        DATETIME COMMENT '执行结束时间',
    execution_duration   INT COMMENT '执行耗时(毫秒)',
    retry_count          INT DEFAULT 0 COMMENT '本次执行时的重试次数',
    
    -- 元数据字段
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    -- 索引
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    INDEX idx_task_id (task_id),
    INDEX idx_task_code (task_code),
    INDEX idx_execution_id (execution_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行结果表';

-- =============================================
-- 6. 数据源配置表（元数据管理）
-- =============================================
CREATE TABLE IF NOT EXISTS data_source (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_code          VARCHAR(100) UNIQUE NOT NULL COMMENT '数据源编码',
    source_name          VARCHAR(200) NOT NULL COMMENT '数据源名称',
    source_type          ENUM('table','view','query') NOT NULL COMMENT '类型',
    source_expr          VARCHAR(500) NOT NULL COMMENT '表名或SQL视图',
    description          TEXT COMMENT '描述',
    enabled              BOOLEAN DEFAULT TRUE COMMENT '启用状态',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    INDEX idx_enabled (enabled),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源配置表';

-- =============================================
-- 7. 字段元数据表（元数据管理）
-- =============================================
CREATE TABLE IF NOT EXISTS field_metadata (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_id            BIGINT NOT NULL COMMENT '关联 data_source.id',
    field_name           VARCHAR(100) NOT NULL COMMENT '字段名',
    field_type           ENUM('string','number','datetime','json','array') NOT NULL COMMENT '字段类型',
    field_label          VARCHAR(200) COMMENT '显示名称',
    filterable           BOOLEAN DEFAULT FALSE COMMENT '是否可筛选',
    groupable            BOOLEAN DEFAULT FALSE COMMENT '是否可分组',
    aggregatable         BOOLEAN DEFAULT FALSE COMMENT '是否可聚合',
    sortable             BOOLEAN DEFAULT FALSE COMMENT '是否可排序',
    enabled              BOOLEAN DEFAULT TRUE COMMENT '启用状态',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    FOREIGN KEY (source_id) REFERENCES data_source(id),
    UNIQUE KEY uk_source_field (source_id, field_name),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='字段元数据表';

-- =============================================
-- 8. 指标统计项表（多字段统计支持）
-- =============================================
CREATE TABLE IF NOT EXISTS metric_stat_item (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    metric_id            BIGINT NOT NULL COMMENT '关联 metric_config.id',
    item_code            VARCHAR(100) NOT NULL COMMENT '统计项编码',
    item_name            VARCHAR(200) NOT NULL COMMENT '统计项名称',
    field_name           VARCHAR(100) NOT NULL COMMENT '统计字段',
    aggregation          ENUM('count','sum','avg','max','min') NOT NULL COMMENT '聚合方式',
    sort_order           INT DEFAULT 0 COMMENT '显示顺序',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    FOREIGN KEY (metric_id) REFERENCES metric_config(id),
    UNIQUE KEY uk_metric_item (metric_id, item_code),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标统计项表';

-- =============================================
-- 9. 指标业务分类表
-- =============================================
CREATE TABLE IF NOT EXISTS metric_category (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_code        VARCHAR(50) UNIQUE NOT NULL COMMENT '分类编码',
    category_name        VARCHAR(100) NOT NULL COMMENT '分类名称',
    description          TEXT COMMENT '分类描述',
    sort_order           INT DEFAULT 0 COMMENT '排序顺序',
    enabled              BOOLEAN DEFAULT TRUE COMMENT '启用状态',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    INDEX idx_enabled (enabled),
    INDEX idx_sort_order (sort_order),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标业务分类表';

-- =============================================
-- 10. 指标集合表（按业务人员类型组织指标）
-- =============================================
CREATE TABLE IF NOT EXISTS metric_collection (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_code      VARCHAR(100) UNIQUE NOT NULL COMMENT '集合编码',
    collection_name      VARCHAR(200) NOT NULL COMMENT '集合名称',
    description          TEXT COMMENT '集合描述',
    target_role          VARCHAR(100) COMMENT '目标角色/部门',
    enabled              BOOLEAN DEFAULT TRUE COMMENT '启用状态',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    INDEX idx_enabled (enabled),
    INDEX idx_target_role (target_role),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标集合表';

-- =============================================
-- 11. 指标集合关联表
-- =============================================
CREATE TABLE IF NOT EXISTS metric_collection_item (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_id        BIGINT NOT NULL COMMENT '关联 metric_collection.id',
    metric_id            BIGINT NOT NULL COMMENT '关联 metric_config.id',
    sort_order           INT DEFAULT 0 COMMENT '排序顺序',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    FOREIGN KEY (collection_id) REFERENCES metric_collection(id),
    FOREIGN KEY (metric_id) REFERENCES metric_config(id),
    UNIQUE KEY uk_collection_metric (collection_id, metric_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标集合关联表';

-- =============================================
-- 12. AI推荐记录表
-- =============================================
CREATE TABLE IF NOT EXISTS ai_recommendation (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_query           TEXT COMMENT '用户查询语句',
    recommended_metrics  JSON COMMENT '推荐的指标编码列表',
    new_metric_suggest   TEXT COMMENT '新指标生成建议',
    generated_metric_config JSON COMMENT '生成的指标配置',
    validation_result    TEXT COMMENT '验证结果',
    status               VARCHAR(50) COMMENT '状态',
    error_message        TEXT COMMENT '错误信息',
    confidence           DECIMAL(5,2) COMMENT '推荐置信度',
    
    create_time          DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted              TINYINT DEFAULT 0 COMMENT '逻辑删除标识',
    
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI推荐记录表';

-- =============================================
-- 13. AI聊天历史表
-- =============================================
CREATE TABLE IF NOT EXISTS ai_chat_history (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_query           TEXT NOT NULL COMMENT '用户查询',
    source_interface     VARCHAR(100) COMMENT '来源接口',
    metric_id            BIGINT COMMENT '关联的指标ID',
    metric_source        ENUM('EXISTING', 'GENERATED') COMMENT '指标来源：现有指标/生成指标',
    satisfaction         ENUM('SATISFIED', 'DISSATISFIED') COMMENT '满意度：满意/不满意',
    ai_prompt            TEXT COMMENT 'AI提示词',
    prompt_tokens        BIGINT COMMENT '输入提示词的token数量',
    ai_response          TEXT COMMENT 'AI响应',
    completion_tokens    BIGINT COMMENT '输出响应的token数量',
    request_duration_ms  BIGINT COMMENT '请求处理时长（毫秒）',
    created_at           DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_metric_id (metric_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天历史表';

-- =============================================
-- 初始化数据
-- =============================================

-- 1. 初始化素材数据（30条）
INSERT INTO asset (
    asset_id, title, uploader, uploaded_at, file_size_bytes, status, reviewer, reviewed_at, tags, city, platform, duration_seconds,
    impression_count, play_count, complete_play_count
) VALUES
('A10001', '春季促销活动 - 抖音版', '张三', '2026-05-01 10:00:00', 104857600, 'approved', '审核员A', '2026-05-01 14:00:00', '["活动","短视频"]', '上海', '抖音', 30, 50000, 12000, 6000),
('A10002', '产品介绍 - 快手版', '李四', '2026-05-02 11:00:00', 78643200, 'approved', '审核员B', '2026-05-02 15:30:00', '["产品","介绍"]', '北京', '快手', 45, 35000, 8500, 4200),
('A10003', '用户使用教程', '张三', '2026-05-03 09:00:00', 157286400, 'pending', NULL, NULL, '["教程","教学"]', '广州', '小红书', 60, 0, 0, 0),
('A10004', '618大促预热视频', '王五', '2026-05-04 14:00:00', 125829120, 'approved', '审核员A', '2026-05-04 18:00:00', '["活动","大促"]', '深圳', '抖音', 35, 80000, 25000, 12000),
('A10005', '品牌故事短片', '赵六', '2026-05-05 16:00:00', 209715200, 'approved', '审核员C', '2026-05-06 10:00:00', '["品牌","故事"]', '上海', '小红书', 120, 45000, 11000, 5500),
('A10006', '产品测评 - 博主合作', '李四', '2026-05-06 08:00:00', 188743680, 'rejected', '审核员B', '2026-05-06 12:00:00', '["测评","合作"]', '北京', '抖音', 90, 0, 0, 0),
('A10007', '新品发布预告', '张三', '2026-05-07 15:00:00', 94371840, 'approved', '审核员A', '2026-05-07 19:00:00', '["新品","预告"]', '成都', '快手', 25, 60000, 18000, 9000),
('A10008', '双11回顾视频', '王五', '2026-05-08 10:00:00', 262144000, 'approved', '审核员C', '2026-05-08 14:00:00', '["回顾","大促"]', '杭州', '小红书', 80, 75000, 22000, 11000),
('A10009', '用户真实反馈', '赵六', '2026-05-09 12:00:00', 115343360, 'pending', NULL, NULL, '["用户","反馈"]', '上海', '抖音', 40, 0, 0, 0),
('A10010', '产品功能演示', '李四', '2026-05-10 14:00:00', 171966464, 'approved', '审核员B', '2026-05-10 18:30:00', '["功能","演示"]', '北京', '快手', 70, 40000, 9500, 4700),
('A10011', '春节营销方案', '张三', '2026-05-11 09:00:00', 141557760, 'approved', '审核员A', '2026-05-11 13:00:00', '["春节","营销"]', '广州', '小红书', 55, 55000, 13500, 6700),
('A10012', '明星合作宣传片', '王五', '2026-05-12 16:00:00', 314572800, 'approved', '审核员C', '2026-05-12 20:00:00', '["明星","合作"]', '深圳', '抖音', 45, 120000, 40000, 20000),
('A10013', '日常产品分享', '赵六', '2026-05-13 11:00:00', 62914560, 'pending', NULL, NULL, '["日常","分享"]', '成都', '快手', 20, 0, 0, 0),
('A10014', '劳动节特别策划', '李四', '2026-05-14 13:00:00', 125829120, 'approved', '审核员B', '2026-05-14 17:00:00', '["劳动节","策划"]', '杭州', '小红书', 50, 38000, 9000, 4500),
('A10015', '产品细节展示', '张三', '2026-05-15 08:00:00', 94371840, 'approved', '审核员A', '2026-05-15 12:00:00', '["细节","展示"]', '上海', '抖音', 35, 42000, 10500, 5200),
('A10016', '粉丝问答视频', '王五', '2026-05-16 14:00:00', 209715200, 'rejected', '审核员C', '2026-05-16 18:00:00', '["问答","互动"]', '北京', '快手', 85, 0, 0, 0),
('A10017', '新品开箱体验', '赵六', '2026-05-17 10:00:00', 171966464, 'approved', '审核员A', '2026-05-17 14:00:00', '["开箱","体验"]', '广州', '小红书', 60, 70000, 21000, 10500),
('A10018', '品牌年度回顾', '李四', '2026-05-18 12:00:00', 262144000, 'approved', '审核员B', '2026-05-18 16:30:00', '["回顾","年度"]', '深圳', '抖音', 100, 90000, 28000, 14000),
('A10019', '产品使用技巧', '张三', '2026-05-19 15:00:00', 115343360, 'pending', NULL, NULL, '["技巧","使用"]', '成都', '快手', 45, 0, 0, 0),
('A10020', '圣诞节主题视频', '王五', '2026-05-20 09:00:00', 188743680, 'approved', '审核员C', '2026-05-20 13:00:00', '["圣诞","主题"]', '杭州', '小红书', 55, 52000, 12500, 6200),
('A10021', '产品对比评测', '赵六', '2026-05-21 11:00:00', 230686720, 'approved', '审核员A', '2026-05-21 15:00:00', '["对比","评测"]', '上海', '抖音', 75, 48000, 11500, 5700),
('A10022', '员工风采展示', '李四', '2026-05-22 14:00:00', 141557760, 'approved', '审核员B', '2026-05-22 18:00:00', '["员工","风采"]', '北京', '快手', 35, 25000, 6000, 3000),
('A10023', '新功能上线预告', '张三', '2026-05-23 16:00:00', 83886080, 'pending', NULL, NULL, '["新功能","预告"]', '广州', '小红书', 25, 0, 0, 0),
('A10024', '双十一购物攻略', '王五', '2026-05-24 10:00:00', 209715200, 'approved', '审核员C', '2026-05-24 14:00:00', '["双十一","攻略"]', '深圳', '抖音', 65, 85000, 26000, 13000),
('A10025', '用户成功案例', '赵六', '2026-05-25 12:00:00', 171966464, 'approved', '审核员A', '2026-05-25 16:00:00', '["案例","成功"]', '成都', '快手', 55, 35000, 8500, 4200),
('A10026', '节日祝福视频', '李四', '2026-05-26 08:00:00', 62914560, 'approved', '审核员B', '2026-05-26 12:00:00', '["节日","祝福"]', '杭州', '小红书', 20, 30000, 7200, 3600),
('A10027', '产品迭代说明', '张三', '2026-05-27 14:00:00', 125829120, 'rejected', '审核员C', '2026-05-27 18:00:00', '["迭代","说明"]', '上海', '抖音', 40, 0, 0, 0),
('A10028', '年终促销预告', '王五', '2026-05-28 11:00:00', 188743680, 'approved', '审核员A', '2026-05-28 15:00:00', '["年终","促销"]', '北京', '快手', 50, 65000, 19500, 9700),
('A10029', '品牌升级宣传片', '赵六', '2026-05-29 13:00:00', 262144000, 'approved', '审核员B', '2026-05-29 17:00:00', '["升级","品牌"]', '广州', '小红书', 70, 80000, 24000, 12000),
('A10030', '产品安装教程', '李四', '2026-05-30 09:00:00', 157286400, 'approved', '审核员C', '2026-05-30 13:00:00', '["安装","教程"]', '深圳', '抖音', 80, 42000, 10000, 5000),
('A10031', '新年祝福视频', '张三', '2026-01-02 10:00:00', 94371840, 'approved', '审核员A', '2026-01-02 14:00:00', '["节日","祝福"]', '上海', '抖音', 30, 45000, 11000, 5500),
('A10032', '新品发布会预告', '李四', '2026-01-05 11:00:00', 125829120, 'pending', NULL, NULL, '["新品","预告"]', '北京', '快手', 45, 0, 0, 0),
('A10033', '产品功能详解', '王五', '2026-01-08 09:00:00', 188743680, 'approved', '审核员B', '2026-01-08 13:00:00', '["功能","讲解"]', '广州', '小红书', 60, 52000, 12500, 6200),
('A10034', '用户使用分享', '赵六', '2026-01-12 14:00:00', 78643200, 'approved', '审核员C', '2026-01-12 18:00:00', '["用户","分享"]', '深圳', '抖音', 35, 38000, 9000, 4500),
('A10035', '品牌宣传片', '张三', '2026-01-15 16:00:00', 262144000, 'approved', '审核员A', '2026-01-15 20:00:00', '["品牌","宣传"]', '成都', '快手', 90, 85000, 26000, 13000),
('A10036', '产品测试视频', '李四', '2026-01-18 08:00:00', 104857600, 'rejected', '审核员B', '2026-01-18 12:00:00', '["测试","产品"]', '杭州', '小红书', 50, 0, 0, 0),
('A10037', '春节活动预热', '王五', '2026-01-22 10:00:00', 141557760, 'approved', '审核员C', '2026-01-22 14:00:00', '["春节","活动"]', '上海', '抖音', 40, 60000, 18000, 9000),
('A10038', '员工培训课程', '赵六', '2026-01-25 12:00:00', 314572800, 'pending', NULL, NULL, '["培训","课程"]', '北京', '快手', 75, 0, 0, 0),
('A10039', '客户成功故事', '张三', '2026-01-28 15:00:00', 171966464, 'approved', '审核员A', '2026-01-28 19:00:00', '["客户","案例"]', '广州', '小红书', 55, 48000, 11500, 5700),
('A10040', '产品对比视频', '李四', '2026-02-02 11:00:00', 209715200, 'approved', '审核员B', '2026-02-02 15:00:00', '["对比","评测"]', '深圳', '抖音', 65, 70000, 21000, 10500),
('A10041', '品牌故事视频', '王五', '2026-02-05 09:00:00', 115343360, 'pending', NULL, NULL, '["品牌","故事"]', '成都', '快手', 30, 0, 0, 0),
('A10042', '产品操作指南', '赵六', '2026-02-08 14:00:00', 157286400, 'approved', '审核员C', '2026-02-08 18:00:00', '["指南","操作"]', '杭州', '小红书', 70, 55000, 13500, 6700),
('A10043', '元宵节特别节目', '张三', '2026-02-12 10:00:00', 230686720, 'approved', '审核员A', '2026-02-12 14:00:00', '["节日","元宵"]', '上海', '抖音', 85, 90000, 28000, 14000),
('A10044', '新品开箱视频', '李四', '2026-02-15 12:00:00', 83886080, 'rejected', '审核员B', '2026-02-15 16:00:00', '["开箱","新品"]', '北京', '快手', 25, 0, 0, 0),
('A10045', '产品升级说明', '王五', '2026-02-18 15:00:00', 188743680, 'approved', '审核员C', '2026-02-18 19:00:00', '["升级","说明"]', '广州', '小红书', 45, 42000, 10500, 5200),
('A10046', '用户教程系列', '赵六', '2026-02-22 08:00:00', 262144000, 'pending', NULL, NULL, '["教程","系列"]', '深圳', '抖音', 100, 0, 0, 0),
('A10047', '品牌推广视频', '张三', '2026-02-25 11:00:00', 125829120, 'approved', '审核员A', '2026-02-25 15:00:00', '["推广","品牌"]', '成都', '快手', 55, 65000, 19500, 9700),
('A10048', '产品使用技巧', '李四', '2026-03-02 13:00:00', 94371840, 'approved', '审核员B', '2026-03-02 17:00:00', '["技巧","使用"]', '杭州', '小红书', 35, 25000, 6000, 3000),
('A10049', '女神节活动', '王五', '2026-03-05 09:00:00', 141557760, 'pending', NULL, NULL, '["节日","活动"]', '上海', '抖音', 50, 0, 0, 0),
('A10050', '客户见证视频', '赵六', '2026-03-08 14:00:00', 171966464, 'approved', '审核员C', '2026-03-08 18:00:00', '["见证","客户"]', '北京', '快手', 65, 80000, 24000, 12000),
('A10051', '产品介绍视频', '张三', '2026-03-12 16:00:00', 78643200, 'approved', '审核员A', '2026-03-12 20:00:00', '["介绍","产品"]', '广州', '小红书', 40, 35000, 8500, 4200),
('A10052', '春季新品发布', '李四', '2026-03-15 10:00:00', 209715200, 'rejected', '审核员B', '2026-03-15 14:00:00', '["春季","新品"]', '深圳', '抖音', 55, 0, 0, 0),
('A10053', '用户体验分享', '王五', '2026-03-18 12:00:00', 104857600, 'pending', NULL, NULL, '["体验","用户"]', '成都', '快手', 30, 0, 0, 0),
('A10054', '品牌年度规划', '赵六', '2026-03-22 08:00:00', 230686720, 'approved', '审核员C', '2026-03-22 12:00:00', '["规划","年度"]', '杭州', '小红书', 75, 50000, 12000, 6000),
('A10055', '产品评测视频', '张三', '2026-03-25 15:00:00', 157286400, 'approved', '审核员A', '2026-03-25 19:00:00', '["评测","产品"]', '上海', '抖音', 60, 75000, 22000, 11000),
('A10056', '清明节主题', '李四', '2026-04-02 11:00:00', 125829120, 'pending', NULL, NULL, '["清明","主题"]', '北京', '快手', 45, 0, 0, 0),
('A10057', '客户服务指南', '王五', '2026-04-05 09:00:00', 188743680, 'approved', '审核员B', '2026-04-05 13:00:00', '["服务","指南"]', '广州', '小红书', 70, 58000, 14000, 7000),
('A10058', '产品更新说明', '赵六', '2026-04-08 14:00:00', 83886080, 'approved', '审核员C', '2026-04-08 18:00:00', '["更新","说明"]', '深圳', '抖音', 25, 32000, 7800, 3900),
('A10059', '品牌招商视频', '张三', '2026-04-12 10:00:00', 262144000, 'rejected', '审核员A', '2026-04-12 14:00:00', '["招商","品牌"]', '成都', '快手', 95, 0, 0, 0),
('A10060', '产品使用教程', '李四', '2026-04-15 12:00:00', 171966464, 'pending', NULL, NULL, '["教程","使用"]', '杭州', '小红书', 55, 0, 0, 0),
('A10061', '劳动节特别视频', '王五', '2026-04-18 15:00:00', 141557760, 'approved', '审核员B', '2026-04-18 19:00:00', '["劳动节","特别"]', '上海', '抖音', 60, 68000, 20500, 10250),
('A10062', '用户评价收集', '赵六', '2026-04-22 08:00:00', 115343360, 'approved', '审核员C', '2026-04-22 12:00:00', '["评价","用户"]', '北京', '快手', 40, 36000, 8800, 4400),
('A10063', '品牌升级发布', '张三', '2026-04-25 11:00:00', 209715200, 'pending', NULL, NULL, '["升级","品牌"]', '广州', '小红书', 80, 0, 0, 0),
('A10064', '产品亮点展示', '李四', '2026-04-28 14:00:00', 125829120, 'approved', '审核员A', '2026-04-28 18:00:00', '["亮点","展示"]', '深圳', '抖音', 45, 55000, 13200, 6600),
('A10065', '青年节活动', '王五', '2026-05-04 10:00:00', 157286400, 'rejected', '审核员B', '2026-05-04 14:00:00', '["青年节","活动"]', '成都', '快手', 50, 0, 0, 0),
('A10066', '客户感谢视频', '赵六', '2026-05-07 12:00:00', 94371840, 'pending', NULL, NULL, '["感谢","客户"]', '杭州', '小红书', 30, 0, 0, 0),
('A10067', '品牌文化介绍', '张三', '2026-05-10 15:00:00', 230686720, 'approved', '审核员C', '2026-05-10 19:00:00', '["文化","品牌"]', '上海', '抖音', 85, 92000, 28500, 14250),
('A10068', '母亲节特别策划', '李四', '2026-05-12 09:00:00', 171966464, 'approved', '审核员A', '2026-05-12 13:00:00', '["母亲节","策划"]', '北京', '快手', 55, 60000, 18000, 9000),
('A10069', '产品销售技巧', '王五', '2026-05-15 11:00:00', 141557760, 'pending', NULL, NULL, '["技巧","销售"]', '广州', '小红书', 65, 0, 0, 0),
('A10070', '品牌活动回顾', '赵六', '2026-05-18 14:00:00', 188743680, 'rejected', '审核员B', '2026-05-18 18:00:00', '["回顾","活动"]', '深圳', '抖音', 70, 0, 0, 0),
('A10071', '儿童节主题视频', '张三', '2026-05-22 10:00:00', 104857600, 'pending', NULL, NULL, '["儿童节","主题"]', '成都', '快手', 40, 0, 0, 0),
('A10072', '产品演示视频', '李四', '2026-05-25 12:00:00', 157286400, 'approved', '审核员C', '2026-05-25 16:00:00', '["演示","产品"]', '杭州', '小红书', 60, 47000, 11200, 5600),
('A10073', '端午节特别活动', '王五', '2026-05-28 15:00:00', 209715200, 'approved', '审核员A', '2026-05-28 19:00:00', '["端午","活动"]', '上海', '抖音', 50, 78000, 23500, 11750),
('A10074', '618活动预热', '赵六', '2026-06-01 11:00:00', 262144000, 'pending', NULL, NULL, '["618","预热"]', '北京', '快手', 75, 0, 0, 0),
('A10075', '产品创新展示', '张三', '2026-06-03 09:00:00', 125829120, 'approved', '审核员B', '2026-06-03 13:00:00', '["创新","展示"]', '广州', '小红书', 45, 40000, 9600, 4800),
('A10076', '客户关怀视频', '李四', '2026-06-05 14:00:00', 188743680, 'rejected', '审核员C', '2026-06-05 18:00:00', '["关怀","客户"]', '深圳', '抖音', 80, 0, 0, 0),
('A10077', '年中总结视频', '王五', '2026-06-08 10:00:00', 230686720, 'pending', NULL, NULL, '["总结","年中"]', '成都', '快手', 65, 0, 0, 0),
('A10078', '产品使用手册', '赵六', '2026-06-10 12:00:00', 171966464, 'approved', '审核员A', '2026-06-10 16:00:00', '["手册","使用"]', '杭州', '小红书', 55, 52000, 12800, 6400),
('A10079', '父亲节特别策划', '张三', '2026-06-12 15:00:00', 115343360, 'approved', '审核员B', '2026-06-12 19:00:00', '["父亲节","策划"]', '上海', '抖音', 35, 33000, 7900, 3950),
('A10080', '品牌未来展望', '李四', '2026-06-15 08:00:00', 157286400, 'pending', NULL, NULL, '["展望","未来"]', '北京', '快手', 60, 0, 0, 0),
('A10081', '产品安全说明', '王五', '2026-06-17 11:00:00', 94371840, 'approved', '审核员C', '2026-06-17 15:00:00', '["安全","说明"]', '广州', '小红书', 40, 38000, 9200, 4600),
('A10082', '用户案例分享', '赵六', '2026-06-19 14:00:00', 209715200, 'approved', '审核员A', '2026-06-19 18:00:00', '["案例","用户"]', '深圳', '抖音', 70, 72000, 21600, 10800),
('A10083', '毕业季特别视频', '张三', '2026-06-21 10:00:00', 141557760, 'rejected', '审核员B', '2026-06-21 14:00:00', '["毕业季","特别"]', '成都', '快手', 50, 0, 0, 0),
('A10084', '产品推广指南', '李四', '2026-06-23 12:00:00', 188743680, 'pending', NULL, NULL, '["推广","指南"]', '杭州', '小红书', 65, 0, 0, 0),
('A10085', '品牌周年纪念', '王五', '2026-06-25 15:00:00', 262144000, 'approved', '审核员C', '2026-06-25 19:00:00', '["周年","纪念"]', '上海', '抖音', 90, 100000, 30000, 15000),
('A10086', '产品优化说明', '赵六', '2026-06-27 09:00:00', 104857600, 'approved', '审核员A', '2026-06-27 13:00:00', '["优化","说明"]', '北京', '快手', 35, 45000, 10800, 5400),
('A10087', '用户互动活动', '张三', '2026-06-29 11:00:00', 125829120, 'pending', NULL, NULL, '["互动","用户"]', '广州', '小红书', 50, 0, 0, 0),
('A10088', '品牌形象展示', '李四', '2026-06-30 14:00:00', 171966464, 'approved', '审核员B', '2026-06-30 18:00:00', '["形象","品牌"]', '深圳', '抖音', 60, 66000, 19800, 9900),
('A10089', '产品售后指南', '王五', '2026-01-03 16:00:00', 188743680, 'approved', '审核员C', '2026-01-03 20:00:00', '["售后","指南"]', '成都', '快手', 45, 54000, 13000, 6500),
('A10090', '新春开工视频', '赵六', '2026-01-29 08:00:00', 94371840, 'pending', NULL, NULL, '["开工","新春"]', '杭州', '小红书', 30, 0, 0, 0),
('A10091', '产品技术解析', '张三', '2026-02-01 10:00:00', 230686720, 'approved', '审核员A', '2026-02-01 14:00:00', '["技术","解析"]', '上海', '抖音', 80, 82000, 24500, 12250),
('A10092', '员工激励视频', '李四', '2026-02-10 12:00:00', 141557760, 'approved', '审核员B', '2026-02-10 16:00:00', '["激励","员工"]', '北京', '快手', 55, 44000, 10600, 5300),
('A10093', '产品发布回顾', '王五', '2026-02-16 15:00:00', 115343360, 'rejected', '审核员C', '2026-02-16 19:00:00', '["回顾","发布"]', '广州', '小红书', 35, 0, 0, 0),
('A10094', '用户需求调研', '赵六', '2026-02-20 09:00:00', 78643200, 'pending', NULL, NULL, '["调研","需求"]', '深圳', '抖音', 25, 0, 0, 0),
('A10095', '品牌营销方案', '张三', '2026-03-01 13:00:00', 209715200, 'approved', '审核员A', '2026-03-01 17:00:00', '["营销","方案"]', '成都', '快手', 70, 70000, 21000, 10500),
('A10096', '产品包装设计', '李四', '2026-03-10 11:00:00', 157286400, 'approved', '审核员B', '2026-03-10 15:00:00', '["设计","包装"]', '杭州', '小红书', 60, 46000, 11000, 5500),
('A10097', '消费者体验日', '王五', '2026-03-20 10:00:00', 188743680, 'pending', NULL, NULL, '["体验","消费者"]', '上海', '抖音', 50, 0, 0, 0),
('A10098', '产品品质展示', '赵六', '2026-04-01 15:00:00', 125829120, 'approved', '审核员C', '2026-04-01 19:00:00', '["品质","展示"]', '北京', '快手', 45, 39000, 9300, 4650),
('A10099', '新品上市攻略', '张三', '2026-04-10 12:00:00', 262144000, 'approved', '审核员A', '2026-04-10 16:00:00', '["上市","新品"]', '广州', '小红书', 85, 95000, 28500, 14250),
('A10100', '品牌故事讲述', '李四', '2026-04-20 08:00:00', 171966464, 'pending', NULL, NULL, '["故事","品牌"]', '深圳', '抖音', 55, 0, 0, 0);

-- 2. 初始化指标配置（预置9个）
INSERT INTO metric_config (
    metric_code, metric_name, metric_desc, source_table,
    category_code, category_name,
    group_by_fields, filter_conditions, having_conditions, sort_rules, limit_value, window_functions, query_count, enabled
) VALUES
(
    'asset_count_by_status',
    '按审核状态统计素材数量',
    '统计各审核状态的素材数量分布',
    'asset',
    'audit_management', '审核管理',
    '["status"]',
    NULL,
    NULL,
    '[{"field":"asset_count","order":"desc","param_name":"sort_direction"}]',
    NULL,
    NULL,
    120, TRUE
),
(
    'avg_file_size_by_uploader',
    '各上传人平均文件大小',
    '统计已通过审核素材中，各上传人的平均文件大小',
    'asset',
    'content_operation', '内容运营',
    '["uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"avg_file_size","order":"desc","param_name":"avg_sort_direction"}]',
    NULL,
    NULL,
    85, TRUE
),
(
    'avg_duration_by_platform',
    '各平台平均视频时长',
    '统计各投放平台的视频平均时长，用于评估内容适配性',
    'asset',
    'content_operation', '内容运营',
    '["platform"]',
    NULL,
    NULL,
    '[{"field":"avg_duration","order":"desc","param_name":"duration_sort"}]',
    NULL,
    NULL,
    200, TRUE
),
(
    'platform_performance',
    '平台效果概览',
    '按平台统计素材数量、平均时长、总曝光量等多维度指标',
    'asset',
    'data_analysis', '数据分析',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"total_impressions","order":"desc","param_name":"perf_sort"}]',
    NULL,
    NULL,
    350, TRUE
),
(
    'platform_city_status_analysis',
    '平台×城市×状态三维分析',
    '多维度交叉分析素材分布',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "city", "status"]',
    NULL,
    NULL,
    '[{"field":"platform","order":"asc","param_name":"platform_order"},{"field":"city","order":"asc","param_name":"city_order"},{"field":"asset_count","order":"desc","param_name":"cross_sort"}]',
    NULL,
    NULL,
    180, TRUE
),
(
    'multi_filter_example',
    '多条件过滤示例',
    '演示多条件过滤能力',
    'asset',
    'content_operation', '内容运营',
    '["platform", "uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"platform","operator":"IN","value":["抖音","快手"],"param_name":"platforms"},{"field":"uploaded_at","operator":">=","value":"2026-01-01","param_name":"min_upload_date"},{"field":"file_size_bytes","operator":">","value":10485760,"param_name":"min_file_size"}],"logic":"AND"}',
    NULL,
    '[{"field":"platform","order":"asc","param_name":"platform_order"},{"field":"total_plays","order":"desc","param_name":"main_sort_order"}]',
    NULL,
    NULL,
    65, TRUE
),
(
    'dynamic_date_range_example',
    '动态日期范围查询',
    '支持动态传入开始日期和结束日期',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "city"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"uploaded_at","operator":">=","value":"2026-01-01","param_name":"start_date"},{"field":"uploaded_at","operator":"<=","value":"2026-12-31","param_name":"end_date"}],"logic":"AND"}',
    NULL,
    '[{"field":"total_plays","order":"desc","param_name":"sort_direction"}]',
    NULL,
    NULL,
    420, TRUE
),
(
    'having_limit_example',
    '高产出上传人TOP10',
    '筛选素材数量大于3的上传人，取TOP10',
    'asset',
    'content_operation', '内容运营',
    '["uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">","value":3,"param_name":"min_asset_count"},{"field":"total_impressions","operator":">","value":100000,"param_name":"min_impressions"}],"logic":"AND"}',
    '[{"field":"asset_count","order":"desc","param_name":"having_sort_order"}]',
    10,
    NULL,
    95, TRUE
),
(
    'window_function_example',
    '各平台素材播放量排名',
    '使用窗口函数计算各平台内素材播放量排名',
    'asset',
    'data_analysis', '数据分析',
    NULL,
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"platform","order":"asc","param_name":"platform_sort"},{"field":"rank_in_platform","order":"asc","param_name":"rank_sort"}]',
    NULL,
    '{"functions":[{"name":"ROW_NUMBER","partition_by":["platform"],"order_by":[{"field":"play_count","direction":"desc","param_name":"window_order"}],"item_name":"rank_in_platform"}]}',
    150, TRUE
);

-- 3. 初始化指标统计项
INSERT INTO metric_stat_item (metric_id, item_code, item_name, field_name, aggregation, sort_order) VALUES
(1, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(2, 'avg_file_size', '平均文件大小', 'file_size_bytes', 'avg', 1),
(3, 'avg_duration', '平均视频时长', 'duration_seconds', 'avg', 1),
(4, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(4, 'avg_duration', '平均时长(秒)', 'duration_seconds', 'avg', 2),
(4, 'total_impressions', '总曝光量', 'impression_count', 'sum', 3),
(4, 'total_plays', '总播放量', 'play_count', 'sum', 4),
(5, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(5, 'total_impressions', '总曝光量', 'impression_count', 'sum', 2),
(6, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(6, 'total_plays', '总播放量', 'play_count', 'sum', 2),
(6, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 3),
(7, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(7, 'total_plays', '总播放量', 'play_count', 'sum', 2),
(8, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(8, 'total_impressions', '总曝光量', 'impression_count', 'sum', 2),
(9, 'play_count', '播放量', 'play_count', 'sum', 1);

-- 4. 初始化业务分类数据
INSERT INTO metric_category (category_code, category_name, description, sort_order, enabled) VALUES
('content_operation', '内容运营', '内容上传、管理相关指标', 1, TRUE),
('audit_management', '审核管理', '审核状态、审核效率相关指标', 2, TRUE),
('data_analysis', '数据分析', '多维分析、效果评估相关指标', 3, TRUE);

-- 5. 初始化指标集合
INSERT INTO metric_collection (collection_code, collection_name, description, target_role, enabled) VALUES
('content_operator', '内容运营指标包', '内容运营人员常用指标集合', '内容运营', TRUE),
('audit_operator', '审核管理指标包', '审核人员常用指标集合', '审核管理', TRUE),
('data_analyst', '数据分析指标包', '数据分析人员常用指标集合', '数据分析', TRUE),
('manager_dashboard', '管理层仪表盘', '管理层视角的核心指标', '管理层', TRUE);

-- 6. 初始化指标集合关联
INSERT INTO metric_collection_item (collection_id, metric_id, sort_order) VALUES
(1, 1, 1), (1, 2, 2), (1, 3, 3), (1, 6, 4), (1, 8, 5),
(2, 1, 1), (2, 5, 2),
(3, 4, 1), (3, 5, 2), (3, 7, 3), (3, 9, 4),
(4, 4, 1), (4, 1, 2), (4, 3, 3);

-- =============================================
-- 追加新指标配置（20个业务指标）
-- =============================================

-- 新增指标配置1-20
INSERT INTO metric_config (
    metric_code, metric_name, metric_desc, source_table,
    category_code, category_name,
    group_by_fields, filter_conditions, having_conditions, sort_rules, limit_value, window_functions, query_count, enabled
) VALUES
(
    'content_trend_analysis',
    '内容发布趋势分析',
    '按平台和时间维度分析内容发布量的时间序列变化',
    'asset',
    'content_operation', '内容运营',
    '["platform", "uploaded_at"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"uploaded_at","order":"desc","param_name":"time_sort"},{"field":"platform","order":"asc","param_name":"platform_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'uploader_performance_rank',
    '上传人绩效排名',
    '按素材数量、播放量、完播率综合评估上传人绩效的TOP20',
    'asset',
    'content_operation', '内容运营',
    '["uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":5,"param_name":"min_assets"}],"logic":"AND"}',
    '[{"field":"total_plays","order":"desc","param_name":"perf_sort"},{"field":"avg_complete_rate","order":"desc","param_name":"rate_sort"}]',
    20,
    NULL,
    0, TRUE
),
(
    'platform_content_hotspot',
    '平台内容热点分析',
    '分析各平台最热门的内容类型和标签组合，筛选完播率TOP内容',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "status"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"complete_play_count","operator":">","value":0,"param_name":"has_complete"}],"logic":"AND"}',
    NULL,
    '[{"field":"avg_complete_rate","order":"desc","param_name":"rate_sort"},{"field":"total_impressions","order":"desc","param_name":"impression_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'review_efficiency_tracking',
    '审核效率追踪',
    '按审核人和审核状态统计审核时长和审核量的效率指标',
    'asset',
    'audit_management', '审核管理',
    '["reviewer", "status"]',
    '{"conditions":[{"field":"status","operator":"IN","value":["approved","rejected"],"param_name":"review_status"}],"logic":"AND"}',
    NULL,
    '[{"field":"reviewer","order":"asc","param_name":"reviewer_sort"},{"field":"avg_review_hours","order":"asc","param_name":"time_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'city_content_distribution',
    '城市内容分布',
    '按城市维度分析素材的数量、播放量和时长分布情况',
    'asset',
    'data_analysis', '数据分析',
    '["city"]',
    '{"conditions":[{"field":"city","operator":"IS NOT NULL","value":null,"param_name":"has_city"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":3,"param_name":"min_city_assets"}],"logic":"AND"}',
    '[{"field":"total_impressions","order":"desc","param_name":"city_impression_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'video_duration_effectiveness',
    '视频时长效果分析',
    '分析不同视频时长区间的完播率和播放量表现，找出最佳时长',
    'asset',
    'data_analysis', '数据分析',
    '["platform"]',
    '{"conditions":[{"field":"duration_seconds","operator":"IS NOT NULL","value":null,"param_name":"has_duration"},{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"avg_complete_rate","order":"desc","param_name":"duration_effect_sort"},{"field":"avg_duration","order":"asc","param_name":"duration_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'pending_content_backlog',
    '待审核内容积压分析',
    '按上传时间和上传人维度分析待审核内容的积压情况',
    'asset',
    'audit_management', '审核管理',
    '["uploader", "uploaded_at"]',
    '{"conditions":[{"field":"status","operator":"=","value":"pending","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"uploaded_at","order":"asc","param_name":"oldest_first"},{"field":"asset_count","order":"desc","param_name":"volume_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'platform_vs_city_heatmap',
    '平台×城市热度矩阵',
    '分析各平台在不同城市的素材播放量分布热力图',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "city"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"city","operator":"IS NOT NULL","value":null,"param_name":"has_city"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_impressions","operator":">=","value":1000,"param_name":"min_impressions"}],"logic":"AND"}',
    '[{"field":"total_impressions","order":"desc","param_name":"heat_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'rejection_reason_insights',
    '拒绝内容洞察',
    '分析被拒绝内容的特征，包括上传人、平台、时长等维度',
    'asset',
    'audit_management', '审核管理',
    '["uploader", "platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"rejected","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"asset_count","order":"desc","param_name":"reject_sort"},{"field":"uploader","order":"asc","param_name":"uploader_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'content_viral_potential',
    '内容传播潜力分析',
    '通过曝光、播放、完播的转化漏斗分析内容的病毒传播潜力TOP30',
    'asset',
    'data_analysis', '数据分析',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"impression_count","operator":">","value":1000,"param_name":"has_impression"}],"logic":"AND"}',
    NULL,
    '[{"field":"play_through_rate","order":"desc","param_name":"viral_sort"},{"field":"total_plays","order":"desc","param_name":"play_sort"}]',
    30,
    NULL,
    0, TRUE
),
(
    'uploader_content_consistency',
    '上传人内容一致性',
    '分析各上传人的内容发布频率、时长、平台选择的一致性',
    'asset',
    'content_operation', '内容运营',
    '["uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":3,"param_name":"min_uploads"}],"logic":"AND"}',
    '[{"field":"upload_interval_days","order":"asc","param_name":"frequency_sort"},{"field":"asset_count","order":"desc","param_name":"count_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'platform_content_timing',
    '平台内容发布时机',
    '分析不同时间点发布的内容在各平台的表现差异',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "uploaded_at"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"avg_plays","order":"desc","param_name":"timing_sort"},{"field":"avg_complete_rate","order":"desc","param_name":"rate_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'content_size_analysis',
    '文件大小分布影响',
    '分析不同文件大小区间对播放量和完播率的影响',
    'asset',
    'content_operation', '内容运营',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"avg_file_size_mb","order":"asc","param_name":"size_sort"},{"field":"total_plays","order":"desc","param_name":"play_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'reviewer_performance_comparison',
    '审核人绩效对比',
    '横向对比各审核人的审核数量、通过率和平均审核时长',
    'asset',
    'audit_management', '审核管理',
    '["reviewer"]',
    '{"conditions":[{"field":"status","operator":"IN","value":["approved","rejected"],"param_name":"reviewed"},{"field":"reviewer","operator":"IS NOT NULL","value":null,"param_name":"has_reviewer"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_reviews","operator":">=","value":5,"param_name":"min_reviews"}],"logic":"AND"}',
    '[{"field":"approval_rate","order":"desc","param_name":"rate_sort"},{"field":"avg_review_hours","order":"asc","param_name":"speed_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'top_playing_content',
    '高播放内容特征分析',
    '识别播放量TOP50素材的共同特征：平台、时长、上传人等',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"total_plays","order":"desc","param_name":"top_play_sort"},{"field":"avg_complete_rate","order":"desc","param_name":"complete_sort"}]',
    50,
    NULL,
    0, TRUE
),
(
    'approval_pipeline_monitor',
    '审核流程监控',
    '实时监控各状态素材的分布和流转情况，支持预警',
    'asset',
    'audit_management', '审核管理',
    '["status"]',
    NULL,
    NULL,
    '[{"field":"status","order":"asc","param_name":"status_sort"},{"field":"asset_count","order":"desc","param_name":"count_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'seasonal_content_patterns',
    '季节性内容模式',
    '分析不同时间段的内容主题偏好和平台表现',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "uploaded_at"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"uploaded_at","order":"desc","param_name":"time_sort"},{"field":"avg_plays","order":"desc","param_name":"seasonal_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'uploader_productivity_matrix',
    '上传人产能矩阵',
    '从数量、质量、效率三个维度评估上传人综合产能',
    'asset',
    'content_operation', '内容运营',
    '["uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":5,"param_name":"min_productivity"}],"logic":"AND"}',
    '[{"field":"quality_score","order":"desc","param_name":"quality_sort"},{"field":"asset_count","order":"desc","param_name":"quantity_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'cross_platform_content_performance',
    '跨平台内容表现对比',
    '对比同类素材在不同平台的表现差异，找出平台适配性',
    'asset',
    'data_analysis', '数据分析',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"avg_complete_rate","order":"desc","param_name":"cross_platform_sort"},{"field":"total_impressions","order":"desc","param_name":"exposure_sort"}]',
    NULL,
    NULL,
    0, TRUE
),
(
    'content_lifecycle_analysis',
    '内容生命周期分析',
    '分析内容从上传到审核到推广的全周期表现和转化率',
    'asset',
    'data_analysis', '数据分析',
    '["status", "platform"]',
    NULL,
    NULL,
    '[{"field":"status","order":"asc","param_name":"lifecycle_sort"},{"field":"platform","order":"asc","param_name":"platform_lifecycle_sort"}]',
    NULL,
    NULL,
    0, TRUE
);

-- 新增指标的统计项配置（第10-29个指标）
INSERT INTO metric_stat_item (metric_id, item_code, item_name, field_name, aggregation, sort_order) VALUES
(10, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(10, 'total_plays', '总播放量', 'play_count', 'sum', 2),
(10, 'avg_plays', '平均播放量', 'play_count', 'avg', 3),
(11, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(11, 'total_plays', '总播放量', 'play_count', 'sum', 2),
(11, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 3),
(12, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(12, 'total_impressions', '总曝光', 'impression_count', 'sum', 2),
(12, 'total_plays', '总播放', 'play_count', 'sum', 3),
(12, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 4),
(13, 'asset_count', '审核数量', 'asset_id', 'count', 1),
(13, 'avg_review_hours', '平均审核时长(小时)', 'reviewed_at', 'avg', 2),
(13, 'approval_rate', '通过率', 'status', 'avg', 3),
(14, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(14, 'total_impressions', '总曝光', 'impression_count', 'sum', 2),
(14, 'total_plays', '总播放', 'play_count', 'sum', 3),
(14, 'avg_duration', '平均时长', 'duration_seconds', 'avg', 4),
(15, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(15, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 2),
(15, 'avg_duration', '平均时长', 'duration_seconds', 'avg', 3),
(16, 'asset_count', '待审核数量', 'asset_id', 'count', 1),
(16, 'avg_pending_hours', '平均待审时长', 'uploaded_at', 'avg', 2),
(17, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(17, 'total_impressions', '总曝光', 'impression_count', 'sum', 2),
(17, 'total_plays', '总播放', 'play_count', 'sum', 3),
(18, 'asset_count', '拒绝数量', 'asset_id', 'count', 1),
(18, 'rejection_rate', '拒绝率', 'status', 'avg', 2),
(19, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(19, 'play_through_rate', '播放转化率', 'play_count', 'avg', 2),
(19, 'complete_through_rate', '完播转化率', 'complete_play_count', 'avg', 3),
(19, 'total_plays', '总播放量', 'play_count', 'sum', 4),
(20, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(20, 'avg_duration', '平均时长', 'duration_seconds', 'avg', 2),
(20, 'upload_interval_days', '发布间隔(天)', 'uploaded_at', 'avg', 3),
(21, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(21, 'avg_plays', '平均播放', 'play_count', 'avg', 2),
(21, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 3),
(22, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(22, 'avg_file_size_mb', '平均文件大小(MB)', 'file_size_bytes', 'avg', 2),
(22, 'total_plays', '总播放', 'play_count', 'sum', 3),
(23, 'total_reviews', '总审核数', 'asset_id', 'count', 1),
(23, 'approval_rate', '通过率', 'status', 'avg', 2),
(23, 'avg_review_hours', '平均审核时长', 'reviewed_at', 'avg', 3),
(24, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(24, 'total_plays', '总播放', 'play_count', 'sum', 2),
(24, 'avg_duration', '平均时长', 'duration_seconds', 'avg', 3),
(24, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 4),
(25, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(25, 'pending_count', '待审核数', 'status', 'count', 2),
(25, 'approved_count', '已通过数', 'status', 'count', 3),
(26, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(26, 'total_plays', '总播放', 'play_count', 'sum', 2),
(26, 'avg_plays', '平均播放量', 'play_count', 'avg', 3),
(26, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 4),
(27, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(27, 'quality_score', '质量评分', 'play_count', 'avg', 2),
(27, 'total_plays', '总播放', 'play_count', 'sum', 3),
(28, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(28, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 2),
(28, 'total_impressions', '总曝光', 'impression_count', 'sum', 3),
(29, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(29, 'approval_cycle', '审核周期', 'reviewed_at', 'avg', 2),
(29, 'total_plays', '总播放', 'play_count', 'sum', 3);

-- 将新增指标加入到对应的指标集合中
INSERT INTO metric_collection_item (collection_id, metric_id, sort_order) VALUES
(1, 10, 6), (1, 11, 7), (1, 15, 8), (1, 20, 9), (1, 22, 10), (1, 27, 11),
(2, 13, 3), (2, 16, 4), (2, 18, 5), (2, 23, 6), (2, 25, 7),
(3, 12, 5), (3, 14, 6), (3, 17, 7), (3, 19, 8), (3, 21, 9), (3, 24, 10), (3, 26, 11), (3, 28, 12), (3, 29, 13),
(4, 10, 4), (4, 12, 5), (4, 17, 6), (4, 24, 7);

-- =============================================
-- 追加21个完全体业务指标（第30-50个指标）
-- =============================================
INSERT INTO metric_config (
    id, metric_code, metric_name, metric_desc, source_table,
    category_code, category_name,
    group_by_fields, filter_conditions, having_conditions, sort_rules, limit_value, window_functions, query_count, enabled
) VALUES
(
    30, 'uploader_platform_performance',
    '上传人平台表现',
    '分析上传人在各平台的内容表现，包括曝光、播放、完播等核心指标',
    'asset',
    'content_operation', '内容运营',
    '["platform", "uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"impression_count","operator":">","value":5000,"param_name":"min_impression"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":3,"param_name":"min_assets"}],"logic":"AND"}',
    '[{"field":"total_plays","order":"desc","param_name":"play_sort"},{"field":"avg_complete_rate","order":"desc","param_name":"quality_sort"}]',
    50,
    '{"functions":[{"name":"NTILE","partition_by":["platform"],"order_by":[{"field":"total_plays","direction":"desc","param_name":"tile_order"}],"item_name":"performance_tile","n":4},{"name":"PERCENT_RANK","partition_by":["platform"],"order_by":[{"field":"avg_complete_rate","direction":"desc","param_name":"percent_order"}],"item_name":"completion_percentile"}]}',
    0, TRUE
),
(
    32, 'platform_content_summary',
    '平台内容概况',
    '分析各平台的内容时长、文件大小、完播率等基本特征',
    'asset',
    'data_analysis', '数据分析',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":10,"param_name":"min_assets_for_matrix"}],"logic":"AND"}',
    '[{"field":"avg_complete_rate","order":"desc","param_name":"rate_sort"},{"field":"asset_count","order":"desc","param_name":"volume_sort"}]',
    50,
    '{"functions":[{"name":"NTILE","order_by":[{"field":"avg_complete_rate","direction":"desc","param_name":"matrix_tile"}],"item_name":"quality_tier","n":4}]}',
    0, TRUE
),
(
    31, 'uploader_contribution_rank',
    '上传人贡献排名',
    '按总播放量、内容数量综合排名，识别核心贡献者',
    'asset',
    'content_operation', '内容运营',
    '["uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_uploads","operator":">=","value":5,"param_name":"min_uploads"}],"logic":"AND"}',
    '[{"field":"total_plays","order":"desc","param_name":"play_sort"},{"field":"total_uploads","order":"desc","param_name":"upload_sort"}]',
    50,
    NULL,
    0, TRUE
),
(
    33, 'reviewer_quality_audit',
    '审核人工作表现',
    '统计审核人的审核通过率和审核效率，评估审核人员工作表现',
    'asset',
    'audit_management', '审核管理',
    '["reviewer"]',
    '{"conditions":[{"field":"reviewer","operator":"IS NOT NULL","value":null,"param_name":"has_reviewer"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_reviews","operator":">=","value":10,"param_name":"min_reviews"}],"logic":"AND"}',
    '[{"field":"approval_rate","order":"desc","param_name":"quality_sort"},{"field":"avg_review_hours","order":"asc","param_name":"speed_sort"}]',
    20,
    '{"functions":[{"name":"DENSE_RANK","order_by":[{"field":"approval_rate","direction":"desc","param_name":"rank_order"}],"item_name":"approval_rank"}]}',
    0, TRUE
),
(
    34, 'content_tag_performance',
    '内容平台标签分析',
    '分析有标签内容在各平台的播放表现和完播率，了解标签内容的整体表现',
    'asset',
    'data_analysis', '数据分析',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"tags","operator":"IS NOT NULL","value":null,"param_name":"has_tags"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":5,"param_name":"min_tagged_assets"}],"logic":"AND"}',
    '[{"field":"avg_plays_per_tag","order":"desc","param_name":"tag_perf_sort"}]',
    30,
    '{"functions":[{"name":"PERCENT_RANK","order_by":[{"field":"avg_complete_rate","direction":"desc","param_name":"tag_percentile"}],"item_name":"tag_performance_rank"}]}',
    0, TRUE
),
(
    35, 'platform_upload_trend',
    '平台上传趋势',
    '分析各平台在不同时间的内容上传和表现情况',
    'asset',
    'content_operation', '内容运营',
    '["platform", "uploaded_at"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":3,"param_name":"min_hourly_assets"}],"logic":"AND"}',
    '[{"field":"platform","order":"asc","param_name":"platform_sort"},{"field":"avg_plays","order":"desc","param_name":"time_slot_sort"}]',
    100,
    '{"functions":[{"name":"ROW_NUMBER","partition_by":["platform"],"order_by":[{"field":"avg_plays","direction":"desc","param_name":"hour_rank"}],"item_name":"best_hour_rank"}]}',
    0, TRUE
),
(
    36, 'rejected_content_analysis',
    '被拒内容分析',
    '统计各上传人和平台的被拒内容情况，了解审核状况',
    'asset',
    'audit_management', '审核管理',
    '["uploader", "platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"rejected","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"rejection_count","operator":">=","value":2,"param_name":"min_rejects"}],"logic":"AND"}',
    '[{"field":"rejection_count","order":"desc","param_name":"count_sort"}]',
    50,
    '{"functions":[{"name":"RANK","order_by":[{"field":"rejection_count","direction":"desc","param_name":"reject_rank"}],"item_name":"rejection_rank"}]}',
    0, TRUE
),
(
    37, 'city_platform_comparison',
    '城市平台对比',
    '对比不同城市在各平台的内容表现，了解区域差异',
    'asset',
    'data_analysis', '数据分析',
    '["city", "platform"]',
    '{"conditions":[{"field":"city","operator":"IS NOT NULL","value":null,"param_name":"has_city"},{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":5,"param_name":"min_city_assets"}],"logic":"AND"}',
    '[{"field":"city","order":"asc","param_name":"city_sort"},{"field":"total_plays","order":"desc","param_name":"play_sort"}]',
    100,
    '{"functions":[{"name":"NTILE","partition_by":["city"],"order_by":[{"field":"total_plays","direction":"desc","param_name":"city_tile"}],"item_name":"city_content_tile","n":3}]}',
    0, TRUE
),
(
    38, 'uploader_platform_comparison',
    '上传人跨平台对比',
    '分析上传人在不同平台的内容表现，了解跨平台状况',
    'asset',
    'content_operation', '内容运营',
    '["uploader", "platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":3,"param_name":"min_assets"}],"logic":"AND"}',
    '[{"field":"uploader","order":"asc","param_name":"uploader_sort"},{"field":"total_plays","order":"desc","param_name":"play_sort"}]',
    80,
    '{"functions":[{"name":"ROW_NUMBER","partition_by":["uploader"],"order_by":[{"field":"total_plays","direction":"desc","param_name":"platform_rank"}],"item_name":"platform_rank_in_uploader"}]}',
    0, TRUE
),
(
    40, 'reviewer_workload_summary',
    '审核人工作量统计',
    '统计审核人的审核数量和平均审核时长，分析工作量分配',
    'asset',
    'audit_management', '审核管理',
    '["reviewer"]',
    '{"conditions":[{"field":"reviewer","operator":"IS NOT NULL","value":null,"param_name":"has_reviewer"},{"field":"status","operator":"IN","value":["approved","rejected"],"param_name":"reviewed"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_reviews","operator":">=","value":5,"param_name":"min_reviews"}],"logic":"AND"}',
    '[{"field":"total_reviews","order":"desc","param_name":"count_sort"},{"field":"avg_review_hours","order":"asc","param_name":"speed_sort"}]',
    50,
    '{"functions":[{"name":"CUME_DIST","order_by":[{"field":"total_reviews","direction":"desc","param_name":"cume_order"}],"item_name":"workload_percentile"}]}',
    0, TRUE
),
(
    39, 'daily_upload_trend',
    '每日上传趋势',
    '统计每日内容上传数量，分析上传规律',
    'asset',
    'content_operation', '内容运营',
    '["uploaded_at"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    NULL,
    '[{"field":"uploaded_at","order":"desc","param_name":"date_sort"}]',
    100,
    NULL,
    0, TRUE
),
(
    41, 'high_performance_content',
    '高表现内容分析',
    '分析高曝光内容的播放和完播表现，找出表现优秀的内容',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"impression_count","operator":">","value":10000,"param_name":"high_exposure"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":2,"param_name":"min_assets"}],"logic":"AND"}',
    '[{"field":"avg_complete_rate","order":"desc","param_name":"quality_sort"},{"field":"total_plays","order":"desc","param_name":"play_sort"}]',
    30,
    '{"functions":[{"name":"PERCENT_RANK","order_by":[{"field":"avg_complete_rate","direction":"desc","param_name":"percent_rank"}],"item_name":"quality_percentile"}]}',
    0, TRUE
),
(
    42, 'uploader_content_profile',
    '上传人内容概况',
    '分析上传人的内容时长分布和播放表现，了解上传人内容特征',
    'asset',
    'content_operation', '内容运营',
    '["uploader"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":5,"param_name":"min_diversity_assets"}],"logic":"AND"}',
    '[{"field":"total_plays","order":"desc","param_name":"play_sort"},{"field":"avg_complete_rate","order":"desc","param_name":"quality_sort"}]',
    60,
    '{"functions":[{"name":"DENSE_RANK","order_by":[{"field":"total_plays","direction":"desc","param_name":"diversity_rank"}],"item_name":"content_tier"}]}',
    0, TRUE
),
(
    43, 'platform_approval_rate',
    '平台审核率统计',
    '统计各平台内容审核通过率，对比平台差异',
    'asset',
    'audit_management', '审核管理',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"IN","value":["approved","rejected"],"param_name":"reviewed"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_reviews","operator":">=","value":20,"param_name":"min_reviews"}],"logic":"AND"}',
    '[{"field":"approval_rate","order":"desc","param_name":"rate_sort"},{"field":"total_reviews","order":"desc","param_name":"count_sort"}]',
    20,
    NULL,
    0, TRUE
),
(
    44, 'platform_engagement_metrics',
    '平台参与度指标',
    '分析各平台的曝光、播放、完播等核心参与度指标，了解平台表现',
    'asset',
    'data_analysis', '数据分析',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"impression_count","operator":">","value":0,"param_name":"has_impression"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_impressions","operator":">=","value":1000,"param_name":"min_funnel_impressions"}],"logic":"AND"}',
    '[{"field":"avg_complete_rate","order":"desc","param_name":"complete_sort"},{"field":"total_plays","order":"desc","param_name":"play_sort"}]',
    50,
    '{"functions":[{"name":"NTILE","order_by":[{"field":"avg_complete_rate","direction":"desc","param_name":"funnel_tile"}],"item_name":"engagement_tier","n":5}]}',
    0, TRUE
),
(
    45, 'city_platform_content',
    '城市平台内容分析',
    '分析各城市在不同平台的内容表现，了解区域内容状况',
    'asset',
    'data_analysis', '数据分析',
    '["city", "platform"]',
    '{"conditions":[{"field":"city","operator":"IS NOT NULL","value":null,"param_name":"has_city"},{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":10,"param_name":"min_city_data"}],"logic":"AND"}',
    '[{"field":"city","order":"asc","param_name":"city_sort"},{"field":"total_plays","order":"desc","param_name":"play_sort"}]',
    100,
    '{"functions":[{"name":"PERCENT_RANK","partition_by":["city"],"order_by":[{"field":"avg_complete_rate","direction":"desc","param_name":"city_pref_rank"}],"item_name":"performance_percentile"}]}',
    0, TRUE
),
(
    46, 'uploader_performance_summary',
    '上传人表现概况',
    '统计上传人的内容数量、审核通过率和播放表现，分析上传人综合表现',
    'asset',
    'content_operation', '内容运营',
    '["uploader"]',
    '{"conditions":[{"field":"uploader","operator":"IS NOT NULL","value":null,"param_name":"has_uploader"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_uploads","operator":">=","value":5,"param_name":"min_uploads_for_score"}],"logic":"AND"}',
    '[{"field":"total_plays","order":"desc","param_name":"play_sort"},{"field":"approval_rate","order":"desc","param_name":"approval_sort"}]',
    40,
    '{"functions":[{"name":"DENSE_RANK","order_by":[{"field":"total_plays","direction":"desc","param_name":"reliability_rank"}],"item_name":"performance_tier"}]}',
    0, TRUE
),
(
    47, 'platform_content_overview',
    '平台内容概况',
    '分析各平台的内容数量、平均完播率等基本指标，了解各平台内容状况',
    'asset',
    'data_analysis', '数据分析',
    '["platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":20,"param_name":"min_competition_samples"}],"logic":"AND"}',
    '[{"field":"asset_count","order":"desc","param_name":"count_sort"},{"field":"avg_complete_rate","order":"desc","param_name":"quality_sort"}]',
    30,
    '{"functions":[{"name":"PERCENT_RANK","order_by":[{"field":"avg_complete_rate","direction":"desc","param_name":"percent_competition"}],"item_name":"content_percentile"}]}',
    0, TRUE
),
(
    48, 'reviewer_platform_performance',
    '审核人平台表现',
    '统计审核人在各平台的审核数量和通过率，分析审核人员跨平台表现',
    'asset',
    'audit_management', '审核管理',
    '["reviewer", "platform"]',
    '{"conditions":[{"field":"reviewer","operator":"IS NOT NULL","value":null,"param_name":"has_reviewer"},{"field":"status","operator":"IN","value":["approved","rejected"],"param_name":"reviewed"}],"logic":"AND"}',
    '{"conditions":[{"field":"reviewer_reviews","operator":">=","value":8,"param_name":"min_consistency_checks"}],"logic":"AND"}',
    '[{"field":"approval_rate","order":"desc","param_name":"rate_sort"},{"field":"reviewer_reviews","order":"desc","param_name":"count_sort"}]',
    40,
    '{"functions":[{"name":"ROW_NUMBER","partition_by":["platform"],"order_by":[{"field":"approval_rate","direction":"desc","param_name":"consistency_rank"}],"item_name":"platform_approval_rank"}]}',
    0, TRUE
),
(
    49, 'high_play_uploaders',
    '高播放上传人分析',
    '分析各平台上高播放量的上传人表现，识别优质内容创作者',
    'asset',
    'content_operation', '内容运营',
    '["uploader", "platform"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"},{"field":"play_count","operator":">","value":5000,"param_name":"momentum_threshold"}],"logic":"AND"}',
    '{"conditions":[{"field":"total_plays","operator":">=","value":10000,"param_name":"min_momentum_plays"}],"logic":"AND"}',
    '[{"field":"total_plays","order":"desc","param_name":"play_sort"},{"field":"avg_plays","order":"desc","param_name":"avg_play_sort"}]',
    25,
    '{"functions":[{"name":"ROW_NUMBER","partition_by":["platform"],"order_by":[{"field":"total_plays","direction":"desc","param_name":"momentum_rank"}],"item_name":"platform_play_rank"}]}',
    0, TRUE
),
(
    50, 'platform_time_performance',
    '平台时间表现分析',
    '分析各平台在不同时期的内容表现，了解平台内容趋势',
    'asset',
    'data_analysis', '数据分析',
    '["platform", "uploaded_at"]',
    '{"conditions":[{"field":"status","operator":"=","value":"approved","param_name":"status"}],"logic":"AND"}',
    '{"conditions":[{"field":"asset_count","operator":">=","value":3,"param_name":"min_holiday_assets"}],"logic":"AND"}',
    '[{"field":"uploaded_at","order":"desc","param_name":"time_sort"},{"field":"avg_plays","order":"desc","param_name":"perf_sort"}]',
    80,
    '{"functions":[{"name":"NTILE","partition_by":["platform"],"order_by":[{"field":"avg_plays","direction":"desc","param_name":"holiday_tile"}],"item_name":"performance_tier","n":4}]}',
    0, TRUE
);

-- =============================================
-- 21个完全体指标的统计项配置（第30-50个指标）
-- =============================================
INSERT INTO metric_stat_item (metric_id, item_code, item_name, field_name, aggregation, sort_order) VALUES
-- 30: uploader_platform_performance
(30, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(30, 'total_impressions', '总曝光', 'impression_count', 'sum', 2),
(30, 'total_plays', '总播放', 'play_count', 'sum', 3),
(30, 'total_complete_plays', '完播总量', 'complete_play_count', 'sum', 4),
(30, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 5),
(30, 'avg_plays_per_impression', '播放/曝光比', 'play_count', 'avg', 6),

-- 32: platform_content_summary
(32, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(32, 'avg_duration', '平均时长', 'duration_seconds', 'avg', 2),
(32, 'avg_file_size_mb', '平均文件大小(MB)', 'file_size_bytes', 'avg', 3),
(32, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 4),
(32, 'total_plays', '总播放', 'play_count', 'sum', 5),
-- 33: reviewer_quality_audit
(33, 'total_reviews', '总审核数', 'asset_id', 'count', 1),
(33, 'approval_rate', '通过率', 'status', 'avg', 2),
(33, 'avg_review_hours', '平均审核时长(小时)', 'reviewed_at', 'avg', 3),
(33, 'rejection_count', '拒绝数量', 'status', 'count', 4),
-- 34: content_tag_performance
(34, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(34, 'avg_plays_per_tag', '平均播放量', 'play_count', 'avg', 2),
(34, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 3),
-- 35: platform_upload_trend
(35, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(35, 'avg_plays', '平均播放量', 'play_count', 'avg', 2),
(35, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 3),
-- 36: rejected_content_analysis
(36, 'rejection_count', '拒绝数量', 'asset_id', 'count', 1),
-- 37: city_platform_comparison
(37, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(37, 'total_plays', '总播放', 'play_count', 'sum', 2),
(37, 'avg_plays', '平均播放', 'play_count', 'avg', 3),
(37, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 4),
-- 38: uploader_platform_comparison
(38, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(38, 'total_plays', '总播放', 'play_count', 'sum', 2),
(38, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 3),

-- 40: reviewer_workload_summary
(40, 'total_reviews', '总审核数', 'asset_id', 'count', 1),
(40, 'avg_review_hours', '平均审核时长', 'reviewed_at', 'avg', 2),
-- 41: high_performance_content
(41, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(41, 'total_impressions', '总曝光', 'impression_count', 'sum', 2),
(41, 'total_plays', '总播放', 'play_count', 'sum', 3),
(41, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 4),
-- 42: uploader_content_profile
(42, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(42, 'avg_duration', '平均时长', 'duration_seconds', 'avg', 2),
(42, 'total_plays', '总播放', 'play_count', 'sum', 3),
(42, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 4),

-- 44: platform_engagement_metrics
(44, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(44, 'total_impressions', '总曝光', 'impression_count', 'sum', 2),
(44, 'total_plays', '总播放', 'play_count', 'sum', 3),
(44, 'total_complete_plays', '完播总量', 'complete_play_count', 'sum', 4),
(44, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 5),
-- 45: city_platform_content
(45, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(45, 'total_plays', '总播放', 'play_count', 'sum', 2),
(45, 'avg_plays', '平均播放', 'play_count', 'avg', 3),
(45, 'avg_duration', '平均时长', 'duration_seconds', 'avg', 4),
(45, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 5),
-- 46: uploader_performance_summary
(46, 'total_uploads', '总上传数', 'asset_id', 'count', 1),
(46, 'approval_rate', '审核通过率', 'status', 'avg', 2),
(46, 'total_plays', '总播放', 'play_count', 'sum', 3),
(46, 'avg_complete_rate', '内容完播率', 'complete_play_count', 'avg', 4),
-- 47: platform_content_overview
(47, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(47, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 2),
(47, 'total_plays', '总播放', 'play_count', 'sum', 3),
-- 48: reviewer_platform_performance
(48, 'reviewer_reviews', '审核数', 'asset_id', 'count', 1),
(48, 'approval_rate', '通过率', 'status', 'avg', 2),
-- 49: high_play_uploaders
(49, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(49, 'total_plays', '总播放', 'play_count', 'sum', 2),
(49, 'avg_plays', '平均播放', 'play_count', 'avg', 3),
-- 50: platform_time_performance
(50, 'asset_count', '素材数量', 'asset_id', 'count', 1),
(50, 'total_plays', '总播放', 'play_count', 'sum', 2),
(50, 'avg_plays', '平均播放', 'play_count', 'avg', 3),
(50, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 4),
-- 31: uploader_contribution_rank
(31, 'total_uploads', '总上传数', 'asset_id', 'count', 1),
(31, 'total_plays', '总播放', 'play_count', 'sum', 2),
(31, 'avg_complete_rate', '平均完播率', 'complete_play_count', 'avg', 3),
-- 39: daily_upload_trend
(39, 'upload_count', '上传数量', 'asset_id', 'count', 1),
-- 43: platform_approval_rate
(43, 'total_reviews', '总审核数', 'asset_id', 'count', 1),
(43, 'approval_rate', '审核通过率', 'status', 'avg', 2);

-- =============================================
-- 将21个新指标加入对应的指标集合
-- =============================================
INSERT INTO metric_collection_item (collection_id, metric_id, sort_order) VALUES
-- 内容运营指标包 (collection_id=1)
(1, 30, 12), (1, 35, 13), (1, 38, 14), (1, 42, 15), (1, 46, 16), (1, 49, 17), (1, 31, 18), (1, 39, 19),
-- 审核管理指标包 (collection_id=2)
(2, 33, 8), (2, 36, 9), (2, 40, 10), (2, 48, 11), (2, 43, 12),
-- 数据分析指标包 (collection_id=3)
(3, 32, 14), (3, 34, 15), (3, 37, 16), (3, 41, 17), (3, 44, 18), (3, 45, 19), (3, 47, 20), (3, 50, 21),
-- 管理层仪表盘 (collection_id=4)
(4, 30, 8), (4, 41, 9), (4, 44, 10), (4, 49, 11);

-- =============================================
-- 初始化完成
-- =============================================
