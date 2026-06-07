#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : settings.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 全局配置

# API基础地址
BASE_URL = "http://localhost:8080/api"

# 数据库配置
DATABASE_CONFIG = {
    "host": "192.168.0.105",
    "port": 3306,
    "database": "metric_platform",
    "username": "root",
    "password": "1qazZAQ!"
}

# 超时配置（秒）
REQUEST_TIMEOUT = 120

# 测试数据标识前缀
TEST_DATA_PREFIX = "test_"
