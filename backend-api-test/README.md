# Backend API测试项目

这是一个基于 pytest 的完整集成测试套件，专门用于测试 metric-platform 后端服务的所有 API 接口。

## 项目概述

### 项目简介

本项目是 metric-platform 后端服务的完整集成测试套件，基于 Python pytest 框架构建，覆盖了指标管理、元数据管理、任务管理和指标集合管理等核心功能模块的接口测试。

### 项目用途

- **API接口验证**：确保后端所有接口的正常功能、边界条件和异常处理逻辑符合预期
- **回归测试**：在代码变更后快速验证现有功能是否正常工作，防止回归问题
- **接口契约验证**：验证 API 接口的响应格式、状态码和数据结构符合契约规范
- **持续集成**：可集成到 CI/CD 流程中，在每次部署前自动运行测试
- **质量保障**：作为 metric-platform 后端服务质量保障体系的重要组成部分

### 项目特性

- ✅ **全功能覆盖**：覆盖指标、元数据、任务、指标集合、AI指标推荐五大核心模块的所有接口
- ✅ **结构化设计**：采用分层架构（API封装层、测试用例层、工具层），代码清晰易维护
- ✅ **数据库断言**：支持直接连接数据库验证数据变更的正确性
- ✅ **HTML测试报告**：支持生成美观的 HTML 格式测试报告
- ✅ **灵活配置**：支持通过配置文件灵活调整测试环境
- ✅ **独立测试用例**：每个测试用例独立运行，互不影响
- ✅ **自动数据清理**：测试完成后自动清理测试数据，不污染环境
- ✅ **完整的错误码覆盖**：覆盖所有主要业务异常码的测试
- ✅ **AI智能测试**：包含AI指标推荐和生成功能的完整测试覆盖
- ✅ **便捷入口**：提供main.py作为统一测试执行入口，简化使用流程

## 项目架构

本项目采用清晰的分层架构设计，各层职责明确，便于维护和扩展：

```
┌─────────────────────────────────────────────────────────┐
│                    测试用例层 (tests/)                    │
│  - 按业务模块组织测试文件                                 │
│  - 包含正常流程测试和异常流程测试                         │
│  - 遵循 AAA (Arrange-Act-Assert) 模式                    │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   API 封装层 (libs/)                      │
│  - 对后端 REST API 进行封装                               │
│  - 提供统一的 Result 返回对象                             │
│  - 自动记录请求日志                                       │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   公共工具层 (common/)                    │
│  - models: Result 响应模型封装                            │
│  - utils: 数据库工具、数据生成工具                        │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                   配置层 (config/)                        │
│  - settings: 全局配置（API地址、数据库连接等）             │
│  - log_config: 日志配置                                  │
└─────────────────────────────────────────────────────────┘
```

### 设计原则

1. **分层清晰**：API 封装、测试用例、工具函数各司其职
2. **独立测试**：每个测试用例独立运行，互不影响
3. **自动清理**：测试后自动重置数据库，不污染环境
4. **配置灵活**：通过配置文件灵活调整测试环境

## 功能模块

### 1. 指标管理模块 (Metrics)

**API 封装文件**：`libs/metrics/metric_api.py`

**功能描述**：
- 指标的创建、更新、删除
- 指标查询（按编码查询、分页列表查询）
- 指标启用/停用状态切换
- 指标执行查询

**主要接口**：
- `create_metric()` - 创建指标
- `update_metric()` - 更新指标
- `delete_metric()` - 删除指标
- `get_metric_by_code()` - 按编码查询指标详情
- `list_metrics()` - 分页查询指标列表
- `toggle_metric()` - 启用/停用指标
- `execute_metric()` - 执行指标查询

**测试文件**：`tests/metrics/`

---

### 2. 元数据管理模块 (Metadata)

**API 封装文件**：`libs/metadata/metadata_api.py`

**功能描述**：
- 元数据加载
- 数据源管理（查询、状态更新）
- 字段管理（查询、状态更新、属性设置）
- 批量更新字段属性

**主要接口**：
- `load_metadata()` - 加载元数据
- `list_data_sources()` - 查询所有数据源
- `get_data_source_detail()` - 查询数据源详情
- `get_fields_by_source()` - 查询数据源字段列表
- `update_data_source_status()` - 更新数据源状态
- `update_field_status()` - 更新字段状态
- `update_field_properties()` - 更新字段属性
- `batch_update_field_properties()` - 批量更新字段属性

