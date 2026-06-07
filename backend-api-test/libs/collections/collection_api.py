#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : collection_api.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 指标集合API封装

from common.utils.api_client import ApiClient
import allure


class CollectionApi:
    """
    指标集合API封装
    """
    
    @staticmethod
    @allure.step("查询所有指标集合")
    def list_collections():
        """
        查询所有指标集合
        :return: Result对象
        """
        return ApiClient.get("/collections")
    
    @staticmethod
    @allure.step("查询集合详情: {code}")
    def get_collection_detail(code: str):
        """
        查询集合详情
        :param code: 集合编码
        :return: Result对象
        """
        return ApiClient.get(f"/collections/{code}")
    
    @staticmethod
    @allure.step("按角色查询集合: {role}")
    def list_collections_by_role(role: str):
        """
        按角色查询集合
        :param role: 目标角色
        :return: Result对象
        """
        return ApiClient.get(f"/collections/role/{role}")
