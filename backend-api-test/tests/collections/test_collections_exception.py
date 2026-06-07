#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_collections_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 指标集合异常流程测试

import pytest
from libs.collections.collection_api import CollectionApi
from config import log_config

log = log_config.get_logger(__file__)


class TestCollectionsException:
    """指标集合异常流程测试"""
    
    def test_get_collection_detail_6102(self, db_reset):
        """查询不存在的集合-指标集合不存在"""
        # 执行
        result = CollectionApi.get_collection_detail("non_existent_collection_code")
        
        # 断言
        assert result.code == 6102