**测试文件**：`tests/metadata/`

---

### 3. 任务管理模块 (Tasks)

**API 封装文件**：`libs/tasks/task_api.py`

**功能描述**：
- 任务创建和删除
- 任务生命周期管理（暂停、继续、取消、触发）
- 任务状态查询
- 任务执行历史查询

**主要接口**：
- `create_task()` - 创建任务
- `get_task_by_code()` - 按编码查询任务
- `list_tasks()` - 分页查询任务列表
- `delete_task()` - 删除任务
- `pause_task()` - 暂停任务
- `resume_task()` - 继续任务
- `cancel_task()` - 取消任务
- `trigger_task()` - 手动触发任务
- `get_task_status_detail()` - 查询任务状态详情
- `get_task_metrics()` - 查询任务子指标执行状态
- `get_task_executions()` - 查询任务执行历史
- `get_task_executions_page()` - 分页查询任务执行历史
- `get_task_execution_latest()` - 获取任务最新执行结果
- `list_all_executions()` - 分页查询所有任务执行历史

**测试文件**：`tests/tasks/`

---

### 4. 指标集合模块 (Collections)

**API 封装文件**：`libs/collections/collection_api.py`

**功能描述**：
- 指标集合查询
- 按角色查询指标集合

**主要接口**：
- `list_collections()` - 查询所有指标集合
- `get_collection_detail()` - 查询集合详情
- `list_collections_by_role()` - 按角色查询集合

**测试文件**：`tests/collections/`

---

### 5. AI 指标推荐模块 (AI)

**API 封装文件**：`libs/ai/ai_api.py`

**功能描述**：
- 查询现有指标（根据用户查询词推荐）
- 生成新指标（根据用户描述自动创建）
- 提交指标评价（收集用户反馈）
- 查询聊天历史
- 检查 AI 配置

**主要接口**：
- `query_metrics()` - 查询现有指标
- `generate_metric()` - 生成新指标
- `submit_feedback()` - 提交指标评价
- `get_history()` - 查询聊天历史
- `check_config()` - 检查 AI 配置

**测试文件**：`tests/ai/`

**测试用例**：
- `test_ai_normal.py`：AI 正常流程测试
  - 查询现有指标的多个场景测试（平台效果概览、平台素材统计、完播率分析等）
  - 聊天历史查询测试
  - AI 配置检查测试
  - 生成指标的多个场景测试（统计平均文件大小、平均视频时长、审核通过内容、播放量统计等）
- `test_ai_exception.py`：AI 异常流程测试
  - 各种异常场景和错误码的验证

## 技术栈

### 核心框架

| 技术 | 版本 | 用途 |
|------|------|------|
| Python | 3.x | 主要开发语言 |
| pytest | 7.4.0 | 测试框架 |
| requests | 2.31.0 | HTTP 请求库 |

### 测试报告

| 技术 | 版本 | 用途 |
|------|------|------|
| pytest-html | 4.0.2 | 生成 HTML 测试报告 |
| allure-pytest | 2.13.2 | 生成 Allure 测试报告 |

### 数据库

| 技术 | 版本 | 用途 |
|------|------|------|
| SQLAlchemy | 2.0.20 | ORM 框架 |
| mysql-connector-python | 8.0.33 | MySQL 数据库驱动 |

### 其他工具

| 技术 | 版本 | 用途 |
|------|------|------|
| python-dotenv | 1.0.0 | 环境变量管理 |
| pytest-timeout | 2.1.0 | 测试超时控制 |

## 项目结构

```
backend-api-test/
├── common/              # 公共工具模块
│   ├── models/          # 数据模型（Result响应封装等）
│   └── utils/           # 工具类（数据、数据库等）
├── config/              # 配置文件
│   ├── log_config.py    # 日志配置
│   └── settings.py      # 全局配置
├── libs/                # API封装层
│   ├── metrics/         # 指标管理API
│   ├── metadata/        # 元数据管理API
│   ├── tasks/           # 任务管理API
│   ├── collections/     # 指标集合API
│   └── ai/              # AI指标推荐API
├── tests/               # 测试用例目录
│   ├── metrics/         # 指标管理测试
│   ├── metadata/        # 元数据管理测试
│   ├── tasks/           # 任务管理测试
│   ├── collections/     # 指标集合测试
│   └── ai/              # AI指标推荐测试
├── logs/                # 日志目录
├── report/              # 测试报告目录
├── conftest.py          # pytest配置和fixture定义
├── main.py              # 测试执行入口
├── pytest.ini           # pytest配置文件
├── requirements.txt     # 依赖声明
└── README.md            # 项目说明
```

