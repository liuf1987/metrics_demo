# 指标配置后台服务

## 项目介绍

- **项目名称**：指标配置后台服务
- **功能描述**：面向业务人员的指标配置后台服务，提供指标配置、查询、任务管理等功能，支持 AI 智能推荐指标。

## 技术栈

| 技术 | 版本 |
|------|------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| MyBatis-Plus | 3.5.5 |
| MySQL | 8.0+ |
| Redis | 7.x |
| Springdoc OpenAPI | 2.3.0 |
| Flyway | 10.10.0 |
| MapStruct | 1.5.5 |
| Spring AI | 1.0.0-M4 |
| Lombok | - |

## 项目结构

```
backend/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/metricplatform/
│       │       ├── ai/                  # AI 智能推荐模块
│       │       │   ├── controller/      # AI 控制器
│       │       │   ├── dto/             # AI 数据传输对象
│       │       │   ├── entity/          # AI 实体类
│       │       │   ├── mapper/          # AI Mapper
│       │       │   └── service/         # AI 服务层
│       │       ├── common/              # 通用模块
│       │       │   ├── entity/          # 基础实体类
│       │       │   ├── enums/           # 枚举类
│       │       │   └── exception/       # 异常处理
│       │       ├── config/              # 配置类
│       │       ├── metadata/            # 元数据管理模块
│       │       │   ├── controller/      # 元数据控制器
│       │       │   ├── dto/             # 元数据传输对象
│       │       │   ├── entity/          # 元数据实体类
│       │       │   ├── mapper/          # 元数据 Mapper
│       │       │   └── service/         # 元数据服务层
│       │       ├── metric/              # 指标管理模块
│       │       │   ├── controller/      # 指标控制器
│       │       │   ├── converter/       # 指标转换器
│       │       │   ├── dto/             # 指标数据传输对象
│       │       │   ├── entity/          # 指标实体类
│       │       │   ├── handler/         # 指标类型处理器
│       │       │   ├── mapper/          # 指标 Mapper
│       │       │   └── service/         # 指标服务层
│       │       ├── task/                # 任务管理模块
│       │       │   ├── controller/      # 任务控制器
│       │       │   ├── dto/             # 任务数据传输对象
│       │       │   ├── entity/          # 任务实体类
│       │       │   ├── mapper/          # 任务 Mapper
│       │       │   └── service/         # 任务服务层
│       │       └── MetricPlatformApplication.java  # 启动类
│       └── resources/
│           ├── application.yml         # 配置文件
│           └── db/migration/           # Flyway 迁移脚本
└── pom.xml                             # Maven 配置
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 7.x

### 数据库初始化

本项目使用 Flyway 进行数据库版本管理和自动迁移。

#### Flyway 说明

1. **迁移脚本位置**：`src/main/resources/db/migration/`
2. **迁移脚本命名规则**：`V{版本号}__{描述}.sql`，例如 `V1__init.sql`
3. **执行机制**：
   - 应用启动时，Flyway 自动检查 `flyway_schema_history` 表
   - 按版本号顺序执行未执行的迁移脚本
   - 已执行的脚本不会重复执行，确保幂等性

#### 首次初始化步骤

1. 创建空数据库：
```sql
CREATE DATABASE metric_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```
2. 启动应用，Flyway 自动执行 `V1__init.sql` 完成初始化

#### 添加新迁移脚本

在 `src/main/resources/db/migration/` 目录创建新脚本，命名遵循版本递增规则（如 `V2__add_new_table.sql`），启动应用自动执行。

### 配置文件说明

修改 `src/main/resources/application.yml` 进行配置：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  application:
    name: metric-platform
  
  # MySQL 数据源配置
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/metric_platform?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 30000
      pool-name: MetricPlatformHikariCP
  
  # Flyway 数据库迁移配置
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    sql-migration-prefix: V
    sql-migration-separator: __
    sql-migration-suffixes: .sql
    encoding: UTF-8
    validate-on-migrate: false
    clean-disabled: true
  
  # Redis 配置
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
      database: 0
      timeout: 10000ms
      lettuce:
        pool:
          max-active: 8
          max-wait: -1ms
          max-idle: 8
          min-idle: 0
  
  # Spring AI 配置 - 阿里云百炼
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY:your_api_key}
      base-url: ${DASHSCOPE_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode}
      chat:
        options:
          model: ${DASHSCOPE_MODEL:deepseek-v4-flash}
          temperature: 0.7
          max-tokens: 2000

# 任务队列配置
task:
  queue:
    poll-interval: 1000
    max-retry-count: 3
    queue-key: task:queue

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.example.metricplatform.common.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# SpringDoc OpenAPI (Swagger) 配置
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  group-configs:
    - group: 'default'
      paths-to-match: '/**'
      packages-to-scan: com.example.metricplatform.controller

# 元数据配置
metadata:
  exclude-tables:
    - ai_recommendation
    - ai_chat_history
    - ai_config
    - data_source
    - field_metadata
    - flyway_schema_history
    - metric_category
    - metric_collection
    - metric_collection_item
    - metric_config
    - metric_stat_item
    - task
    - task_execution_result
    - task_metric_item
```

