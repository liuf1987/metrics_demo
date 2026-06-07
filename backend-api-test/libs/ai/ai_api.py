#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : ai_api.py
# @author       : test
# @date         : 2026/06/06
# @Description  : AI指标推荐API封装

from common.utils.api_client import ApiClient
import allure


class AiApi:
    """
    AI指标推荐API封装
    """
    
    @staticmethod
    @allure.step("AI查询指标")
    def query_metrics(data: dict):
        """
        查询现有指标
        :param data: 查询参数
        :return: Result对象
        """
        return ApiClient.post("/ai/query", json=data)
    
    @staticmethod
    @allure.step("AI生成指标")
    def generate_metric(data: dict):
        """
        生成新指标
        :param data: 生成参数
        :return: Result对象
        """
        return ApiClient.post("/ai/generate", json=data)
    
    @staticmethod
    @allure.step("AI指标评价")
    def submit_feedback(data: dict):
        """
        提交指标评价
        :param data: 评价参数
        :return: Result对象
        """
        return ApiClient.post("/ai/feedback", json=data)
    
    @staticmethod
    @allure.step("查询AI历史")
    def get_history(page_num=1, page_size=10):
        """
        查询聊天历史
        :param page_num: 页码
        :param page_size: 每页大小
        :return: Result对象
        """
        params = {
            'pageNum': page_num,
            'pageSize': page_size
        }
        return ApiClient.get("/ai/history", params=params)
    
    @staticmethod
    @allure.step("检查AI配置")
    def check_config():
        """
        检查AI配置
        :return: Result对象
        """
        return ApiClient.get("/ai/config/check")
