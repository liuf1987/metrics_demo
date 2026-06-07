#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : api_client.py
# @author       : test
# @date         : 2026/06/07
# @Description  : API 客户端封装，带 allure 报告支持

import requests
import json
from typing import Optional, Dict, Any
from config.settings import BASE_URL, REQUEST_TIMEOUT
from common.models.result import Result
from config import log_config
import allure

log = log_config.get_logger(__name__)


class ApiClient:
    """
    API 客户端封装类，提供带 allure 报告支持的 HTTP 请求方法
    """

    @staticmethod
    def _log_request(method: str, url: str, **kwargs):
        """记录请求信息"""
        log.info(f"请求: {method} {url}")
        if kwargs.get('params'):
            log.info(f"查询参数: {kwargs['params']}")
        if kwargs.get('json'):
            log.info(f"请求体: {json.dumps(kwargs['json'], ensure_ascii=False, indent=2)}")
        if kwargs.get('data'):
            log.info(f"请求数据: {kwargs['data']}")

    @staticmethod
    def _log_response(resp):
        """记录响应信息"""
        log.info(f"响应状态码: {resp.status_code}")
        try:
            resp_json = resp.json()
            log.info(f"响应内容: {json.dumps(resp_json, ensure_ascii=False, indent=2)}")
        except Exception:
            log.info(f"响应内容: {resp.text}")

    @staticmethod
    def _attach_to_allure(method: str, url: str, resp, **kwargs):
        """将请求和响应信息附加到 allure 报告"""
        # 附加请求信息
        request_info = {
            "method": method,
            "url": url,
            "params": kwargs.get('params'),
            "body": kwargs.get('json') or kwargs.get('data')
        }
        allure.attach(
            json.dumps(request_info, ensure_ascii=False, indent=2),
            name="请求信息",
            attachment_type=allure.attachment_type.JSON
        )

        # 附加响应信息
        try:
            resp_json = resp.json()
            allure.attach(
                json.dumps(resp_json, ensure_ascii=False, indent=2),
                name="响应内容",
                attachment_type=allure.attachment_type.JSON
            )
        except Exception:
            allure.attach(
                resp.text,
                name="响应内容",
                attachment_type=allure.attachment_type.TEXT
            )

        # 附加响应状态码
        allure.attach(
            str(resp.status_code),
            name="响应状态码",
            attachment_type=allure.attachment_type.TEXT
        )

    @staticmethod
    @allure.step("GET 请求: {endpoint}")
    def get(endpoint: str, params: Optional[Dict] = None, **kwargs) -> Result:
        """
        发送 GET 请求
        :param endpoint: API 端点（不含 BASE_URL）
        :param params: 查询参数
        :return: Result 对象
        """
        url = f"{BASE_URL}{endpoint}"
        ApiClient._log_request("GET", url, params=params, **kwargs)
        
        resp = requests.get(url, params=params, timeout=REQUEST_TIMEOUT, **kwargs)
        
        ApiClient._log_response(resp)
        ApiClient._attach_to_allure("GET", url, resp, params=params, **kwargs)
        
        return Result.of(resp.json())

    @staticmethod
    @allure.step("POST 请求: {endpoint}")
    def post(endpoint: str, json: Optional[Dict] = None, **kwargs) -> Result:
        """
        发送 POST 请求
        :param endpoint: API 端点（不含 BASE_URL）
        :param json: 请求体 JSON
        :return: Result 对象
        """
        url = f"{BASE_URL}{endpoint}"
        ApiClient._log_request("POST", url, json=json, **kwargs)
        
        resp = requests.post(url, json=json, timeout=REQUEST_TIMEOUT, **kwargs)
        
        ApiClient._log_response(resp)
        ApiClient._attach_to_allure("POST", url, resp, json=json, **kwargs)
        
        return Result.of(resp.json())

    @staticmethod
    @allure.step("PUT 请求: {endpoint}")
    def put(endpoint: str, json: Optional[Dict] = None, **kwargs) -> Result:
        """
        发送 PUT 请求
        :param endpoint: API 端点（不含 BASE_URL）
        :param json: 请求体 JSON
        :return: Result 对象
        """
        url = f"{BASE_URL}{endpoint}"
        ApiClient._log_request("PUT", url, json=json, **kwargs)
        
        resp = requests.put(url, json=json, timeout=REQUEST_TIMEOUT, **kwargs)
        
        ApiClient._log_response(resp)
        ApiClient._attach_to_allure("PUT", url, resp, json=json, **kwargs)
        
        return Result.of(resp.json())

    @staticmethod
    @allure.step("DELETE 请求: {endpoint}")
    def delete(endpoint: str, **kwargs) -> Result:
        """
        发送 DELETE 请求
        :param endpoint: API 端点（不含 BASE_URL）
        :return: Result 对象
        """
        url = f"{BASE_URL}{endpoint}"
        ApiClient._log_request("DELETE", url, **kwargs)
        
        resp = requests.delete(url, timeout=REQUEST_TIMEOUT, **kwargs)
        
        ApiClient._log_response(resp)
        ApiClient._attach_to_allure("DELETE", url, resp, **kwargs)
        
        return Result.of(resp.json())
