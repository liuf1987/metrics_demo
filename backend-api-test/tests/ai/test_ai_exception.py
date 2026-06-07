#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_ai_exception.py
# @author       : test
# @date         : 2026/06/06
# @Description  : AI指标推荐异常流程测试

import pytest
from libs.ai.ai_api import AiApi
from libs.metrics.metric_api import MetricApi
from config import log_config

log = log_config.get_logger(__file__)


@pytest.mark.timeout(60)  # AI测试超时设置为60秒
class TestAiException:
    """AI指标推荐异常流程测试"""

    def test_query_metrics_empty_query(self, db_reset):
        """查询现有指标 - query为空"""
        query_data = {
            'query': ''
        }
        log.info(f"测试场景: 查询现有指标 - 空query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code == 1001

    def test_query_metrics_missing_query(self, db_reset):
        """查询现有指标 - 缺少query参数"""
        query_data = {}
        log.info(f"测试场景: 查询现有指标 - 缺少query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code == 1001

    def test_generate_metric_empty_query(self, db_reset):
        """生成新指标 - query为空"""
        generate_data = {
            'query': ''
        }
        log.info(f"测试场景: 生成新指标 - 空query: {generate_data}")
        result = AiApi.generate_metric(generate_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code == 1001

    def test_generate_metric_missing_query(self, db_reset):
        """生成新指标 - 缺少query参数"""
        generate_data = {}
        log.info(f"测试场景: 生成新指标 - 缺少query: {generate_data}")
        result = AiApi.generate_metric(generate_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code == 1001

    def test_submit_feedback_missing_chat_history_id(self, db_reset):
        """提交评价 - 缺少聊天历史ID"""
        feedback_data = {
            'satisfaction': 'SATISFIED'
        }
        log.info(f"测试场景: 提交评价 - 缺少chatHistoryId: {feedback_data}")
        result = AiApi.submit_feedback(feedback_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code == 1001

    def test_submit_feedback_invalid_chat_history_id(self, db_reset):
        """提交评价 - 无效的聊天历史ID"""
        feedback_data = {
            'chatHistoryId': 999999,
            'satisfaction': 'SATISFIED'
        }
        log.info(f"测试场景: 提交评价 - 无效chatHistoryId: {feedback_data}")
        result = AiApi.submit_feedback(feedback_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code >= 3000

    def test_submit_feedback_missing_satisfaction(self, db_reset):
        """提交评价 - 缺少满意度"""
        # 先创建一个聊天历史记录
        query_data = {'query': '测试数据'}
        AiApi.query_metrics(query_data)
        
        # 获取最新的历史记录
        history_result = AiApi.get_history(page_num=1, page_size=1)
        if history_result.code == 200 and history_result.data.get('records'):
            chat_history_id = history_result.data['records'][0]['id']
            
            feedback_data = {
                'chatHistoryId': chat_history_id
            }
            log.info(f"测试场景: 提交评价 - 缺少satisfaction: {feedback_data}")
            result = AiApi.submit_feedback(feedback_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 400 or result.code == 1001

    def test_submit_feedback_invalid_satisfaction(self, db_reset):
        """提交评价 - 无效的满意度值"""
        # 先创建一个聊天历史记录
        query_data = {'query': '测试数据'}
        AiApi.query_metrics(query_data)
        
        # 获取最新的历史记录
        history_result = AiApi.get_history(page_num=1, page_size=1)
        if history_result.code == 200 and history_result.data.get('records'):
            chat_history_id = history_result.data['records'][0]['id']
            
            feedback_data = {
                'chatHistoryId': chat_history_id,
                'satisfaction': 'INVALID'  # 无效值
            }
            log.info(f"测试场景: 提交评价 - 无效satisfaction: {feedback_data}")
            result = AiApi.submit_feedback(feedback_data)
            log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
            assert result.code == 400 or result.code == 1001

    def test_get_history_invalid_page_num(self, db_reset):
        """查询历史 - 无效的页码"""
        log.info("测试场景: 查询聊天历史 - 无效pageNum")
        result = AiApi.get_history(page_num=0, page_size=10)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code == 1001

    def test_get_history_invalid_page_size(self, db_reset):
        """查询历史 - 无效的每页大小"""
        log.info("测试场景: 查询聊天历史 - 无效pageSize")
        result = AiApi.get_history(page_num=1, page_size=0)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code == 1001

    # ============ 任务一：查询指标不存在的场景 ============

    def test_query_nonexistent_metric_code(self, db_reset):
        """使用不存在的指标编码进行查询"""
        query_data = {
            'query': '查询不存在的指标编码：nonexistent_metric_123456'
        }
        log.info(f"测试场景: 使用不存在的指标编码 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code >= 3000 or result.code == 200

    def test_query_nonexistent_field_name(self, db_reset):
        """使用不存在的字段名作为查询条件"""
        query_data = {
            'query': '使用material_unknown_field字段做统计'
        }
        log.info(f"测试场景: 使用不存在的字段名 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code >= 3000 or result.code == 200

    def test_query_empty_result(self, db_reset):
        """查询条件导致结果为空的场景"""
        query_data = {
            'query': '查询2050年1月1日之后上传的、状态为不存的、文件大小为0的素材数量'
        }
        log.info(f"测试场景: 查询条件导致结果为空 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code >= 3000 or result.code == 200

    # ============ 任务二：规则引擎无法支持的查询诉求 ============

    def test_query_complex_logic(self, db_reset):
        """使用过于复杂的查询逻辑"""
        query_data = {
            'query': '跨多表的复杂关联查询，包括素材表、用户表、订单表、支付表的多维交叉分析'
        }
        log.info(f"测试场景: 使用过于复杂的查询逻辑 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code >= 3000 or result.code == 200

    def test_query_unsupported_aggregation(self, db_reset):
        """使用规则引擎不支持的聚合函数"""
        query_data = {
            'query': '查询素材表的file_size_bytes字段的协方差、方差、偏度、峰度等统计指标'
        }
        log.info(f"测试场景: 使用不支持的聚合函数 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code >= 3000 or result.code == 200

    def test_query_nonexistent_datasource(self, db_reset):
        """使用不存在的数据源进行查询"""
        query_data = {
            'query': '查询用户订单表的统计数据，包括总订单数、平均订单金额、订单转化率'
        }
        log.info(f"测试场景: 使用不存在的数据源 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 400 or result.code >= 3000 or result.code == 200

    # ============ 任务三：不支持的指标生成测试用例 ============

    def test_generate_metric_cross_table_analysis(self, db_reset):
        """生成指标 - 跨表多维度关联分析"""
        generate_data = {
            'query': '查询素材表和用户表的关联数据，统计每个用户上传的素材总数和总播放量'
        }
        log.info(f"测试场景: 生成指标 - 跨表多维度关联分析 - query: {generate_data}")
        result = AiApi.generate_metric(generate_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200 or result.code >= 3000 or result.code == 500
        if result.code == 200 and result.data.get('suggestion') is None:
            assert result.data.get('isCannotProceed') is True


    def test_generate_metric_custom_sql(self, db_reset):
        """生成指标 - 编写自定义SQL统计指标"""
        generate_data = {
            'query': '请用自定义SQL语句统计素材表中每天的平均播放量，需要使用复杂的子查询和JOIN'
        }
        log.info(f"测试场景: 生成指标 - 编写自定义SQL统计指标 - query: {generate_data}")
        result = AiApi.generate_metric(generate_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200 or result.code >= 3000 or result.code == 400
        if result.code == 200 and result.data.get('suggestion') is None:
            assert result.data.get('isCannotProceed')  is True

    def test_generate_metric_complex_window_function(self, db_reset):
        """生成指标 - 使用复杂窗口函数（如PERCENT_RANK等规则未支持的）"""
        generate_data = {
            'query': '使用PERCENT_RANK和CUME_DIST窗口函数，统计素材表中每个上传者在平台中的播放量百分位排名，需要复杂的窗口框架定义'
        }
        log.info(f"测试场景: 生成指标 - 使用复杂窗口函数 - query: {generate_data}")
        result = AiApi.generate_metric(generate_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200 or result.code >= 3000 or result.code == 400
        if result.code == 200 and result.data.get('suggestion') is None:
            assert result.data.get('isCannotProceed') is True

    def test_generate_metric_unsupported_aggregation(self, db_reset):
        """生成指标 - 使用不支持的聚合函数（如STDDEV、VARIANCE等）"""
        generate_data = {
            'query': '查询素材表中file_size_bytes字段的标准差（STDDEV）和方差（VARIANCE），按平台分组统计'
        }
        log.info(f"测试场景: 生成指标 - 使用不支持的聚合函数 - query: {generate_data}")
        result = AiApi.generate_metric(generate_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200 or result.code >= 3000 or result.code == 400
        if result.code == 200 and result.data.get('suggestion') is None:
            assert result.data.get('isCannotProceed') is True
