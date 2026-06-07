#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : task_api.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 任务管理API封装

from common.utils.api_client import ApiClient
import allure


class TaskApi:
    """
    任务管理API封装
    """
    
    @staticmethod
    @allure.step("创建任务")
    def create_task(data: dict):
        """
        创建任务
        :param data: 任务数据
        :return: Result对象
        """
        return ApiClient.post("/tasks", json=data)
    
    @staticmethod
    @allure.step("查询任务详情: {code}")
    def get_task_by_code(code: str):
        """
        根据编码查询任务
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.get(f"/tasks/{code}")
    
    @staticmethod
    @allure.step("查询任务列表")
    def list_tasks(page_num=1, page_size=10, metric_code=None, status=None):
        """
        分页查询任务列表
        :param page_num: 页码
        :param page_size: 每页大小
        :param metric_code: 指标编码
        :param status: 任务状态
        :return: Result对象
        """
        params = {
            'pageNum': page_num,
            'pageSize': page_size
        }
        if metric_code:
            params['metricCode'] = metric_code
        if status:
            params['status'] = status
        
        return ApiClient.get("/tasks", params=params)
    
    @staticmethod
    @allure.step("删除任务: {code}")
    def delete_task(code: str):
        """
        删除任务
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.delete(f"/tasks/{code}")
    
    @staticmethod
    @allure.step("暂停任务: {code}")
    def pause_task(code: str):
        """
        暂停定时任务
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.post(f"/tasks/{code}/pause")
    
    @staticmethod
    @allure.step("继续任务: {code}")
    def resume_task(code: str):
        """
        继续定时任务
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.post(f"/tasks/{code}/resume")
    
    @staticmethod
    @allure.step("取消任务: {code}")
    def cancel_task(code: str):
        """
        取消定时任务
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.post(f"/tasks/{code}/cancel")
    
    @staticmethod
    @allure.step("触发任务执行: {code}")
    def trigger_task(code: str):
        """
        手动触发任务执行
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.post(f"/tasks/{code}/trigger")
    
    @staticmethod
    @allure.step("查询任务状态详情: {code}")
    def get_task_status_detail(code: str):
        """
        查询任务状态详情
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.get(f"/tasks/{code}/status-detail")
    
    @staticmethod
    @allure.step("查询任务指标状态: {code}")
    def get_task_metrics(code: str):
        """
        查询任务子指标执行状态
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.get(f"/tasks/{code}/metrics")
    
    @staticmethod
    @allure.step("查询任务执行历史: {code}")
    def get_task_executions(code: str):
        """
        查询任务执行历史
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.get(f"/tasks/{code}/executions")
    
    @staticmethod
    @allure.step("查询任务执行历史分页: {code}")
    def get_task_executions_page(code: str, page_num=1, page_size=10):
        """
        分页查询任务执行历史
        :param code: 任务编码
        :param page_num: 页码
        :param page_size: 每页大小
        :return: Result对象
        """
        params = {
            'pageNum': page_num,
            'pageSize': page_size
        }
        return ApiClient.get(f"/tasks/{code}/executions/page", params=params)
    
    @staticmethod
    @allure.step("查询任务最新执行结果: {code}")
    def get_task_execution_latest(code: str):
        """
        获取任务最新执行结果
        :param code: 任务编码
        :return: Result对象
        """
        return ApiClient.get(f"/tasks/{code}/executions/latest")
    
    @staticmethod
    @allure.step("查询所有任务执行历史")
    def list_all_executions(page_num=1, page_size=10, task_code=None, status=None):
        """
        分页查询所有任务执行历史
        :param page_num: 页码
        :param page_size: 每页大小
        :param task_code: 任务编码
        :param status: 执行状态
        :return: Result对象
        """
        params = {
            'pageNum': page_num,
            'pageSize': page_size
        }
        if task_code:
            params['taskCode'] = task_code
        if status:
            params['status'] = status
        
        return ApiClient.get("/tasks/executions/page", params=params)