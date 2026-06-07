#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : result.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 统一API响应封装

class Result:
    """
    API响应封装类
    """
    
    def __init__(self):
        self.code = 200
        self.message = ""
        self.data = None
    
    @staticmethod
    def of(response_dict: dict):
        """
        从字典转换为Result对象
        :param response_dict: 响应字典
        :return: Result对象
        """
        result = Result()
        result.code = response_dict.get('code', 200)
        result.message = response_dict.get('message', '')
        result.data = response_dict.get('data', None)
        return result
    
    def is_success(self):
        """
        判断是否成功
        :return: 是否成功
        """
        return self.code == 200
    
    def __repr__(self):
        return f"Result(code={self.code}, message='{self.message}', data={self.data})"
