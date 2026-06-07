#!/usr/bin/env python3
# -*- coding: utf-8 -*-
# @file         : test_ai_normal.py
# @author       : test
# @date         : 2026/06/06
# @Description  : AI指标推荐正常流程测试

import pytest
from libs.ai.ai_api import AiApi
from config import log_config

log = log_config.get_logger(__file__)

# AI生成指标测试用例数组
GENERATE_METRIC_TEST_CASES = [
    {
        "name": "统计各上传人的平均文件大小",
        "query": "统计各上传人的平均文件大小，按文件大小降序排列"
    },
    {
        "name": "统计各平台的平均视频时长",
        "query": "统计各平台的平均视频时长，按时长降序排列"
    },
    {
        "name": "按上传人统计已审核通过的内容数量",
        "query": "按上传人统计已审核通过的内容数量，取前20名"
    },
    {
        "name": "按平台和状态统计总播放量",
        "query": "按平台和状态统计总播放量，筛选已审核通过的内容"
    },
    {
        "name": "统计上传人的素材数量、总播放量和平均完播率",
        "query": "统计上传人的素材数量、总播放量和平均完播率，按完播率降序排列"
    },
    {
        "name": "统计各平台的高质量内容（完播率高）",
        "query": "统计各平台的完播率大于50%的素材数量，按完播率降序排列"
    },
    {
        "name": "按上传人分组，对每个上传人的素材按播放量排名",
        "query": "按上传人分组，对每个上传人的素材按播放量从高到低排名，显示排名"
    }
]

# AI查询指标测试用例数组
QUERY_METRIC_TEST_CASES = [
    {
        "name": "平台效果概览",
        "query": "平台效果概览"
    },
    {
        "name": "按平台统计素材数量",
        "query": "按平台统计素材数量"
    },
    {
        "name": "抖音平台完播率分析",
        "query": "抖音平台完播率分析"
    },
    {
        "name": "高产出上传人TOP10",
        "query": "高产出上传人TOP10"
    },
    {
        "name": "各平台平均视频时长",
        "query": "各平台平均视频时长"
    },
    {
        "name": "素材审核状态分布",
        "query": "素材审核状态分布"
    }
]


