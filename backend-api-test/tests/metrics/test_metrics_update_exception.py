#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metrics_update_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 更新指标异常流程测试

import pytest
from libs.metrics.metric_api import MetricApi
from libs.metadata.metadata_api import MetadataApi
from config import log_config

log = log_config.get_logger(__file__)


class TestMetricsUpdateException:
    """更新指标异常流程测试"""

    @pytest.mark.parametrize(
        "metric_id,expected_code,test_name",
        [
            (999999, 3001, "不存在的指标ID"),
            (0, 3001, "指标ID为0"),
        ],
        ids=["不存在的指标ID", "指标ID为0"]
    )
    def test_update_metric_not_found(self, db_reset, metric_id, expected_code, test_name):
        """更新不存在的指标 (3001)"""
        update_data = {
            "metricName": "测试指标",
            "sourceTable": "asset"
        }
        log.info(f"测试场景: {test_name}, 请求参数: id={metric_id}")
        result = MetricApi.update_metric(metric_id, update_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code

    def test_update_metric_empty_source_table(self, db_reset):
        """更新指标-源表名不能为空 (3002)"""
        # 先获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        log.info(f"找到指标: ID={metric_id}")

        update_data = {
            "sourceTable": ""
        }
        log.info(f"测试场景: 更新时源表名为空")
        result = MetricApi.update_metric(metric_id, update_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3002, f"期望返回码3002，实际返回: {result.code}"

    def test_update_metric_invalid_source_table(self, db_reset):
        """更新指标-数据源不存在 (3003)"""
        # 先获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        log.info(f"找到指标: ID={metric_id}")

        update_data = {
            "sourceTable": "non_existent_table_xxx"
        }
        log.info(f"测试场景: 更新时数据源不存在")
        result = MetricApi.update_metric(metric_id, update_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3003, f"期望返回码3003，实际返回: {result.code}"

    def test_update_metric_disabled_source_table(self, db_reset):
        """更新指标-数据源已禁用 (3004)"""
        # 先加载元数据获取数据源信息
        load_result = MetadataApi.load_metadata()
        log.info(f"加载元数据结果: code={load_result.code}")

        # 获取asset表的数据源信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        # 找到asset表对应的数据源
        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')
        log.info(f"找到asset数据源: id={source_id}")

        # 先启用数据源
        MetadataApi.update_data_source_status(source_id, True)

        # 获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        log.info(f"找到指标: ID={metric_id}")

        # 停用数据源
        disable_result = MetadataApi.update_data_source_status(source_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用数据源: {disable_result.message}")

        log.info(f"已停用数据源: {source_id}")

        try:
            update_data = {
                "sourceTable": "asset"
            }
            log.info(f"测试场景: 更新时数据源已禁用")
            result = MetricApi.update_metric(metric_id, update_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 3004, f"期望返回码3004，实际返回: {result.code}"
        finally:
            # 恢复数据源状态
            MetadataApi.update_data_source_status(source_id, True)
            log.info(f"已恢复数据源状态: {source_id} -> 启用")

    def test_update_metric_nonexistent_filter_field(self, db_reset):
        """更新指标-过滤字段不存在 (3005)"""
        # 先获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        log.info(f"找到指标: ID={metric_id}")

        update_data = {
            "filterConditions": {
                "conditions": [
                    {
                        "field": "non_existent_filter_field_xxx",
                        "operator": "=",
                        "value": "test",
                        "paramName": "filter_status"
                    }
                ],
                "logic": "AND"
            }
        }
        log.info(f"测试场景: 更新时过滤字段不存在")
        result = MetricApi.update_metric(metric_id, update_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3005, f"期望返回码3005，实际返回: {result.code}"

    def test_update_metric_disabled_filter_field(self, db_reset):
        """更新指标-过滤字段已禁用 (3006)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        # 找到asset表对应的数据源
        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')

        # 获取字段列表
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

        # 获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        log.info(f"找到指标: ID={metric_id}")

        # 先确保字段是启用状态
        MetadataApi.update_field_status(field_id, True)

        # 停用该字段
        disable_result = MetadataApi.update_field_status(field_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用字段: {disable_result.message}")

        log.info(f"已停用字段: {field_name} (id={field_id})")

        try:
            update_data = {
                "filterConditions": {
                    "conditions": [
                        {
                            "field": field_name,
                            "operator": "=",
                            "value": "test",
                            "paramName": "filter_status"
                        }
                    ],
                    "logic": "AND"
                }
            }
            log.info(f"测试场景: 更新时过滤字段已禁用, field={field_name}")
            result = MetricApi.update_metric(metric_id, update_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 3006, f"期望返回码3006，实际返回: {result.code}"
        finally:
            # 恢复字段状态
            MetadataApi.update_field_status(field_id, True)
            log.info(f"已恢复字段状态: {field_name} -> 启用")

    def test_update_metric_nonexistent_stat_field(self, db_reset):
        """更新指标-统计字段不存在 (3007)"""
        # 先获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        log.info(f"找到指标: id={metric_id}")

        update_data = {
            "statItems": [
                {
                    "fieldName": "non_existent_stat_field_xxx",
                    "aggregation": "count",
                    "itemCode": "test_count",
                    "itemName": "测试统计"
                }
            ]
        }
        log.info(f"测试场景: 更新时统计字段不存在")
        result = MetricApi.update_metric(metric_id, update_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3007, f"期望返回码3007，实际返回: {result.code}"

    def test_update_metric_disabled_stat_field(self, db_reset):
        """更新指标-统计字段已禁用 (3008)"""
        # 先加载元数据
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        # 找到asset表对应的数据源
        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')

        # 获取字段列表
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个aggregatable=True的数值字段来测试
        aggregatable_field = None
        for field in fields_result.data:
            if field.get('aggregatable') and field.get('enabled'):
                aggregatable_field = field
                break

        if not aggregatable_field:
            pytest.skip("未找到可聚合的字段")

        field_id = aggregatable_field.get('id')
        field_name = aggregatable_field.get('fieldName')
        log.info(f"找到可聚合字段: {field_name} (id={field_id})")

        # 获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        log.info(f"找到指标: ID={metric_id}")

        # 先确保字段是启用状态
        MetadataApi.update_field_status(field_id, True)

        # 停用该字段
        disable_result = MetadataApi.update_field_status(field_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用字段: {disable_result.message}")

        log.info(f"已停用统计字段: {field_name} (id={field_id})")

        try:
            update_data = {
                "statItems": [
                    {
                        "fieldName": field_name,
                        "aggregation": "count",
                        "itemCode": "test_count",
                        "itemName": "测试统计"
                    }
                ]
            }
            log.info(f"测试场景: 更新时统计字段已禁用, field={field_name}")
            result = MetricApi.update_metric(metric_id, update_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 3008, f"期望返回码3008，实际返回: {result.code}"
        finally:
            # 恢复字段状态
            MetadataApi.update_field_status(field_id, True)
            log.info(f"已恢复字段状态: {field_name} -> 启用")

    def test_update_metric_unsupported_aggregation(self, db_reset):
        """更新指标-字段类型不支持聚合函数 (3011)"""
        # 先加载元数据获取字段类型信息
        MetadataApi.load_metadata()

        # 获取数据源和字段信息
        ds_list = MetadataApi.list_data_sources()
        if ds_list.code != 200 or not ds_list.data:
            pytest.skip(f"无法获取数据源列表: {ds_list.message}")

        # 找到asset表对应的数据源
        asset_source = None
        for ds in ds_list.data:
            if ds.get('sourceCode') == 'asset' or ds.get('sourceExpr') == 'asset':
                asset_source = ds
                break

        if not asset_source:
            pytest.skip("未找到asset表对应的数据源")

        source_id = asset_source.get('id')

        # 获取字段列表
        fields_result = MetadataApi.get_fields_by_source(source_id)
        if fields_result.code != 200 or not fields_result.data:
            pytest.skip(f"无法获取字段列表: {fields_result.message}")

        # 找一个string类型的字段（不支持sum/avg）来测试
        string_field = None
        for field in fields_result.data:
            if field.get('fieldType') == 'string' and field.get('enabled'):
                string_field = field
                break

        if not string_field:
            pytest.skip("未找到string类型的字段")

        field_name = string_field.get('fieldName')
        log.info(f"找到string类型字段: {field_name}")

        # 获取一个存在的指标
        list_result = MetricApi.list_metrics(page_num=1, page_size=1)
        assert list_result.code == 200
        assert len(list_result.data.get('records', [])) > 0, "数据库中没有可用的指标"

        metric = list_result.data['records'][0]
        metric_id = metric.get('id')
        log.info(f"找到指标: ID={metric_id}")

        update_data = {
            "statItems": [
                {
                    "fieldName": field_name,
                    "aggregation": "sum",
                    "itemCode": "test_sum",
                    "itemName": "测试求和"
                }
            ]
        }
        log.info(f"测试场景: 更新时字段类型不支持聚合函数, field={field_name}, aggregation=sum")
        result = MetricApi.update_metric(metric_id, update_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3011, f"期望返回码3011，实际返回: {result.code}"