### 核心文件说明

#### conftest.py
pytest 配置文件，定义了全局 fixtures 和测试钩子：
- **api_client fixture**：会话级 fixture，确保报告和日志目录存在
- **cleanup_test_data fixture**：函数级 fixture，用于测试数据清理（预留扩展）
- **db_reset fixture**：自动执行的 fixture，测试后执行数据库重置和元数据重新加载
- **pytest_sessionfinish hook**：整个测试会话结束时的钩子，自动重置数据库至初始状态

#### main.py
测试执行入口脚本，提供了便捷的 pytest 调用方式，支持常用的命令行参数说明：
- 简化了测试执行，直接运行 `python main.py` 即可执行所有测试
- 包含详细的 pytest 常用参数说明

## 快速开始

### 1. 安装依赖

```bash
pip install -r requirements.txt
```

### 2. 配置环境

修改 `config/settings.py` 文件，配置后端服务地址和数据库连接：

```python
# API基础地址
BASE_URL = "http://localhost:8080/api"

# 数据库配置
DATABASE_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "database": "metric_platform",
    "username": "root",
    "password": "password"
}
```

### 3. 运行测试

#### 方式一：使用 main.py（推荐）⭐

`main.py` 是项目提供的统一测试执行入口，使用简单方便，推荐作为首选方式：

```bash
# 运行所有测试（推荐）
python main.py

# 详细输出模式（显示每个测试用例的执行信息）
python main.py -v

# 显示print输出（调试时使用）
python main.py -s

# 简化输出信息
python main.py -q

# 遇到失败直接停止
python main.py -x

# 只运行上次失败的用例
python main.py --lf

# 先运行上次失败的用例，再运行其他用例
python main.py --ff

# 允许最多失败N次后停止
python main.py --maxfail=3

# 清除缓存后运行
python main.py --cache-clear

# 运行指定模块
python main.py tests/ai/

# 运行指定测试文件
python main.py tests/ai/test_ai_normal.py

# 运行指定测试类
python main.py tests/ai/test_ai_normal.py::TestAiNormal

# 运行指定测试函数
python main.py tests/ai/test_ai_normal.py::TestAiNormal::test_query_metrics_200

# 组合使用多个参数
python main.py -v -s tests/ai/
```

#### 方式二：直接使用 pytest

如果您熟悉 pytest，也可以直接使用 pytest 命令，参数与 main.py 完全相同：

```bash
# 运行所有测试
pytest

# 运行指定模块
pytest tests/ai/

# 运行指定测试文件
pytest tests/ai/test_ai_normal.py

# 运行指定测试类
pytest tests/ai/test_ai_normal.py::TestAiNormal

# 运行指定测试函数
pytest tests/ai/test_ai_normal.py::TestAiNormal::test_query_metrics_200

# 详细输出
pytest -v

# 显示print输出
pytest -s

# 只运行失败的用例
pytest --lf

# 先运行失败的用例
pytest --ff

# 遇到失败停止
pytest -x

# 生成HTML报告（指定路径）
pytest --html=./report/index.html

# 生成自包含HTML报告（方便分享）
pytest --html=./report/index.html --self-contained-html

# 组合参数
pytest -v -s tests/ai/test_ai_normal.py --lf
```

### 实用技巧

#### 1. 快速调试单个用例

当开发或修复测试时，只运行特定的测试用例可以大大提高效率：

```bash
# 运行单个测试函数并显示详细输出
python main.py -v -s tests/ai/test_ai_normal.py::TestAiNormal::test_query_metrics_200
```

#### 2. 重新运行失败的用例

修改代码后，只需重新运行上次失败的用例即可快速验证修复：

```bash
# 只运行上次失败的用例
python main.py --lf

# 先运行失败的，再运行其他
python main.py --ff
```

