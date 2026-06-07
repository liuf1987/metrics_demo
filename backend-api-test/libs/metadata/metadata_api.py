#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : metadata_api.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 元数据管理API封装

from common.utils.api_client import ApiClient
import allure


class MetadataApi:
    """
    元数据管理API封装
    """
    
    @staticmethod
    @allure.step("加载元数据")
    def load_metadata(pattern=None):
        """
        加载元数据
        :param pattern: 表名匹配模式
        :return: Result对象
        """
        data = {"tableNamePattern": pattern} if pattern else {}
        return ApiClient.post("/metadata/load", json=data)
    
    @staticmethod
    @allure.step("查询所有数据源")
    def list_data_sources():
        """
        查询所有数据源
        :return: Result对象
        """
        return ApiClient.get("/metadata/datasources")
    
    @staticmethod
    @allure.step("查询数据源详情: {source_id}")
    def get_data_source_detail(source_id: int):
        """
        查询数据源详情
        :param source_id: 数据源ID
        :return: Result对象
        """
        return ApiClient.get(f"/metadata/datasources/{source_id}")
    
    @staticmethod
    @allure.step("查询数据源字段列表: {source_id}")
    def get_fields_by_source(source_id: int):
        """
        查询数据源字段列表
        :param source_id: 数据源ID
        :return: Result对象
        """
        return ApiClient.get(f"/metadata/datasources/{source_id}/fields")
    
    @staticmethod
    @allure.step("更新数据源状态: source_id={source_id}, enabled={enabled}")
    def update_data_source_status(source_id: int, enabled: bool):
        """
        更新数据源状态
        :param source_id: 数据源ID
        :param enabled: 是否启用
        :return: Result对象
        """
        data = {"enabled": enabled}
        return ApiClient.post(f"/metadata/datasources/{source_id}/status", json=data)
    
    @staticmethod
    @allure.step("更新字段状态: field_id={field_id}, enabled={enabled}")
    def update_field_status(field_id: int, enabled: bool):
        """
        更新字段状态
        :param field_id: 字段ID
        :param enabled: 是否启用
        :return: Result对象
        """
        data = {"enabled": enabled}
        return ApiClient.post(f"/metadata/fields/{field_id}/status", json=data)
    
    @staticmethod
    @allure.step("更新字段属性: field_id={field_id}")
    def update_field_properties(field_id: int, properties: dict):
        """
        更新字段属性
        :param field_id: 字段ID
        :param properties: 属性字典
        :return: Result对象
        """
        return ApiClient.put(f"/metadata/fields/{field_id}/properties", json=properties)
    
    @staticmethod
    @allure.step("更新字段单个属性: field_id={field_id}, attribute={attribute_name}")
    def update_field_attribute(field_id: int, attribute_name: str, value):
        """
        更新字段单个属性
        :param field_id: 字段ID
        :param attribute_name: 属性名称（filterable, groupable, aggregatable, sortable）
        :param value: 属性值
        :return: Result对象
        """
        properties = {attribute_name: value}
        return MetadataApi.update_field_properties(field_id, properties)
    
    @staticmethod
    @allure.step("批量更新字段属性")
    def batch_update_field_properties(field_ids: list, properties: dict):
        """
        批量更新字段属性
        :param field_ids: 字段ID列表
        :param properties: 属性字典
        :return: Result对象
        """
        data = {"fieldIds": field_ids}
        data.update(properties)
        return ApiClient.put("/metadata/fields/batch/properties", json=data)
