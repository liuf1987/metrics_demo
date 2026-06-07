#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metrics_create_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 创建指标异常流程测试

import pytest
import uuid
from libs.metrics.metric_api import MetricApi
from libs.metadata.metadata_api import MetadataApi
from config import log_config

log = log_config.get_logger(__file__)


class TestMetricsCreateException:
    """创建指标异常流程测试"""

    def test_create_metric_code_exists(self, db_reset):
        """创建指标-编码已存在 (3001)"""
        list_result = MetricApi.list_metrics()
        if list_result.code == 200 and list_result.data.get('records'):
            existing_code = list_result.data['records'][0].get('metricCode')
            create_data = {
                "metricCode": existing_code,
                "metricName": "重复编码测试",
                "sourceTable": "asset",
                "statItems": []
            }
            log.info(f"测试场景: 指标编码已存在, 请求参数: metricCode={existing_code}")
            result = MetricApi.create_metric(create_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 3001

    def test_create_metric_empty_source_table(self, db_reset):
        """创建指标-源表名不能为空 (1001)"""
        create_data = {
            "metricCode": f"test_empty_source_{uuid.uuid4().hex[:8]}",
            "metricName": "测试指标",
            "sourceTable": "",
            "statItems": []
        }
        log.info(f"测试场景: 源表名为空")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 1001

    def test_create_metric_invalid_source_table(self, db_reset):
        """创建指标-数据源不存在 (3003)"""
        create_data = {
            "metricCode": f"test_invalid_source_{uuid.uuid4().hex[:8]}",
            "metricName": "测试指标",
            "sourceTable": "non_existent_table_xxx",
            "statItems": []
        }
        log.info(f"测试场景: 数据源不存在")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3003

    def test_create_metric_disabled_source_table(self, db_reset):
        """创建指标-数据源已禁用 (3004)"""
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

        # 先启用数据源确保可以创建指标
        MetadataApi.update_data_source_status(source_id, True)

        # 创建指标前先停用数据源
        disable_result = MetadataApi.update_data_source_status(source_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用数据源: {disable_result.message}")

        log.info(f"已停用数据源: {source_id}")

        try:
            # 尝试创建指标
            unique_code = f"test_disabled_source_{uuid.uuid4().hex[:8]}"
            create_data = {
                "metricCode": unique_code,
                "metricName": "数据源禁用测试",
                "sourceTable": "asset",
                "statItems": []
            }
            log.info(f"测试场景: 数据源已禁用, 请求参数: metricCode={unique_code}")
            result = MetricApi.create_metric(create_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 3004, f"期望返回码3004，实际返回: {result.code}"
        finally:
            # 恢复数据源状态
            MetadataApi.update_data_source_status(source_id, True)
            log.info(f"已恢复数据源状态: {source_id} -> 启用")

    def test_create_metric_nonexistent_filter_field(self, db_reset):
        """创建指标-过滤字段不存在 (3005)"""
        unique_code = f"test_nonexistent_filter_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "不存在过滤字段测试",
            "sourceTable": "asset",
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
            },
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
        log.info(f"测试场景: 过滤字段不存在")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3005, f"期望返回码3005，实际返回: {result.code}"

    def test_create_metric_disabled_filter_field(self, db_reset):
        """创建指标-过滤字段已禁用 (3006)"""
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

        # 先确保字段是启用状态
        MetadataApi.update_field_status(field_id, True)

        # 停用该字段
        disable_result = MetadataApi.update_field_status(field_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用字段: {disable_result.message}")

        log.info(f"已停用字段: {field_name} (id={field_id})")

        try:
            unique_code = f"test_disabled_filter_{uuid.uuid4().hex[:8]}"
            create_data = {
                "metricCode": unique_code,
                "metricName": "禁用过滤字段测试",
                "sourceTable": "asset",
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
                },
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
            log.info(f"测试场景: 过滤字段已禁用, field={field_name}")
            result = MetricApi.create_metric(create_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 3006, f"期望返回码3006，实际返回: {result.code}"
        finally:
            # 恢复字段状态
            MetadataApi.update_field_status(field_id, True)
            log.info(f"已恢复字段状态: {field_name} -> 启用")

    def test_create_metric_nonexistent_stat_field(self, db_reset):
        """创建指标-统计字段不存在 (3007)"""
        unique_code = f"test_nonexistent_stat_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "不存在统计字段测试",
            "sourceTable": "asset",
            "statItems": [
                {
                    "fieldName": "non_existent_stat_field_xxx",
                    "aggregation": "count",
                    "itemCode": "count",
                    "itemName": "总数"
                }
            ],
            "enabled": True
        }
        log.info(f"测试场景: 统计字段不存在")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3007, f"期望返回码3007，实际返回: {result.code}"

    def test_create_metric_disabled_stat_field(self, db_reset):
        """创建指标-统计字段已禁用 (3008)"""
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

        # 先确保字段是启用状态
        MetadataApi.update_field_status(field_id, True)

        # 停用该字段
        disable_result = MetadataApi.update_field_status(field_id, False)
        if disable_result.code != 200:
            pytest.skip(f"无法停用字段: {disable_result.message}")

        log.info(f"已停用统计字段: {field_name} (id={field_id})")

        try:
            unique_code = f"test_disabled_stat_{uuid.uuid4().hex[:8]}"
            create_data = {
                "metricCode": unique_code,
                "metricName": "禁用统计字段测试",
                "sourceTable": "asset",
                "statItems": [
                    {
                        "fieldName": field_name,
                        "aggregation": "count",
                        "itemCode": "count",
                        "itemName": "总数"
                    }
                ],
                "enabled": True
            }
            log.info(f"测试场景: 统计字段已禁用, field={field_name}")
            result = MetricApi.create_metric(create_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 3008, f"期望返回码3008，实际返回: {result.code}"
        finally:
            # 恢复字段状态
            MetadataApi.update_field_status(field_id, True)
            log.info(f"已恢复字段状态: {field_name} -> 启用")

    def test_create_metric_unsupported_aggregation(self, db_reset):
        """创建指标-字段类型不支持聚合函数 (3011)"""
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

        unique_code = f"test_unsupported_agg_{uuid.uuid4().hex[:8]}"
        create_data = {
            "metricCode": unique_code,
            "metricName": "不支持聚合函数测试",
            "sourceTable": "asset",
            "statItems": [
                {
                    "fieldName": field_name,
                    "aggregation": "sum",
                    "itemCode": "sum_value",
                    "itemName": "求和值"
                }
            ],
            "enabled": True
        }
        log.info(f"测试场景: 字段类型不支持聚合函数, field={field_name}, aggregation=sum")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3011, f"期望返回码3011，实际返回: {result.code}"

    def test_create_metric_empty_metric_code(self, db_reset):
        """创建指标-指标编码为空 (1001)"""
        create_data = {
            "metricCode": "",
            "metricName": "测试指标",
            "sourceTable": "asset",
            "statItems": []
        }
        log.info(f"测试场景: 指标编码为空")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 1001, f"期望返回码1001，实际返回: {result.code}"

    def test_create_metric_empty_metric_name(self, db_reset):
        """创建指标-指标名称为空 (1001)"""
        create_data = {
            "metricCode": f"test_empty_name_{uuid.uuid4().hex[:8]}",
            "metricName": "",
            "sourceTable": "asset",
            "statItems": []
        }
        log.info(f"测试场景: 指标名称为空")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 1001, f"期望返回码1001，实际返回: {result.code}"

    def test_create_metric_missing_metric_code(self, db_reset):
        """创建指标-缺少必填字段metricCode (1001)"""
        create_data = {
            "metricName": "测试指标",
            "sourceTable": "asset",
            "statItems": []
        }
        log.info(f"测试场景: 缺少必填字段metricCode")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 1001, f"期望返回码1001，实际返回: {result.code}"

    def test_create_metric_missing_metric_name(self, db_reset):
        """创建指标-缺少必填字段metricName (1001)"""
        create_data = {
            "metricCode": f"test_missing_name_{uuid.uuid4().hex[:8]}",
            "sourceTable": "asset",
            "statItems": []
        }
        log.info(f"测试场景: 缺少必填字段metricName")
        result = MetricApi.create_metric(create_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 1001, f"期望返回码1001，实际返回: {result.code}"
