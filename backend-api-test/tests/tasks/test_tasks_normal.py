#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_tasks_normal.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 任务管理正常流程测试

import pytest
import time
from libs.tasks.task_api import TaskApi
from libs.metrics.metric_api import MetricApi
from config import log_config

log = log_config.get_logger(__file__)


def wait_for_task_execution(task_code, expected_count=1, max_wait_seconds=5, poll_interval=0.5):
    """
    等待任务执行完成，直到满足期望的执行历史数量
    :param task_code: 任务编码
    :param expected_count: 期望的执行历史数量
    :param max_wait_seconds: 最大等待时间（秒）
    :param poll_interval: 轮询间隔（秒）
    :return: 最终查询到的执行历史
    """
    start_time = time.time()
    last_executions = []
    
    while time.time() - start_time < max_wait_seconds:
        result = TaskApi.get_task_executions(task_code)
        if result.code == 200 and result.data:
            last_executions = result.data
            if len(last_executions) >= expected_count:
                log.info(f"任务执行历史已满足预期: taskCode={task_code}, expectedCount={expected_count}, actualCount={len(last_executions)}")
                return last_executions
        
        log.debug(f"等待任务执行中... taskCode={task_code}, elapsed={time.time() - start_time:.1f}s")
        time.sleep(poll_interval)
    
    log.warning(f"等待任务执行超时: taskCode={task_code}, maxWaitSeconds={max_wait_seconds}")
    return last_executions


