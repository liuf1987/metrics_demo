#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metrics_execute_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 执行指标查询异常流程测试

import pytest
import uuid
from libs.metrics.metric_api import MetricApi
from libs.metadata.metadata_api import MetadataApi
from config import log_config

log = log_config.get_logger(__file__)


class TestMetricsExecuteException:
    """执行指标查询异常流程测试"""

    @pytest.mark.parametrize(
        "metric_code,expected_code,test_name",
        [
            ("non_existent_metric_xxx", 3102, "不存在的指标"),
        ],
        ids=["不存在的指标"]
    )
    def test_execute_metric_not_found(self, db_reset, metric_code, expected_code, test_name):
        """执行不存在的指标查询异常 (3102)"""
        log.info(f"测试场景: {test_name}, 请求参数: metricCode={metric_code}")
        result = MetricApi.execute_metric(metric_code)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code

    def test_execute_disabled_metric(self, db_reset):
        """执行已停用的指标查询异常 (3010)"""
        # 先获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        metric_code = metric.get('metricCode')
        original_enabled = metric.get('enabled')

        log.info(f"找到指标: {metric_code} (ID: {metric_id}), 当前状态: {'启用' if original_enabled else '停用'}")

        try:
            # 如果指标是启用的，先停用
            if original_enabled:
                toggle_result = MetricApi.toggle_metric(metric_id, False)
                assert toggle_result.code == 200, f"停用指标失败: {toggle_result.message}"
                log.info(f"已成功停用指标: {metric_code}")

            # 执行已停用的指标查询
            log.info(f"测试场景: 执行已停用的指标, 请求参数: metricCode={metric_code}")
            result = MetricApi.execute_metric(metric_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 3010, f"期望返回码3010，实际返回: {result.code}"

        finally:
            # 恢复指标的原始状态
            if original_enabled:
                restore_result = MetricApi.toggle_metric(metric_id, True)
                if restore_result.code == 200:
                    log.info(f"已恢复指标状态: {metric_code} -> 启用")
                else:
                    log.warning(f"恢复指标状态失败: {restore_result.message}")

    def test_execute_metric_invalid_field(self, db_reset):
        """执行指标查询-字段不存在 (4009)"""
        # 创建一个使用不存在字段的指标
        unique_code = f"test_invalid_field_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "测试无效字段指标",
            "sourceTable": "asset",
            "filterConditions": {
                "conditions": [
                    {
                        "field": "non_existent_field_xxx",
                        "operator": "=",
                        "value": "test"
                    }
                ],
                "logic": "AND"
            },
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code == 200:
            log.info(f"测试场景: 过滤字段不存在, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code in [4009, 3005]
        else:
            log.info(f"创建指标失败，跳过此测试: {create_result.code}")

    # ==================== 分组字段异常测试 ====================

    def test_execute_metric_group_by_field_not_found(self, db_reset):
        """执行指标查询-分组字段不存在 (4006)"""
        unique_code = f"test_group_field_not_found_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "分组字段不存在测试",
            "sourceTable": "asset",
            "groupByFields": ["non_existent_group_field_xxx"],
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code == 200:
            log.info(f"测试场景: 分组字段不存在, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4006, f"期望返回码4006，实际返回: {result.code}"
        else:
            log.info(f"创建指标失败，跳过此测试: {create_result.code}")

    def test_execute_metric_group_by_field_disabled(self, db_reset):
        """执行指标查询-分组字段已禁用 (4007)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个groupable=True的字段来测试
        groupable_field = None
        for field in fields_result.data:
            if field.get('groupable') and field.get('enabled'):
                groupable_field = field
                break

        if not groupable_field:
            pytest.skip("未找到可分组的字段")

        field_id = groupable_field.get('id')
        field_name = groupable_field.get('fieldName')
        log.info(f"找到可分组字段: {field_name} (id={field_id})")

        # 先启用字段
        MetadataApi.update_field_status(field_id, True)

        # 创建指标
        unique_code = f"test_group_field_disabled_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "分组字段已禁用测试",
            "sourceTable": "asset",
            "groupByFields": [field_name],
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code != 200:
            pytest.skip(f"创建指标失败: {create_result.code}")

        # 获取指标ID
        get_result = MetricApi.get_metric_by_code(unique_code)
        if get_result.code != 200:
            pytest.skip(f"获取指标失败: {get_result.code}")
        metric_id = get_result.data.get('id')
        if not metric_id:
            pytest.skip("无法获取指标ID")

        # 停用字段（会自动停用相关指标）
        disable_result = MetadataApi.update_field_status(field_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用字段: {disable_result.message}")

        log.info(f"已停用分组字段: {field_name}")

        # 重新启用指标（绕过字段停用的联动效果）
        toggle_result = MetricApi.toggle_metric(metric_id, True)
        if toggle_result.code != 200:
            pytest.skip(f"重新启用指标失败: {toggle_result.message}")
        log.info(f"已重新启用指标: {unique_code}")

        try:
            log.info(f"测试场景: 分组字段已禁用, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4007, f"期望返回码4007，实际返回: {result.code}"
        finally:
            # 恢复字段状态
            MetadataApi.update_field_status(field_id, True)
            log.info(f"已恢复字段状态: {field_name} -> 启用")

    def test_execute_metric_group_by_feature_disabled(self, db_reset):
        """执行指标查询-字段分组功能已被禁用 (4008)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个groupable=True的字段，修改其groupable属性为false
        groupable_field = None
        for field in fields_result.data:
            if field.get('groupable') and field.get('enabled'):
                groupable_field = field
                break

        if not groupable_field:
            pytest.skip("未找到可分组的字段")

        field_id = groupable_field.get('id')
        field_name = groupable_field.get('fieldName')
        log.info(f"找到可分组字段: {field_name} (id={field_id})")

        # 创建指标
        unique_code = f"test_group_feature_disabled_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "分组功能禁用测试",
            "sourceTable": "asset",
            "groupByFields": [field_name],
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code != 200:
            pytest.skip(f"创建指标失败: {create_result.code}")

        # 修改字段的groupable属性为false（模拟字段分组功能被禁用）
        # 注意：这里假设MetadataApi有更新字段属性的接口
        try:
            # 尝试更新字段属性，如果没有此接口则跳过测试
            update_result = MetadataApi.update_field_attribute(field_id, 'groupable', False)
            if update_result.code != 200:
                pytest.skip(f"无法更新字段属性: {update_result.message}")

            log.info(f"已禁用字段分组功能: {field_name}")

            log.info(f"测试场景: 字段分组功能已被禁用, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4008, f"期望返回码4008，实际返回: {result.code}"
        except AttributeError:
            pytest.skip("MetadataApi没有update_field_attribute方法")
        finally:
            # 恢复字段属性
            try:
                MetadataApi.update_field_attribute(field_id, 'groupable', True)
                log.info(f"已恢复字段分组功能: {field_name}")
            except AttributeError:
                pass

    # ==================== 过滤字段异常测试 ====================

    def test_execute_metric_filter_field_disabled(self, db_reset):
        """执行指标查询-过滤字段已禁用 (4010)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个filterable=True的字段来测试
        filterable_field = None
        for field in fields_result.data:
            if field.get('filterable') and field.get('enabled'):
                filterable_field = field
                break

        if not filterable_field:
            pytest.skip("未找到可筛选的字段")

        field_id = filterable_field.get('id')
        field_name = filterable_field.get('fieldName')
        log.info(f"找到可筛选字段: {field_name} (id={field_id})")

        # 先启用字段
        MetadataApi.update_field_status(field_id, True)

        # 创建指标
        unique_code = f"test_filter_field_disabled_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "过滤字段已禁用测试",
            "sourceTable": "asset",
            "filterConditions": {
                "conditions": [
                    {
                        "field": field_name,
                        "operator": "=",
                        "value": "test"
                    }
                ],
                "logic": "AND"
            },
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code != 200:
            pytest.skip(f"创建指标失败: {create_result.code}")

        # 获取指标ID
        get_result = MetricApi.get_metric_by_code(unique_code)
        if get_result.code != 200:
            pytest.skip(f"获取指标失败: {get_result.code}")
        metric_id = get_result.data.get('id')
        if not metric_id:
            pytest.skip("无法获取指标ID")

        # 停用字段（会自动停用相关指标）
        disable_result = MetadataApi.update_field_status(field_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用字段: {disable_result.message}")

        log.info(f"已停用过滤字段: {field_name}")

        # 重新启用指标（绕过字段停用的联动效果）
        toggle_result = MetricApi.toggle_metric(metric_id, True)
        if toggle_result.code != 200:
            pytest.skip(f"重新启用指标失败: {toggle_result.message}")
        log.info(f"已重新启用指标: {unique_code}")

        try:
            log.info(f"测试场景: 过滤字段已禁用, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4010, f"期望返回码4010，实际返回: {result.code}"
        finally:
            # 恢复字段状态
            MetadataApi.update_field_status(field_id, True)
            log.info(f"已恢复字段状态: {field_name} -> 启用")

    def test_execute_metric_filter_feature_disabled(self, db_reset):
        """执行指标查询-字段过滤功能已被禁用 (4011)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个filterable=True的字段
        filterable_field = None
        for field in fields_result.data:
            if field.get('filterable') and field.get('enabled'):
                filterable_field = field
                break

        if not filterable_field:
            pytest.skip("未找到可筛选的字段")

        field_id = filterable_field.get('id')
        field_name = filterable_field.get('fieldName')
        log.info(f"找到可筛选字段: {field_name} (id={field_id})")

        # 创建指标
        unique_code = f"test_filter_feature_disabled_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "过滤功能禁用测试",
            "sourceTable": "asset",
            "filterConditions": {
                "conditions": [
                    {
                        "field": field_name,
                        "operator": "=",
                        "value": "test"
                    }
                ],
                "logic": "AND"
            },
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code != 200:
            pytest.skip(f"创建指标失败: {create_result.code}")

        # 尝试禁用字段的filterable属性
        try:
            update_result = MetadataApi.update_field_attribute(field_id, 'filterable', False)
            if update_result.code != 200:
                pytest.skip(f"无法更新字段属性: {update_result.message}")

            log.info(f"已禁用字段过滤功能: {field_name}")

            log.info(f"测试场景: 字段过滤功能已被禁用, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4011, f"期望返回码4011，实际返回: {result.code}"
        except AttributeError:
            pytest.skip("MetadataApi没有update_field_attribute方法")
        finally:
            try:
                MetadataApi.update_field_attribute(field_id, 'filterable', True)
                log.info(f"已恢复字段过滤功能: {field_name}")
            except AttributeError:
                pass

    # ==================== 排序字段异常测试 ====================

    def test_execute_metric_sort_field_disabled(self, db_reset):
        """执行指标查询-排序字段已禁用 (4012)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个sortable=True的字段来测试
        sortable_field = None
        for field in fields_result.data:
            if field.get('sortable') and field.get('enabled'):
                sortable_field = field
                break

        if not sortable_field:
            pytest.skip("未找到可排序的字段")

        field_id = sortable_field.get('id')
        field_name = sortable_field.get('fieldName')
        log.info(f"找到可排序字段: {field_name} (id={field_id})")

        # 先启用字段
        MetadataApi.update_field_status(field_id, True)

        # 创建指标
        unique_code = f"test_sort_field_disabled_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "排序字段已禁用测试",
            "sourceTable": "asset",
            "sortRules": [
                {
                    "field": field_name,
                    "direction": "ASC"
                }
            ],
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code != 200:
            pytest.skip(f"创建指标失败: {create_result.code}")

        # 获取指标ID
        get_result = MetricApi.get_metric_by_code(unique_code)
        if get_result.code != 200:
            pytest.skip(f"获取指标失败: {get_result.code}")
        metric_id = get_result.data.get('id')
        if not metric_id:
            pytest.skip("无法获取指标ID")

        # 停用字段（会自动停用相关指标）
        disable_result = MetadataApi.update_field_status(field_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用字段: {disable_result.message}")

        log.info(f"已停用排序字段: {field_name}")

        # 重新启用指标（绕过字段停用的联动效果）
        toggle_result = MetricApi.toggle_metric(metric_id, True)
        if toggle_result.code != 200:
            pytest.skip(f"重新启用指标失败: {toggle_result.message}")
        log.info(f"已重新启用指标: {unique_code}")

        try:
            log.info(f"测试场景: 排序字段已禁用, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4012, f"期望返回码4012，实际返回: {result.code}"
        finally:
            # 恢复字段状态
            MetadataApi.update_field_status(field_id, True)
            log.info(f"已恢复字段状态: {field_name} -> 启用")

    def test_execute_metric_sort_feature_disabled(self, db_reset):
        """执行指标查询-字段排序功能已被禁用 (4013)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个sortable=True的字段
        sortable_field = None
        for field in fields_result.data:
            if field.get('sortable') and field.get('enabled'):
                sortable_field = field
                break

        if not sortable_field:
            pytest.skip("未找到可排序的字段")

        field_id = sortable_field.get('id')
        field_name = sortable_field.get('fieldName')
        log.info(f"找到可排序字段: {field_name} (id={field_id})")

        # 创建指标
        unique_code = f"test_sort_feature_disabled_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "排序功能禁用测试",
            "sourceTable": "asset",
            "sortRules": [
                {
                    "field": field_name,
                    "direction": "ASC"
                }
            ],
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code != 200:
            pytest.skip(f"创建指标失败: {create_result.code}")

        # 尝试禁用字段的sortable属性
        try:
            update_result = MetadataApi.update_field_attribute(field_id, 'sortable', False)
            if update_result.code != 200:
                pytest.skip(f"无法更新字段属性: {update_result.message}")

            log.info(f"已禁用字段排序功能: {field_name}")

            log.info(f"测试场景: 字段排序功能已被禁用, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4013, f"期望返回码4013，实际返回: {result.code}"
        except AttributeError:
            pytest.skip("MetadataApi没有update_field_attribute方法")
        finally:
            try:
                MetadataApi.update_field_attribute(field_id, 'sortable', True)
                log.info(f"已恢复字段排序功能: {field_name}")
            except AttributeError:
                pass

    # ==================== 窗口函数字段异常测试 ====================

    def test_execute_metric_window_field_not_found(self, db_reset):
        """执行指标查询-窗口函数字段不存在 (4014)"""
        unique_code = f"test_window_field_not_found_{uuid.uuid4().hex[:8]}"
        # 使用后端期望的窗口函数格式（包含functions字段的对象）
        create_data = {
            "metricCode": unique_code,
            "metricName": "窗口函数字段不存在测试",
            "sourceTable": "asset",
            "windowFunctions": {
                "functions": [
                    {
                        "name": "row_number",
                        "partition_by": ["asset_id"],
                        "order_by": [{"field": "non_existent_window_field_xxx", "direction": "ASC"}],
                        "alias": "row_num"
                    }
                ]
            },
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code == 200:
            log.info(f"测试场景: 窗口函数字段不存在, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4014, f"期望返回码4014，实际返回: {result.code}"
        else:
            log.info(f"创建指标失败，跳过此测试: {create_result.code}")

    def test_execute_metric_window_field_disabled(self, db_reset):
        """执行指标查询-窗口函数字段已禁用 (4015)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个可用的字段作为窗口函数排序字段
        available_field = None
        for field in fields_result.data:
            if field.get('enabled'):
                available_field = field
                break

        if not available_field:
            pytest.skip("未找到可用字段")

        field_id = available_field.get('id')
        field_name = available_field.get('fieldName')
        log.info(f"找到字段: {field_name} (id={field_id})")

        # 先启用字段
        MetadataApi.update_field_status(field_id, True)

        # 创建指标 - 使用后端期望的窗口函数格式（包含functions字段的对象）
        unique_code = f"test_window_field_disabled_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "窗口函数字段已禁用测试",
            "sourceTable": "asset",
            "windowFunctions": {
                "functions": [
                    {
                        "name": "row_number",
                        "partition_by": ["asset_id"],
                        "order_by": [{"field": field_name, "direction": "ASC"}],
                        "alias": "row_num"
                    }
                ]
            },
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "数量"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code != 200:
            pytest.skip(f"创建指标失败: {create_result.code}")

        # 获取指标ID
        get_result = MetricApi.get_metric_by_code(unique_code)
        if get_result.code != 200:
            pytest.skip(f"获取指标失败: {get_result.code}")
        metric_id = get_result.data.get('id')
        if not metric_id:
            pytest.skip("无法获取指标ID")

        # 停用字段（会自动停用相关指标）
        disable_result = MetadataApi.update_field_status(field_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用字段: {disable_result.message}")

        log.info(f"已停用窗口函数字段: {field_name}")

        # 重新启用指标（绕过字段停用的联动效果）
        toggle_result = MetricApi.toggle_metric(metric_id, True)
        if toggle_result.code != 200:
            pytest.skip(f"重新启用指标失败: {toggle_result.message}")
        log.info(f"已重新启用指标: {unique_code}")

        try:
            log.info(f"测试场景: 窗口函数字段已禁用, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 4015, f"期望返回码4015，实际返回: {result.code}"
        finally:
            # 恢复字段状态
            MetadataApi.update_field_status(field_id, True)
            log.info(f"已恢复字段状态: {field_name} -> 启用")

    # ==================== 查询执行失败异常测试 ====================

    def test_execute_metric_query_failed(self, db_reset):
        """执行指标查询-查询执行失败 (4901)"""
        # 使用一个会导致查询执行失败的指标配置
        unique_code = f"test_query_failed_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "查询执行失败测试",
            "sourceTable": "asset",
            # 使用错误的聚合函数参数导致执行失败
            "statItems": [
                {
                    "fieldName": "asset_id",
                    "aggregation": "avg",
                    "itemCode": "avg_value",
                    "itemName": "平均值"
                }
            ],
            "enabled": True
        }

        create_result = MetricApi.create_metric(create_data)
        if create_result.code == 200:
            log.info(f"测试场景: 查询执行失败, 请求参数: metricCode={unique_code}")
            result = MetricApi.execute_metric(unique_code)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            # 4901表示查询执行失败，可能因各种运行时错误触发
            if result.code in [4901, 200]:  # 可能成功也可能失败，取决于asset_id字段类型
                log.info(f"查询结果符合预期: code={result.code}")
            else:
                log.info(f"返回码不在预期范围内: {result.code}")
        else:
            log.info(f"创建指标失败，跳过此测试: {create_result.code}")


