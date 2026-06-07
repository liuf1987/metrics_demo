#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : metric_api.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 指标管理API封装

from common.utils.api_client import ApiClient
import allure


class MetricApi:
    """
    指标管理API封装
    """
    
    @staticmethod
    @allure.step("创建指标")
    def create_metric(data: dict):
        """
        创建指标
        :param data: 指标数据
        :return: Result对象
        """
        return ApiClient.post("/metrics", json=data)
    
    @staticmethod
    @allure.step("更新指标: {metric_id}")
    def update_metric(metric_id: int, data: dict):
        """
        更新指标
        :param metric_id: 指标ID
        :param data: 更新数据
        :return: Result对象
        """
        return ApiClient.put(f"/metrics/{metric_id}", json=data)
    
    @staticmethod
    @allure.step("删除指标: {metric_id}")
    def delete_metric(metric_id: int):
        """
        删除指标
        :param metric_id: 指标ID
        :return: Result对象
        """
        return ApiClient.delete(f"/metrics/{metric_id}")
    
    @staticmethod
    @allure.step("查询指标详情: {code}")
    def get_metric_by_code(code: str):
        """
        根据编码查询指标详情
        :param code: 指标编码
        :return: Result对象
        """
        return ApiClient.get(f"/metrics/{code}")
    
    @staticmethod
    @allure.step("查询指标列表")
    def list_metrics(page_num=1, page_size=10, category_code=None, enabled=None, keyword=None):
        """
        分页查询指标列表
        :param page_num: 页码
        :param page_size: 每页大小
        :param category_code: 分类编码
        :param enabled: 启用状态
        :param keyword: 关键词
        :return: Result对象
        """
        params = {
            'pageNum': page_num,
            'pageSize': page_size
        }
        if category_code:
            params['categoryCode'] = category_code
        if enabled is not None:
            params['enabled'] = enabled
        if keyword:
            params['keyword'] = keyword
        
        return ApiClient.get("/metrics", params=params)
    
    @staticmethod
    @allure.step("切换指标状态: metric_id={metric_id}, enabled={enabled}")
    def toggle_metric(metric_id: int, enabled: bool):
        """
        启用/停用指标
        :param metric_id: 指标ID
        :param enabled: 是否启用
        :return: Result对象
        """
        params = {'enabled': enabled}
        return ApiClient.post(f"/metrics/{metric_id}/toggle", params=params)
    
    @staticmethod
    @allure.step("执行指标查询: {code}")
    def execute_metric(code: str, params=None):
        """
        执行指标查询
        :param code: 指标编码
        :param params: 查询参数
        :return: Result对象
        """
        request_data = params or {}
        return ApiClient.post(f"/metrics/{code}/execute", json=request_data)