#### 3. 按模块分组测试

可以按业务模块分组运行测试，方便针对特定功能进行验证：

```bash
# 运行AI模块测试
python main.py tests/ai/

# 运行指标管理模块测试
python main.py tests/metrics/

# 运行元数据管理模块测试
python main.py tests/metadata/

# 运行任务管理模块测试
python main.py tests/tasks/

# 运行指标集合管理模块测试
python main.py tests/collections/
```

#### 4. 调试模式运行

在调试时，建议同时使用 `-v` 和 `-s` 参数，以便看到详细的执行信息和 print 输出：

```bash
python main.py -v -s
```

#### 5. 按标记运行测试（如果配置了markers）

如果测试用例配置了标记，可以按标记筛选运行：

```bash
# 查看所有可用标记
pytest --markers

# 按标记运行（示例，需在pytest.ini中配置）
# pytest -m smoke
```

#### 6. 并行运行测试（提高速度）

如果安装了 pytest-xdist，可以并行运行测试（需要先安装）：

```bash
# 安装 pytest-xdist
# pip install pytest-xdist

# 使用4个进程并行运行
# pytest -n 4
```

#### 7. 测试覆盖率检查（可选）

如果需要检查测试覆盖率（需要先安装 pytest-cov）：

```bash
# 安装 pytest-cov
# pip install pytest-cov

# 生成覆盖率报告
# pytest --cov=. --cov-report=html
```

## 测试用例编写规范

### 测试文件命名
- 测试文件必须以 `test_` 开头
- 文件名应清晰反映测试模块

### 测试函数命名
- 函数名必须以 `test_` 开头
- 函数名应包含：操作 + 场景 + 期望状态码

```python
def test_create_metric_200():      # 创建指标成功
def test_create_metric_3001():     # 创建指标-编码重复
```

### 测试类命名
- 类名必须以 `Test` 开头
- 类用于组织相关的测试用例

### 测试用例结构（AAA模式）

```python
def test_create_metric_200():
    # 1. Arrange（准备）- 设置测试数据和环境
    metric_code = "test_metric_xxx"
    
    # 2. Act（执行）- 调用被测API
    result = MetricApi.create_metric(data)
    
    # 3. Assert（断言）- 验证结果
    assert result.code == 200
```

## 响应封装

所有API响应使用 `Result` 类封装：

```python
from common.models.result import Result

result = MetricApi.get_metric_by_code("asset_count")
assert result.code == 200
assert result.data is not None
```

## 异常码说明

| 异常码 | 说明 | 模块 |
|--------|------|------|
| 200 | 成功 | 通用 |
| 1001 | 参数验证失败 | 通用 |
| 1002 | 必填参数为空 | 通用 |
| 2101 | 数据源不存在 | 元数据 |
| 2102 | 字段不存在 | 元数据 |
| 3001 | 指标编码已存在 | 指标 |
| 3002 | 源表名不能为空 | 指标 |
| 3003 | 数据源不存在 | 指标 |
| 3102 | 指标不存在(按ID) | 指标 |
| 3103 | 指标不存在(按Code) | 指标 |
| 4001 | 指标不存在 | 查询引擎 |
| 4002 | 指标已停用 | 查询引擎 |
| 5103 | 任务不存在 | 任务 |
| 6102 | 指标集合不存在 | 集合 |

## 测试报告

本项目支持两种测试报告格式：HTML 报告和 Allure 报告。两种报告各有优势，可以根据需要选择使用。

### HTML 报告

HTML 报告使用 `pytest-html` 插件生成，是一个轻量级的、易于查看的测试报告格式。

#### 生成方式

**方式一：默认生成（推荐）**

由于 `pytest.ini` 中已配置默认生成 HTML 报告，直接运行测试即可自动生成：

```bash
# 运行所有测试并自动生成 HTML 报告
python main.py

# 或直接使用 pytest
pytest
```

**方式二：手动指定报告路径**

```bash
# 生成 HTML 报告到指定路径
pytest --html=./report/index.html
```

**方式三：合并 CSS 内联模式**

```bash
# 生成包含 CSS 的单文件报告（方便分享）
pytest --html=./report/index.html --self-contained-html
```

#### 查看报告

