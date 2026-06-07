#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metadata_normal.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 元数据管理正常流程测试

import pytest
from libs.metadata.metadata_api import MetadataApi
from config import log_config

log = log_config.get_logger(__file__)


class TestMetadataNormal:
    """元数据管理正常流程测试"""

    @pytest.mark.parametrize(
        "request_params,expected_code,expected_msg",
        [
            (None, 200, "操作成功"),
            ({"tableNamePattern": "asset"}, 200, "操作成功"),
            ({"tableNamePattern": "%"}, 200, "操作成功"),
        ],
        ids=["无参数", "指定表名模式", "通配符模式"]
    )
    def test_load_metadata(self, db_reset, request_params, expected_code, expected_msg):
        """加载元数据"""
        log.info(f"请求参数: {request_params}")
        result = MetadataApi.load_metadata(request_params.get("tableNamePattern") if request_params else None)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code
        assert expected_msg in result.message

    def test_list_data_sources(self, db_reset):
        """查询所有数据源"""
        log.info("测试场景: 查询所有数据源")
        result = MetadataApi.list_data_sources()
        log.info(f"响应结果: code={result.code}, data_length={len(result.data) if result.data else 0}")
        assert result.code == 200
        assert isinstance(result.data, list)

    def test_get_data_source_detail(self, db_reset):
        """查询数据源详情"""
        # 先获取数据源列表
        list_result = MetadataApi.list_data_sources()
        assert list_result.code == 200
        assert len(list_result.data) > 0

        # 参数化测试多个数据源
        for data_source in list_result.data[:2]:
            source_id = data_source.get('id')
            log.info(f"请求参数: sourceId={source_id}")
            result = MetadataApi.get_data_source_detail(source_id)
            log.info(f"响应结果: code={result.code}, sourceCode={result.data.get('sourceCode')}")
            assert result.code == 200
            assert result.data.get('id') == source_id
            assert 'fields' in result.data

    def test_get_fields_by_source(self, db_reset):
        """查询数据源字段列表"""
        list_result = MetadataApi.list_data_sources()
        assert list_result.code == 200
        assert len(list_result.data) > 0

        for data_source in list_result.data[:2]:
            source_id = data_source.get('id')
            log.info(f"请求参数: sourceId={source_id}")
            result = MetadataApi.get_fields_by_source(source_id)
            log.info(f"响应结果: code={result.code}, field_count={len(result.data) if result.data else 0}")
            assert result.code == 200
            assert isinstance(result.data, list)

    @pytest.mark.parametrize(
        "enabled",
        [True, False],
        ids=["启用数据源", "禁用数据源"]
    )
    def test_update_data_source_status(self, db_reset, enabled):
        """更新数据源状态"""
        # 获取第一个数据源
        list_result = MetadataApi.list_data_sources()
        assert list_result.code == 200
        assert len(list_result.data) > 0

        source_id = list_result.data[0].get('id')
        log.info(f"请求参数: sourceId={source_id}, enabled={enabled}")
        result = MetadataApi.update_data_source_status(source_id, enabled)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        
        # 二次查询验证数据源状态已正确变更
        verify_result = MetadataApi.get_data_source_detail(source_id)
        log.info(f"二次查询验证: code={verify_result.code}, enabled={verify_result.data.get('enabled')}")
        assert verify_result.code == 200
        assert verify_result.data.get('enabled') == enabled, f"数据源状态未正确更新，期望: {enabled}, 实际: {verify_result.data.get('enabled')}"

    @pytest.mark.parametrize(
        "enabled",
        [True, False],
        ids=["启用字段", "禁用字段"]
    )
    def test_update_field_status(self, db_reset, enabled):
        """更新字段状态"""
        # 获取字段
        list_result = MetadataApi.list_data_sources()
        assert list_result.code == 200
        assert len(list_result.data) > 0

        source_id = list_result.data[0].get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        assert fields_result.code == 200
        assert len(fields_result.data) > 0

        field_id = fields_result.data[0].get('id')
        log.info(f"请求参数: fieldId={field_id}, enabled={enabled}")
        result = MetadataApi.update_field_status(field_id, enabled)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        
        # 二次查询验证字段状态已正确变更
        verify_result = MetadataApi.get_fields_by_source(source_id)
        log.info(f"二次查询验证: code={verify_result.code}, field_count={len(verify_result.data)}")
        assert verify_result.code == 200
        # 找到更新的字段并验证状态
        updated_field = next((f for f in verify_result.data if f.get('id') == field_id), None)
        assert updated_field is not None, f"未找到字段ID为 {field_id} 的字段"
        assert updated_field.get('enabled') == enabled, f"字段状态未正确更新，期望: {enabled}, 实际: {updated_field.get('enabled')}"

    @pytest.mark.parametrize(
        "properties",
        [
            {"filterable": True, "groupable": True, "aggregatable": False, "sortable": True},
            {"filterable": False, "groupable": False, "aggregatable": True, "sortable": False},
            {"filterable": True, "groupable": False, "aggregatable": False, "sortable": True, "fieldLabel": "测试标签"},
        ],
        ids=["全属性设置", "部分属性设置", "带标签设置"]
    )
    def test_update_field_properties(self, db_reset, properties):
        """更新字段属性"""
        list_result = MetadataApi.list_data_sources()
        source_id = list_result.data[0].get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        field_id = fields_result.data[0].get('id')

        log.info(f"请求参数: fieldId={field_id}, properties={properties}")
        result = MetadataApi.update_field_properties(field_id, properties)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        
        # 二次查询验证字段属性已正确更新
        verify_result = MetadataApi.get_fields_by_source(source_id)
        log.info(f"二次查询验证: code={verify_result.code}, field_count={len(verify_result.data)}")
        assert verify_result.code == 200
        # 找到更新的字段并验证属性
        updated_field = next((f for f in verify_result.data if f.get('id') == field_id), None)
        assert updated_field is not None, f"未找到字段ID为 {field_id} 的字段"
        # 验证每个更新的属性
        for prop_key, prop_value in properties.items():
            actual_value = updated_field.get(prop_key)
            assert actual_value == prop_value, f"字段属性 {prop_key} 未正确更新，期望: {prop_value}, 实际: {actual_value}"
        log.info(f"字段属性验证通过: {properties}")

    @pytest.mark.parametrize(
        "properties",
        [
            {"filterable": True, "groupable": True},
            {"filterable": False, "sortable": True},
        ],
        ids=["批量更新两个字段", "批量更新单个字段"]
    )
    def test_batch_update_field_properties(self, db_reset, properties):
        """批量更新字段属性"""
        # 获取实际字段ID
        list_result = MetadataApi.list_data_sources()
        source_id = list_result.data[0].get('id')
        fields_result = MetadataApi.get_fields_by_source(source_id)
        
        # 使用实际字段ID
        actual_field_ids = [f['id'] for f in fields_result.data[:2]]

        log.info(f"请求参数: fieldIds={actual_field_ids}, properties={properties}")
        result = MetadataApi.batch_update_field_properties(actual_field_ids, properties)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        
        # 二次查询验证所有字段属性已正确更新
        verify_result = MetadataApi.get_fields_by_source(source_id)
        log.info(f"二次查询验证: code={verify_result.code}, field_count={len(verify_result.data)}")
        assert verify_result.code == 200
        # 验证每个更新的字段
        for field_id in actual_field_ids:
            updated_field = next((f for f in verify_result.data if f.get('id') == field_id), None)
            assert updated_field is not None, f"未找到字段ID为 {field_id} 的字段"
            # 验证每个更新的属性
            for prop_key, prop_value in properties.items():
                actual_value = updated_field.get(prop_key)
                assert actual_value == prop_value, f"字段 {field_id} 的属性 {prop_key} 未正确更新，期望: {prop_value}, 实际: {actual_value}"
        log.info(f"所有字段属性验证通过: field_ids={actual_field_ids}, properties={properties}")
