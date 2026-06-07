#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metrics_normal.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 指标管理正常流程测试

import pytest
import uuid

from pymysql.constants.FIELD_TYPE import NULL

from libs.metrics.metric_api import MetricApi
from libs.metadata.metadata_api import MetadataApi
from common.utils.datautil import DataUtil
from config import log_config

log = log_config.get_logger(__file__)

# 创建指标测试用例数组
CREATE_METRIC_TEST_CASES = [
    {
        "name": "按平台统计素材数量",
        "create_data": {
            "metricName": "按平台统计素材数量",
            "metricDesc": "统计各平台的素材数量",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["platform"],
            "filterConditions": {
                "conditions": [{"field": "status", "operator": "=", "value": "approved", "paramName": "status"}],
                "logic": "AND"
            },
            "sortRules": [{"field": "asset_count", "order": "desc", "paramName": "sort_1"}],
            "statItems": [{"fieldName": "asset_id", "aggregation": "count", "itemCode": "asset_count", "itemName": "素材数量"}],
            "enabled": True
        }
    },
    {
        "name": "统计平均播放量按上传人分组",
        "create_data": {
            "metricName": "统计平均播放量按上传人分组",
            "metricDesc": "按上传人分组统计平均播放量",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["uploader"],
            "sortRules": [{"field": "avg_play", "order": "desc", "paramName": "sort_1"}],
            "statItems": [{"fieldName": "play_count", "aggregation": "avg", "itemCode": "avg_play", "itemName": "平均播放量"}],
            "enabled": True
        }
    },
    {
        "name": "按城市和平台统计内容数量",
        "create_data": {
            "metricName": "按城市和平台统计内容数量",
            "metricDesc": "统计各城市和平台的内容数量",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["city", "platform"],
            "sortRules": [{"field": "content_count", "order": "desc", "paramName": "sort_1"}],
            "statItems": [{"fieldName": "asset_id", "aggregation": "count", "itemCode": "content_count", "itemName": "内容数量"}],
            "enabled": True
        }
    },
    {
        "name": "各上传人平均文件大小",
        "create_data": {
            "metricCode": "avg_file_size_by_uploader",
            "metricName": "各上传人平均文件大小",
            "metricDesc": "统计各上传人的平均文件大小，按文件大小降序排列",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["uploader"],
            "filterConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "havingConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "sortRules": [
                {
                "field": "avg_file_size",
                "order": "desc",
                "paramName": "sort_by_avg_size"
                }
            ],
            "limitValue": 100,
            "windowFunctions": {
                "functions": []
            },
            "statItems": [
                {
                "fieldName": "file_size_bytes",
                "aggregation": "avg",
                "itemCode": "avg_file_size",
                "itemName": "平均文件大小"
                }
            ],
            "enabled": True
        }
    },
    {
        "name": "各平台平均视频时长",
        "create_data": {
            "metricCode": "avg_video_duration_by_platform",
            "metricName": "各平台平均视频时长",
            "metricDesc": "统计各投放平台的平均视频时长，并按平均时长降序排列",
            "sourceTable": "asset",
            "categoryCode": "platform_analysis",
            "categoryName": "平台分析",
            "groupByFields": [
                "platform"
            ],
            "filterConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "havingConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "sortRules": [
                {
                "field": "avg_duration",
                "order": "desc",
                "paramName": "sort_by_avg_duration"
                }
            ],
            "limitValue": 100,
            "statItems": [
                {
                "fieldName": "duration_seconds",
                "aggregation": "avg",
                "itemCode": "avg_duration",
                "itemName": "平均视频时长"
                }
            ],
            "enabled": True
        }
    },
    {
        "name": "按上传人统计已审核通过内容数Top20",
        "create_data": {
            "metricCode": "approved_asset_count_by_uploader_top20",
            "metricName": "按上传人统计已审核通过内容数Top20",
            "metricDesc": "统计已审核通过的内容数量，按上传人分组，取数量前20名",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["uploader"],
            "filterConditions": {
                "conditions": [
                {
                    "field": "status",
                    "operator": "=",
                    "value": "approved",
                    "paramName": "filter_status"
                }
                ],
                "logic": "AND"
            },
            "havingConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "sortRules": [
                {
                "field": "total_count",
                "order": "desc",
                "paramName": "sort_by_count"
                }
            ],
            "limitValue": 20,
            "windowFunctions": {
                "functions": []
            },
            "statItems": [
                {
                "fieldName": "asset_id",
                "aggregation": "count",
                "itemCode": "total_count",
                "itemName": "已审核通过内容数"
                }
            ],
            "enabled": True
        }
    },
    {
        "name": "按平台和状态统计总播放量",
        "create_data": {
            "metricCode": "total_play_count_by_platform_status",
            "metricName": "按平台和状态统计总播放量",
            "metricDesc": "按投放平台和审核状态分组，统计已审核通过内容的总播放量",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["platform", "status"],
            "filterConditions": {
                "conditions": [
                {
                    "field": "status",
                    "operator": "=",
                    "value": "approved",
                    "paramName": "filter_status"
                }
                ],
                "logic": "AND"
            },
            "havingConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "sortRules": [
                {
                "field": "total_play_count",
                "order": "desc",
                "paramName": "sort_by_play_count"
                }
            ],
            "limitValue": 100,
            "statItems": [
                {
                "fieldName": "play_count",
                "aggregation": "sum",
                "itemCode": "total_play_count",
                "itemName": "总播放量"
                }
            ],
            "enabled": True
        }
    },
    {
        "name": "上传人素材统计",
        "create_data": {
            "metricCode": "uploader_asset_stats",
            "metricName": "上传人素材统计",
            "metricDesc": "统计上传人的素材数量、总播放量和平均完播率，按完播率降序排列",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["uploader"],
            "filterConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "havingConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "sortRules": [
                {
                "field": "avg_completion_rate",
                "order": "desc",
                "paramName": "sort_by_completion_rate"
                }
            ],
            "limitValue": 100,
            "windowFunctions": {
                "functions": []
            },
            "statItems": [
                {
                "fieldName": "asset_id",
                "aggregation": "count",
                "itemCode": "asset_count",
                "itemName": "素材数量"
                },
                {
                "fieldName": "play_count",
                "aggregation": "sum",
                "itemCode": "total_play_count",
                "itemName": "总播放量"
                },
                {
                "fieldName": "complete_play_count",
                "aggregation": "avg",
                "itemCode": "avg_completion_rate",
                "itemName": "平均完播率"
                }
            ],
            "enabled": True
        }
    },
    {
        "name": "各平台完播率大于50%的素材数量",
        "create_data": {
            "metricCode": "asset_count_by_completion_rate_platform",
            "metricName": "各平台完播率大于50%的素材数量",
            "metricDesc": "统计各平台完播率大于50%的素材数量，按完播率降序排列",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": [
                "platform"
            ],
            "filterConditions": {
                "conditions": [
                {
                    "field": "complete_play_count / play_count",
                    "operator": ">",
                    "value": 0.5,
                    "paramName": "filter_completion_rate"
                },
                {
                    "field": "play_count",
                    "operator": ">",
                    "value": 0,
                    "paramName": "filter_play_count_positive"
                },
                {
                    "field": "deleted",
                    "operator": "=",
                    "value": 0,
                    "paramName": "filter_deleted"
                }
                ],
                "logic": "AND"
            },
            "sortRules": [
                {
                "field": "complete_play_count / play_count",
                "order": "desc",
                "paramName": "sort_completion_rate"
                }
            ],
            "limitValue": 100,
            "statItems": [
                {
                "fieldName": "asset_id",
                "aggregation": "count",
                "itemCode": "asset_count",
                "itemName": "素材数量",
                "sortOrder": 0
                }
            ],
            "enabled": True
        }
    },
    {
        "name": "按上传人分组的素材播放量排名",
        "create_data": {
            "metricCode": "asset_rank_by_uploader_play",
            "metricName": "按上传人分组的素材播放量排名",
            "metricDesc": "按上传人分组，对每个上传人的素材按播放量从高到低排名，显示排名",
            "sourceTable": "asset",
            "categoryCode": "asset_operation",
            "categoryName": "素材运营",
            "groupByFields": [],
            "filterConditions": {
                "conditions": [
                {
                    "field": "deleted",
                    "operator": "=",
                    "value": 0,
                    "paramName": "filter_deleted"
                }
                ],
                "logic": "AND"
            },
            "havingConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "sortRules": [
                {
                "field": "play_rank",
                "order": "asc",
                "paramName": "sort_by_rank"
                }
            ],
            "limitValue": 100,
            "windowFunctions": {
                "functions": [
                {
                    "name": "row_number",
                    "partition_by": ["uploader"],
                    "order_by": [
                    {
                        "field": "play_count",
                        "direction": "DESC",
                        "paramName": "window_sort_play"
                    }
                    ],
                    "item_name": "play_rank"
                }
                ]
            },
            "statItems": [
                {
                "fieldName": "asset_id",
                "aggregation": "max",
                "itemCode": "asset_id",
                "itemName": "素材ID"
                },
                {
                "fieldName": "uploader",
                "aggregation": "max",
                "itemCode": "uploader",
                "itemName": "上传人"
                },
                {
                "fieldName": "title",
                "aggregation": "max",
                "itemCode": "title",
                "itemName": "素材标题"
                },
                {
                "fieldName": "play_count",
                "aggregation": "max",
                "itemCode": "play_count",
                "itemName": "播放次数"
                }
            ],
            "enabled": True
        }
    }
]

