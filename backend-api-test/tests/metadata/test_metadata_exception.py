#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_metadata_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 元数据管理异常流程测试

import pytest
from libs.metadata.metadata_api import MetadataApi
from config import log_config

log = log_config.get_logger(__file__)


class TestMetadataException:
    """元数据管理异常流程测试"""

    @pytest.mark.parametrize(
        "source_id,expected_code,test_name",
        [
            (999999, 3003, "不存在的数据源ID"),
            (0, 3003, "数据源ID为0"),
        ],
        ids=["不存在的数据源ID", "数据源ID为0"]
    )
    def test_get_data_source_detail_not_found(self, db_reset, source_id, expected_code, test_name):
        """查询不存在的数据源"""
        log.info(f"测试场景: {test_name}, 请求参数: sourceId={source_id}")
        result = MetadataApi.get_data_source_detail(source_id)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code

    @pytest.mark.parametrize(
        "source_id,expected_code,test_name",
        [
            (999999, 3003, "不存在的数据源ID"),
            (0, 3003, "数据源ID为0"),
        ],
        ids=["不存在的数据源ID", "数据源ID为0"]
    )
    def test_get_fields_by_source_not_found(self, db_reset, source_id, expected_code, test_name):
        """查询不存在的数据源字段"""
        log.info(f"测试场景: {test_name}, 请求参数: sourceId={source_id}")
        result = MetadataApi.get_fields_by_source(source_id)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code

    @pytest.mark.parametrize(
        "source_id,enabled,expected_code,test_name",
        [
            (999999, True, 3003, "不存在的数据源-启用"),
            (0, False, 3003, "数据源ID为0-禁用"),
        ],
        ids=["不存在的数据源-启用", "数据源ID为0-禁用"]
    )
    def test_update_data_source_status_not_found(self, db_reset, source_id, enabled, expected_code, test_name):
        """更新不存在的数据源状态"""
        log.info(f"测试场景: {test_name}, 请求参数: sourceId={source_id}, enabled={enabled}")
        result = MetadataApi.update_data_source_status(source_id, enabled)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code

    @pytest.mark.parametrize(
        "field_id,enabled,expected_code,test_name",
        [
            (999999, True, 3005, "不存在的字段ID-启用"),
            (0, False, 3005, "字段ID为0-禁用"),
        ],
        ids=["不存在的字段ID-启用", "字段ID为0-禁用"]
    )
    def test_update_field_status_not_found(self, db_reset, field_id, enabled, expected_code, test_name):
        """更新不存在的字段状态"""
        log.info(f"测试场景: {test_name}, 请求参数: fieldId={field_id}, enabled={enabled}")
        result = MetadataApi.update_field_status(field_id, enabled)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code

    @pytest.mark.parametrize(
        "field_id,properties,expected_code,test_name",
        [
            (999999, {"filterable": True}, 3005, "不存在的字段ID"),
            (0, {"groupable": False}, 3005, "字段ID为0"),
        ],
        ids=["不存在的字段ID", "字段ID为0"]
    )
    def test_update_field_properties_not_found(self, db_reset, field_id, properties, expected_code, test_name):
        """更新不存在的字段属性"""
        log.info(f"测试场景: {test_name}, 请求参数: fieldId={field_id}, properties={properties}")
        result = MetadataApi.update_field_properties(field_id, properties)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == expected_code

    def test_batch_update_field_properties_empty_ids(self, db_reset):
        """批量更新字段属性-字段ID列表为空"""
        field_ids = []
        properties = {"filterable": True}
        log.info(f"测试场景: 字段ID列表为空, 请求参数: fieldIds={field_ids}, properties={properties}")
        result = MetadataApi.batch_update_field_properties(field_ids, properties)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 1002

    def test_batch_update_field_properties_field_not_found(self, db_reset):
        """批量更新字段属性-字段不存在"""
        field_ids = [999999, 0]
        properties = {"filterable": True}
        log.info(f"测试场景: 字段不存在, 请求参数: fieldIds={field_ids}, properties={properties}")
        result = MetadataApi.batch_update_field_properties(field_ids, properties)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 3005