#### Spring AI 阿里云百炼配置

**功能说明**：
- 集成 Spring AI 框架，使用阿里云百炼大模型服务
- 通过 OpenAI 兼容模式调用，支持多种模型
- 提供指标智能推荐和自动生成能力

**API Key 获取步骤**：
1. 访问阿里云百炼控制台：https://dashscope.console.aliyun.com/
2. 注册/登录阿里云账号
3. 进入 API-KEY 管理页面
4. 创建新的 API Key（格式为 `sk-` 开头）
5. 将 API Key 配置到 `application.yml` 或环境变量 `DASHSCOPE_API_KEY`

**支持的模型**：
- `deepseek-v4-flash` - DeepSeek V4 极速版（推荐，性价比高）
- `deepseek-v4-pro` - DeepSeek V4 专业版
- `qwen3.6-flash-2026-04-16` - 通义千问 3.6 极速版
- `qwen3.7-max-2026-05-17` - 通义千问 3.7 高级版

#### 任务队列和执行机制

**架构说明**：
- 基于 Redis List 实现任务队列
- 定时轮询机制消费任务
- 支持失败重试机制

**核心组件**：
1. **TaskQueueService** - 任务队列服务
2. **TaskConsumer** - 任务消费者
3. **TaskExecutor** - 任务执行器

**执行流程**：
```
创建任务 → 存入数据库 → 加入 Redis 队列
    ↓
TaskConsumer 轮询获取任务
    ↓
更新状态为 RUNNING
    ↓
TaskExecutor 执行任务
    ↓
成功 → 更新状态为 SUCCESS，保存结果
失败 → 检查重试次数 → 未超限 → 重新入队
                    → 已超限 → 更新状态为 FAILED
```

**任务状态**：
- `PENDING` - 待执行
- `RUNNING` - 执行中
- `SUCCESS` - 执行成功
- `FAILED` - 执行失败（重试超限）
- `PAUSED` - 已暂停
- `CANCELLED` - 已取消

### 启动命令

```bash
# 编译项目
mvn clean package -DskipTests

# 启动服务
java -jar target/metric-platform-1.0.0.jar

# 或使用 Maven 启动
mvn spring-boot:run
```

服务启动后访问：
- API 文档：http://localhost:8080/api/swagger-ui.html

## API 接口文档

### 指标管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/metrics | 创建指标 |
| PUT | /api/metrics/{id} | 更新指标 |
| DELETE | /api/metrics/{id} | 删除指标 |
| GET | /api/metrics/{code} | 根据编码查询指标详情 |
| GET | /api/metrics | 分页查询指标列表 |
| POST | /api/metrics/{id}/toggle | 启用/停用指标 |
| POST | /api/metrics/{code}/execute | 执行指标查询 |