# 执行指标测试用例数组（使用已有的指标编码）
EXECUTE_METRIC_TEST_CASES = [
    {
        "name": "执行 asset_count_by_status",
        "metric_code": "asset_count_by_status",
        "params": {"status": "approved"}
    },
    {
        "name": "执行 platform_content_summary",
        "metric_code": "platform_content_summary",
        "params": {}
    },
    {
        "name": "执行 uploader_content_profile",
        "metric_code": "uploader_content_profile",
        "params": {}
    }
]


class TestMetricsNormal:
    """指标管理正常流程测试"""

    def test_list_metrics_200(self, db_reset):
        """分页查询指标列表成功"""
        result = MetricApi.list_metrics(page_num=1, page_size=10)

        assert result.code == 200
        assert result.data is not None
        assert 'records' in result.data
        assert 'total' in result.data

    def test_get_metric_by_code_200(self, db_reset):
        """根据编码查询指标详情成功"""
        list_result = MetricApi.list_metrics()
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0

        first_metric = list_result.data['records'][0]
        metric_code = first_metric.get('metricCode')

        result = MetricApi.get_metric_by_code(metric_code)

        assert result.code == 200
        assert result.data.get('metricCode') == metric_code

    def test_create_metric_200(self, db_reset):
        """创建指标成功"""
        unique_code = f"test_metric_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "测试指标",
            "sourceTable": "asset",
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "total_count",
                    "itemName": "总数"
                }
            ],
            "filterConditions": {
                "conditions": [
                    {
                        "field": "status",
                        "operator": "=",
                        "value": "approved",
                        "paramName": "filter_status"
                    }
                ],
                "logic": "AND"
            },
            "groupByFields": ["status"],
            "sortRules": [
                {
                    "field": "total_count",
                    "order": "desc",
                    "paramName": "sort_order"
                }
            ],
            "enabled": True
        }

        result = MetricApi.create_metric(create_data)

        assert result.code == 200
        assert result.data.get('metricCode') == unique_code
        assert result.data.get('metricName') == "测试指标"

    def test_update_metric_200(self, db_reset):
        """更新指标成功"""
        list_result = MetricApi.list_metrics()
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0

        first_metric = list_result.data['records'][0]
        metric_id = first_metric.get('id')
        original_name = first_metric.get('metricName')

        update_data = {
            "metricName": f"{original_name}_updated",
            "metricDesc": "更新后的描述"
        }

        result = MetricApi.update_metric(metric_id, update_data)

        assert result.code == 200
        assert result.data.get('metricName') == f"{original_name}_updated"
        assert result.data.get('metricDesc') == "更新后的描述"

    def test_delete_metric_200(self, db_reset):
        """删除指标成功"""
        unique_code = f"test_delete_metric_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "待删除测试指标",
            "sourceTable": "asset",
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "总数"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        assert create_result.code == 200
        metric_id = create_result.data.get('id')

        delete_result = MetricApi.delete_metric(metric_id)

        assert delete_result.code == 200

        get_result = MetricApi.get_metric_by_code(unique_code)
        assert get_result.code == 3102

    def test_toggle_metric_200(self, db_reset):
        """启用/停用指标成功"""
        list_result = MetricApi.list_metrics()
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0

        first_metric = list_result.data['records'][0]
        metric_id = first_metric.get('id')
        original_enabled = first_metric.get('enabled')

        toggle_result = MetricApi.toggle_metric(metric_id, not original_enabled)

        assert toggle_result.code == 200
        assert toggle_result.data.get('enabled') == (not original_enabled)

        toggle_back_result = MetricApi.toggle_metric(metric_id, original_enabled)
        assert toggle_back_result.code == 200
        assert toggle_back_result.data.get('enabled') == original_enabled

    def test_execute_all_metrics_200(self, db_reset):
        """遍历执行所有可查询的指标"""
        # 从API获取所有指标列表
        list_result = MetricApi.list_metrics(page_num=1, page_size=1000)
        assert list_result.code == 200
        
        metrics = list_result.data.get('records', [])
        assert len(metrics) > 0, "数据库中没有可用的指标"
        
        log.info(f"找到 {len(metrics)} 个指标，开始遍历执行")
        
        # 遍历执行每个指标
        success_count = 0
        failed_metrics = []
        
        for metric in metrics:
            metric_code = metric.get('metricCode')
            metric_name = metric.get('metricName')
            
            try:
                result = MetricApi.execute_metric(metric_code)
                
                if result.code == 200:
                    success_count += 1
                    log.info(f"✓ 指标 [{metric_code}] ({metric_name}) 执行成功")
                    assert isinstance(result.data, list), f"指标 {metric_code} 返回的数据不是列表类型"
                else:
                    failed_metrics.append({
                        'code': metric_code,
                        'name': metric_name,
                        'error': f"返回码: {result.code}, 消息: {result.message}"
                    })
                    log.warning(f"✗ 指标 [{metric_code}] ({metric_name}) 执行失败: {result.message}")
            except Exception as e:
                failed_metrics.append({
                    'code': metric_code,
                    'name': metric_name,
                    'error': str(e)
                })
                log.error(f"✗ 指标 [{metric_code}] ({metric_name}) 执行异常: {e}")
        
        # 输出执行结果摘要
        log.info(f"\n{'='*60}")
        log.info(f"执行结果摘要:")
        log.info(f"总指标数: {len(metrics)}")
        log.info(f"成功: {success_count}")
        log.info(f"失败: {len(failed_metrics)}")
        
        if failed_metrics:
            log.warning(f"\n失败的指标列表:")
            for failed in failed_metrics:
                log.warning(f"  - {failed['code']} ({failed['name']}): {failed['error']}")
        
        log.info(f"{'='*60}\n")
        
        # 断言所有指标都执行成功
        assert len(failed_metrics) == 0, f"以下指标执行失败: {failed_metrics}"
        assert success_count == len(metrics), f"只有 {success_count}/{len(metrics)} 个指标执行成功"

    def test_execute_metric_with_params_200(self, db_reset):
        """执行带参数的指标查询成功"""
        params = {
            "params": {
                "status": "approved"
            }
        }
        result = MetricApi.execute_metric("asset_count_by_status", params)

        assert result.code == 200
        assert isinstance(result.data, list)

    def test_create_update_query_full_metric_200(self, db_reset):
        """创建、修改、查询完整指标（包含所有条件）"""
        # ========== 步骤1: 创建完整指标 ==========
        unique_code = f"test_full_metric_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "完整指标测试",
            "metricDesc": "包含所有指标规则条件的测试指标",
            "sourceTable": "asset",
            "categoryCode": "test_category",
            "categoryName": "测试分类",
            # 分组维度（使用实际存在的字段：status, platform）
            "groupByFields": ["status", "platform"],
            # 筛选条件（使用实际存在的字段：uploaded_at, status）
            "filterConditions": {
                "conditions": [
                    {
                        "field": "uploaded_at",
                        "operator": ">=",
                        "value": "2026-05-01",
                        "paramName": "filter_start_date"
                    },
                    {
                        "field": "status",
                        "operator": "IN",
                        "value": ["approved", "pending"],
                        "paramName": "filter_status"
                    }
                ],
                "logic": "AND"
            },
            # HAVING条件（基于统计项）
            "havingConditions": {
                "conditions": [
                    {
                        "field": "total_count",
                        "operator": ">",
                        "value": 1,
                        "paramName": "having_min_count"
                    }
                ],
                "logic": "AND"
            },
            # 排序规则
            "sortRules": [
                {
                    "field": "total_count",
                    "order": "desc",
                    "paramName": "sort_by_count"
                },
                {
                    "field": "status",
                    "order": "asc",
                    "paramName": "sort_by_status"
                }
            ],
            # LIMIT值
            "limitValue": 50,
            # 窗口函数（使用实际存在的字段：platform, uploaded_at）
            "windowFunctions": {
                "functions": [
                    {
                        "name": "row_number",
                        "partition_by": ["platform"],
                        "order_by": [
                            {
                                "field": "uploaded_at",
                                "direction": "DESC",
                                "paramName": "window_sort_date"
                            }
                        ],
                        "alias": "row_num"
                    }
                ]
            },
            # 统计项列表（使用实际存在的数值字段：asset_id, file_size_bytes, impression_count）
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "total_count",
                    "itemName": "总数"
                },
                {
                    "fieldName": "file_size_bytes",
                    "aggregation": "sum",
                    "itemCode": "total_size",
                    "itemName": "总文件大小"
                },
                {
                    "fieldName": "impression_count",
                    "aggregation": "avg",
                    "itemCode": "avg_impressions",
                    "itemName": "平均曝光次数"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        assert create_result.code == 200, f"创建指标失败: {create_result.message}"
        log.info(f"✓ 创建完整指标成功: {unique_code}")

        # 获取创建后的指标ID
        metric_id = create_result.data.get('id')
        assert metric_id is not None, "创建指标后未返回ID"

        # ========== 步骤2: 修改指标（修改所有属性） ==========
        update_data = {
            "metricName": "完整指标测试_已修改",
            "metricDesc": "修改后的描述信息",
            "categoryCode": "updated_category",
            "categoryName": "更新分类",
            # 修改分组维度（使用实际存在的字段：platform, city）
            "groupByFields": ["platform", "city"],
            # 修改筛选条件（使用实际存在的字段：uploaded_at, status, file_size_bytes）
            "filterConditions": {
                "conditions": [
                    {
                        "field": "uploaded_at",
                        "operator": ">",
                        "value": "2026-05-15",
                        "paramName": "filter_start_date"
                    },
                    {
                        "field": "status",
                        "operator": "=",
                        "value": "approved",
                        "paramName": "filter_status"
                    },
                    {
                        "field": "file_size_bytes",
                        "operator": ">",
                        "value": 10485760,
                        "paramName": "filter_min_size"
                    }
                ],
                "logic": "AND"
            },
            # 修改HAVING条件（基于统计项别名：total_count, total_size）
            "havingConditions": {
                "conditions": [
                    {
                        "field": "total_count",
                        "operator": ">=",
                        "value": 1,
                        "paramName": "having_min_count"
                    },
                    {
                        "field": "total_size",
                        "operator": ">",
                        "value": 10485760,
                        "paramName": "having_min_size"
                    }
                ],
                "logic": "AND"
            },
            # 修改排序规则（使用统计项别名 total_count）
            "sortRules": [
                {
                    "field": "total_count",
                    "order": "desc",
                    "paramName": "sort_by_count"
                },
                {
                    "field": "platform",
                    "order": "asc",
                    "paramName": "sort_by_platform"
                }
            ],
            # 修改LIMIT值
            "limitValue": 100,
            # 修改窗口函数（使用实际存在的字段：city, uploaded_at）
            "windowFunctions": {
                "functions": [
                    {
                        "name": "rank",
                        "partition_by": ["city"],
                        "order_by": [
                            {
                                "field": "uploaded_at",
                                "direction": "DESC",
                                "paramName": "window_sort_date"
                            }
                        ],
                        "alias": "rank_num"
                    },
                    {
                        "name": "dense_rank",
                        "partition_by": ["platform"],
                        "order_by": [
                            {
                                "field": "play_count",
                                "direction": "DESC",
                                "paramName": "window_sort_plays"
                            }
                        ],
                        "alias": "dense_rank_num"
                    }
                ]
            },
            # 修改统计项（使用实际存在的数值字段）
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "total_count",
                    "itemName": "记录数"
                },
                {
                    "fieldName": "file_size_bytes",
                    "aggregation": "sum",
                    "itemCode": "total_size",
                    "itemName": "总大小"
                },
                {
                    "fieldName": "play_count",
                    "aggregation": "max",
                    "itemCode": "max_plays",
                    "itemName": "最大播放"
                },
                {
                    "fieldName": "play_count",
                    "aggregation": "min",
                    "itemCode": "min_plays",
                    "itemName": "最小播放"
                }
            ],
            "enabled": False
        }

        update_result = MetricApi.update_metric(metric_id, update_data)
        assert update_result.code == 200, f"更新指标失败: {update_result.message}"
        log.info(f"✓ 更新完整指标成功: {unique_code}")

        # ========== 步骤3: 查询指标并验证修改结果 ==========
        get_result = MetricApi.get_metric_by_code(unique_code)
        assert get_result.code == 200, f"查询指标失败: {get_result.message}"
        
        metric_data = get_result.data
        assert metric_data.get('metricName') == "完整指标测试_已修改", "指标名称未更新"
        assert metric_data.get('metricDesc') == "修改后的描述信息", "指标描述未更新"
        assert metric_data.get('categoryCode') == "updated_category", "分类编码未更新"
        assert metric_data.get('categoryName') == "更新分类", "分类名称未更新"
        assert metric_data.get('enabled') == False, "启用状态未更新"
        assert metric_data.get('limitValue') == 100, "LIMIT值未更新"
        
        # 验证分组字段（更新后应为 platform 和 city）
        assert set(metric_data.get('groupByFields', [])) == {'platform', 'city'}, "分组字段未更新"
        
        # 验证统计项数量
        stat_items = metric_data.get('statItems', [])
        assert len(stat_items) == 4, f"统计项数量不正确: 期望4个，实际{len(stat_items)}个"
        
        log.info(f"✓ 查询并验证完整指标成功")

        # ========== 步骤4: 测试执行指标 ==========
        # 先启用指标
        enable_result = MetricApi.toggle_metric(metric_id, True)
        assert enable_result.code == 200, f"启用指标失败: {enable_result.message}"
        
        execute_result = MetricApi.execute_metric(unique_code)
        assert execute_result.code == 200, f"执行指标失败: {execute_result.message}"
        assert isinstance(execute_result.data, list), "执行结果不是列表类型"
        log.info(f"✓ 执行完整指标成功，返回 {len(execute_result.data)} 条记录")

    def test_create_uploader_rank_metric_200(self, db_reset):
        """创建按平台分组的完播率上传者排名指标"""
        unique_code = f"uploader_rank_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "按平台分组的完播率上传者排名",
            "metricDesc": "计算各平台下上传者的完播率（完播次数/播放次数），并按完播率降序排名，支持平台内排名",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["platform", "uploader"],
            "filterConditions": {
                "conditions": [
                    {
                        "field": "play_count",
                        "operator": ">",
                        "value": 0,
                        "paramName": "filter_play_count_gt_zero"
                    },
                    {
                        "field": "deleted",
                        "operator": "=",
                        "value": 0,
                        "paramName": "filter_not_deleted"
                    }
                ],
                "logic": "AND"
            },
            "sortRules": [
                {
                    "field": "completion_rate",
                    "order": "desc",
                    "paramName": "sort_by_completion_rate"
                }
            ],
            "limitValue": 100,
            "windowFunctions": {
                "functions": [
                    {
                        "name": "row_number",
                        "partition_by": ["platform"],
                        "order_by": [
                            {
                                "field": "uploaded_at",
                                "direction": "DESC",
                                "paramName": "window_sort_uploaded_at"
                            }
                        ],
                        "alias": "rank_in_platform"
                    }
                ]
            },
            "statItems": [
                {
                    "fieldName": "complete_play_count",
                    "aggregation": "sum",
                    "itemCode": "total_complete_play_count",
                    "itemName": "总完播次数"
                },
                {
                    "fieldName": "play_count",
                    "aggregation": "sum",
                    "itemCode": "total_play_count",
                    "itemName": "总播放次数"
                },
                {
                    "fieldName": "complete_play_count",
                    "aggregation": "sum",
                    "itemCode": "completion_rate",
                    "itemName": "完播率"
                }
            ],
            "enabled": True
        }

        log.info(f"测试场景: 创建按平台分组的完播率上传者排名指标")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        
        assert result.code == 200, f"创建指标失败: {result.message}"
        assert result.data.get('metricCode') == unique_code
        assert result.data.get('metricName') == "按平台分组的完播率上传者排名"
        log.info(f"✓ 创建按平台分组的完播率上传者排名指标成功: {unique_code}")

    def test_create_uploader_total_play_rank_metric_200(self, db_reset):
        """创建上传人播放量总和排名指标"""
        unique_code = f"uploader_total_play_rank_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "上传人播放量总和排名",
            "metricDesc": "按上传人分组，统计各上传人所有素材在不同平台的播放量总和，并按总和从高到低进行排名显示",
            "sourceTable": "asset",
            "categoryCode": "content_operation",
            "categoryName": "内容运营",
            "groupByFields": ["uploader"],
            "filterConditions": {
                "conditions": [
                    {
                        "field": "deleted",
                        "operator": "=",
                        "value": 0,
                        "paramName": "filter_deleted"
                    }
                ],
                "logic": "AND"
            },
            "havingConditions": {
                "conditions": [],
                "logic": "AND"
            },
            "sortRules": [
                {
                    "field": "total_play_count",
                    "order": "desc",
                    "paramName": "sort_by_total_play"
                }
            ],
            "limitValue": 100,
            "windowFunctions": {
                "functions": [
                    {
                        "name": "rank",
                        "partition_by": [],
                        "order_by": [
                            {
                                "field": "total_play_count",
                                "direction": "DESC",
                                "paramName": "window_rank_order"
                            }
                        ],
                        "item_name": "play_rank"
                    }
                ]
            },
            "statItems": [
                {
                    "fieldName": "play_count",
                    "aggregation": "sum",
                    "itemCode": "total_play_count",
                    "itemName": "播放量总和",
                    "sortOrder": 0
                }
            ],
            "enabled": True
        }

        log.info(f"测试场景: 创建上传人播放量总和排名指标")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        
        assert result.code == 200, f"创建指标失败: {result.message}"
        assert result.data.get('metricCode') == unique_code
        assert result.data.get('metricName') == "上传人播放量总和排名"
        log.info(f"✓ 创建上传人播放量总和排名指标成功: {unique_code}")
        
        # 执行指标验证
        log.info(f"执行指标验证: {unique_code}")
        execute_result = MetricApi.execute_metric(unique_code)
        assert execute_result.code == 200, f"执行指标失败: {execute_result.message}"
        assert isinstance(execute_result.data, list), "执行结果不是列表类型"
        log.info(f"✓ 执行指标成功，返回 {len(execute_result.data)} 条记录")

    @pytest.mark.parametrize("test_case", CREATE_METRIC_TEST_CASES, ids=lambda x: x["name"])
    def test_create_metric_from_array(self, db_reset, test_case):
        """从JSON数组中循环执行创建指标测试用例"""
        unique_code = f"array_test_{uuid.uuid4().hex[:8]}"
        create_data = test_case['create_data'].copy()
        create_data['metricCode'] = unique_code
        
        log.info(f"测试场景: {test_case['name']} - metricCode: {unique_code}")
        result = MetricApi.create_metric(create_data)
        
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200, f"创建指标失败: {result.message}"
        assert result.data.get('metricCode') == unique_code
        log.info(f"✓ 创建指标成功: {unique_code}")
        
        # 执行指标验证
        execute_result = MetricApi.execute_metric(unique_code)
        assert execute_result.code == 200, f"执行指标失败: {execute_result.message}"
        assert isinstance(execute_result.data, list), "执行结果不是列表类型"
        log.info(f"✓ 执行指标成功，返回 {len(execute_result.data)} 条记录")

    @pytest.mark.parametrize("test_case", EXECUTE_METRIC_TEST_CASES, ids=lambda x: x["name"])
    def test_execute_metric_from_array(self, db_reset, test_case):
        """从JSON数组中循环执行指标测试用例"""
        metric_code = test_case['metric_code']
        params = test_case.get('params', {})
        
        log.info(f"测试场景: {test_case['name']} - metricCode: {metric_code}")
        result = MetricApi.execute_metric(metric_code, params)
        
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200, f"执行指标失败: {result.message}"
        assert isinstance(result.data, list), "执行结果不是列表类型"
        log.info(f"✓ 执行指标成功，返回 {len(result.data)} 条记录")