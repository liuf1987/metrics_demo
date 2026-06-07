#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_collections_normal.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 指标集合正常流程测试

import pytest
from libs.collections.collection_api import CollectionApi
from config import log_config

log = log_config.get_logger(__file__)


class TestCollectionsNormal:
    """指标集合正常流程测试"""
    
    def test_list_collections_200(self, db_reset):
        """查询所有指标集合成功"""
        # 执行
        result = CollectionApi.list_collections()
        
        # 断言
        assert result.code == 200
        assert isinstance(result.data, list)
    
    def test_get_collection_detail_200(self, db_reset):
        """查询集合详情成功"""
        # 先获取集合列表
        list_result = CollectionApi.list_collections()
        assert list_result.code == 200
        assert len(list_result.data) > 0
        
        first_collection = list_result.data[0]
        collection_code = first_collection.get('collectionCode')
        
        # 执行
        result = CollectionApi.get_collection_detail(collection_code)
        
        # 断言
        assert result.code == 200
        assert 'metrics' in result.data
    
    def test_list_collections_by_role_200(self, db_reset):
        """按角色查询集合成功"""
        # 执行
        result = CollectionApi.list_collections_by_role("admin")
        
        # 断言
        assert result.code == 200
        assert isinstance(result.data, list)