@pytest.mark.timeout(120)  # AI测试超时设置为60秒
class TestAiNormal:
    """AI指标推荐正常流程测试"""

    def test_query_metrics_200(self, db_reset):
        """查询现有指标成功 - 平台效果概览"""
        query_data = {
            'query': '平台效果概览'
        }
        log.info(f"测试场景: 查询现有指标 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        assert result.data is not None

    def test_query_metrics_by_platform(self, db_reset):
        """测试用'按平台统计素材数量'作为查询词"""
        query_data = {
            'query': '按平台统计素材数量'
        }
        log.info(f"测试场景: 按平台统计素材数量 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        assert result.data is not None

    def test_query_douyin_complete_rate(self, db_reset):
        """测试用'抖音平台完播率分析'作为查询词"""
        query_data = {
            'query': '抖音平台完播率分析'
        }
        log.info(f"测试场景: 抖音平台完播率分析 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        assert result.data is not None

    def test_query_top_uploaders(self, db_reset):
        """测试用'高产出上传人TOP10'作为查询词"""
        query_data = {
            'query': '高产出上传人TOP10'
        }
        log.info(f"测试场景: 高产出上传人TOP10 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        assert result.data is not None

    def test_query_avg_duration_by_platform(self, db_reset):
        """测试用'各平台平均视频时长'作为查询词"""
        query_data = {
            'query': '各平台平均视频时长'
        }
        log.info(f"测试场景: 各平台平均视频时长 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        assert result.data is not None

    def test_query_audit_status_distribution(self, db_reset):
        """测试用'素材审核状态分布'作为查询词"""
        query_data = {
            'query': '素材审核状态分布'
        }
        log.info(f"测试场景: 素材审核状态分布 - query: {query_data}")
        result = AiApi.query_metrics(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        assert result.data is not None

    def test_get_history_200(self, db_reset):
        """查询聊天历史成功"""
        log.info("测试场景: 查询聊天历史")
        result = AiApi.get_history(page_num=1, page_size=10)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
        assert result.data is not None
        assert 'records' in result.data
        assert 'total' in result.data

    def test_check_config_200(self, db_reset):
        """检查AI配置成功"""
        log.info("测试场景: 检查AI配置")
        result = AiApi.check_config()
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200

    def test_generate_metric_uploader_avg_file_size(self, db_reset):
        """生成指标 - 统计各上传人的平均文件大小"""
        query_data = {
            'query': '统计各上传人的平均文件大小，按文件大小降序排列'
        }
        log.info(f"测试场景: 生成指标 - 统计各上传人的平均文件大小 - query: {query_data}")
        result = AiApi.generate_metric(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200

    def test_generate_metric_platform_avg_duration(self, db_reset):
        """生成指标 - 统计各平台的平均视频时长"""
        query_data = {
            'query': '统计各平台的平均视频时长，按时长降序排列'
        }
        log.info(f"测试场景: 生成指标 - 统计各平台的平均视频时长 - query: {query_data}")
        result = AiApi.generate_metric(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200

    def test_generate_metric_uploader_approved_count(self, db_reset):
        """生成指标 - 按上传人统计已审核通过的内容数量"""
        query_data = {
            'query': '按上传人统计已审核通过的内容数量，取前20名'
        }
        log.info(f"测试场景: 生成指标 - 按上传人统计已审核通过的内容数量 - query: {query_data}")
        result = AiApi.generate_metric(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200

    def test_generate_metric_platform_status_total_plays(self, db_reset):
        """生成指标 - 按平台和状态统计总播放量"""
        query_data = {
            'query': '按平台和状态统计总播放量，筛选已审核通过的内容'
        }
        log.info(f"测试场景: 生成指标 - 按平台和状态统计总播放量 - query: {query_data}")
        result = AiApi.generate_metric(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200

    def test_generate_metric_uploader_plays_complete_rate(self, db_reset):
        """生成指标 - 统计上传人的素材数量、总播放量和平均完播率"""
        query_data = {
            'query': '统计上传人的素材数量、总播放量和平均完播率，按完播率降序排列'
        }
        log.info(f"测试场景: 生成指标 - 统计上传人的素材数量、总播放量和平均完播率 - query: {query_data}")
        result = AiApi.generate_metric(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200

    def test_generate_metric_platform_high_quality(self, db_reset):
        """生成指标 - 统计各平台的高质量内容（完播率高）"""
        query_data = {
            'query': '统计各平台的完播率大于50%的素材数量，按完播率降序排列'
        }
        log.info(f"测试场景: 生成指标 - 统计各平台的高质量内容 - query: {query_data}")
        result = AiApi.generate_metric(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200

    def test_generate_metric_uploader_play_rank(self, db_reset):
        """生成指标 - 按上传人分组，对每个上传人的素材按播放量排名"""
        query_data = {
            'query': '按上传人分组，对每个上传人的素材按播放量从高到低排名，显示排名'
        }
        log.info(f"测试场景: 生成指标 - 按上传人分组，对每个上传人的素材按播放量排名 - query: {query_data}")
        result = AiApi.generate_metric(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200

    def test_generate_metric_uploader_moving_avg_play(self, db_reset):
        """生成指标 - 计算各上传人连续3个素材的移动平均播放量"""
        query_data = {
            'query': '计算各上传人连续3个素材的移动平均播放量'
        }
        log.info(f"测试场景: 生成指标 - 计算各上传人连续3个素材的移动平均播放量 - query: {query_data}")
        result = AiApi.generate_metric(query_data)
        log.info(f"响应结果: code={result.code}, message={result.message}, data={result.data}")
        assert result.code == 200