class TestTasksNormal:
    """任务管理正常流程测试"""
    
    def test_list_tasks_200(self, db_reset):
        """分页查询任务列表成功"""
        # 执行
        result = TaskApi.list_tasks(page_num=1, page_size=10)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert 'records' in result.data

    def test_create_instant_task_200(self, db_reset):
        """创建即时执行任务成功"""
        # 准备数据
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "测试即时查询任务",
            "taskDesc": "测试即时执行的查询任务",
            "metricConfigs": [
                {
                    "metricCode": "asset_count_by_status",
                    "metricName": "按状态统计素材数量"
                }
            ]
        }
        
        # 执行
        result = TaskApi.create_task(task_data)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert 'taskCode' in result.data
        assert result.data['taskType'] == 'query'
        assert result.data['executeMode'] == 'instant'
        assert result.data['taskName'] == '测试即时查询任务'

    def test_create_scheduled_task_200(self, db_reset):
        """创建定时执行任务成功"""
        # 准备数据
        task_data = {
            "taskType": "export",
            "executeMode": "scheduled",
            "cronExpression": "0 0 2 * * ?",
            "taskName": "测试定时导出任务",
            "taskDesc": "测试定时执行的导出任务（每天凌晨2点）",
            "metricConfigs": [
                {
                    "metricCode": "platform_performance",
                    "metricName": "平台效果概览"
                }
            ]
        }
        
        # 执行
        result = TaskApi.create_task(task_data)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert 'taskCode' in result.data
        assert result.data['taskType'] == 'export'
        assert result.data['executeMode'] == 'scheduled'
        assert result.data['cronExpression'] == '0 0 2 * * ?'

    def test_create_task_with_multiple_metrics_200(self, db_reset):
        """创建包含多个指标的任务成功"""
        # 准备数据
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "多指标查询任务",
            "metricConfigs": [
                {
                    "metricCode": "asset_count_by_status",
                    "metricName": "按状态统计"
                },
                {
                    "metricCode": "platform_performance",
                    "metricName": "平台效果"
                }
            ]
        }
        
        # 执行
        result = TaskApi.create_task(task_data)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert len(result.data.get('metricConfigs', [])) == 2

    def test_create_task_with_params_200(self, db_reset):
        """创建带有动态参数的任务成功"""
        # 准备数据
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "带参数的查询任务",
            "metricConfigs": [
                {
                    "metricCode": "avg_file_size_by_uploader",
                    "metricName": "上传人平均文件大小",
                    "params": {
                        "status": "approved"
                    },
                    "limitValue": 5
                }
            ]
        }
        
        # 执行
        result = TaskApi.create_task(task_data)
        
        # 断言
        assert result.code == 200
        assert result.data is not None

    def test_get_task_by_code_200(self, db_reset):
        """根据编码查询任务成功"""
        # 先创建任务
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "查询任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 执行查询
        result = TaskApi.get_task_by_code(task_code)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert result.data['taskCode'] == task_code

    def test_delete_task_200(self, db_reset):
        """删除任务成功"""
        # 先创建任务
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "待删除任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 执行删除
        result = TaskApi.delete_task(task_code)
        
        # 断言
        assert result.code == 200
        
        # 验证任务已删除
        get_result = TaskApi.get_task_by_code(task_code)
        assert get_result.code == 5103  # 任务不存在

    def test_pause_resume_task_200(self, db_reset):
        """暂停和继续定时任务成功"""
        # 先创建定时任务
        task_data = {
            "taskType": "query",
            "executeMode": "scheduled",
            "cronExpression": "0 0 * * * ?",
            "taskName": "可暂停任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 暂停任务
        pause_result = TaskApi.pause_task(task_code)
        assert pause_result.code == 200
        assert pause_result.data['success'] is True
        
        # 检查状态详情
        status_result = TaskApi.get_task_status_detail(task_code)
        assert status_result.code == 200
        assert status_result.data['taskStatus'] == 'paused'
        assert status_result.data['isPaused'] is True
        
        # 继续任务
        resume_result = TaskApi.resume_task(task_code)
        assert resume_result.code == 200
        assert resume_result.data['success'] is True
        
        # 检查状态详情
        status_result = TaskApi.get_task_status_detail(task_code)
        assert status_result.code == 200
        assert status_result.data['taskStatus'] == 'pending'
        assert status_result.data['isPaused'] is False

    def test_cancel_task_200(self, db_reset):
        """取消定时任务成功"""
        # 先创建定时任务
        task_data = {
            "taskType": "query",
            "executeMode": "scheduled",
            "cronExpression": "0 0 * * * ?",
            "taskName": "可取消任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 取消任务
        cancel_result = TaskApi.cancel_task(task_code)
        assert cancel_result.code == 200
        assert cancel_result.data['success'] is True
        
        # 检查状态详情
        status_result = TaskApi.get_task_status_detail(task_code)
        assert status_result.code == 200
        assert status_result.data['taskStatus'] == 'cancelled'

    def test_trigger_task_200(self, db_reset):
        """手动触发任务执行成功"""
        # 先创建任务
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "可触发任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 触发任务
        trigger_result = TaskApi.trigger_task(task_code)
        assert trigger_result.code == 200
        assert trigger_result.data['taskCode'] == task_code

    def test_get_task_status_detail_200(self, db_reset):
        """查询任务状态详情成功"""
        # 先创建任务
        task_data = {
            "taskType": "query",
            "executeMode": "scheduled",
            "cronExpression": "0 0 * * * ?",
            "taskName": "状态详情任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 查询状态详情
        result = TaskApi.get_task_status_detail(task_code)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert 'taskCode' in result.data
        assert 'taskStatus' in result.data
        assert 'isScheduled' in result.data
        assert 'isPaused' in result.data
    
    def test_get_task_metrics_200(self, db_reset):
        """查询任务子指标执行状态成功"""
        # 创建包含多个指标的任务
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "多指标查询任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status", "metricName": "按状态统计"},
                {"metricCode": "platform_performance", "metricName": "平台效果"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 查询任务子指标执行状态
        result = TaskApi.get_task_metrics(task_code)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert isinstance(result.data, list)
        assert len(result.data) == 2
        for metric in result.data:
            assert 'metricCode' in metric
            assert 'status' in metric
            assert metric['status'] == 'pending'
    
    def test_get_task_executions_200(self, db_reset):
        """查询任务执行历史成功"""
        # 创建任务并触发执行
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "有执行历史的任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 触发任务执行
        TaskApi.trigger_task(task_code)
        
        # 等待任务执行完成
        executions = wait_for_task_execution(task_code, expected_count=1, max_wait_seconds=5)
        
        # 断言
        assert len(executions) >= 1, f"期望至少1条执行历史，但实际只有{len(executions)}条"
        # 检查我们的任务是否在执行历史中
        task_codes = [e['taskCode'] for e in executions]
        assert task_code in task_codes, f"期望任务 {task_code} 在执行历史中，但没有找到"
    
    def test_get_task_executions_page_200(self, db_reset):
        """分页查询任务执行历史成功"""
        # 创建任务并多次触发执行
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "多次执行的任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 多次触发任务执行（2次）
        TaskApi.trigger_task(task_code)
        TaskApi.trigger_task(task_code)
        
        # 等待任务执行完成（期望至少1条执行历史，因为第二次可能还没执行完）
        wait_for_task_execution(task_code, expected_count=1, max_wait_seconds=8)
        
        # 分页查询任务执行历史
        result = TaskApi.get_task_executions_page(task_code, page_num=1, page_size=10)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert 'records' in result.data
        # 确保至少有1条执行历史（因为任务执行是异步的，可能第二次还没执行完）
        assert len(result.data['records']) >= 1, f"期望至少1条执行历史，但实际只有{len(result.data['records'])}条"
    
    def test_get_task_execution_latest_200(self, db_reset):
        """获取任务最新执行结果成功"""
        # 创建任务并多次触发执行
        task_data = {
            "taskType": "query",
            "taskName": "最新执行任务",
            "executeMode": "instant",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 多次触发任务执行
        TaskApi.trigger_task(task_code)
        TaskApi.trigger_task(task_code)
        
        # 等待任务执行完成（期望至少1条执行历史，因为第二次可能还没执行完）
        wait_for_task_execution(task_code, expected_count=1, max_wait_seconds=8)
        
        # 获取任务最新执行结果
        result = TaskApi.get_task_execution_latest(task_code)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert 'taskCode' in result.data
        assert result.data['taskCode'] == task_code
    
    def test_list_all_executions_200(self, db_reset):
        """查询所有任务执行历史成功"""
        # 创建多个任务并触发执行
        task_data1 = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "任务1",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result1 = TaskApi.create_task(task_data1)
        task_code1 = create_result1.data['taskCode']
        
        task_data2 = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "任务2",
            "metricConfigs": [
                {"metricCode": "platform_performance"}
            ]
        }
        create_result2 = TaskApi.create_task(task_data2)
        task_code2 = create_result2.data['taskCode']
        
        # 触发任务执行
        TaskApi.trigger_task(task_code1)
        TaskApi.trigger_task(task_code2)
        
        # 等待两个任务都执行完成
        wait_for_task_execution(task_code1, expected_count=1, max_wait_seconds=10)
        wait_for_task_execution(task_code2, expected_count=1, max_wait_seconds=10)
        
        # 查询所有任务执行历史
        result = TaskApi.list_all_executions(page_num=1, page_size=10)
        
        # 断言
        assert result.code == 200
        assert result.data is not None
        assert 'records' in result.data
        assert len(result.data['records']) >= 2
        
        # 测试按 task_code 筛选
        filter_result1 = TaskApi.list_all_executions(task_code=task_code1)
        assert filter_result1.code == 200
        assert filter_result1.data is not None
        for record in filter_result1.data['records']:
            assert record['taskCode'] == task_code1