报告生成后位于 `./report/index.html`，可以直接使用浏览器打开：

```bash
# Windows
start report/index.html

# macOS
open report/index.html

# Linux
xdg-open report/index.html
```

#### HTML 报告包含以下内容：
- 测试执行概览（通过/失败/跳过数量）
- 每个测试用例的执行状态
- 失败用例的详细错误信息和堆栈跟踪
- 测试执行时间统计
- 环境信息

---

### Allure 报告

Allure 报告使用 `allure-pytest` 插件生成，是一个功能强大的、交互式的测试报告框架，提供丰富的可视化功能和友好的用户界面。

#### 前置条件

使用 Allure 报告需要先安装 Allure 命令行工具：

**Windows 安装（推荐使用 Scoop）**
```bash
# 安装 Scoop（如果未安装）
Set-ExecutionPolicy RemoteSigned -Scope CurrentUser
Invoke-RestMethod -Uri https://get.scoop.sh | Invoke-Expression

# 安装 Allure
scoop install allure
```

**macOS 安装**
```bash
brew install allure
```

**Linux 安装**
```bash
# 下载并解压 Allure
# 访问 https://github.com/allure-framework/allure2/releases 下载最新版本
# 解压后将 bin 目录添加到 PATH
```

#### 生成方式

**方式一：默认生成（推荐）**

由于 `pytest.ini` 中已配置默认生成 Allure 原始数据，直接运行测试即可：

```bash
# 运行所有测试并自动生成 Allure 原始数据
python main.py

# 或直接使用 pytest
pytest
```

测试执行完成后，Allure 原始数据会保存到 `./report/allure` 目录。

**方式二：手动指定原始数据目录**

```bash
# 生成 Allure 原始数据到指定目录
pytest --alluredir=./report/allure
```

#### 查看报告

**方式一：启动 Allure 服务（推荐）**

使用 Allure 命令行工具启动一个本地服务器查看报告：

```bash
# 生成并打开 Allure 报告
allure serve ./report/allure
```

这会自动打开默认浏览器显示 Allure 报告。

**方式二：生成静态报告文件**

```bash
# 生成静态报告文件到指定目录
allure generate ./report/allure -o ./report/allure-report --clean

# 打开生成的报告
allure open ./report/allure-report
```

#### Allure 报告功能

Allure 报告提供了丰富的功能：
- 精美的仪表板展示测试概览
- 按模块、标签、功能等多维度分类展示
- 测试用例执行历史趋势图
- 详细的测试步骤和附件
- 失败用例的截图和日志
- 测试环境信息展示
- 交互式的图表和统计分析

---

### 两种报告对比

| 特性 | HTML 报告 | Allure 报告 |
|------|-----------|-------------|
| **安装复杂度** | 简单，只需安装 pytest-html 插件 | 需要安装 Allure 命令行工具 |
| **生成速度** | 快，直接生成 HTML 文件 | 先生成原始数据，再生成报告 |
| **查看方式** | 直接用浏览器打开 HTML 文件 | 需要启动服务或生成静态文件 |
| **界面美观度** | 简洁实用 | 精美现代 |
| **交互性** | 基础 | 强大（筛选、搜索、图表交互） |
| **历史趋势** | 不支持 | 支持（需要历史数据） |
| **多维度分析** | 有限 | 丰富（模块、标签、功能等） |
| **附件支持** | 有限 | 支持截图、日志等附件 |
| **分享便捷性** | 单文件分享方便 | 需要分享整个目录 |
| **适用场景** | 快速查看、CI/CD 集成 | 详细分析、项目管理 |

#### 选择建议

- **使用 HTML 报告的场景**：
  - 需要快速查看测试结果
  - CI/CD 流程中需要简单的报告展示
  - 需要方便地分享单个文件报告
  - 团队对报告要求相对简单

- **使用 Allure 报告的场景**：
  - 需要详细的测试分析和可视化
  - 需要查看测试历史趋势
  - 需要多维度的测试结果分类
  - 需要附加截图、日志等附件信息
  - 项目管理和质量汇报

---

## 注意事项

1. 确保后端服务已启动并运行在配置的端口上
2. 测试数据库需要有测试数据
3. 测试用例执行顺序不影响结果（每个用例独立）
4. 测试完成后会自动清理测试数据
