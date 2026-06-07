#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : datautil.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 数据工具类

import random
import string
from datetime import datetime, timedelta


class DataUtil:
    """
    数据生成工具类
    """
    
    @staticmethod
    def random_string(length=8):
        """
        生成随机字符串
        :param length: 字符串长度
        :return: 随机字符串
        """
        return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))
    
    @staticmethod
    def random_int(min_val=1, max_val=10000):
        """
        生成随机整数
        :param min_val: 最小值
        :param max_val: 最大值
        :return: 随机整数
        """
        return random.randint(min_val, max_val)
    
    @staticmethod
    def random_code(prefix="test_", length=6):
        """
        生成带前缀的随机编码
        :param prefix: 前缀
        :param length: 随机部分长度
        :return: 编码字符串
        """
        return f"{prefix}{DataUtil.random_string(length)}"
    
    @staticmethod
    def random_date(days_ago=30):
        """
        生成随机日期
        :param days_ago: 多少天前
        :return: 随机日期字符串
        """
        today = datetime.now()
        random_days = random.randint(0, days_ago)
        random_date = today - timedelta(days=random_days)
        return random_date.strftime("%Y-%m-%d")
    
    @staticmethod
    def random_datetime(days_ago=30):
        """
        生成随机日期时间
        :param days_ago: 多少天前
        :return: 随机日期时间字符串
        """
        today = datetime.now()
        random_days = random.randint(0, days_ago)
        random_seconds = random.randint(0, 86400)
        random_datetime = today - timedelta(days=random_days, seconds=random_seconds)
        return random_datetime.strftime("%Y-%m-%d %H:%M:%S")
    
    @staticmethod
    def current_timestamp():
        """
        获取当前时间戳
        :return: 当前时间戳字符串
        """
        return datetime.now().strftime("%Y%m%d%H%M%S")
