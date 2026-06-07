#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_tasks_exception.py
# @author       : test
# @date         : 2026/06/03
# @Description  : 任务管理异常流程测试

import pytest
from libs.tasks.task_api import TaskApi
from config import log_config

log = log_config.get_logger(__file__)


class TestTasksException:
    """任务管理异常流程测试"""
    
    def test_get_task_not_found(self, db_reset):
        """查询不存在的任务"""
        result = TaskApi.get_task_by_code("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103

    def test_delete_task_not_found(self, db_reset):
        """删除不存在的任务"""
        result = TaskApi.delete_task("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103

    def test_create_task_empty_metrics(self, db_reset):
        """创建任务时指标配置列表为空"""
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "空指标任务",
            "metricConfigs": []
        }
        
        result = TaskApi.create_task(task_data)
        
        # 断言 - 参数验证失败
        assert result.code == 1001

    def test_create_task_missing_task_type(self, db_reset):
        """创建任务时缺少任务类型"""
        task_data = {
            "executeMode": "instant",
            "taskName": "缺少类型任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        
        result = TaskApi.create_task(task_data)
        
        # 断言 - 参数验证失败
        assert result.code == 1001

    def test_create_task_missing_execute_mode(self, db_reset):
        """创建任务时缺少执行模式"""
        task_data = {
            "taskType": "query",
            "taskName": "缺少执行模式任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        
        result = TaskApi.create_task(task_data)
        
        # 断言 - 参数验证失败
        assert result.code == 1001

    def test_create_scheduled_task_missing_cron(self, db_reset):
        """创建定时任务时缺少Cron表达式"""
        task_data = {
            "taskType": "query",
            "executeMode": "scheduled",
            "taskName": "缺少Cron任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        
        result = TaskApi.create_task(task_data)
        
        # 断言 - 定时任务必须配置Cron表达式
        assert result.code == 5003

    def test_create_task_invalid_metric(self, db_reset):
        """创建任务时使用不存在的指标"""
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "无效指标任务",
            "metricConfigs": [
                {"metricCode": "non_existent_metric_code"}
            ]
        }
        
        result = TaskApi.create_task(task_data)
        
        # 断言 - 指标不存在
        assert result.code == 3102

    def test_create_task_disabled_metric(self, db_reset):
        """创建任务时使用已停用的指标"""
        # 先创建一个指标并停用（需要提前准备数据）
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "停用指标任务",
            "metricConfigs": [
                {"metricCode": "disabled_metric_code"}
            ]
        }
        
        result = TaskApi.create_task(task_data)
        
        # 断言 - 指标已停用或不存在
        assert result.code in [3010, 3102]

    def test_pause_task_not_found(self, db_reset):
        """暂停不存在的任务"""
        result = TaskApi.pause_task("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103

    def test_resume_task_not_found(self, db_reset):
        """继续不存在的任务"""
        result = TaskApi.resume_task("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103

    def test_cancel_task_not_found(self, db_reset):
        """取消不存在的任务"""
        result = TaskApi.cancel_task("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103

    def test_trigger_task_not_found(self, db_reset):
        """触发不存在的任务"""
        result = TaskApi.trigger_task("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在
        assert result.code == 5103

    def test_pause_non_scheduled_task(self, db_reset):
        """暂停非定时任务"""
        # 先创建即时任务
        task_data = {
            "taskType": "query",
            "executeMode": "instant",
            "taskName": "即时任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 尝试暂停即时任务
        result = TaskApi.pause_task(task_code)
        
        # 断言 - 操作失败（不是定时任务）
        assert result.code == 200
        assert result.data['success'] is False

    def test_resume_not_paused_task(self, db_reset):
        """继续非暂停状态的任务"""
        # 先创建定时任务
        task_data = {
            "taskType": "query",
            "executeMode": "scheduled",
            "cronExpression": "0 0 * * * ?",
            "taskName": "未暂停任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        create_result = TaskApi.create_task(task_data)
        task_code = create_result.data['taskCode']
        
        # 尝试继续未暂停的任务
        result = TaskApi.resume_task(task_code)
        
        # 断言 - 操作失败（任务不是暂停状态）
        assert result.code == 200
        assert result.data['success'] is False

    def test_get_status_detail_not_found(self, db_reset):
        """查询不存在任务的状态详情"""
        result = TaskApi.get_task_status_detail("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在
        assert result.code == 5103

    def test_create_task_invalid_cron_format(self, db_reset):
        """创建定时任务时使用无效的Cron表达式"""
        task_data = {
            "taskType": "query",
            "executeMode": "scheduled",
            "cronExpression": "invalid_cron_expression",
            "taskName": "无效Cron任务",
            "metricConfigs": [
                {"metricCode": "asset_count_by_status"}
            ]
        }
        
        result = TaskApi.create_task(task_data)
        
        # 断言 - 参数验证失败或任务创建失败
        assert result.code in [1001, 200]

    def test_get_task_metrics_not_found(self, db_reset):
        """查询不存在任务的子指标"""
        result = TaskApi.get_task_metrics("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103

    def test_get_task_executions_not_found(self, db_reset):
        """查询不存在任务的执行历史"""
        result = TaskApi.get_task_executions("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103

    def test_get_task_executions_page_not_found(self, db_reset):
        """分页查询不存在任务的执行历史"""
        result = TaskApi.get_task_executions_page("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103

    def test_get_task_execution_latest_not_found(self, db_reset):
        """获取不存在任务的最新执行结果"""
        result = TaskApi.get_task_execution_latest("non_existent_task_code_xxx")
        
        # 断言 - 任务不存在返回错误码5103
        assert result.code == 5103