### 任务管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/tasks | 创建任务 |
| GET | /api/tasks/{code} | 根据编码查询任务状态 |
| GET | /api/tasks | 分页查询任务列表 |
| DELETE | /api/tasks/{code} | 删除任务 |
| POST | /api/tasks/{code}/pause | 暂停定时任务 |
| POST | /api/tasks/{code}/resume | 继续定时任务 |
| POST | /api/tasks/{code}/cancel | 取消定时任务 |
| POST | /api/tasks/{code}/trigger | 手动触发任务 |
| GET | /api/tasks/{code}/status-detail | 查询任务调度状态 |
| GET | /api/tasks/{code}/metrics | 查询任务子指标执行状态 |
| GET | /api/tasks/{code}/executions | 查询任务执行历史 |
| GET | /api/tasks/{code}/executions/page | 分页查询任务执行历史 |
| GET | /api/tasks/{code}/executions/latest | 获取任务最新执行结果 |
| GET | /api/tasks/executions/page | 分页查询所有任务执行历史 |

### 指标集合

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | /api/collections | 查询所有启用的指标集合 |
| GET | /api/collections/{code} | 查询集合详情（含指标列表） |
| GET | /api/collections/role/{role} | 根据角色查询指标集合 |

### AI 推荐

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/ai/query | 查询现有指标 |
| POST | /api/ai/generate | 生成新指标 |
| POST | /api/ai/feedback | 提交指标评价 |
| GET | /api/ai/history | 查询聊天历史 |
| GET | /api/ai/config/check | 检查AI配置 |

### 元数据管理

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | /api/metadata/load | 加载元数据 |
| GET | /api/metadata/datasources | 查询所有数据源 |
| GET | /api/metadata/datasources/{sourceId} | 查询数据源详情 |
| GET | /api/metadata/datasources/{sourceId}/fields | 查询数据源字段 |
| POST | /api/metadata/datasources/{sourceId}/status | 更新数据源状态 |
| POST | /api/metadata/fields/{fieldId}/status | 更新字段状态 |
| PUT | /api/metadata/fields/{fieldId}/properties | 更新字段属性 |
| PUT | /api/metadata/fields/batch/properties | 批量更新字段属性 |

## 响应格式

### 统一响应格式

所有接口返回统一的 JSON 格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | Integer | 状态码，200 表示成功，其他表示失败 |
| message | String | 响应消息 |
| data | T | 响应数据，类型根据具体接口而定 |

### 常见状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 操作成功 |
| 500 | 服务器内部错误 |

## AI 使用说明

本项目在开发过程中使用了 AI 辅助开发工具，主要涉及：
- 代码生成：实体类、DTO、Mapper 接口等
- 配置文件：Spring Boot、MyBatis-Plus、Swagger 等配置
- 接口设计：RESTful API 设计及 Swagger 注解
- 异常处理：全局异常处理器及业务异常类

## 测试示例

```bash
# 分页查询指标列表
curl -X GET "http://localhost:8080/api/metrics?pageNum=1&pageSize=10"

# 查询指标详情
curl -X GET http://localhost:8080/api/metrics/metric_001

# 执行指标查询
curl -X POST http://localhost:8080/api/metrics/metric_001/execute \
  -H "Content-Type: application/json"

# 创建任务
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "taskName": "测试任务",
    "taskType": "QUERY",
    "metricCodes": ["metric_001"],
    "scheduleType": "IMMEDIATE"
  }'

# 查询任务状态
curl -X GET http://localhost:8080/api/tasks/TASK_001

# 查询指标集合列表
curl -X GET http://localhost:8080/api/collections

# 按角色查询指标集合
curl -X GET http://localhost:8080/api/collections/role/admin

# AI 查询现有指标
curl -X POST http://localhost:8080/api/ai/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "查询最近一个月的销售数据"
  }'

# AI 生成新指标
curl -X POST http://localhost:8080/api/ai/generate \
  -H "Content-Type: application/json" \
  -d '{
    "query": "查询最近一个月的销售数据"
  }'

# 加载元数据
curl -X POST http://localhost:8080/api/metadata/load

# 查询所有数据源
curl -X GET http://localhost:8080/api/metadata/datasources
```
